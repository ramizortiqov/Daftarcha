package com.example.daftarcha.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.daftarcha.ui.components.AddDialog
import com.example.daftarcha.ui.components.EmployeeCard
import com.example.daftarcha.ui.components.ProjectStatsCard
import com.example.daftarcha.viewmodel.ProjectDialog
import com.example.daftarcha.viewmodel.ProjectViewModel
import com.example.daftarcha.ui.components.AttendanceTable
import com.example.daftarcha.ui.components.SelectEmployeesDialog
import androidx.compose.material.icons.filled.Edit // <-- НОВЫЙ IMPORT
import androidx.compose.material.icons.filled.Save // <-- НОВЫЙ IMPORT
import androidx.compose.material.icons.filled.Cancel // <-- НОВЫЙ IMPORT
import androidx.compose.runtime.*
import androidx.compose.material.icons.filled.CheckCircle // Новые иконки
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.FloatingActionButton // <-- ADD THIS LINE
import androidx.compose.material3.FabPosition // <-- For positioning
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.foundation.layout.Spacer // <-- For spacing between FABs
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Archive // <-- 1. ДОБАВЬТЕ ИКОНКУ
import androidx.compose.material3.ButtonDefaults
import com.example.daftarcha.ui.components.ConfirmDialog

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ProjectScreen(
    onBackClick: () -> Unit,
    onEmployeeClick: (Int) -> Unit,
    onHistoryClick: (Int) -> Unit,// <-- 1. ПАРАМЕТР ДОБАВЛЕН
    viewModel: ProjectViewModel = hiltViewModel()
) {
    val project by viewModel.project.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val employees by viewModel.employees.collectAsState()
    val currentDialog by viewModel.dialogState.collectAsState()
    val table by viewModel.attendanceTable.collectAsState()
    val availableEmployees by viewModel.availableEmployees.collectAsState()
    var isEditMode by remember { mutableStateOf(false) }
    val localChanges = viewModel.localAttendanceChanges as Map<Pair<Int, String>, Boolean>
    val projectValue = project

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project?.name ?: "Юкланмоқда...") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Орқага")
                    }
                },
                actions = {
                    if (projectValue != null) {
                        if (projectValue.endDate == null) {
                            IconButton(onClick = { viewModel.completeProject() }) {
                                Icon(Icons.Default.CheckCircle, "Ишни якунлаш", tint = Color.Green)
                            }
                        } else {
                            IconButton(onClick = { viewModel.reopenProject() }) {
                                Icon(Icons.Default.PlayCircle, "Ишни қайта бошлаш", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    if (!isEditMode) { 
                        IconButton(onClick = {
                            projectValue?.id?.let { id ->
                                onHistoryClick(id)
                            }
                        }) {
                            Icon(Icons.Default.History, "Иш тарихи")
                        }
                    }

                    if (!isEditMode) {
                        IconButton(onClick = { viewModel.openDialog(ProjectDialog.ADD_EMPLOYEE) }) {
                            Icon(Icons.Default.PersonAdd, "Шерик қўшиш")
                        }
                        IconButton(onClick = { viewModel.openDialog(ProjectDialog.ADD_EXPENSE) }) {
                            Icon(Icons.Default.Remove, "Харажат қўшиш")
                        }
                        IconButton(onClick = { viewModel.openDialog(ProjectDialog.ADD_BONUS) }) {
                            Icon(Icons.Default.Add, "Пул берди")
                        }
                    }
                    if (projectValue != null && !isEditMode) {
                        IconButton(onClick = {
                            viewModel.openDialog(ProjectDialog.ARCHIVE_PROJECT)
                        }) {
                            Icon(Icons.Default.Archive, "Архивга солиш", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (projectValue != null) {
                        if (isEditMode) {
                        } else {
                            IconButton(onClick = {
                                viewModel.openDialog(ProjectDialog.EDIT_PROJECT)
                            }) {
                                Icon(Icons.Default.Edit, "Ишни таҳрирлаш")
                            }
                        }
                    }
                }
            )
        }, floatingActionButton = {
            if (projectValue != null) {
                if (isEditMode) {
                    FloatingActionButton(
                        onClick = {
                            viewModel.discardAttendanceChanges()
                            isEditMode = false
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(Icons.Default.Cancel, "Бекор қилиш")
                    }
                    Spacer(modifier = Modifier.width(8.dp)) 
                    FloatingActionButton(
                        onClick = {
                            viewModel.saveAttendanceChanges()
                            isEditMode = false
                        }
                    ) {
                        Icon(Icons.Default.Save, "Сақлаш")
                    }
                } else {
                    if (projectValue.endDate == null) {
                        FloatingActionButton(onClick = { isEditMode = true }) {
                            Icon(Icons.Default.Edit, "Давоматни таҳрирлаш")
                        }
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        if (project == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {

                ProjectStatsCard(stats = stats)

//                Text(
//                    text = "Шериклар (${employees.size})",
//                    style = MaterialTheme.typography.headlineSmall,
//                    fontWeight = FontWeight.Bold,
//                    modifier = Modifier.padding(start = 16.dp, top = 16.dp)
//                )
//
//                Column(modifier = Modifier.fillMaxWidth()) {
//                    employees.forEach { employee ->
//                        EmployeeCard(
//                            employeeName = employee.name,
//                            employeePhone = employee.phone ?: "Телефон кўрсатилмаган",
//                            balance = 0.0, // TODO: Баланс нужно считать
//                            onClick = { onEmployeeClick(employee.id) }
//                        )
//                    }
//                }
                Text(
                    text = "Давомат журнали",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp)
                )
                if (employees.isNotEmpty() && table.dates.isNotEmpty()) {
                    AttendanceTable(
                        employees = employees,
                        table = table,
                        onMarkClick = { employeeId, date, isPresent ->
                            viewModel.toggleLocalAttendance(
                                employeeId,
                                date,
                                isPresent
                            ) 
                        },
                        onEmployeeNameClick = { employeeId ->
                            if (!isEditMode) onEmployeeClick(employeeId)
                        },
                        isEditing = isEditMode,
                        localChanges = localChanges
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Кўрсатиш учун маълумотлар йўқ.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    } 

    when (currentDialog) {
        ProjectDialog.ADD_EXPENSE -> {
            AddDialog(
                title = "Харажат қўшиш",
                nameLabel = "Суммаси (с)",
                phoneLabel = "Тавсиф (ихтиёрий)",
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { amountStr, description, _ ->
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    viewModel.addExpense(amount, description)
                }
            )
        }

        ProjectDialog.ADD_BONUS -> {
            AddDialog(
                title = "Пул берди",
                nameLabel = "Суммаси (с)",
                phoneLabel = "Тавсиф (ихтиёрий)",
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { amountStr, description, _ ->
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    viewModel.addBonus(amount, description)
                }
            )
        }

        ProjectDialog.ADD_EMPLOYEE -> {
            SelectEmployeesDialog(
                availableEmployees = availableEmployees, 
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { selectedIds ->
                    viewModel.addEmployeesToProject(selectedIds)
                }
            )
        }
        ProjectDialog.ARCHIVE_PROJECT -> {
            ConfirmDialog(
                title = "Ишни архивга солишми?",
                text = "'${projectValue?.name}' иши умумий рўйхатдан яширилади. Уни кейинчалик тиклаш мумкин (функция ишлаб чиқилмоқда).",
                onConfirm = {
                    viewModel.archiveProject()
                    onBackClick()
                },
                onDismiss = { viewModel.dismissDialog() },
                confirmButtonText = "Ҳа, архивга солиш",
                confirmButtonColor = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            )
        }
        ProjectDialog.EDIT_PROJECT -> {
            if (projectValue != null) {
                AddDialog(
                    title = "Ишни таҳрирлаш",
                    nameLabel = "Иш номи",
                    showDateField = true, 
                    initialName = projectValue.name, 
                    initialDate = projectValue.startDate, 
                    onDismiss = { viewModel.dismissDialog() },
                    onConfirm = { name, _, date ->
                        viewModel.updateProject(name, date)
                    }
                )
            }
        }
        ProjectDialog.NONE -> {
        }
    }
}
