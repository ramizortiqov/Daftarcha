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
import com.example.daftarcha.data.dao.ProjectTotalWorkdays
import com.example.daftarcha.data.model.Employee
import com.example.daftarcha.data.model.Payment
import com.example.daftarcha.data.model.Project
import com.example.daftarcha.data.model.ProjectExpenseTotal
import com.example.daftarcha.data.model.WorkerProfileData
import com.example.daftarcha.data.model.WorkerProjectStat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class WorkerDashboardViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val employeeDao: EmployeeDao,
    private val attendanceDao: AttendanceDao,
    private val paymentDao: PaymentDao,
    private val projectDao: ProjectDao,
    private val projectEmployeeDao: ProjectEmployeeDao,
    private val bonusDao: BonusDao,
    private val expenseDao: ExpenseDao
) : ViewModel() {

    val currentUser: StateFlow<AuthUser?> = authManager.currentUser

    // Employee ID for the logged in worker
    private val targetEmployeeId: StateFlow<Int?> = currentUser.map { it?.employeeId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val workerData: StateFlow<WorkerProfileData?> = targetEmployeeId.flatMapLatest { empId ->
        if (empId == null || empId == 0) {
            flowOf(null)
        } else {
            combine(
                employeeDao.getEmployeeByIdFlow(empId).filterNotNull(),
                paymentDao.getPaymentsForEmployee(empId),
                paymentDao.getTotalPayments(empId),
                attendanceDao.getTotalWorkdaysForEmployeeAcrossAllProjects(empId),
                attendanceDao.getEmployeeWorkdaysPerProjectFlow(empId),
                bonusDao.getAllBonusesGroupedByProject(),
                expenseDao.getAllExpensesGroupedByProject(),
                attendanceDao.getAllProjectTotalWorkdays(),
                projectDao.getAllProjects()
            ) { values ->
                val emp = values[0] as? Employee ?: return@combine null
                @Suppress("UNCHECKED_CAST")
                val paymentsList = (values[1] as? List<*>)?.filterIsInstance<Payment>() ?: emptyList()
                val totalPaid = (values[2] as? Number)?.toDouble() ?: 0.0
                val totalWorkdays = (values[3] as? Number)?.toInt() ?: 0
                @Suppress("UNCHECKED_CAST")
                val empDaysPerProj = ((values[4] as? List<*>)?.filterIsInstance<ProjectEmployeeWorkdays>() ?: emptyList())
                    .associate { it.projectId to it.workdays }
                @Suppress("UNCHECKED_CAST")
                val bonusesMap = ((values[5] as? List<*>)?.filterIsInstance<ProjectBonusTotal>() ?: emptyList())
                    .associate { it.projectId to it.total }
                @Suppress("UNCHECKED_CAST")
                val expensesMap = ((values[6] as? List<*>)?.filterIsInstance<ProjectExpenseTotal>() ?: emptyList())
                    .associate { it.projectId to it.total }
                @Suppress("UNCHECKED_CAST")
                val totalDaysMap = ((values[7] as? List<*>)?.filterIsInstance<ProjectTotalWorkdays>() ?: emptyList())
                    .associate { it.projectId to it.count }
                @Suppress("UNCHECKED_CAST")
                val allProjs = ((values[8] as? List<*>)?.filterIsInstance<Project>() ?: emptyList())
                    .associateBy { it.id }

                var totalEarned = 0.0
                var totalExpensesAcrossProjects = 0.0
                val breakdowns = mutableListOf<WorkerProjectStat>()

                empDaysPerProj.forEach { (projectId, workerDaysInProj) ->
                    val projBonus = bonusesMap[projectId] ?: 0.0
                    val projExpense = expensesMap[projectId] ?: 0.0
                    val projTotalDays = totalDaysMap[projectId] ?: 0
                    val netCost = projBonus - projExpense
                    val dailyRate = if (projTotalDays > 0) netCost / projTotalDays else 0.0
                    val earnedHere = workerDaysInProj * dailyRate

                    totalEarned += earnedHere
                    totalExpensesAcrossProjects += projExpense

                    val projName = allProjs[projectId]?.name ?: "Иш #$projectId"
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

                val balance = totalEarned - totalPaid

                WorkerProfileData(
                    employee = emp,
                    totalWorkdays = totalWorkdays,
                    totalEarned = totalEarned,
                    totalPaid = totalPaid,
                    balance = balance,
                    totalExpensesOnProjects = totalExpensesAcrossProjects,
                    payments = paymentsList,
                    projectBreakdowns = breakdowns
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun logout() {
        authManager.logout()
    }
}
