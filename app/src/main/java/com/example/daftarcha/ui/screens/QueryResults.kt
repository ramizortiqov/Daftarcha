package com.example.daftarcha.data.model // Or your chosen package

// Used by ProjectEmployeeDao
data class ProjectEmployeeCount(
    val projectId: Int,
    val count: Int
)

// Used by AttendanceDao
data class ProjectTotalWorkdays(
    val projectId: Int,
    val count: Int
)

/**
 * Хранит результат расчета для одного сотрудника.
 */
data class CalculatedEmployee(
    val id: Int,
    val name: String,
    val workdays: Int,
    val earned: Double,
    val paid: Double,
    val balance: Double // К выплате
)

/**
 * Хранит общий итог расчета по всем выбранным проектам.
 */
data class CalculationSummary(
    val totalBonuses: Double,
    val totalExpenses: Double,  // <-- 1. ДОБАВЬТЕ ЭТУ СТРОКУ
    val netCost: Double,
    val totalWorkdays: Int,
    val dailyRate: Double,
    val employeeStats: List<CalculatedEmployee> // Список результатов по сотрудникам
)

data class ProjectExpenseTotal(
    val projectId: Int,
    val total: Double // 'as total' в ExpenseDao
)