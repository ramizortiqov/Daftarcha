package com.or.daftarcha.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.or.daftarcha.data.model.UserRole
import com.or.daftarcha.data.sync.SyncStatus
import com.or.daftarcha.viewmodel.CalculatorViewModel
import com.or.daftarcha.viewmodel.HomeViewModel
import com.or.daftarcha.viewmodel.SyncViewModel

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
    homeViewModel: HomeViewModel = hiltViewModel(),
    syncViewModel: SyncViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val currentUser by homeViewModel.currentUser.collectAsState()
    val isBrigadier = currentUser?.role == UserRole.BRIGADIER
    val showWorkerEarningsAndDebt by homeViewModel.showWorkerEarningsAndDebt.collectAsState()
    val syncStatus by syncViewModel.syncStatus.collectAsState()
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showBrigadierMenuDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // Auto-sync with Firebase in background on start
    LaunchedEffect(Unit) {
        syncViewModel.triggerSync()
    }

    Scaffold(
        topBar = {
            val roleName = when (currentUser?.role) {
                UserRole.BRIGADIER -> "Усто"
                UserRole.ADMIN -> "Админ"
                UserRole.WORKER -> "Шерик"
                null -> null
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .padding(top = 10.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "ДАФТАРЧА",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        currentUser?.let { user ->
                            Text(
                                text = "${user.name}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (roleName != null) {
                            Text(
                                text = roleName.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground))
                                    .padding(horizontal = 6.dp, vertical = 5.dp)
                            )
                        }

                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                if (syncStatus is SyncStatus.InProgress) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = "Кўпроқ",
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                            DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Булут билан синхронизация") },
                                    leadingIcon = { Icon(Icons.Default.CloudSync, contentDescription = null) },
                                    enabled = syncStatus !is SyncStatus.InProgress,
                                    onClick = {
                                        showMoreMenu = false
                                        syncViewModel.triggerSync()
                                    }
                                )
                                if (isBrigadier) {
                                    DropdownMenuItem(
                                        text = { Text("Усто менюси") },
                                        leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
                                        onClick = {
                                            showMoreMenu = false
                                            showBrigadierMenuDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Фойдаланувчилар ва роллар") },
                                        leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
                                        onClick = {
                                            showMoreMenu = false
                                            navController.navigate(Screen.UserManagement.route)
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Тизимдан чиқиш", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Logout,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        showLogoutConfirm = true
                                    }
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.onBackground)
            }
        },
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .border(BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground))
            ) {
                navItems.forEach { screen ->
                    val selected = currentRoute == screen.route
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                BorderStroke(
                                    3.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent
                                )
                            )
                            .background(MaterialTheme.colorScheme.background)
                            .clickable {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                            .padding(top = 9.dp, bottom = 11.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            screen.icon!!,
                            contentDescription = screen.title,
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = screen.title!!.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RectangleShape
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

    if (showBrigadierMenuDialog) {
        AlertDialog(
            onDismissRequest = { showBrigadierMenuDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Усто менюси")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "Шерикларга пулни кўрсатиш",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (showWorkerEarningsAndDebt)
                                        "Ёқилган: оддий шериклар ишлаган пули ва қарзини кўра олади"
                                    else
                                        "Ўчирилган: оддий шериклардан ишланган пул ва қарз яширилган",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = showWorkerEarningsAndDebt,
                                onCheckedChange = { homeViewModel.setShowWorkerEarningsAndDebt(it) }
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            showBrigadierMenuDialog = false
                            navController.navigate(Screen.UserManagement.route)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RectangleShape
                    ) {
                        Icon(
                            Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Фойдаланувчилар ва роллар")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showBrigadierMenuDialog = false }, shape = RectangleShape) {
                    Text("Тайёр")
                }
            }
        )
    }
}
