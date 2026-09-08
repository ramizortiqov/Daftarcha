package com.example.daftarcha.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daftarcha.data.dao.*
import com.example.daftarcha.data.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import androidx.compose.runtime.mutableStateMapOf

enum class ProjectDialog {
    NONE,
    ADD_EXPENSE,
    ADD_BONUS,
    ADD_EMPLOYEE,
    ARCHIVE_PROJECT,
    EDIT_PROJECT
}

data class AttendanceTable(
    val dates: List<String> = emptyList(),
    val employeeMarks: Map<Int, List<Boolean>> = emptyMap()
)

data class ProjectStats(
    val totalWorkdays: Int = 0,
    val totalBonuses: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netCost: Double = 0.0,
    val dailyRate: Double = 0.0
)

data class HistoryItem(
    val type: String,
    val amount: Double,
    val date: String,
    val details: String
)

@HiltViewModel
class ProjectViewModel @Inject constructor(
    private val projectDao: ProjectDao,
    private val employeeDao: EmployeeDao,
    private val projectEmployeeDao: ProjectEmployeeDao,
    private val attendanceDao: AttendanceDao,
    private val expenseDao: ExpenseDao,
    private val bonusDao: BonusDao,
    private val paymentDao: PaymentDao,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val projectId: StateFlow<Int> = savedStateHandle.getStateFlow("projectId", 0)

    val project: StateFlow<Project?> = projectId.flatMapLatest { id ->
        if (id > 0) {
            projectDao.getProjectByIdFlow(id).filterNotNull()
        } else {
            flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val employees: StateFlow<List<Employee>> = projectId.flatMapLatest { id ->
        if (id > 0) {
            projectEmployeeDao.getEmployeesForProject(id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<ProjectStats> = combine(
        projectId,
        bonusDao.getTotalBonusesForProject(projectId.value),
        expenseDao.getTotalExpensesForProject(projectId.value)
    ) { id, totalBonuses, totalExpenses ->
        if (id > 0) {
            val totalWorkdays = attendanceDao.getTotalWorkdaysForProject(id)
            val netCost = totalBonuses - totalExpenses
            val dailyRate = if (totalWorkdays > 0) netCost / totalWorkdays else 0.0

            ProjectStats(
                totalWorkdays = totalWorkdays,
                totalBonuses = totalBonuses,
                totalExpenses = totalExpenses,
                netCost = netCost,
                dailyRate = dailyRate
            )
        } else {
            ProjectStats()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProjectStats())

    private val allAttendance: StateFlow<List<Attendance>> = projectId.flatMapLatest { id ->
        if (id > 0) {
            attendanceDao.getAllAttendanceForProject(id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendanceTable: StateFlow<AttendanceTable> = combine(
        project,
        employees,
        allAttendance
    ) { proj, emps, attendanceList ->

        if (proj == null || proj.startDate == null) {
            return@combine AttendanceTable()
        }

        val dates = mutableListOf<String>()
        try {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val startDate = formatter.parse(proj.startDate!!)
            val endDate = proj.endDate?.let { formatter.parse(it) } ?: Date()


            val cal = java.util.Calendar.getInstance()
            cal.time = startDate

            while (!cal.time.after(endDate)) {
                dates.add(formatter.format(cal.time))
                cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        } catch (e: Exception) {
            return@combine AttendanceTable()
        }

        val lookup: Map<Pair<Int, String>, Boolean> = attendanceList
            .associate { Pair(it.employeeId, it.date) to it.present }

        val employeeMarks = emps.associate { employee ->
            val marks = dates.map { date ->
                lookup[Pair(employee.id, date)] ?: false
            }
            employee.id to marks
        }

        AttendanceTable(dates, employeeMarks)

    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AttendanceTable())

    private val allEmployees: StateFlow<List<Employee>> = employeeDao.getAllEmployees()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableEmployees: StateFlow<List<Employee>> = combine(
        allEmployees,
        employees
    ) { all, current ->
        val currentIds = current.map { it.id }.toSet()
        all.filter { it.id !in currentIds }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val expensesFlow: Flow<List<Expense>> = projectId.flatMapLatest { id ->
        if (id > 0) expenseDao.getExpensesForProject(id) else flowOf(emptyList())
    }

    private val bonusesFlow: Flow<List<Bonus>> = projectId.flatMapLatest { id ->
        if (id > 0) bonusDao.getBonusesForProject(id) else flowOf(emptyList())
    }

    private val paymentsFlow: Flow<List<Payment>> = projectId.flatMapLatest { id ->
        if (id > 0) paymentDao.getPaymentsForProject(id) else flowOf(emptyList())
    }

    val combinedHistory: StateFlow<List<HistoryItem>> = combine(
        expensesFlow,
        bonusesFlow,
        paymentsFlow,
        allEmployees
    ) { expenses, bonuses, payments, allEmps ->

        val history = mutableListOf<HistoryItem>()

        val employeeNameMap = allEmps.associate { it.id to it.name }

        expenses.mapTo(history) {
            HistoryItem("Харажат", it.amount, it.date, it.description ?: "Харажат")
        }
        bonuses.mapTo(history) {
            HistoryItem("Пул берди", it.amount, it.date, it.description ?: "Пул берди")
        }
        payments.mapTo(history) {
            val name = employeeNameMap[it.employeeId] ?: "Номаълум шерик"
            HistoryItem("Тўлов", it.amount, it.date, "Тўлов: $name")
        }

        history.sortedByDescending { it.date }

    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _localAttendanceChanges = mutableStateMapOf<Pair<Int, String>, Boolean>()
    val localAttendanceChanges: Map<Pair<Int, String>, Boolean>
        get() = _localAttendanceChanges

    fun toggleLocalAttendance(employeeId: Int, date: String, isPresent: Boolean) {
        _localAttendanceChanges[Pair(employeeId, date)] = isPresent
    }

    fun saveAttendanceChanges() {
        if (projectId.value == 0 || _localAttendanceChanges.isEmpty()) return
        val changesToSave = _localAttendanceChanges.toMap()
        _localAttendanceChanges.clear()
        viewModelScope.launch {
            changesToSave.forEach { (key, isPresent) ->
                val (employeeId, date) = key
                attendanceDao.upsert(Attendance(
                    projectId = projectId.value,
                    employeeId = employeeId,
                    date = date,
                    present = isPresent
                ))
            }
        }
    }

    fun discardAttendanceChanges() {
        _localAttendanceChanges.clear()
    }

    private val _dialogState = MutableStateFlow(ProjectDialog.NONE)
    val dialogState: StateFlow<ProjectDialog> = _dialogState.asStateFlow()

     fun openDialog(dialog: ProjectDialog) {
        _dialogState.value = dialog
    }

    fun dismissDialog() {
        _dialogState.value = ProjectDialog.NONE
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    fun updateProject(name: String, startDate: String?) {
        val currentProject = project.value ?: return
        viewModelScope.launch {
            projectDao.update(
                currentProject.copy(
                    name = name.trim(),
                    startDate = startDate?.trim().takeIf { !it.isNullOrBlank() }
                )
            )
            dismissDialog()
        }
    }
    fun archiveProject() {
        if (projectId.value == 0) return
        viewModelScope.launch {
            projectDao.archiveProject(projectId.value)
            dismissDialog()
        }
    }
    fun addExpense(amount: Double, description: String?) {
        if (projectId.value == 0 || amount <= 0) return
        viewModelScope.launch {
            expenseDao.insert(Expense(
                projectId = projectId.value,
                amount = amount,
                description = description,
                date = getCurrentDate()
            ))
        }
    }

    fun addBonus(amount: Double, description: String?) {
        if (projectId.value == 0 || amount <= 0) return
        viewModelScope.launch {
            bonusDao.insert(Bonus(
                projectId = projectId.value,
                amount = amount,
                description = description,
                date = getCurrentDate()
            ))
        }
    }

    fun addEmployeeToProject(employeeId: Int) {
        if (projectId.value == 0) return
        viewModelScope.launch {
            projectEmployeeDao.insert(ProjectEmployee(
                projectId = projectId.value,
                employeeId = employeeId
            ))
        }
    }

    fun setAttendance(employeeId: Int, date: String, isPresent: Boolean) {
        if (projectId.value == 0) return
        viewModelScope.launch {
            attendanceDao.upsert(Attendance(
                projectId = projectId.value,
                employeeId = employeeId,
                date = date,
                present = isPresent
            ))
        }
    }

    fun addEmployeesToProject(employeeIds: List<Int>) {
        if (projectId.value == 0 || employeeIds.isEmpty()) return
        viewModelScope.launch {
            employeeIds.forEach { empId ->
                projectEmployeeDao.insert(ProjectEmployee(
                    projectId = projectId.value,
                    employeeId = empId
                ))
            }
        }
    }

    fun completeProject() {
        if (projectId.value > 0) {
            viewModelScope.launch {
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                projectDao.setProjectEndDate(projectId.value, currentDate)
            }
        }
    }

    fun reopenProject() {
        if (projectId.value > 0) {
            viewModelScope.launch {
                projectDao.setProjectEndDate(projectId.value, null)
            }
        }
    }
}
