package com.example.daftarcha.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daftarcha.data.auth.AuthManager
import com.example.daftarcha.data.model.AuthUser
import com.example.daftarcha.data.dao.AttendanceDao
import com.example.daftarcha.data.dao.BonusDao
import com.example.daftarcha.data.dao.EmployeeDao
import com.example.daftarcha.data.dao.ExpenseDao
import com.example.daftarcha.data.dao.PaymentDao
import com.example.daftarcha.data.dao.ProjectBonusTotal
import com.example.daftarcha.data.dao.ProjectDao
import com.example.daftarcha.data.dao.ProjectEmployeeDao
import com.example.daftarcha.data.dao.ProjectEmployeeWorkdays
import com.example.daftarcha.data.model.Employee
import com.example.daftarcha.data.model.Payment
import com.example.daftarcha.data.model.Project
import com.example.daftarcha.data.model.ProjectExpenseTotal
import com.example.daftarcha.data.model.ProjectTotalWorkdays
import com.example.daftarcha.data.model.WorkerProfileData
import com.example.daftarcha.data.model.WorkerProjectStat
import com.example.daftarcha.data.sync.FirestoreSyncManager
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
    val empDaysMap: Map<Int, Int>
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

            val workerBaseFlow: Flow<WorkerBaseData> = combine(
                employeeFlow,
                paymentsFlow,
                totalPaidFlow,
                totalWorkdaysFlow,
                empDaysPerProjFlow
            ) { emp, payments, totalPaid, totalWorkdays, empDaysMap ->
                WorkerBaseData(emp, payments, totalPaid, totalWorkdays, empDaysMap)
            }

            combine(
                workerBaseFlow,
                allProjectsFlow,
                projectStatsFlow
            ) { base, projectsList, stats ->
                val projectsMap = projectsList.associateBy { it.id }
                val projectIds = (base.empDaysMap.keys + projectsMap.keys).toSet()

                var totalEarned = 0.0
                var totalExpensesAcrossProjects = 0.0
                val breakdowns = mutableListOf<WorkerProjectStat>()

                for (projectId in projectIds) {
                    val proj = projectsMap[projectId]
                    val workerDaysInProj = base.empDaysMap[projectId] ?: 0
                    if (workerDaysInProj <= 0) continue

                    val projBonus = stats.bonuses[projectId] ?: 0.0
                    val projExpense = stats.expenses[projectId] ?: 0.0
                    val recordedTotalDays = stats.totalWorkdays[projectId] ?: 0
                    val projTotalDays = maxOf(recordedTotalDays, workerDaysInProj)

                    val projectRevenue = if (projBonus > 0.0) projBonus else (proj?.cost ?: 0.0)
                    val netCost = projectRevenue - projExpense
                    val dailyRate = if (projTotalDays > 0) netCost / projTotalDays else 0.0
                    val earnedHere = workerDaysInProj * dailyRate

                    totalEarned += earnedHere
                    totalExpensesAcrossProjects += projExpense

                    val projName = proj?.name ?: "Иш #$projectId"
                    breakdowns.add(
                        WorkerProjectStat(
                            projectId = projectId,
                            projectName = projName,
                            workdays = workerDaysInProj,
                            totalProjectExpense = projExpense,
                            earnedInProject = earnedHere
                        )
                    )
                }

                val balance = totalEarned - base.totalPaid

                WorkerProfileData(
                    employee = base.employee,
                    totalWorkdays = base.totalWorkdays,
                    totalEarned = totalEarned,
                    totalPaid = base.totalPaid,
                    balance = balance,
                    totalExpensesOnProjects = totalExpensesAcrossProjects,
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
