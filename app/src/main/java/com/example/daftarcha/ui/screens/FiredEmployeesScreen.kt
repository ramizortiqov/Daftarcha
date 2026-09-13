package com.example.daftarcha.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // Импорт всех иконок
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
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
    val ink = MaterialTheme.colorScheme.onSurface

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Собиқ шериклар") },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Орқага") } }
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            items(employees) { employee ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 13.dp)
                ) {
                    Text(employee.name, style = MaterialTheme.typography.titleMedium, color = ink)
                    Text(
                        employee.phone ?: "Рақам йўқ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Row(
                        modifier = Modifier.padding(top = 11.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "ҚАЙТА ТИКЛАШ",
                            style = MaterialTheme.typography.labelSmall,
                            color = ink,
                            modifier = Modifier
                                .border(BorderStroke(2.dp, ink), RectangleShape)
                                .clickable { viewModel.openDialog(FiredEmployeeDialog.RESTORE, employee) }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                        Text(
                            "БУТУНЛАЙ ЎЧИРИШ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .border(BorderStroke(2.dp, MaterialTheme.colorScheme.error), RectangleShape)
                                .clickable { viewModel.openDialog(FiredEmployeeDialog.DELETE, employee) }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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