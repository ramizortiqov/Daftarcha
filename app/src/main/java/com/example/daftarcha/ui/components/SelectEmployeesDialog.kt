package com.example.daftarcha.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.daftarcha.data.model.Employee

@Composable
fun SelectEmployeesDialog(
    availableEmployees: List<Employee>,
    onDismiss: () -> Unit,
    onConfirm: (List<Int>) -> Unit
) {
    val selectedEmployeeIds = remember { mutableStateListOf<Int>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Шерикларни қўшиш") },
        text = {
            Box(modifier = Modifier.heightIn(max = 400.dp)) {
                LazyColumn {
                    items(availableEmployees) { employee ->
                        val isSelected = selectedEmployeeIds.contains(employee.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) {
                                        selectedEmployeeIds.remove(employee.id)
                                    } else {
                                        selectedEmployeeIds.add(employee.id)
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        selectedEmployeeIds.add(employee.id)
                                    } else {
                                        selectedEmployeeIds.remove(employee.id)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(employee.name, style = MaterialTheme.typography.bodyLarge)
                                Text(employee.phone ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedEmployeeIds.toList())
                    onDismiss()
                },
                enabled = selectedEmployeeIds.isNotEmpty(),
                shape = androidx.compose.ui.graphics.RectangleShape
            ) {
                Text("ҚЎШИШ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("БЕКОР ҚИЛИШ")
            }
        }
    )
}
