package com.or.daftarcha.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.or.daftarcha.data.model.Expense
import kotlinx.coroutines.flow.Flow
import com.or.daftarcha.data.model.ProjectExpenseTotal // <-- Добавьте импорт

@Dao
interface ExpenseDao {

    // Аналог add_expense()
    @Upsert
    suspend fun insert(expense: Expense)

    // Аналог get_expenses_for_project()
    @Query("SELECT * FROM expenses WHERE projectId = :projectId ORDER BY date DESC")
    fun getExpensesForProject(projectId: Int): Flow<List<Expense>>

    // Аналог get_total_expenses()
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE projectId = :projectId")
    fun getTotalExpensesForProject(projectId: Int): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE projectId IN (:projectIds)")
    suspend fun getExpensesForProjectList(projectIds: List<Int>): Double

    @Query("SELECT * FROM expenses WHERE projectId IN (:projectIds) ORDER BY date DESC")
    suspend fun getExpenseListForProjects(projectIds: List<Int>): List<Expense>
    @Query("SELECT projectId, COALESCE(SUM(amount), 0.0) as total FROM expenses GROUP BY projectId")
    fun getAllExpensesGroupedByProject(): Flow<List<ProjectExpenseTotal>>

    @Query("SELECT * FROM expenses")
    suspend fun getAllExpenses(): List<Expense>

    @Upsert
    suspend fun insertAll(expenses: List<Expense>)

    // --- Шахсий (кимгадир бириктирилган) харажатлар ---

    @Query("SELECT * FROM expenses WHERE employeeId = :employeeId ORDER BY date DESC")
    fun getExpensesForEmployeeAcrossAllProjects(employeeId: Int): Flow<List<Expense>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE employeeId = :employeeId")
    fun getTotalExpensesForEmployee(employeeId: Int): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE employeeId = :employeeId AND projectId IN (:projectIds)")
    suspend fun getTotalExpensesForEmployeeInProjects(employeeId: Int, projectIds: List<Int>): Double

    @Query("SELECT * FROM expenses WHERE employeeId = :employeeId AND projectId IN (:projectIds) ORDER BY date DESC")
    suspend fun getExpensesForEmployeeInProjects(employeeId: Int, projectIds: List<Int>): List<Expense>
}
