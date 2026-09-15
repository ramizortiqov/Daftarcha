package com.or.daftarcha.ui.screens

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.or.daftarcha.ui.components.ConfirmDialog
import com.or.daftarcha.viewmodel.ArchiveDialog
import com.or.daftarcha.viewmodel.ArchiveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterialCode")
@Composable
fun ArchiveScreen(
    onBackClick: () -> Unit,
    viewModel: ArchiveViewModel = hiltViewModel()
) {
    val projects by viewModel.archivedProjects.collectAsState()
    val dialogState by viewModel.dialogState.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()
    val ink = MaterialTheme.colorScheme.onSurface

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Архив ишлар") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Орқага")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            items(projects) { project ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 13.dp)
                ) {
                    Text(project.name, style = MaterialTheme.typography.titleMedium, color = ink)
                    Text(
                        project.startDate ?: "Сана йўқ",
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
                                .clickable { viewModel.openDialog(ArchiveDialog.RESTORE, project) }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                        Text(
                            "ЎЧИРИШ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .border(BorderStroke(2.dp, MaterialTheme.colorScheme.error), RectangleShape)
                                .clickable { viewModel.openDialog(ArchiveDialog.DELETE, project) }
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
        ArchiveDialog.RESTORE -> {
            ConfirmDialog(
                title = "Ишни қайта тиклашми?",
                text = "'${selectedProject?.name}' иши умумий рўйхатга қайтарилади.",
                onConfirm = { viewModel.restoreSelectedProject() },
                onDismiss = { viewModel.dismissDialog() },
                confirmButtonText = "Қайта тиклаш"
                // (Цвет кнопки по умолчанию)
            )
        }
        ArchiveDialog.DELETE -> {
            ConfirmDialog(
                title = "ИШНИ ЎЧИРИШМИ?",
                text = "Сиз ростдан ҳам '${selectedProject?.name}' ишини БУТУНЛАЙ ЎЧИРМОҚЧИМИСИЗ?\n\nУ билан боғлиқ барча маълумотлар (давомат, харажатлар, бонуслар, тўловлар) БУТУНЛАЙ ЎЧИРИЛАДИ.",
                onConfirm = { viewModel.deleteSelectedProject() },
                onDismiss = { viewModel.dismissDialog() },
                confirmButtonText = "ЎЧИРИШ",
                confirmButtonColor = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) // Красная кнопка
            )
        }
        ArchiveDialog.NONE -> { /* Ничего */ }
    }
}
