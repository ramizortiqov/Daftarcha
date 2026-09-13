package com.example.daftarcha.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.daftarcha.data.model.AppUser
import com.example.daftarcha.data.model.Employee
import com.example.daftarcha.data.model.UserRole

@Composable
fun EmployeeAccountDialog(
    employee: Employee,
    currentAccount: AppUser?,
    canManageRoles: Boolean, // true only for BRIGADIER
    onDismiss: () -> Unit,
    onSave: (loginId: String, password: String, role: UserRole) -> Unit
) {
    var loginId by remember {
        mutableStateOf(currentAccount?.loginId ?: "emp_${employee.id}")
    }
    var password by remember {
        mutableStateOf(currentAccount?.password ?: "1234")
    }
    var selectedRole by remember {
        mutableStateOf(currentAccount?.role ?: UserRole.WORKER)
    }

    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Шерик учун кириш (ID ва Парол)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = employee.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = loginId,
                    onValueChange = { loginId = it },
                    label = { Text("Кириш ID (Логин)") },
                    placeholder = { Text("масалан: ${employee.name.lowercase().replace(" ", "")}") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Парол") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (canManageRoles) {
                    Text(
                        text = "Тизимдаги роли:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedRole == UserRole.WORKER,
                            onClick = { selectedRole = UserRole.WORKER },
                            label = { Text("Шерик (Ишчи)") },
                            leadingIcon = if (selectedRole == UserRole.WORKER) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )

                        FilterChip(
                            selected = selectedRole == UserRole.ADMIN,
                            onClick = { selectedRole = UserRole.ADMIN },
                            label = { Text("Админ") },
                            leadingIcon = if (selectedRole == UserRole.ADMIN) {
                                { Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        )
                    }

                    if (selectedRole == UserRole.ADMIN) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🛡️ Админ барча ишлар, давомат ва харажатларни бошқара олади, лекин янги шерик/иш қўша олмайди ва админ тайинлай олмайди.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Рол: ${if (selectedRole == UserRole.ADMIN) "Админ" else "Шерик"} (Фақат Усто ролни ўзгартириши мумкин)",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (loginId.isNotBlank() && password.isNotBlank()) {
                        onSave(loginId.trim(), password.trim(), selectedRole)
                    }
                },
                enabled = loginId.isNotBlank() && password.isNotBlank(),
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
