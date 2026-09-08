package com.example.daftarcha.data.dao

import androidx.room.*
import com.example.daftarcha.data.model.AppUser
import com.example.daftarcha.data.model.UserRole
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUserDao {

    @Upsert
    suspend fun insert(user: AppUser)

    @Update
    suspend fun update(user: AppUser)

    @Query("SELECT * FROM app_users WHERE LOWER(TRIM(loginId)) = LOWER(TRIM(:loginId)) LIMIT 1")
    suspend fun getUserByLoginId(loginId: String): AppUser?

    @Query("SELECT * FROM app_users WHERE (LOWER(TRIM(loginId)) = LOWER(TRIM(:loginOrName)) OR LOWER(TRIM(name)) = LOWER(TRIM(:loginOrName))) AND password = :password LIMIT 1")
    suspend fun authenticate(loginOrName: String, password: String): AppUser?

    @Query("SELECT * FROM app_users ORDER BY name ASC")
    fun getAllUsersFlow(): Flow<List<AppUser>>

    @Query("SELECT * FROM app_users")
    suspend fun getAllUsers(): List<AppUser>

    @Query("SELECT * FROM app_users WHERE employeeId = :employeeId LIMIT 1")
    suspend fun getUserByEmployeeId(employeeId: Int): AppUser?

    @Query("SELECT * FROM app_users WHERE employeeId = :employeeId LIMIT 1")
    fun getUserByEmployeeIdFlow(employeeId: Int): Flow<AppUser?>

    @Query("UPDATE app_users SET role = :newRole WHERE loginId = :loginId")
    suspend fun updateUserRole(loginId: String, newRole: UserRole)

    @Query("UPDATE app_users SET password = :newPassword WHERE loginId = :loginId")
    suspend fun updateUserPassword(loginId: String, newPassword: String)

    @Query("DELETE FROM app_users WHERE loginId = :loginId")
    suspend fun deleteUser(loginId: String)

    @Query("DELETE FROM app_users WHERE employeeId = :employeeId")
    suspend fun deleteByEmployeeId(employeeId: Int)

    @Query("SELECT COUNT(*) FROM app_users")
    suspend fun getUserCount(): Int

    @Upsert
    suspend fun insertAll(users: List<AppUser>)

    @Query("SELECT * FROM app_users WHERE (brigadierId = :brigadierId OR loginId = :brigadierId OR :brigadierId = '') ORDER BY name ASC")
    fun getUsersByBrigadierFlow(brigadierId: String): Flow<List<AppUser>>
}
