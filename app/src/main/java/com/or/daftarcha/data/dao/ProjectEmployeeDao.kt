package com.or.daftarcha.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.or.daftarcha.data.model.Employee
import com.or.daftarcha.data.model.ProjectEmployee
import com.or.daftarcha.data.model.ProjectEmployeeCount
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectEmployeeDao {

    // Аналог add_employee_to_project()
    @Upsert
    suspend fun insert(projectEmployee: ProjectEmployee)

    // Аналог get_project_employees()
    @Query("""
        SELECT e.* FROM employees e 
        JOIN project_employees pe ON e.id = pe.employeeId 
        WHERE pe.projectId = :projectId 
        ORDER BY e.name
    """)
    fun getEmployeesForProject(projectId: Int): Flow<List<Employee>>

    @Query("""
    SELECT projectId, COUNT(employeeId) as count 
    FROM project_employees 
    GROUP BY projectId
""")
    fun getProjectEmployeeCounts(): Flow<List<ProjectEmployeeCount>>
    //fun getProjectEmployeeCounts(): Flow<Map<Int, Int>>
    @Query("SELECT projectId FROM project_employees WHERE employeeId = :employeeId")
    fun getProjectIdsForEmployee(employeeId: Int): Flow<List<Int>>

    @Query("SELECT * FROM project_employees")
    suspend fun getAllProjectEmployees(): List<ProjectEmployee>

    @Upsert
    suspend fun insertAll(list: List<ProjectEmployee>)
}