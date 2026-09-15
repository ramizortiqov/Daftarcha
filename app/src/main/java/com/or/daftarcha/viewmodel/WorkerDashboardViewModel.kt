package com.or.daftarcha.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.or.daftarcha.data.auth.AuthManager
import com.or.daftarcha.data.model.AuthUser
import com.or.daftarcha.data.dao.AttendanceDao
import com.or.daftarcha.data.dao.BonusDao
import com.or.daftarcha.data.dao.EmployeeDao
import com.or.daftarcha.data.dao.ExpenseDao
import com.or.daftarcha.data.dao.PaymentDao
import com.or.daftarcha.data.dao.ProjectBonusTotal
import com.or.daftarcha.data.dao.ProjectDao
import com.or.daftarcha.data.dao.ProjectEmployeeDao
import com.or.daftarcha.data.dao.ProjectEmployeeWorkdays
import com.or.daftarcha.data.model.Employee
import com.or.daftarcha.data.model.Payment
import com.or.daftarcha.data.model.Project
import com.or.daftarcha.data.model.ProjectExpenseTotal
import com.or.daftarcha.data.model.ProjectTotalWorkdays
import com.or.daftarcha.data.model.WorkerProfileData
import com.or.daftarcha.data.model.WorkerProjectStat
import com.or.daftarcha.data.sync.FirestoreSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectStatsData(
    val bonuses: Map<Int, Double>,
    val expenses: Map<Int, Double>,
    val totalWorkdays: Map<Int, Int>
)

data class WorkerBaseData(
    val employee: Employee,
    val payments: List<Payment>,
    val totalPaid: Double,
    val totalWorkdays: Int,
    val empDaysMap: Map<Int, Int>,
    val personalExpenses: List<com.or.daftarcha.data.model.Expense>,
    val totalPersonalExpenses: Double
)

