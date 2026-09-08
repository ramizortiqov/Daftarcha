package com.example.daftarcha.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.daftarcha.data.dao.AppUserDao
import com.example.daftarcha.data.dao.EmployeeDao
import com.example.daftarcha.data.model.AppUser
import com.example.daftarcha.data.model.AuthUser
import com.example.daftarcha.data.model.Employee
import com.example.daftarcha.data.model.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appUserDao: AppUserDao,
    private val employeeDao: EmployeeDao
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("daftarcha_auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    suspend fun initialize() {
        seedInitialBrigadierIfEmpty()

        val savedLoginId = prefs.getString("saved_login_id", null)
        val savedRoleName = prefs.getString("saved_user_role", null)

        if (savedLoginId != null) {
            val user = appUserDao.getUserByLoginId(savedLoginId)
            if (user != null) {
                _currentUser.value = AuthUser(
                    id = user.loginId,
                    name = user.name,
                    role = user.role,
                    employeeId = user.employeeId,
                    phone = user.phone
                )
            } else {
                // If user was removed or not found
                clearSession()
            }
        }
        _isInitialized.value = true
    }

    private suspend fun seedInitialBrigadierIfEmpty() {
        val count = appUserDao.getUserCount()
        if (count == 0) {
            // Создаем начального Бригадира по умолчанию
            val defaultBrigadier = AppUser(
                loginId = "brigadier",
                password = "123",
                name = "Бригадир (Асосий)",
                role = UserRole.BRIGADIER,
                employeeId = null,
                phone = null
            )
            appUserDao.insert(defaultBrigadier)
        }
    }

    suspend fun login(loginId: String, password: String):Result<AuthUser> {
        val trimmedLogin = loginId.trim()
        val trimmedPass = password.trim()

        if (trimmedLogin.isEmpty() || trimmedPass.isEmpty()) {
            return Result.failure(IllegalArgumentException("ID ва паролни киритинг"))
        }

        val user = appUserDao.authenticate(trimmedLogin, trimmedPass)
            ?: return Result.failure(IllegalArgumentException("ID ёки парол нотўғри"))

        val authUser = AuthUser(
            id = user.loginId,
            name = user.name,
            role = user.role,
            employeeId = user.employeeId,
            phone = user.phone
        )

        _currentUser.value = authUser
        prefs.edit()
            .putString("saved_login_id", user.loginId)
            .putString("saved_user_role", user.role.name)
            .apply()

        return Result.success(authUser)
    }

    fun logout() {
        clearSession()
    }

    private fun clearSession() {
        _currentUser.value = null
        prefs.edit().clear().apply()
    }

    /**
     * Создает или обновляет учетную запись для сотрудника (ID и пароль)
     */
    suspend fun createOrUpdateEmployeeAccount(
        employeeId: Int,
        loginId: String,
        password: String,
        role: UserRole = UserRole.WORKER
    ): Result<Unit> {
        val employee = employeeDao.getEmployeeById(employeeId)
            ?: return Result.failure(IllegalArgumentException("Шерик топилмади"))

        val existingUserWithLogin = appUserDao.getUserByLoginId(loginId.trim())
        if (existingUserWithLogin != null && existingUserWithLogin.employeeId != employeeId) {
            return Result.failure(IllegalArgumentException("Бу ID ('$loginId') аллақачон банд"))
        }

        // Check if employee already had another loginId
        val existingAccountForEmp = appUserDao.getUserByEmployeeId(employeeId)
        if (existingAccountForEmp != null && existingAccountForEmp.loginId != loginId.trim()) {
            appUserDao.deleteUser(existingAccountForEmp.loginId)
        }

        val appUser = AppUser(
            loginId = loginId.trim(),
            password = password.trim(),
            name = employee.name,
            role = role,
            employeeId = employee.id,
            phone = employee.phone
        )
        appUserDao.insert(appUser)

        return Result.success(Unit)
    }

    suspend fun setUserRole(loginId: String, role: UserRole): Result<Unit> {
        val user = appUserDao.getUserByLoginId(loginId)
            ?: return Result.failure(IllegalArgumentException("Фойдаланувчи топилмади"))

        appUserDao.updateUserRole(loginId, role)

        // If current user modified their own role (or another logged in)
        if (_currentUser.value?.id == loginId) {
            _currentUser.value = _currentUser.value?.copy(role = role)
            prefs.edit().putString("saved_user_role", role.name).apply()
        }
        return Result.success(Unit)
    }

    suspend fun deleteEmployeeAccount(employeeId: Int) {
        appUserDao.deleteByEmployeeId(employeeId)
    }
}
