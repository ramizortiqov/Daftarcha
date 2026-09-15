package com.or.daftarcha.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.or.daftarcha.data.model.Project

/**
 * "Пул бериш" диалоги — рабочига алоҳида пул бериш. Касса объект бўйича
 * ҳисобланадиган бўлгани сабабли, қайси объект кассасидан чиқарилаётгани
 * танланиши шарт.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentDialog(
    projects: List<Project>,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, description: String?, projectId: Int) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedProject by remember(projects) { mutableStateOf(projects.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val canConfirm = amount > 0 && selectedProject != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Пул бериш", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Суммаси (с)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Тавсиф (ихтиёрий)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (projects.isEmpty()) {
                    Text(
                        "Бу шерик ҳеч бир объектга бириктирилмаган. Пул бериш учун аввал уни объектга қўшинг.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    OutlinedTextField(
                        value = selectedProject?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Қайси объект кассасидан") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Объектни танланг")
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        projects.forEach { project ->
                            DropdownMenuItem(
                                text = { Text(project.name) },
                                onClick = {
                                    selectedProject = project
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedProject?.let { project ->
                        onConfirm(amount, description.trim().takeIf { it.isNotBlank() }, project.id)
                    }
                },
                enabled = canConfirm,
                shape = androidx.compose.ui.graphics.RectangleShape
            ) {
                Text("БЕРИШ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("БЕКОР ҚИЛИШ")
            }
        }
    )
}