@HiltViewModel
class WorkerDashboardViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val firestoreSyncManager: FirestoreSyncManager,
    private val employeeDao: EmployeeDao,
    private val attendanceDao: AttendanceDao,
    private val paymentDao: PaymentDao,
    private val projectDao: ProjectDao,
    private val projectEmployeeDao: ProjectEmployeeDao,
    private val bonusDao: BonusDao,
    private val expenseDao: ExpenseDao
) : ViewModel() {

    val currentUser: StateFlow<AuthUser?> = authManager.currentUser
    val showWorkerEarningsAndDebt: StateFlow<Boolean> = authManager.showWorkerEarningsAndDebt

    private val projectStatsFlow: Flow<ProjectStatsData> = combine(
        bonusDao.getAllBonusesGroupedByProject()
            .map { list -> list.associate { it.projectId to it.total } },
        expenseDao.getAllExpensesGroupedByProject()
            .map { list -> list.associate { it.projectId to it.total } },
        attendanceDao.getAllProjectTotalWorkdays()
            .map { list -> list.associate { it.projectId to it.count } }
    ) { bonuses, expenses, totalDays ->
        ProjectStatsData(bonuses, expenses, totalDays)
    }

    init {
        viewModelScope.launch {
            currentUser.collectLatest { user ->
                val bId = user?.effectiveBrigadierId ?: ""
                authManager.loadAndListenBrigadierSettings(bId)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val bId = currentUser.value?.effectiveBrigadierId ?: ""
            authManager.loadAndListenBrigadierSettings(bId)
            try {
                firestoreSyncManager.syncAll()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    val workerData: StateFlow<WorkerProfileData?> = currentUser.flatMapLatest { user ->
        val empId = if (user?.employeeId != null && user.employeeId > 0) {
            user.employeeId
        } else if (user != null) {
            val found = employeeDao.getAllEmployeesSuspend().firstOrNull { 
                it.name.trim().equals(user.name.trim(), ignoreCase = true) ||
                (!it.phone.isNullOrBlank() && it.phone.trim() == user.phone?.trim())
            }
            found?.id
        } else {
            null
        }

        if (empId == null || empId == 0) {
            flowOf(null)
        } else {
            val employeeFlow = employeeDao.getEmployeeByIdFlow(empId).filterNotNull()
            val paymentsFlow = paymentDao.getPaymentsForEmployee(empId)
            val totalPaidFlow = paymentDao.getTotalPayments(empId)
            val totalWorkdaysFlow = attendanceDao.getTotalWorkdaysForEmployeeAcrossAllProjects(empId)
            val empDaysPerProjFlow = attendanceDao.getEmployeeWorkdaysPerProjectFlow(empId)
                .map { list -> list.associate { it.projectId to it.workdays } }
            val allProjectsFlow = projectDao.getAllProjects()
            val personalExpensesFlow = expenseDao.getExpensesForEmployeeAcrossAllProjects(empId)
            val totalPersonalExpensesFlow = expenseDao.getTotalExpensesForEmployee(empId)

            val workerBaseFlow: Flow<WorkerBaseData> = combine(
                employeeFlow,
                paymentsFlow,
                totalPaidFlow,
                totalWorkdaysFlow,
                empDaysPerProjFlow
            ) { emp, payments, totalPaid, totalWorkdays, empDaysMap ->
                WorkerBaseData(emp, payments, totalPaid, totalWorkdays, empDaysMap, emptyList(), 0.0)
            }.combine(personalExpensesFlow) { base, personalExpenses ->
                base.copy(personalExpenses = personalExpenses)
            }.combine(totalPersonalExpensesFlow) { base, totalPersonalExpenses ->
                base.copy(totalPersonalExpenses = totalPersonalExpenses)
            }

            combine(
                workerBaseFlow,
                allProjectsFlow,
                projectStatsFlow
            ) { base, projectsList, stats ->
                val projectsMap = projectsList.associateBy { it.id }
                val projectIds = (base.empDaysMap.keys + projectsMap.keys).toSet()

                var totalEarned = 0.0
                val personalExpensesByProject = base.personalExpenses
                    .groupBy { it.projectId }
                    .mapValues { (_, list) -> list.sumOf { it.amount } }
                val breakdowns = mutableListOf<WorkerProjectStat>()

                for (projectId in projectIds) {
                    val proj = projectsMap[projectId]
                    val workerDaysInProj = base.empDaysMap[projectId] ?: 0
                    if (workerDaysInProj <= 0) continue

                    val projBonus = stats.bonuses[projectId] ?: 0.0
                    val recordedTotalDays = stats.totalWorkdays[projectId] ?: 0
                    val projTotalDays = maxOf(recordedTotalDays, workerDaysInProj)

                    val projectRevenue = if (projBonus > 0.0) projBonus else (proj?.cost ?: 0.0)
                    // Харажатлар шахсий бўлганлиги сабабли, умумий ставкага таъсир қилмайди.
                    val dailyRate = if (projTotalDays > 0) projectRevenue / projTotalDays else 0.0
                    val earnedHere = workerDaysInProj * dailyRate
                    val personalExpenseHere = personalExpensesByProject[projectId] ?: 0.0

                    totalEarned += earnedHere

                    val projName = proj?.name ?: "Иш #$projectId"
                    breakdowns.add(
                        WorkerProjectStat(
                            projectId = projectId,
                            projectName = projName,
                            workdays = workerDaysInProj,
                            totalProjectExpense = personalExpenseHere,
                            earnedInProject = earnedHere
                        )
                    )
                }

                val balance = totalEarned - base.totalPaid - base.totalPersonalExpenses

                WorkerProfileData(
                    employee = base.employee,
                    totalWorkdays = base.totalWorkdays,
                    totalEarned = totalEarned,
                    totalPaid = base.totalPaid,
                    balance = balance,
                    totalExpensesOnProjects = base.totalPersonalExpenses,
                    personalExpenses = base.personalExpenses,
                    payments = base.payments,
                    projectBreakdowns = breakdowns.sortedByDescending { it.workdays }
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun logout() {
        authManager.logout()
    }
}
