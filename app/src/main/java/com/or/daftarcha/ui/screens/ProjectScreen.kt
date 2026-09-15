package com.or.daftarcha.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.or.daftarcha.ui.components.AddDialog
import com.or.daftarcha.ui.components.EmployeeCard
import com.or.daftarcha.ui.components.ProjectStatsCard
import com.or.daftarcha.viewmodel.ProjectDialog
import com.or.daftarcha.viewmodel.ProjectViewModel
import com.or.daftarcha.ui.components.AttendanceTable
import com.or.daftarcha.ui.components.SelectEmployeesDialog
import com.or.daftarcha.ui.components.EmployeeAmountSplitDialog
import com.or.daftarcha.ui.components.SplitMode
import com.or.daftarcha.ui.components.DatePickerConfirmDialog
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ButtonDefaults
import com.or.daftarcha.ui.components.ConfirmDialog

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
    val pendingSplit by viewModel.pendingSplit.collectAsState()
    var isEditMode by remember { mutableStateOf(false) }
    var showActionsMenu by remember { mutableStateOf(false) }
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
                    if (projectValue != null && !isEditMode) {
                        IconButton(onClick = { showActionsMenu = true }) {
                            Icon(Icons.Default.Menu, "Кўпроқ амаллар")
                        }
                        DropdownMenu(
                            expanded = showActionsMenu,
                            onDismissRequest = { showActionsMenu = false }
                        ) {
                            if (projectValue.endDate == null) {
                                DropdownMenuItem(
                                    text = { Text("Ишни якунлаш") },
                                    leadingIcon = { Icon(Icons.Default.CheckCircle, null, tint = Color.Green) },
                                    onClick = {
                                        showActionsMenu = false
                                        viewModel.openDialog(ProjectDialog.COMPLETE_PROJECT)
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Ишни қайта бошлаш") },
                                    leadingIcon = { Icon(Icons.Default.PlayCircle, null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        showActionsMenu = false
                                        viewModel.reopenProject()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Иш тарихи") },
                                leadingIcon = { Icon(Icons.Default.History, null) },
                                onClick = {
                                    showActionsMenu = false
                                    projectValue.id.let { id -> onHistoryClick(id) }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Шерик қўшиш") },
                                leadingIcon = { Icon(Icons.Default.PersonAdd, null) },
                                onClick = {
                                    showActionsMenu = false
                                    viewModel.openDialog(ProjectDialog.ADD_EMPLOYEE)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Харажат қўшиш") },
                                leadingIcon = { Icon(Icons.Default.Remove, null) },
                                onClick = {
                                    showActionsMenu = false
                                    viewModel.openDialog(ProjectDialog.ADD_EXPENSE)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Пул берди") },
                                leadingIcon = { Icon(Icons.Default.Add, null) },
                                onClick = {
                                    showActionsMenu = false
                                    viewModel.openDialog(ProjectDialog.ADD_BONUS)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Ишни таҳрирлаш") },
                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                                onClick = {
                                    showActionsMenu = false
                                    viewModel.openDialog(ProjectDialog.EDIT_PROJECT)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Архивга солиш", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Archive, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showActionsMenu = false
                                    viewModel.openDialog(ProjectDialog.ARCHIVE_PROJECT)
                                }
                            )
                        }
                    }
                }
            )
        }, floatingActionButton = {
            if (projectValue != null) {
                if (isEditMode) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FloatingActionButton(
                            onClick = {
                                viewModel.discardAttendanceChanges()
                                isEditMode = false
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(Icons.Default.Cancel, "Бекор қилиш")
                        }
                        FloatingActionButton(
                            onClick = {
                                viewModel.saveAttendanceChanges()
                                isEditMode = false
                            }
                        ) {
                            Icon(Icons.Default.Save, "Сақлаш")
                        }
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
                        Text(
                            if (employees.isEmpty()) "Лойиҳага шериклар бириктирилмаган. Шерик қўшиш учун юқоридаги '+' тугмасини босинг."
                            else "Давомат жадвали юкланмоқда...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                    viewModel.startExpenseSplit(amount, description)
                }
            )
        }

        ProjectDialog.ADD_EXPENSE_SPLIT -> {
            val split = pendingSplit
            if (split != null) {
                EmployeeAmountSplitDialog(
                    title = "Харажатни бўлиш",
                    totalAmount = split.amount,
                    employees = employees,
                    mode = SplitMode.EQUAL_SPLIT_SELECTABLE,
                    onDismiss = { viewModel.dismissDialog() },
                    onConfirm = { shares ->
                        viewModel.addExpense(shares, split.description)
                    }
                )
            }
        }

        ProjectDialog.ADD_BONUS -> {
            AddDialog(
                title = "Пул берди",
                nameLabel = "Суммаси (с)",
                phoneLabel = "Тавсиф (ихтиёрий)",
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { amountStr, description, _ ->
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    viewModel.startBonusSplit(amount, description)
                }
            )
        }

        ProjectDialog.ADD_BONUS_SPLIT -> {
            val split = pendingSplit
            if (split != null) {
                EmployeeAmountSplitDialog(
                    title = "Ким қанча олди?",
                    totalAmount = split.amount,
                    employees = employees,
                    mode = SplitMode.MANUAL,
                    onDismiss = { viewModel.dismissDialog() },
                    onConfirm = { shares ->
                        viewModel.confirmBonus(split.amount, split.description, shares)
                    }
                )
            }
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
        ProjectDialog.COMPLETE_PROJECT -> {
            DatePickerConfirmDialog(
                title = "Ишни якунлаш санаси",
                initialDate = null,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { date ->
                    viewModel.completeProject(date)
                }
            )
        }
        ProjectDialog.NONE -> {
        }
    }
}
