package com.or.daftarcha.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.or.daftarcha.data.model.Payment
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    // Аналог add_payment()
    @Upsert
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

    // Кассадан (объект бўйича) чиққан умумий пул — барча шериклар учун.
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE projectId = :projectId")
    fun getTotalPaymentsForProject(projectId: Int): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE projectId IN (:projectIds)")
    suspend fun getTotalPaymentsForProjectList(projectIds: List<Int>): Double

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM payments 
        WHERE employeeId = :employeeId 
        AND (projectId IN (:projectIds) OR projectId IS NULL)
    """)
    suspend fun getTotalPaymentsForProjects(employeeId: Int, projectIds: List<Int>): Double

    @Query("SELECT * FROM payments")
    suspend fun getAllPayments(): List<Payment>

    @Query("DELETE FROM payments WHERE employeeId = :employeeId")
    suspend fun deletePaymentsByEmployeeId(employeeId: Int)

    @Query("DELETE FROM payments WHERE id = :paymentId")
    suspend fun deletePaymentById(paymentId: Int)

    @Upsert
    suspend fun insertAll(payments: List<Payment>)
}
