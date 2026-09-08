package com.example.daftarcha.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // Импорт всех иконок
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.daftarcha.ui.components.ConfirmDialog
import com.example.daftarcha.viewmodel.FiredEmployeeDialog
import com.example.daftarcha.viewmodel.FiredEmployeesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterialCode")
@Composable
fun FiredEmployeesScreen(
    onBackClick: () -> Unit,
    viewModel: FiredEmployeesViewModel = hiltViewModel()
) {
    val employees by viewModel.firedEmployees.collectAsState()
    val dialogState by viewModel.dialogState.collectAsState()
    val selectedEmployee by viewModel.selectedEmployee.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Собиқ шериклар") },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Орқага") } }
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues).padding(8.dp)) {
            items(employees) { employee ->
                Card(modifier = Modifier.fillParentMaxWidth().padding(vertical = 4.dp)) {
                    ListItem(
                        headlineContent = { Text(employee.name) },
                        supportingContent = { Text(employee.phone ?: "Рақам йўқ") },
                        leadingContent = { Icon(Icons.Default.PersonOff, null) },
                        trailingContent = {
                            Row {
                                IconButton(
                                    onClick = { viewModel.openDialog(FiredEmployeeDialog.RESTORE, employee) },
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFF009900))
                                ) { Icon(Icons.Default.Restore, "Қайта тиклаш") }

                                IconButton(
                                    onClick = { viewModel.openDialog(FiredEmployeeDialog.DELETE, employee) },
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) { Icon(Icons.Default.DeleteForever, "Ўчириш") }
                            }
                        }
                    )
                }
            }
        }
    }

    // --- Диалоги ---
    when (dialogState) {
        FiredEmployeeDialog.RESTORE -> {
            ConfirmDialog(
                title = "Шерикни қайта тиклашми?",
                text = "'${selectedEmployee?.name}' умумий рўйхатга қайтарилади.",
                onConfirm = { viewModel.restoreSelectedEmployee() },
                onDismiss = { viewModel.dismissDialog() },
                confirmButtonText = "Қайта тиклаш"
            )
        }
        FiredEmployeeDialog.DELETE -> {
            ConfirmDialog(
                title = "БУТУНЛАЙ ЎЧИРИШМИ?",
                text = "Сиз ростдан ҳам '${selectedEmployee?.name}'ни БУТУНЛАЙ ЎЧИРМОҚЧИМИСИЗ?\n\nУ билан боғлиқ барча маълумотлар (давомат, тўловлар) БУТУНЛАЙ ЎЧИРИЛАДИ.",
                onConfirm = { viewModel.deleteSelectedEmployee() },
                onDismiss = { viewModel.dismissDialog() },
                confirmButtonText = "ЎЧИРИШ",
                confirmButtonColor = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            )
        }
        FiredEmployeeDialog.NONE -> {}
    }
}