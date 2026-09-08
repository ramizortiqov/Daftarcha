package com.example.daftarcha.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.remember
import androidx.compose.material.icons.filled.History
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.daftarcha.viewmodel.CalculatorViewModel
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.IconButton

// 1. --- ИЗМЕНЕНИЕ: Обновляем sealed class ---
// Убрали title и icon у ProjectDetail, т.к. он не в нижней панели
sealed class Screen(val route: String, val title: String? = null, val icon: ImageVector? = null) {
    object Dashboard : Screen("dashboard", "Бош сахифа", Icons.Default.Dashboard)
    object Projects : Screen("projects", "Ишлар", Icons.Default.BusinessCenter)
    object Employees : Screen("employees", "Шериклар", Icons.Default.Group)
    object Calculator : Screen("calculator", "Хисоб китоб", Icons.Default.Calculate)

    // Новый экран. {projectId} - это аргумент, который мы будем передавать
    object ProjectDetail : Screen("projectDetail/{projectId}")
    object EmployeeDetail : Screen("employeeDetail/{employeeId}")
    object ProjectHistory : Screen("projectHistory/{projectId}")

    object CalculationResult : Screen("calculationResult")
    object Archive : Screen("archive")
    object FiredEmployees : Screen("firedEmployees")
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
    onShowAddEmployeeDialog: () -> Unit
) {
    // Теперь NavController нужен для навигации *между* экранами,
    // поэтому выносим его в переменную
    val navController = rememberNavController()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Дафтарча") },
                // --- 2. ДОБАВЬТЕ КНОПКУ АРХИВА В TopAppBar ---
                actions = {

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
        },
        floatingActionButton = {
            // ... (код FAB без изменений) ...
        }
    ) { innerPadding ->

        // 2. --- ИЗМЕНЕНИЕ: Обновляем NavHost ---
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding),
            route = "mainGraph"
        ) {

            composable(Screen.Dashboard.route) {
                    navBackStackEntry ->
                // (Этот код нужен для общего ViewModel, если он у вас есть)
                // val parentEntry = remember(navBackStackEntry) {
                //     navController.getBackStackEntry("mainGraph")
                // }
                // val calculatorViewModel: CalculatorViewModel = hiltViewModel(parentEntry)

                DashboardTab(
                    // --- 2.1 ПЕРЕДАЕМ NAVCONTROLLER ---
                    navController = navController,
                    onAddProjectClick = onShowAddProjectDialog,
                    onAddEmployeeClick = onShowAddEmployeeDialog,
                    onProjectsCardClick = {
                        // Переходим на вкладку "Проекты"
                        navController.navigate(Screen.Projects.route) {
                            // Эта логика имитирует нажатие на нижнюю панель
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onEmployeesCardClick = {
                        // Переходим на вкладку "Сотрудники"
                        navController.navigate(Screen.Employees.route) {
                            // Эта логика имитирует нажатие на нижнюю панель
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    // (Если вы используете общий ViewModel, передайте его здесь)
                    // viewModel = hiltViewModel(parentEntry)
                )
            }

            composable(Screen.Projects.route) {
                ProjectsTab(
                    // 3. --- ИЗМЕНЕНИЕ: Обновляем onClick ---
                    // Теперь при клике мы переходим на новый экран
                    onProjectClick = { projectId ->
                        // Мы заменяем {projectId} на реальный ID
                        navController.navigate(
                            Screen.ProjectDetail.route.replace("{projectId}", "$projectId")
                        )
                    }
                )
            }

            composable(Screen.Employees.route) {
                EmployeesTab(
                    // Передаем лямбду для навигации к деталям
                    onEmployeeClick = { employeeId ->
                        navController.navigate(
                            Screen.EmployeeDetail.route.replace("{employeeId}", "$employeeId")
                        )
                    },
                    // Передаем лямбду для навигации к уволенным
                    onNavigateToFiredEmployees = {
                        navController.navigate(Screen.FiredEmployees.route)
                    }
                )
            }

            composable(Screen.Calculator.route) { navBackStackEntry ->

                // Вызов 'getBackStackEntry' должен быть ВНУТRI 'remember'
                val parentEntry = remember(navBackStackEntry) {
                    navController.getBackStackEntry("mainGraph")
                }

                // Hilt ViewModel привязывается к 'parentEntry' (т.е. к 'mainGraph')
                val calculatorViewModel: CalculatorViewModel = hiltViewModel(parentEntry)

                CalculatorTab(
                    navController = navController,
                    viewModel = calculatorViewModel
                )
            }

            // --- НОВЫЙ БЛОК: Экран "Детали Проекта" ---
            composable(
                route = Screen.ProjectDetail.route,
                arguments = listOf(navArgument("projectId") { type = NavType.IntType })
            ) { backStackEntry ->
                ProjectScreen(
                    onBackClick = { navController.popBackStack() },
                    onEmployeeClick = { employeeId ->
                        navController.navigate(Screen.EmployeeDetail.route.replace("{employeeId}", "$employeeId"))
                    },
                    // --- ДОБАВЬТЕ ЭТОТ ПАРАМЕТР ---
                    onHistoryClick = { projectId -> // <-- Теперь ProjectScreen вернет нам ID
                        navController.navigate(
                            Screen.ProjectHistory.route.replace("{projectId}", "$projectId") // Передаем ID
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

                // Точно такой же вызов 'remember'
                val parentEntry = remember(navBackStackEntry) {
                    navController.getBackStackEntry("mainGraph")
                }

                // Hilt вернет ТОТ ЖЕ ViewModel, привязанный к 'mainGraph'
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

        }
    }
}
