package com.example.daftarcha.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daftarcha.data.auth.AuthManager
import com.example.daftarcha.data.dao.*
import com.example.daftarcha.data.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val appUserDao: AppUserDao,
    private val employeeDao: EmployeeDao
) : ViewModel() {

    val currentUser: StateFlow<AuthUser?> = authManager.currentUser
    val isInitialized: StateFlow<Boolean> = authManager.isInitialized

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allAppUsers: StateFlow<List<AppUser>> = currentUser
        .flatMapLatest { user ->
            val bId = user?.effectiveBrigadierId ?: ""
            appUserDao.getUsersByBrigadierFlow(bId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            authManager.initialize()
        }
    }

    fun login(loginId: String, pass: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)
            val result = authManager.login(loginId, pass)
            if (result.isSuccess) {
                _uiState.value = LoginUiState(isSuccess = true)
            } else {
                _uiState.value = LoginUiState(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Киришда хатолик"
                )
            }
        }
    }

    fun registerBrigadier(
        name: String,
        loginId: String?,
        pass: String,
        phone: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)
            val result = authManager.registerBrigadier(name, loginId, pass, phone)
            if (result.isSuccess) {
                _uiState.value = LoginUiState(isSuccess = true)
            } else {
                _uiState.value = LoginUiState(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Рўйхатдан ўтишда хатолик"
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun logout() {
        authManager.logout()
        _uiState.value = LoginUiState()
    }

    fun setUserRole(loginId: String, newRole: UserRole, onComplete: (Boolean, String?) -> Unit) {
        val current = currentUser.value
        if (current?.role != UserRole.BRIGADIER) {
            onComplete(false, "Фақат Бригадир админлик даражасини ўзгартира олади")
            return
        }

        viewModelScope.launch {
            val res = authManager.setUserRole(loginId, newRole)
            if (res.isSuccess) {
                onComplete(true, null)
            } else {
                onComplete(false, res.exceptionOrNull()?.message)
            }
        }
    }

    fun setEmployeeCredentials(
        employeeId: Int,
        loginId: String,
        pass: String,
        role: UserRole,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val current = currentUser.value
        // Only BRIGADIER can set roles other than WORKER
        val effectiveRole = if (current?.role == UserRole.BRIGADIER) role else UserRole.WORKER

        viewModelScope.launch {
            val res = authManager.createOrUpdateEmployeeAccount(
                employeeId = employeeId,
                loginId = loginId,
                password = pass,
                role = effectiveRole
            )
            if (res.isSuccess) {
                onComplete(true, null)
            } else {
                onComplete(false, res.exceptionOrNull()?.message)
            }
        }
    }

    suspend fun getAccountForEmployee(employeeId: Int): AppUser? {
        return appUserDao.getUserByEmployeeId(employeeId)
    }
}
