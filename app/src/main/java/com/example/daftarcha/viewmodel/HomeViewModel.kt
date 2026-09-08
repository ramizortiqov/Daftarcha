package com.example.daftarcha.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// --- ИМПОРТЫ УПРОЩЕНЫ (НУЖНЫ ТОЛЬКО ДЛЯ СПИСКА ПРОЕКТОВ И СОТРУДНИКОВ) ---
import com.example.daftarcha.data.dao.AttendanceDao
import com.example.daftarcha.data.dao.EmployeeDao
import com.example.daftarcha.data.dao.ProjectDao
import com.example.daftarcha.data.dao.ProjectEmployeeDao
// ---
import com.example.daftarcha.data.model.*
import com.example.daftarcha.data.db.DaftarchaDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlin.collections.associate

// --- УБРАЛИ EmployeeListItem ---
import com.example.daftarcha.viewmodel.ProjectListItem // Убедитесь, что путь правильный

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val projectDao: ProjectDao,
    private val employeeDao: EmployeeDao,
    private val projectEmployeeDao: ProjectEmployeeDao,
    private val attendanceDao: AttendanceDao,
    private val db: DaftarchaDatabase
    // --- УБРАЛИ BonusDao и PaymentDao, они здесь больше не нужны ---
) : ViewModel() {

    // --- projectListItems (ДЛЯ ВКЛАДКИ ПРОЕКТОВ) ОСТАЕТСЯ БЕЗ ИЗМЕНЕНИЙ ---
    val projectListItems: StateFlow<List<ProjectListItem>> = projectDao.getAllProjects()
        .flatMapLatest { projects ->
            combine(
                // Убедитесь, что DAO и data class-ы для этого существуют
                projectEmployeeDao.getProjectEmployeeCounts()
                    .map { list: List<ProjectEmployeeCount> -> list.associateBy { it.projectId }.mapValues { it.value.count } },
                attendanceDao.getProjectTotalWorkdays()
                    .map { list: List<ProjectTotalWorkdays> -> list.associateBy { it.projectId }.mapValues { it.value.count } }
            ) { employeeCounts: Map<Int, Int>, totalDaysMap: Map<Int, Int> ->
                projects.map { project ->
                    ProjectListItem(
                        project = project,
                        employeeCount = employeeCounts[project.id] ?: 0,
                        totalDays = totalDaysMap[project.id] ?: 0
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectCount: StateFlow<Int> = projectListItems.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // --- ВОЗВРАЩАЕМ ПРОСТОЙ СПИСОК СОТРУДНИКОВ (БЕЗ БАЛАНСА) ---
    val employees: StateFlow<List<Employee>> = employeeDao.getAllEmployees()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // employeeCount теперь использует 'employees'
    val employeeCount: StateFlow<Int> = employees.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    // --- КОНЕЦ БЛОКА СОТРУДНИКОВ ---

    // --- Функции addProject, addEmployee без изменений ---
    fun addProject(name: String, startDate: String?) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                projectDao.insert(Project(
                    name = name.trim(),
                    startDate = startDate?.trim().takeIf { !it.isNullOrBlank() } ?: currentDate
                ))
            }
        }
    }
    fun addEmployee(name: String, phone: String?) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                employeeDao.insert(Employee(
                    name = name.trim(),
                    phone = phone?.trim().takeIf { !it.isNullOrBlank() }
                ))
            }
        }
    }
    suspend fun checkpointDatabase() {
        withContext(Dispatchers.IO) { // Выполняем в фоновом потоке
            try {
                // Выполняем SQL-команду "checkpoint"
                db.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL);")
                Log.d("Backup", "Database checkpoint successful.")
            } catch (e: Exception) {
                Log.e("Backup", "Checkpoint failed", e)
            }
        }
    }
}