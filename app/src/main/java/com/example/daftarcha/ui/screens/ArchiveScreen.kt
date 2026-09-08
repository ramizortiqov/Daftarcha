package com.example.daftarcha.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.daftarcha.ui.components.ConfirmDialog
import com.example.daftarcha.viewmodel.ArchiveDialog
import com.example.daftarcha.viewmodel.ArchiveViewModel

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
        LazyColumn(modifier = Modifier.padding(paddingValues).padding(8.dp)) {
            items(projects) { project ->
                Card(
                    modifier = Modifier.fillParentMaxWidth().padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    ListItem(
                        headlineContent = { Text(project.name) },
                        supportingContent = { Text(project.startDate ?: "Сана йўқ") },
                        leadingContent = { Icon(Icons.Default.Archive, null) },
                        trailingContent = {
                            Row {
                                // Кнопка Восстановить
                                IconButton(
                                    onClick = { viewModel.openDialog(ArchiveDialog.RESTORE, project) },
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFF009900)) // Зеленый
                                ) {
                                    Icon(Icons.Default.Restore, "Қайта тиклаш")
                                }
                                // Кнопка Удалить
                                IconButton(
                                    onClick = { viewModel.openDialog(ArchiveDialog.DELETE, project) },
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error) // Красный
                                ) {
                                    Icon(Icons.Default.Delete, "Ўчириш")
                                }
                            }
                        }
                    )
                }
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
