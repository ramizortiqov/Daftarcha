package com.example.daftarcha.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.daftarcha.data.model.Project
import kotlinx.coroutines.flow.Flow
import androidx.room.Update
@Dao
interface ProjectDao {

    // Аналог add_project()
    @Upsert
    suspend fun insert(project: Project)

    // Аналог get_project()
    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Int): Project?

    // Аналог get_projects()
    // Flow<> означает, что список будет обновляться автоматически (в реальном времени)
    @Query("SELECT * FROM projects WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllProjects(): Flow<List<Project>>
    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectByIdFlow(id: Int): Flow<Project?>

    @Query("UPDATE projects SET endDate = :endDate WHERE id = :projectId")
    suspend fun setProjectEndDate(projectId: Int, endDate: String?)

    @Query("UPDATE projects SET isArchived = 1 WHERE id = :projectId")
    suspend fun archiveProject(projectId: Int)

    @Update
    suspend fun update(project: Project)

    @Query("SELECT * FROM projects WHERE isArchived = 1 ORDER BY name ASC")
    fun getArchivedProjects(): Flow<List<Project>>

    /** Восстанавливает проект из архива (устанавливает isArchived = 0) */
    @Query("UPDATE projects SET isArchived = 0 WHERE id = :projectId")
    suspend fun restoreProject(projectId: Int)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: Int)

    @Query("SELECT * FROM projects")
    suspend fun getAllProjectsList(): List<Project>

    @Upsert
    suspend fun insertAll(projects: List<Project>)

    @Query("SELECT * FROM projects WHERE (brigadierId = :brigadierId OR :brigadierId = '') AND isArchived = 0 ORDER BY name ASC")
    fun getProjectsForBrigadier(brigadierId: String): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE (brigadierId = :brigadierId OR :brigadierId = '') AND isArchived = 1 ORDER BY name ASC")
    fun getArchivedProjectsForBrigadier(brigadierId: String): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE (brigadierId = :brigadierId OR :brigadierId = '') ORDER BY name ASC")
    suspend fun getProjectsForBrigadierList(brigadierId: String): List<Project>

    @Query("UPDATE projects SET brigadierId = :brigadierId WHERE brigadierId = '' OR brigadierId IS NULL")
    suspend fun assignUnassignedProjectsToBrigadier(brigadierId: String)
}