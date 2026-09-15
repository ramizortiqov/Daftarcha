package com.or.daftarcha.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.or.daftarcha.data.model.Employee

/**
 * Кимга қанча берилди/сарфланди — рабочилар бўйича суммани тарқатиш диалоги.
 */
enum class SplitMode {
    // Ҳар бир рабочи учун қиймат қўлда киритилади (масалан, "Пул берди" тарқатиш).
    MANUAL,
    // Танланган рабочилар орасида сумма тенг бўлинади, лекин қўлда тузатиш мумкин (масалан, "Харажат").
    EQUAL_SPLIT_SELECTABLE
}

@Composable
fun EmployeeAmountSplitDialog(
    title: String,
    totalAmount: Double,
    employees: List<Employee>,
    mode: SplitMode,
    onDismiss: () -> Unit,
    onConfirm: (Map<Int, Double>) -> Unit
) {
    // employeeId -> matn (input maydoni holati)
    val amounts = remember { mutableStateMapOf<Int, String>() }
    val selected = remember {
        val map = mutableStateMapOf<Int, Boolean>()
        employees.forEach { map[it.id] = mode == SplitMode.EQUAL_SPLIT_SELECTABLE }
        map
    }

    fun recalcEqualSplit() {
        if (mode != SplitMode.EQUAL_SPLIT_SELECTABLE) return
        val selectedIds = employees.filter { selected[it.id] == true }.map { it.id }
        if (selectedIds.isEmpty()) return
        val share = totalAmount / selectedIds.size
        selectedIds.forEach { id ->
            amounts[id] = if (share == share.toLong().toDouble()) share.toLong().toString() else "%.2f".format(share)
        }
    }

    // Boshlang'ich teng bo'linish (EQUAL_SPLIT_SELECTABLE uchun)
    remember {
        recalcEqualSplit()
        true
    }

    val sumEntered = amounts.values.sumOf { it.toDoubleOrNull() ?: 0.0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                Text(
                    text = "Умумий сумма: %.0f с".format(totalAmount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.padding(bottom = 8.dp))

                Box(modifier = Modifier.heightIn(max = 380.dp)) {
                    LazyColumn {
                        items(employees) { employee ->
                            val isSelected = selected[employee.id] ?: true
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (mode == SplitMode.EQUAL_SPLIT_SELECTABLE) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            selected[employee.id] = checked
                                            if (!checked) amounts[employee.id] = ""
                                            recalcEqualSplit()
                                        }
                                    )
                                }
                                Text(
                                    text = employee.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = if (mode == SplitMode.EQUAL_SPLIT_SELECTABLE) 0.dp else 8.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = amounts[employee.id] ?: "",
                                    onValueChange = { newValue ->
                                        amounts[employee.id] = newValue
                                    },
                                    enabled = mode == SplitMode.MANUAL || isSelected,
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    placeholder = { Text("0") },
                                    modifier = Modifier.widthIn(min = 100.dp, max = 130.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.padding(top = 8.dp))
                val diff = totalAmount - sumEntered
                Text(
                    text = "Тарқатилди: %.0f с (Қолди: %.0f с)".format(sumEntered, diff),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val shares = employees.mapNotNull { employee ->
                        val amount = amounts[employee.id]?.toDoubleOrNull() ?: 0.0
                        if (amount > 0.0) employee.id to amount else null
                    }.toMap()
                    onConfirm(shares)
                    onDismiss()
                },
                shape = androidx.compose.ui.graphics.RectangleShape
            ) {
                Text("САҚЛАШ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("БЕКОР ҚИЛИШ")
            }
        }
    )
}
