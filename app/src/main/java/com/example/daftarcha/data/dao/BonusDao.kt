package com.example.daftarcha.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.daftarcha.data.model.Bonus
import kotlinx.coroutines.flow.Flow

data class ProjectBonusTotal(val projectId: Int, val total: Double)
@Dao
interface BonusDao {

    // Аналог add_bonus()
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bonus: Bonus)

    // Аналог get_bonuses_for_project()
    @Query("SELECT * FROM bonuses WHERE projectId = :projectId ORDER BY date DESC")
    fun getBonusesForProject(projectId: Int): Flow<List<Bonus>>

    // Аналог get_total_bonuses()
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM bonuses WHERE projectId = :projectId")
    fun getTotalBonusesForProject(projectId: Int): Flow<Double>
    @Query("SELECT projectId, COALESCE(SUM(amount), 0.0) as total FROM bonuses GROUP BY projectId")
    fun getAllBonusesGroupedByProject(): Flow<List<ProjectBonusTotal>> // <-- Эта функция
    // ... (внутри interface BonusDao)

    // Сумма бонусов для СПИСКА проектов
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM bonuses WHERE projectId IN (:projectIds)")
    suspend fun getBonusesForProjectList(projectIds: List<Int>): Double

    @Query("SELECT * FROM bonuses")
    suspend fun getAllBonuses(): List<Bonus>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bonuses: List<Bonus>)
}