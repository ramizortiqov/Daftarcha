package com.or.daftarcha

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.or.daftarcha.data.model.UserRole
import com.or.daftarcha.ui.components.AddDialog
import com.or.daftarcha.ui.screens.LoginScreen
import com.or.daftarcha.ui.screens.MainScreen
import com.or.daftarcha.ui.screens.WorkerDashboardScreen
import com.or.daftarcha.ui.theme.DaftarchaTheme
import com.or.daftarcha.viewmodel.AuthViewModel
import com.or.daftarcha.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DaftarchaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isAuthInitialized by authViewModel.isInitialized.collectAsState()
                    val currentUser by authViewModel.currentUser.collectAsState()

                    var showAddProjectDialog by remember { mutableStateOf(false) }
                    var showAddEmployeeDialog by remember { mutableStateOf(false) }

                    if (!isAuthInitialized) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (currentUser == null) {
                        // Экран входа по ID и паролю
                        LoginScreen(
                            viewModel = authViewModel,
                            onLoginSuccess = { /* State updates via currentUser */ }
                        )
                    } else if (currentUser?.role == UserRole.WORKER) {
                        // Обычный пользователь (шерик): доступ только по своим данным
                        // (количество рабочих дней, полученные деньги, общий расход)
                        WorkerDashboardScreen(
                            onLogout = { authViewModel.logout() }
                        )
                    } else {
                        // Бригадир (полный доступ) или Админ (доступ ко всему, кроме добавления новых сотрудников/проектов и назначения админов)
                        MainScreen(
                            onShowAddProjectDialog = {
                                if (currentUser?.role == UserRole.BRIGADIER) {
                                    showAddProjectDialog = true
                                }
                            },
                            onShowAddEmployeeDialog = {
                                if (currentUser?.role == UserRole.BRIGADIER) {
                                    showAddEmployeeDialog = true
                                }
                            },
                            onLogout = { authViewModel.logout() }
                        )

                        if (showAddProjectDialog) {
                            AddDialog(
                                title = "Янги иш",
                                nameLabel = "Иш номи",
                                showDateField = true,
                                onDismiss = { showAddProjectDialog = false },
                                onConfirm = { name, _, date ->
                                    homeViewModel.addProject(name, date)
                                    showAddProjectDialog = false
                                }
                            )
                        }

                        if (showAddEmployeeDialog) {
                            AddDialog(
                                title = "Янги шерик",
                                nameLabel = "Шерикнинг исми",
                                phoneLabel = "Телефон (ихтиёрий)",
                                onDismiss = { showAddEmployeeDialog = false },
                                onConfirm = { name, phone, _ ->
                                    homeViewModel.addEmployee(name, phone)
                                    showAddEmployeeDialog = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
