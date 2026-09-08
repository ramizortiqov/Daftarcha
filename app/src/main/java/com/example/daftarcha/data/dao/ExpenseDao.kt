package com.example.daftarcha.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.daftarcha.data.model.Expense
import kotlinx.coroutines.flow.Flow
import com.example.daftarcha.data.model.ProjectExpenseTotal // <-- Добавьте импорт

@Dao
interface ExpenseDao {

    // Аналог add_expense()
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense)

    // Аналог get_expenses_for_project()
    @Query("SELECT * FROM expenses WHERE projectId = :projectId ORDER BY date DESC")
    fun getExpensesForProject(projectId: Int): Flow<List<Expense>>

    // Аналог get_total_expenses()
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE projectId = :projectId")
    fun getTotalExpensesForProject(projectId: Int): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE projectId IN (:projectIds)")
    suspend fun getExpensesForProjectList(projectIds: List<Int>): Double
    @Query("SELECT projectId, COALESCE(SUM(amount), 0.0) as total FROM expenses GROUP BY projectId")
    fun getAllExpensesGroupedByProject(): Flow<List<ProjectExpenseTotal>>
}