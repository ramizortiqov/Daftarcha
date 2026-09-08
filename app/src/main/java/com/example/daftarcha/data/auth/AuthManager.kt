package com.example.daftarcha.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.daftarcha.data.dao.AppUserDao
import com.example.daftarcha.data.dao.EmployeeDao
import com.example.daftarcha.data.dao.ProjectDao
import com.example.daftarcha.data.model.AppUser
import com.example.daftarcha.data.model.AuthUser
import com.example.daftarcha.data.model.Employee
import com.example.daftarcha.data.model.UserRole
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result -> continuation.resume(result) }
    addOnFailureListener { exception -> continuation.resumeWithException(exception) }
    addOnCanceledListener { continuation.cancel() }
}

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appUserDao: AppUserDao,
    private val employeeDao: EmployeeDao,
    private val projectDao: ProjectDao,
    private val firestore: FirebaseFirestore
) {
    private val TAG = "AuthManager"
    private val prefs: SharedPreferences =
        context.getSharedPreferences("daftarcha_auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    suspend fun initialize(): Unit = withContext(Dispatchers.IO) {
        try {
            cleanupDefaultDummyBrigadierIfPresent()

            val savedLoginId = prefs.getString("saved_login_id", null)

            if (savedLoginId != null) {
                var user = appUserDao.getUserByLoginId(savedLoginId)
                if (user == null) {
                    // Try to restore user profile from Firestore if missing locally
                    user = fetchUserFromFirestore(savedLoginId)
                    if (user != null) {
                        appUserDao.insert(user)
                    }
                }

                if (user != null) {
                    val effectiveBId = if (user.role == UserRole.BRIGADIER) {
                        user.loginId
                    } else {
                        user.brigadierId.ifBlank {
                            prefs.getString("saved_brigadier_id", "") ?: ""
                        }
                    }

                    _currentUser.value = AuthUser(
                        id = user.loginId,
                        name = user.name,
                        role = user.role,
                        employeeId = user.employeeId,
                        phone = user.phone,
                        brigadierId = effectiveBId
                    )

                    // Auto-assign existing unassigned legacy data to this brigadier
                    if (user.role == UserRole.BRIGADIER) {
                        try {
                            employeeDao.assignUnassignedEmployeesToBrigadier(user.loginId)
                            projectDao.assignUnassignedProjectsToBrigadier(user.loginId)
                        } catch (e: Exception) {
                            Log.w(TAG, "Legacy migration error: ${e.message}")
                        }
                    }
                    Unit
                } else {
                    clearSession()
                }
            }
            Unit
        } catch (e: Exception) {
            Log.e(TAG, "Initialization failed: ${e.message}", e)
        } finally {
            _isInitialized.value = true
        }
    }

    private suspend fun cleanupDefaultDummyBrigadierIfPresent() {
        try {
            val oldDefault = appUserDao.getUserByLoginId("brigadier")
            if (oldDefault != null && oldDefault.name == "Бригадир (Асосий)" && oldDefault.employeeId == null) {
                appUserDao.deleteUser("brigadier")
                if (prefs.getString("saved_login_id", null) == "brigadier") {
                    clearSession()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cleanup default brigadier warning: ${e.message}")
        }
    }

    /**
     * Регистрация бригадира:
     * Бригадир вводит имя и пароль (и опционально логин ID),
     * автоматически добавляется в шерики (employees) и учетные записи (app_users),
     * сразу авторизуется и сохраняет сессию.
     */
    suspend fun registerBrigadier(
        name: String,
        loginId: String?,
        password: String,
        phone: String? = null
    ): Result<AuthUser> = withContext(Dispatchers.IO) {
        try {
            val trimmedName = name.trim()
            val trimmedPass = password.trim()
            val trimmedPhone = phone?.trim().takeIf { !it.isNullOrBlank() }

            if (trimmedName.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Бригадир исмини киритинг"))
            }
            if (trimmedPass.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Паролни киритинг"))
            }

            // Вычисляем логин ID
            val effectiveLoginId: String = if (!loginId.isNullOrBlank()) {
                loginId.trim()
            } else {
                val sanitized = trimmedName.lowercase()
                    .replace("[^a-z0-9а-яёўқғҳ]".toRegex(), "")
                    .take(15)
                if (sanitized.isNotEmpty()) sanitized else "brigadier"
            }

            // Проверяем, не занят ли логин
            var finalLoginId: String = effectiveLoginId
            var counter = 1
            while (appUserDao.getUserByLoginId(finalLoginId) != null || fetchUserFromFirestore(finalLoginId) != null) {
                if (!loginId.isNullOrBlank()) {
                    return@withContext Result.failure(IllegalArgumentException("Бу ID ('$loginId') аллақачон банд. Бошқа ID танланг."))
                }
                finalLoginId = "${effectiveLoginId}_$counter"
                counter++
            }

            // 1. Создаем шерика (employee), привязанного к этому бригадиру
            val employee = Employee(
                name = trimmedName,
                phone = trimmedPhone,
                brigadierId = finalLoginId
            )
            val employeeId = employeeDao.insert(employee).toInt()

            // 2. Создаем пользователя бригадира (app_users)
            val appUser = AppUser(
                loginId = finalLoginId,
                password = trimmedPass,
                name = trimmedName,
                role = UserRole.BRIGADIER,
                employeeId = employeeId,
                phone = trimmedPhone,
                brigadierId = finalLoginId
            )
            appUserDao.insert(appUser)

            // Сохраняем в облако Firestore (глобально для входа + в подколлекцию бригадира)
            uploadUserToCloud(appUser)

            val authUser = AuthUser(
                id = appUser.loginId,
                name = appUser.name,
                role = appUser.role,
                employeeId = employeeId,
                phone = appUser.phone,
                brigadierId = finalLoginId
            )

            // Запоминаем текущего пользователя и сохраняем сессию
            _currentUser.value = authUser
            prefs.edit()
                .putString("saved_login_id", appUser.loginId)
                .putString("saved_user_role", appUser.role.name)
                .putString("saved_brigadier_id", finalLoginId)
                .apply()

            Result.success(authUser)
        } catch (e: Exception) {
            Log.e(TAG, "Registration error", e)
            Result.failure(e)
        }
    }

    suspend fun login(loginId: String, password: String): Result<AuthUser> = withContext(Dispatchers.IO) {
        try {
            val trimmedLogin = loginId.trim()
            val trimmedPass = password.trim()

            if (trimmedLogin.isEmpty() || trimmedPass.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("ID (ёки исм) ва паролни киритинг"))
            }

            var user = appUserDao.authenticate(trimmedLogin, trimmedPass)

            // If not found locally, try to authenticate via Firestore
            if (user == null) {
                val cloudUser = fetchUserFromFirestore(trimmedLogin)
                if (cloudUser != null && cloudUser.password == trimmedPass) {
                    appUserDao.insert(cloudUser)
                    user = cloudUser
                }
            }

            if (user == null) {
                return@withContext Result.failure(IllegalArgumentException("ID/исм ёки парол нотўғри"))
            }

            val effectiveBId = if (user.role == UserRole.BRIGADIER) {
                user.loginId
            } else {
                user.brigadierId.ifBlank { user.loginId }
            }

            // If this is a brigadier logging in, assign any legacy unassigned projects/employees
            if (user.role == UserRole.BRIGADIER) {
                try {
                    employeeDao.assignUnassignedEmployeesToBrigadier(user.loginId)
                    projectDao.assignUnassignedProjectsToBrigadier(user.loginId)
                } catch (e: Exception) {
                    Log.w(TAG, "Legacy migration warning: ${e.message}")
                }
            }

            val authUser = AuthUser(
                id = user.loginId,
                name = user.name,
                role = user.role,
                employeeId = user.employeeId,
                phone = user.phone,
                brigadierId = effectiveBId
            )

            _currentUser.value = authUser
            prefs.edit()
                .putString("saved_login_id", user.loginId)
                .putString("saved_user_role", user.role.name)
                .putString("saved_brigadier_id", effectiveBId)
                .apply()

            Result.success(authUser)
        } catch (e: Exception) {
            Log.e(TAG, "Login error", e)
            Result.failure(e)
        }
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
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val employee = employeeDao.getEmployeeById(employeeId)
                ?: return@withContext Result.failure(IllegalArgumentException("Шерик топилмади"))

            val existingUserWithLogin = appUserDao.getUserByLoginId(loginId.trim())
            if (existingUserWithLogin != null && existingUserWithLogin.employeeId != employeeId) {
                return@withContext Result.failure(IllegalArgumentException("Бу ID ('$loginId') аллақачон банд"))
            }

            // Check if employee already had another loginId
            val existingAccountForEmp = appUserDao.getUserByEmployeeId(employeeId)
            if (existingAccountForEmp != null && existingAccountForEmp.loginId != loginId.trim()) {
                appUserDao.deleteUser(existingAccountForEmp.loginId)
            }

            val currentBrigadierId = _currentUser.value?.effectiveBrigadierId ?: ""

            val appUser = AppUser(
                loginId = loginId.trim(),
                password = password.trim(),
                name = employee.name,
                role = role,
                employeeId = employee.id,
                phone = employee.phone,
                brigadierId = currentBrigadierId
            )
            appUserDao.insert(appUser)

            uploadUserToCloud(appUser)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Account creation error", e)
            Result.failure(e)
        }
    }

    suspend fun setUserRole(loginId: String, role: UserRole): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = appUserDao.getUserByLoginId(loginId)
                ?: return@withContext Result.failure(IllegalArgumentException("Фойдаланувчи топилмади"))

            appUserDao.updateUserRole(loginId, role)

            if (_currentUser.value?.id == loginId) {
                _currentUser.value = _currentUser.value?.copy(role = role)
                prefs.edit().putString("saved_user_role", role.name).apply()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Set role error", e)
            Result.failure(e)
        }
    }

    suspend fun deleteEmployeeAccount(employeeId: Int): Unit = withContext(Dispatchers.IO) {
        try {
            val user = appUserDao.getUserByEmployeeId(employeeId)
            if (user != null) {
                appUserDao.deleteByEmployeeId(employeeId)
                deleteUserFromCloud(user.loginId, user.brigadierId)
            }
            Unit
        } catch (e: Exception) {
            Log.w(TAG, "Delete account warning: ${e.message}")
        }
    }

    private suspend fun fetchUserFromFirestore(loginId: String): AppUser? {
        return try {
            val doc = firestore.collection("app_users").document(loginId).get().await()
            if (doc.exists()) {
                val password = doc.getString("password") ?: return null
                val name = doc.getString("name") ?: loginId
                val roleStr = doc.getString("role") ?: "WORKER"
                val role = try { UserRole.valueOf(roleStr) } catch (e: Exception) { UserRole.WORKER }
                val empId = doc.getLong("employeeId")?.toInt()
                val phone = doc.getString("phone")
                val brigadierId = doc.getString("brigadierId") ?: ""
                AppUser(
                    loginId = loginId,
                    password = password,
                    name = name,
                    role = role,
                    employeeId = empId,
                    phone = phone,
                    brigadierId = brigadierId
                )
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Firestore fetchUser warning: ${e.message}")
            null
        }
    }

    private suspend fun uploadUserToCloud(user: AppUser) {
        try {
            val map = hashMapOf(
                "loginId" to user.loginId,
                "password" to user.password,
                "name" to user.name,
                "role" to user.role.name,
                "employeeId" to user.employeeId,
                "phone" to user.phone,
                "brigadierId" to user.brigadierId,
                "updatedAt" to System.currentTimeMillis()
            )
            // Global directory for cross-device authentication
            firestore.collection("app_users").document(user.loginId).set(map, SetOptions.merge()).await()

            // Also isolate in brigadier namespace if brigadierId is specified
            if (user.brigadierId.isNotBlank()) {
                firestore.collection("brigadiers")
                    .document(user.brigadierId)
                    .collection("app_users")
                    .document(user.loginId)
                    .set(map, SetOptions.merge())
                    .await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Upload user to cloud warning: ${e.message}")
        }
    }

    private suspend fun deleteUserFromCloud(loginId: String, brigadierId: String) {
        try {
            firestore.collection("app_users").document(loginId).delete().await()
            if (brigadierId.isNotBlank()) {
                firestore.collection("brigadiers")
                    .document(brigadierId)
                    .collection("app_users")
                    .document(loginId)
                    .delete()
                    .await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Delete user from cloud warning: ${e.message}")
        }
    }
}
