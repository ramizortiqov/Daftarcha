package com.example.daftarcha.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daftarcha.data.dao.*
import com.example.daftarcha.data.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.lang.StringBuilder

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val projectDao: ProjectDao,
    private val employeeDao: EmployeeDao,
    private val paymentDao: PaymentDao,
    private val attendanceDao: AttendanceDao,
    private val bonusDao: BonusDao,
    private val expenseDao: ExpenseDao
) : ViewModel() {

    val allProjects: StateFlow<List<Project>> = projectDao.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedProjectIds = MutableStateFlow(emptySet<Int>())
    val selectedProjectIds: StateFlow<Set<Int>> = _selectedProjectIds.asStateFlow()

    private val _calculationResult = MutableStateFlow<CalculationSummary?>(null)
    val calculationResult: StateFlow<CalculationSummary?> = _calculationResult.asStateFlow()

    fun toggleProjectSelection(projectId: Int) {
        _selectedProjectIds.update { currentIds ->
            if (currentIds.contains(projectId)) {
                currentIds - projectId
            } else {
                currentIds + projectId
            }
        }
    }

    fun clearResult() {
        _calculationResult.value = null
    }

    fun calculateSelectedProjects() {
        viewModelScope.launch {
            val projectIds = _selectedProjectIds.value.toList()
            if (projectIds.isEmpty()) return@launch

            val totalBonuses = bonusDao.getBonusesForProjectList(projectIds)
            val totalExpenses = expenseDao.getExpensesForProjectList(projectIds)
            val totalWorkdays = attendanceDao.getTotalWorkdaysForProjects(projectIds)

            val netCost = totalBonuses - totalExpenses
            val dailyRate = if (totalWorkdays > 0) netCost / totalWorkdays else 0.0

            val allEmployees = employeeDao.getAllEmployeesSuspend()
            val workdaysPerEmployeeMap = attendanceDao.getWorkdaysPerEmployeeForProjects(projectIds)
                .associate { it.employeeId to it.workdays }

            val employeeStats = allEmployees.mapNotNull { emp ->
                val workdays = workdaysPerEmployeeMap[emp.id] ?: 0
                if (workdays == 0) return@mapNotNull null

                val earned = workdays * dailyRate
                val paid = paymentDao.getTotalPaymentsForProjects(emp.id, projectIds)

                CalculatedEmployee(
                    id = emp.id,
                    name = emp.name,
                    workdays = workdays,
                    earned = earned,
                    paid = paid,
                    balance = earned - paid
                )
            }

            _calculationResult.value = CalculationSummary(
                totalBonuses = totalBonuses,
                totalExpenses = totalExpenses,
                netCost = netCost,
                totalWorkdays = totalWorkdays,
                dailyRate = dailyRate,
                employeeStats = employeeStats.sortedByDescending { it.balance }
            )
        }
    }

    fun generateShareableText(summary: CalculationSummary): String {
        val sb = StringBuilder()
        sb.appendLine("--- ҲИСОБОТ ---")
        sb.appendLine()
        sb.appendLine("УМУМИЙ МАЪЛУМОТ:")
        sb.appendLine("  • Умумий пул берилди: %.2f с".format(summary.totalBonuses))
        sb.appendLine("  • Умумий харажатлар: %.2f с".format(summary.totalExpenses))
        sb.appendLine("  • Жами (соф фойда): %.2f с".format(summary.netCost))
        sb.appendLine("  • Жами кунлар: ${summary.totalWorkdays}")
        sb.appendLine("  • Кунлик ставка: %.2f с".format(summary.dailyRate))
        sb.appendLine()
        sb.appendLine("--- ШЕРИКЛАР БЎЙИЧА (${summary.employeeStats.size}) ---")
        sb.appendLine()

        summary.employeeStats.forEach { emp ->
            sb.appendLine("${emp.name} (Кунлар: ${emp.workdays})")
            sb.appendLine("  • Ишлади: %.2f с".format(emp.earned))
            sb.appendLine("  • Олди: %.2f с".format(emp.paid))
            sb.appendLine("  • ТЎЛАНИШИ КЕРАК: %.2f с".format(emp.balance))
            sb.appendLine()
        }
        return sb.toString()
    }

    fun generateCsvText(summary: CalculationSummary): String {
        val sb = StringBuilder()

        sb.appendLine("Категория;Қиймат")
        sb.appendLine("---;---")

        sb.appendLine("Умумий пул берилди;\"%.2f\"".format(summary.totalBonuses))
        sb.appendLine("Умумий харажатлар;\"%.2f\"".format(summary.totalExpenses))
        sb.appendLine("Жами (соф фойда);\"%.2f\"".format(summary.netCost))
        sb.appendLine("Жами кунлар;${summary.totalWorkdays}")
        sb.appendLine("Кунлик ставка;\"%.2f\"".format(summary.dailyRate))
        sb.appendLine()
        sb.appendLine()

        sb.appendLine("Шерик;Иш кунлари;Ишлади;Олди;Тўланиши керак (Баланс)")
        sb.appendLine("---;---;---;---;---")

        summary.employeeStats.forEach { emp ->
            sb.appendLine(
                "${emp.name};${emp.workdays};\"%.2f\";\"%.2f\";\"%.2f\"".format(
                    emp.earned,
                    emp.paid,
                    emp.balance
                )
            )
        }
        return sb.toString()
    }
}
