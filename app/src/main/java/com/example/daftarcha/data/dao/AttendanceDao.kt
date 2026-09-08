package com.example.daftarcha.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.daftarcha.data.model.Attendance
import com.example.daftarcha.data.model.ProjectTotalWorkdays
import kotlinx.coroutines.flow.Flow

data class ProjectEmployeeWorkdays(val projectId: Int, val workdays: Int)
data class ProjectTotalWorkdays(val projectId: Int, val count: Int)
@Dao
interface AttendanceDao {

    // Аналог set_attendance()
    // @Upsert = (UPdate + inSERT). Идеально для "ON CONFLICT REPLACE"
    @Upsert
    suspend fun upsert(attendance: Attendance)

    // Аналог get_attendance_for_employee()
    @Query("""
        SELECT * FROM attendance 
        WHERE projectId = :projectId AND employeeId = :employeeId 
        AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun getAttendanceForEmployee(
        projectId: Int,
        employeeId: Int,
        startDate: String,
        endDate: String
    ): List<Attendance>

    // Запрос для подсчета рабочих дней (для статистики)
    @Query("""
        SELECT COUNT(*) FROM attendance 
        WHERE projectId = :projectId AND employeeId = :employeeId AND present = 1
    """)
    suspend fun getWorkdaysForEmployee(projectId: Int, employeeId: Int): Int

    @Query("SELECT COUNT(*) FROM attendance WHERE projectId = :projectId AND present = 1")
    suspend fun getTotalWorkdaysForProject(projectId: Int): Int

    @Query("SELECT * FROM attendance WHERE projectId = :projectId")
    fun getAllAttendanceForProject(projectId: Int): Flow<List<Attendance>>

    @Query("""
    SELECT projectId, COUNT(*) as count 
    FROM attendance 
    WHERE present = 1 
    GROUP BY projectId
""")
    fun getProjectTotalWorkdays(): Flow<List<ProjectTotalWorkdays>>

    @Query("SELECT COUNT(*) FROM attendance WHERE employeeId = :employeeId AND present = 1")
    fun getTotalWorkdaysForEmployeeAcrossAllProjects(employeeId: Int): Flow<Int>

    /** 2. Рабочие дни сотрудника, сгруппированные ПО ПРОЕКТУ (Flow) */
    @Query("""
        SELECT projectId, COUNT(*) as workdays
        FROM attendance
        WHERE employeeId = :employeeId AND present = 1
        GROUP BY projectId
    """)
    fun getEmployeeWorkdaysPerProjectFlow(employeeId: Int): Flow<List<ProjectEmployeeWorkdays>>

    /** 3. Общие рабочие дни по ВСЕМ сотрудникам, сгруппированные ПО ПРОЕКТУ (Flow) */
    @Query("SELECT projectId, COUNT(*) as count FROM attendance WHERE present = 1 GROUP BY projectId")
    fun getAllProjectTotalWorkdays(): Flow<List<ProjectTotalWorkdays>>

    /** 4. Кол-во дней для сотрудника в ОДНОМ КОНКРЕТНОМ проекте (suspend) */
    @Query("SELECT COUNT(*) FROM attendance WHERE employeeId = :employeeId AND projectId = :projectId AND present = 1")
    suspend fun getWorkdaysForEmployeeInProject(employeeId: Int, projectId: Int): Int

    // ... (внутри interface AttendanceDao)

    // Data class (можете поместить в QueryResults.kt)
    data class EmployeeDaysInProjects(val employeeId: Int, val workdays: Int)

    // Общее кол-во дней для СПИСКА проектов
    @Query("SELECT COUNT(*) FROM attendance WHERE projectId IN (:projectIds) AND present = 1")
    suspend fun getTotalWorkdaysForProjects(projectIds: List<Int>): Int

    // Рабочие дни КАЖДОГО сотрудника в СПИСКЕ проектов
    @Query("""
    SELECT employeeId, COUNT(*) as workdays 
    FROM attendance 
    WHERE projectId IN (:projectIds) AND present = 1 
    GROUP BY employeeId
""")
    suspend fun getWorkdaysPerEmployeeForProjects(projectIds: List<Int>): List<EmployeeDaysInProjects>
}
