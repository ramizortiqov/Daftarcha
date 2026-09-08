package com.example.daftarcha.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.daftarcha.data.model.Payment
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    // Аналог add_payment()
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: Payment)

    // Аналог get_payment_history_for_employee()
    @Query("SELECT * FROM payments WHERE employeeId = :employeeId ORDER BY date DESC")
    fun getPaymentsForEmployee(employeeId: Int): Flow<List<Payment>>

    // Аналог get_total_payments_for_employee (для всех проектов)
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE employeeId = :employeeId")
    fun getTotalPayments(employeeId: Int): Flow<Double>

    // Аналог get_total_payments_for_employee (для конкретных проектов)
//    @Query("""
//        SELECT COALESCE(SUM(amount), 0.0) FROM payments
//        WHERE employeeId = :employeeId AND projectId IN (:projectIds)
//    """)
//    suspend fun getTotalPaymentsForProjects(employeeId: Int, projectIds: List<Int>): Double

    @Query("SELECT * FROM payments WHERE projectId = :projectId ORDER BY date DESC")
    fun getPaymentsForProject(projectId: Int): Flow<List<Payment>>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM payments 
        WHERE employeeId = :employeeId 
        AND (projectId IN (:projectIds) OR projectId IS NULL)
    """)
    suspend fun getTotalPaymentsForProjects(employeeId: Int, projectIds: List<Int>): Double
}
