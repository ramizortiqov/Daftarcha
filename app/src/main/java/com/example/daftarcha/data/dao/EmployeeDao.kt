package com.example.daftarcha.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.daftarcha.data.model.Employee
import kotlinx.coroutines.flow.Flow
import androidx.room.Update

@Dao
interface EmployeeDao {

    // Аналог add_employee()
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(employee: Employee)

    // Аналог get_employee()
    @Query("SELECT * FROM employees WHERE id = :id")
    suspend fun getEmployeeById(id: Int): Employee?

    // Аналог get_employees()
    @Query("SELECT * FROM employees WHERE isFired = 0 ORDER BY name ASC") // <-- ИЗМЕНЕНО
    fun getAllEmployees(): Flow<List<Employee>>
    @Query("SELECT * FROM employees WHERE id = :id")
    fun getEmployeeByIdFlow(id: Int): Flow<Employee?>

    @Update
    suspend fun update(employee: Employee)
    // ... (внутри interface EmployeeDao)

    @Query("SELECT * FROM employees ORDER BY name ASC")
    suspend fun getAllEmployeesSuspend(): List<Employee> // Suspend-версия

    @Query("SELECT * FROM employees WHERE isFired = 1 ORDER BY name ASC")
    fun getFiredEmployees(): Flow<List<Employee>>

    /** "Увольняет" сотрудника (устанавливает isFired = 1) */
    @Query("UPDATE employees SET isFired = 1 WHERE id = :employeeId")
    suspend fun fireEmployee(employeeId: Int)

    /** "Восстанавливает" сотрудника (устанавливает isFired = 0) */
    @Query("UPDATE employees SET isFired = 0 WHERE id = :employeeId")
    suspend fun restoreEmployee(employeeId: Int)

    /** ПОЛНОСТЬЮ удаляет сотрудника из базы данных (каскадное удаление) */
    @Query("DELETE FROM employees WHERE id = :employeeId")
    suspend fun deleteEmployeeById(employeeId: Int)
}
