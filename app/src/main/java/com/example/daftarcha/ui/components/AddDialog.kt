package com.example.daftarcha.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalFocusManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private fun parseDateToMillis(dateStr: String?): Long? {
    if (dateStr.isNullOrBlank()) return null
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        sdf.parse(dateStr)?.time
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDialog(
    title: String,
    nameLabel: String,
    phoneLabel: String? = null,
    showDateField: Boolean = false,
    initialName: String = "",
    initialPhone: String = "",
    initialDate: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String?, date: String?) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var phone by remember(initialPhone) { mutableStateOf(initialPhone) }
    var selectedDateMillis by remember(initialDate) {
        mutableStateOf(parseDateToMillis(initialDate))
    }
    var showDatePicker by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val formattedDate = remember(selectedDateMillis) {
        selectedDateMillis?.let {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.format(Date(it))
        } ?: ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(nameLabel) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (phoneLabel != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(phoneLabel) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (showDateField) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = formattedDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Бошланиш санаси") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = {
                                focusManager.clearFocus()
                                showDatePicker = true
                            }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Санани танланг")
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Диалогни ёпиш энди чақирувчининг зиммасида — баъзи ҳолларда
                    // onConfirm кейинги диалогни очиши мумкин (масалан, суммани
                    // рабочилар бўйича тарқатиш), шунда бу ерда onDismiss() ни
                    // автоматик чақириш ўша ҳолатни бекор қилиб қўяди.
                    onConfirm(name, phone.takeIf { phoneLabel != null }, formattedDate.takeIf { it.isNotBlank() })
                },
                enabled = name.isNotBlank() && (!showDateField || selectedDateMillis != null),
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

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Бекор қилиш") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
