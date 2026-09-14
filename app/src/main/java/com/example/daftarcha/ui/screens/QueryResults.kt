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
 * Одна строка личного харажата (с комментарием), для показа в расчёте.
 */
data class ExpenseLineItem(
    val description: String,
    val amount: Double,
    val date: String
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
    val expenses: Double = 0.0,
    val expenseDetails: List<ExpenseLineItem> = emptyList(),
    val balance: Double // К выплате
)

/**
 * Хранит общий итог расчета по всем выбранным проектам.
 */
data class CalculationSummary(
    val totalBonuses: Double,
    val totalExpenses: Double,  // <-- 1. ДОБАВЬТЕ ЭТУ СТРОКУ
    val allExpenses: List<ExpenseLineItem> = emptyList(),
    val netCost: Double,
    val totalWorkdays: Int,
    val dailyRate: Double,
    val employeeStats: List<CalculatedEmployee> // Список результатов по сотрудникам
)

data class ProjectExpenseTotal(
    val projectId: Int,
    val total: Double // 'as total' в ExpenseDao
)