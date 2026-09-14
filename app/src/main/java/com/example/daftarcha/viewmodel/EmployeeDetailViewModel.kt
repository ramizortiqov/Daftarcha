package com.example.daftarcha.viewmodel

import android.util.Log // <-- Убедитесь, что импорт есть
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daftarcha.data.dao.*
import com.example.daftarcha.data.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
// <-- Убедитесь, что импорт есть
import javax.inject.Inject
import kotlin.collections.associate // <-- Убедитесь, что импорт есть

import androidx.lifecycle.viewModelScope
// DAO интерфейсы (убедитесь, что все 5 здесь)
import com.example.daftarcha.data.dao.AttendanceDao
import com.example.daftarcha.data.dao.BonusDao
import com.example.daftarcha.data.dao.EmployeeDao
import com.example.daftarcha.data.dao.PaymentDao
import com.example.daftarcha.data.dao.ProjectEmployeeDao
import com.example.daftarcha.data.dao.ProjectEmployeeWorkdays
import com.example.daftarcha.data.model.ProjectTotalWorkdays
// Основные Model/Entity классы
import com.example.daftarcha.data.model.Employee
import com.example.daftarcha.data.model.Payment

// Вспомогательные Data Class-ы для результатов запросов
import com.example.daftarcha.data.dao.ProjectBonusTotal
//import com.example.daftarcha.data.model.ProjectEmployeeWorkdays
//import com.example.daftarcha.data.model.ProjectTotalWorkdays
import kotlinx.coroutines.flow.map
// Функции и классы Kotlin Coroutines Flow
import kotlinx.coroutines.flow.* // Импортирует Flow, StateFlow, MutableStateFlow, combine, map, onEach, flatMapLatest, filterNotNull, flowOf, asStateFlow, launchIn, SharingStarted
import kotlinx.coroutines.launch // Для запуска корутин (addPayment, updateEmployee)

// Для логирования
import java.text.SimpleDateFormat // Для форматирования дат
import java.util.Date // Для получения текущей даты
import java.util.Locale // Для форматирования дат
import java.util.TimeZone // Для работы с датами в combine
import kotlin.collections.associate // Для преобразования List в Map


import com.example.daftarcha.data.auth.AuthManager
import com.example.daftarcha.data.dao.AppUserDao
import com.example.daftarcha.data.model.AppUser
import com.example.daftarcha.data.model.UserRole
import com.example.daftarcha.data.sync.FirestoreSyncManager

/**
 * Определяет, какой диалог в EmployeeDetailScreen открыт
 */
enum class EmployeeDialog {
    NONE,
    ADD_PAYMENT,
    EDIT_EMPLOYEE,
    FIRE_EMPLOYEE,
    RESET_FINANCIALS,
    ACCOUNT_CREDENTIALS
}

