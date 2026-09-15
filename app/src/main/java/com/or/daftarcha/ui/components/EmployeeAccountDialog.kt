package com.or.daftarcha.ui.components

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
import com.or.daftarcha.data.model.AppUser
import com.or.daftarcha.data.model.Employee
import com.or.daftarcha.data.model.UserRole
import kotlin.random.Random

// Login IDs are unique across EVERY brigadier in the app (see AuthManager's comment on
// createOrUpdateEmployeeAccount), so a small local counter like the old "emp_${employee.id}"
// default was virtually guaranteed to collide with some other brigadier's worker who also
// accepted the default. Suggesting a name-based ID with a random suffix instead makes a
// collision unlikely up front; AuthManager still rejects a genuine collision either way, so
// this is a UX improvement on top of that real fix, not a replacement for it.
private const val ID_SUGGESTION_CHARS = "23456789"
private const val PASSWORD_CHARS = "abcdefghjkmnpqrstuvwxyz23456789"

private fun suggestLoginId(name: String): String {
    val cleaned = name.trim().lowercase()
        .replace(Regex("[^a-z0-9а-яёўқғҳ]"), "")
        .take(12)
        .ifEmpty { "usta" }
    val suffix = (1..3).map { ID_SUGGESTION_CHARS[Random.nextInt(ID_SUGGESTION_CHARS.length)] }.joinToString("")
    return "$cleaned$suffix"
}

private fun suggestPassword(length: Int = 6): String =
    (1..length).map { PASSWORD_CHARS[Random.nextInt(PASSWORD_CHARS.length)] }.joinToString("")

@Composable
fun EmployeeAccountDialog(
    employee: Employee,
    currentAccount: AppUser?,
    canManageRoles: Boolean, // true only for BRIGADIER
    onDismiss: () -> Unit,
    onSave: (loginId: String, password: String, role: UserRole) -> Unit
) {
    var loginId by remember {
        mutableStateOf(currentAccount?.loginId ?: suggestLoginId(employee.name))
    }
    var password by remember {
        mutableStateOf(currentAccount?.password ?: suggestPassword())
    }
    var selectedRole by remember {
        mutableStateOf(currentAccount?.role ?: UserRole.WORKER)
    }

    // Defaults to visible when it's a freshly-generated password (nothing to hide yet, and
    // the brigadier needs to actually read it to hand it to the worker); an existing saved
    // password still opens masked.
    var passwordVisible by remember { mutableStateOf(currentAccount == null) }

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
