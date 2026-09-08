package com.example.daftarcha.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.daftarcha.data.model.UserRole
import com.example.daftarcha.viewmodel.CalculatorViewModel
import com.example.daftarcha.viewmodel.HomeViewModel

sealed class Screen(val route: String, val title: String? = null, val icon: ImageVector? = null) {
    object Dashboard : Screen("dashboard", "Бош сахифа", Icons.Default.Dashboard)
    object Projects : Screen("projects", "Ишлар", Icons.Default.BusinessCenter)
    object Employees : Screen("employees", "Шериклар", Icons.Default.Group)
    object Calculator : Screen("calculator", "Хисоб китоб", Icons.Default.Calculate)

    object ProjectDetail : Screen("projectDetail/{projectId}")
    object EmployeeDetail : Screen("employeeDetail/{employeeId}")
    object ProjectHistory : Screen("projectHistory/{projectId}")

    object CalculationResult : Screen("calculationResult")
    object Archive : Screen("archive")
    object FiredEmployees : Screen("firedEmployees")
    object UserManagement : Screen("userManagement")
}

private val navItems = listOf(
    Screen.Dashboard,
    Screen.Projects,
    Screen.Employees,
    Screen.Calculator,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onShowAddProjectDialog: () -> Unit,
    onShowAddEmployeeDialog: () -> Unit,
    onLogout: () -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val currentUser by homeViewModel.currentUser.collectAsState()
    val isBrigadier = currentUser?.role == UserRole.BRIGADIER
    var showLogoutConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Дафтарча")
                        currentUser?.let { user ->
                            val roleName = when (user.role) {
                                UserRole.BRIGADIER -> "Бригадир"
                                UserRole.ADMIN -> "Админ"
                                UserRole.WORKER -> "Шерик"
                            }
                            Text(
                                text = "${user.name} ($roleName)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Only Brigadier can open User & Role management
                    if (isBrigadier) {
                        IconButton(onClick = { navController.navigate(Screen.UserManagement.route) }) {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = "Фойдаланувчилар ва роллар",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Logout button
                    IconButton(onClick = { showLogoutConfirm = true }) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Тизимдан чиқиш",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                navItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon!!, contentDescription = screen.title) },
                        label = { Text(screen.title!!) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding),
            route = "mainGraph"
        ) {
            composable(Screen.Dashboard.route) {
                DashboardTab(
                    navController = navController,
                    onAddProjectClick = onShowAddProjectDialog,
                    onAddEmployeeClick = onShowAddEmployeeDialog,
                    onProjectsCardClick = {
                        navController.navigate(Screen.Projects.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onEmployeesCardClick = {
                        navController.navigate(Screen.Employees.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(Screen.Projects.route) {
                ProjectsTab(
                    onProjectClick = { projectId ->
                        navController.navigate(
                            Screen.ProjectDetail.route.replace("{projectId}", "$projectId")
                        )
                    }
                )
            }

            composable(Screen.Employees.route) {
                EmployeesTab(
                    onEmployeeClick = { employeeId ->
                        navController.navigate(
                            Screen.EmployeeDetail.route.replace("{employeeId}", "$employeeId")
                        )
                    },
                    onNavigateToFiredEmployees = {
                        navController.navigate(Screen.FiredEmployees.route)
                    }
                )
            }

            composable(Screen.Calculator.route) { navBackStackEntry ->
                val parentEntry = remember(navBackStackEntry) {
                    navController.getBackStackEntry("mainGraph")
                }
                val calculatorViewModel: CalculatorViewModel = hiltViewModel(parentEntry)

                CalculatorTab(
                    navController = navController,
                    viewModel = calculatorViewModel
                )
            }

            composable(
                route = Screen.ProjectDetail.route,
                arguments = listOf(navArgument("projectId") { type = NavType.IntType })
            ) {
                ProjectScreen(
                    onBackClick = { navController.popBackStack() },
                    onEmployeeClick = { employeeId ->
                        navController.navigate(Screen.EmployeeDetail.route.replace("{employeeId}", "$employeeId"))
                    },
                    onHistoryClick = { projectId ->
                        navController.navigate(
                            Screen.ProjectHistory.route.replace("{projectId}", "$projectId")
                        )
                    }
                )
            }

            composable(
                route = Screen.EmployeeDetail.route,
                arguments = listOf(navArgument("employeeId") { type = NavType.IntType })
            ) {
                EmployeeDetailScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                Screen.ProjectHistory.route,
                arguments = listOf(navArgument("projectId") { type = NavType.IntType })
            ) {
                ProjectHistoryScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.CalculationResult.route) { navBackStackEntry ->
                val parentEntry = remember(navBackStackEntry) {
                    navController.getBackStackEntry("mainGraph")
                }
                val calculatorViewModel: CalculatorViewModel = hiltViewModel(parentEntry)

                CalculationResultScreen(
                    onBackClick = { navController.popBackStack() },
                    viewModel = calculatorViewModel
                )
            }

            composable(Screen.Archive.route) {
                ArchiveScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.FiredEmployees.route) {
                FiredEmployeesScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.UserManagement.route) {
                UserManagementScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Тизимдан чиқиш") },
            text = { Text("Ҳақиқатан ҳам тизимдан чиқмоқчимисиз?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        homeViewModel.logout()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Чиқиш")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Бекор қилиш")
                }
            }
        )
    }
}