@HiltViewModel
class EmployeeDetailViewModel @Inject constructor(
    private val employeeDao: EmployeeDao,
    private val paymentDao: PaymentDao,
    private val attendanceDao: AttendanceDao,
    private val bonusDao: BonusDao,
    private val expenseDao: ExpenseDao,// <-- ДОБАВЛЕНО
    private val projectEmployeeDao: ProjectEmployeeDao, // <-- ДОБАВЛЕНО
    private val appUserDao: AppUserDao,
    private val authManager: AuthManager,
    private val syncManager: FirestoreSyncManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // --- ИДЕНТИФИКАЦИЯ ---
    private val employeeId: StateFlow<Int> = savedStateHandle.getStateFlow("employeeId", 0)

    val currentUser = authManager.currentUser

    val employeeAccount: StateFlow<AppUser?> = employeeId.flatMapLatest { id ->
        if (id > 0) appUserDao.getUserByEmployeeIdFlow(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- СОСТОЯНИЕ UI ---
    private val _dialogState = MutableStateFlow(EmployeeDialog.NONE)
    val dialogState: StateFlow<EmployeeDialog> = _dialogState.asStateFlow()

    // --- 1. ПЕРВИЧНЫЕ ДАННЫЕ ---
    val employee: StateFlow<Employee?> = employeeId.flatMapLatest { id ->
        if (id > 0) employeeDao.getEmployeeByIdFlow(id).filterNotNull() else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val payments: StateFlow<List<Payment>> = employeeId.flatMapLatest { id ->
        if (id > 0) paymentDao.getPaymentsForEmployee(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalPaid: StateFlow<Double> = employeeId.flatMapLatest { id ->
        if (id > 0) paymentDao.getTotalPayments(id) else flowOf(0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Ушбу ходимга шахсан бириктирилган харажатлар (шашлик, бензин ва ҳ.к.)
    val personalExpenses: StateFlow<List<Expense>> = employeeId.flatMapLatest { id ->
        if (id > 0) expenseDao.getExpensesForEmployeeAcrossAllProjects(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalPersonalExpenses: StateFlow<Double> = employeeId.flatMapLatest { id ->
        if (id > 0) expenseDao.getTotalExpensesForEmployee(id) else flowOf(0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // --- 2. РАСЧЕТНЫЕ ПОЛЯ ---

    // Общие рабочие дни по всем проектам
    val totalWorkdays: StateFlow<Int> = employeeId.flatMapLatest { id ->
        if (id > 0) attendanceDao.getTotalWorkdaysForEmployeeAcrossAllProjects(id) else flowOf(0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // --- НАЧАЛО: ПРАВИЛЬНЫЙ РАСЧЕТ ЗАРАБОТКА ---

    // Flow ID всех проектов, где сотрудник работал
    private val projectIdsFlow: Flow<List<Int>> = employeeId.flatMapLatest { id ->
        if (id > 0) projectEmployeeDao.getProjectIdsForEmployee(id) else flowOf(emptyList())
    }

    // Flow дней сотрудника по проекту (List -> Map)
    private val employeeWorkdaysPerProjectFlow: Flow<Map<Int, Int>> = employeeId.flatMapLatest { id ->
        if (id > 0) {
            attendanceDao.getEmployeeWorkdaysPerProjectFlow(id)
                .map { list: List<ProjectEmployeeWorkdays> -> list.associate { item -> item.projectId to item.workdays } }
        } else {
            flowOf(emptyMap())
        }
    }

    // Flow бонусов и общих дней по ВСЕМ проектам (для расчета ставки) (List -> Map)
    private val projectStatsFlow: Flow<Triple<Map<Int, Double>, Map<Int, Double>, Map<Int, Int>>> = combine(
        bonusDao.getAllBonusesGroupedByProject()
            .map { list: List<ProjectBonusTotal> -> list.associate { item -> item.projectId to item.total } },
        expenseDao.getAllExpensesGroupedByProject()
            .map { list: List<ProjectExpenseTotal> -> list.associate { item -> item.projectId to item.total } },
        attendanceDao.getAllProjectTotalWorkdays()
            .map { list: List<ProjectTotalWorkdays> -> list.associate { item -> item.projectId to item.count } }
    ) { bonuses: Map<Int, Double>,expenses: Map<Int, Double>, totalDays: Map<Int, Int> ->
        Triple(bonuses, expenses, totalDays)
    }

    // Мутабельное состояние для хранения РЕЗУЛЬТАТА расчета
    private val _totalEarned = MutableStateFlow(0.0)
    val totalEarned: StateFlow<Double> = _totalEarned.asStateFlow() // Отдаем UI неизменяемый Flow

    // Баланс (заработок минус выплаты минус личные харажаты)
    val balance: StateFlow<Double> = combine(_totalEarned, totalPaid, totalPersonalExpenses) { earned, paid, expenses ->
        earned - paid - expenses
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Инициализация расчета заработка при запуске ViewModel
    init {
        combine(
            employeeId,
            employeeWorkdaysPerProjectFlow, // Map<ProjID, EmpDays>
            projectStatsFlow                // Triple<Bonuses, Expenses, TotalDays>
        ) { empId, empDaysMap, statsTriple ->
            Triple(empId, empDaysMap, statsTriple) // Передаем Triple дальше
        }
            .onEach { (empId, empDaysMap, statsTriple) -> // Получаем Triple
                if (empId > 0) {
                    val calculatedEarned = calculateTotalEarned(
                        empId,
                        empDaysMap,
                        statsTriple.first,  // Bonuses
                        statsTriple.second, // Expenses
                        statsTriple.third   // Total Days
                    )
                    _totalEarned.value = calculatedEarned
                    Log.d("EarnedCalc", "ViewModel calculated Earned: $calculatedEarned")
                } else {
                    _totalEarned.value = 0.0
                }
            }
            .launchIn(viewModelScope) // Запускаем и привязываем к жизни ViewModel
    }

    // Suspend-функция для выполнения расчета (вынесена из combine)
    private suspend fun calculateTotalEarned(
        empId: Int,
        employeeDaysMap: Map<Int, Int>,
        bonusesPerProject: Map<Int, Double>,
        expensesPerProject: Map<Int, Double>,
        totalDaysPerProject: Map<Int, Int>
    ): Double {
        var totalEmployeeEarned = 0.0
        Log.d("EarnedCalc", "--- Starting Calculation for Employee $empId ---")

        employeeDaysMap.forEach { (projectId, employeeWorkdays) ->
            val projectBonuses = bonusesPerProject[projectId] ?: 0.0
            val projectTotalWorkdays = totalDaysPerProject[projectId] ?: 0
            // Харажатлар энди шахсий (иштирокчиларга бириктирилган), шу сабабли
            // умумий кунлик ставкага таъсир қилмайди.
            val projectDailyRate = if (projectTotalWorkdays > 0) projectBonuses / projectTotalWorkdays else 0.0

            Log.d("EarnedCalc", "Project $projectId: Bonus=$projectBonuses, TotalDays=$projectTotalWorkdays, Rate=$projectDailyRate, EmployeeDays=$employeeWorkdays")

            // Suspend-вызов здесь РАЗРЕШЕН
            val workdaysSnapshot = attendanceDao.getWorkdaysForEmployeeInProject(empId, projectId)
            // Дополнительная проверка, если вдруг карта employeeDaysMap отстала от suspend-запроса
            if (workdaysSnapshot != employeeWorkdays) {
                Log.w("EarnedCalc", "Mismatch! Flow days: $employeeWorkdays, Suspend days: $workdaysSnapshot for project $projectId. Using suspend value.")
            }

            totalEmployeeEarned += workdaysSnapshot * projectDailyRate // Используем workdaysSnapshot для точности
        }
        Log.d("EarnedCalc", "--- Finished Calculation. Total Earned: $totalEmployeeEarned ---")
        return totalEmployeeEarned
    }

    // --- КОНЕЦ: ПРАВИЛЬНЫЙ РАСЧЕТ ЗАРАБОТКА ---
    fun fireEmployee() {
        if (employeeId.value == 0) return
        viewModelScope.launch {
            employeeDao.fireEmployee(employeeId.value)
            dismissDialog()
        }
    }

    // --- 3. ФУНКЦИИ ДЛЯ UI ---

    /** Opens the specified dialog on the EmployeeDetailScreen */
    fun openDialog(dialog: EmployeeDialog) {
        _dialogState.value = dialog
    }

    /** Closes any currently open dialog */
    fun dismissDialog() {
        _dialogState.value = EmployeeDialog.NONE
    }

    /** Adds a payment record for the current employee */
    fun addPayment(amount: Double, description: String?, projectId: Int? = null) {
        // Ensure valid employee ID and positive amount
        if (employeeId.value == 0 || amount <= 0) return
        viewModelScope.launch {
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            paymentDao.insert(Payment(
                employeeId = employeeId.value,
                projectId = projectId, // Pass optional project ID
                amount = amount,
                date = currentDate,
                description = description?.trim().takeIf { !it.isNullOrBlank() }
            ))
            Log.d("ViewModel", "Payment added: $amount for employee ${employeeId.value}") // Optional: Log success
        }
    }

    /** Updates the name and phone number of the current employee */
    fun updateEmployee(name: String, phone: String?) {
        // Get the current employee data (needed for ID)
        val currentEmployee = employee.value ?: return
        viewModelScope.launch {
            // Only update if the name is not blank
            if (name.isNotBlank()) {
                employeeDao.update(
                    currentEmployee.copy( // Create a copy with updated fields
                        name = name.trim(),
                        // Set phone to null if the trimmed string is blank, otherwise use the trimmed string
                        phone = phone?.trim().takeIf { !it.isNullOrBlank() }
                    )
                )
                Log.d("ViewModel", "Employee updated: ${currentEmployee.id} - $name") // Optional: Log success
            }
        }
    }

    fun saveAccountCredentials(
        loginId: String,
        pass: String,
        role: UserRole,
        onResult: (Boolean, String?) -> Unit
    ) {
        val empId = employeeId.value
        if (empId <= 0) return

        // Логин ва паролни фақат Усто (BRIGADIER) бериши/ўзгартириши мумкин — Админга бу
        // маълумот на кўринади, на ўзгартириш ҳуқуқи берилади.
        if (currentUser.value?.role != UserRole.BRIGADIER) {
            onResult(false, "Бу амални фақат Усто бажара олади")
            return
        }

        viewModelScope.launch {
            val result = authManager.createOrUpdateEmployeeAccount(
                employeeId = empId,
                loginId = loginId,
                password = pass,
                role = role
            )
            if (result.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message)
            }
        }
    }

    /**
     * Очистка / сброс расчетов сотрудника:
     * удаляет полученные выплаты (payments) и отработанные дни/посещаемость (attendance)
     * локально в Room и синхронизирует с Firestore.
     */
    fun resetFinancials() {
        val empId = employeeId.value
        if (empId <= 0) return
        viewModelScope.launch {
            paymentDao.deletePaymentsByEmployeeId(empId)
            attendanceDao.deleteAttendanceByEmployeeId(empId)
            syncManager.deleteAttendanceAndPaymentsForEmployee(empId)
            dismissDialog()
            Log.d("ViewModel", "Financials reset for employee $empId")
        }
    }

    /**
     * Удаление одной выплаты
     */
    fun deletePayment(paymentId: Int) {
        viewModelScope.launch {
            paymentDao.deletePaymentById(paymentId)
            syncManager.deletePaymentFromCloud(paymentId)
        }
    }
}