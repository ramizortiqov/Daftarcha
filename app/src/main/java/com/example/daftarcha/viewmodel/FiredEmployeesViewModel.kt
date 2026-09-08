package com.example.daftarcha.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daftarcha.data.auth.AuthManager
import com.example.daftarcha.data.dao.EmployeeDao
import com.example.daftarcha.data.model.Employee
import com.example.daftarcha.data.sync.FirestoreSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FiredEmployeeDialog { NONE, RESTORE, DELETE }

@HiltViewModel
class FiredEmployeesViewModel @Inject constructor(
    private val employeeDao: EmployeeDao,
    private val syncManager: FirestoreSyncManager,
    private val authManager: AuthManager
) : ViewModel() {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val firedEmployees = authManager.currentUser
        .flatMapLatest { user ->
            val bId = user?.effectiveBrigadierId ?: ""
            employeeDao.getFiredEmployeesForBrigadier(bId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _dialogState = MutableStateFlow(FiredEmployeeDialog.NONE)
    val dialogState = _dialogState.asStateFlow()
    private val _selectedEmployee = MutableStateFlow<Employee?>(null)
    val selectedEmployee = _selectedEmployee.asStateFlow()

    fun openDialog(dialog: FiredEmployeeDialog, employee: Employee) {
        _selectedEmployee.value = employee
        _dialogState.value = dialog
    }
    fun dismissDialog(){ _dialogState.value = FiredEmployeeDialog.NONE }

    fun restoreSelectedEmployee() {
        _selectedEmployee.value?.let { emp ->
            viewModelScope.launch { employeeDao.restoreEmployee(emp.id) }
            dismissDialog()
        }
    }
    fun deleteSelectedEmployee() {
        _selectedEmployee.value?.let { emp ->
            viewModelScope.launch {
                employeeDao.deleteEmployeeById(emp.id)
                syncManager.deleteEmployeeFromCloud(emp.id)
            }
            dismissDialog()
        }
    }
}