package com.example.daftarcha.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.daftarcha.data.model.AppUser
import com.example.daftarcha.data.model.UserRole
import com.example.daftarcha.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    onBackClick: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val users by viewModel.allAppUsers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var userToEditRole by remember { mutableStateOf<AppUser?>(null) }
    var selectedNewRole by remember { mutableStateOf<UserRole>(UserRole.WORKER) }
    val isBrigadier = currentUser?.role == UserRole.BRIGADIER

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Фойдаланувчилар ва Роллар") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Орқага")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "👑 Роллар тақсимоти",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Бригадир: тўлиқ назорат, янги шериклар, янги ишлар, админ тайинлаш.\n" +
                                   "• Админ: барча ишлар, давомат ва харажатларга кириш (янги шерик/иш қўшиш ва админ тайинлашдан ташқари).\n" +
                                   "• Шерик: фақат ўз шахсий ҳисоб-китоби ва харажатларини кўриш.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Тизимдаги барча аккаунтлар (${users.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(users) { user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (user.role) {
                                    UserRole.BRIGADIER -> MaterialTheme.colorScheme.primaryContainer
                                    UserRole.ADMIN -> MaterialTheme.colorScheme.tertiaryContainer
                                    UserRole.WORKER -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = when (user.role) {
                                            UserRole.BRIGADIER -> Icons.Default.Shield
                                            UserRole.ADMIN -> Icons.Default.Shield
                                            UserRole.WORKER -> Icons.Default.Key
                                        },
                                        contentDescription = null,
                                        tint = when (user.role) {
                                            UserRole.BRIGADIER -> MaterialTheme.colorScheme.primary
                                            UserRole.ADMIN -> MaterialTheme.colorScheme.tertiary
                                            UserRole.WORKER -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = user.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "ID: ${user.loginId} | Парол: ${user.password}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Badge / Change role button
                        if (user.role == UserRole.BRIGADIER) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = "Бригадир",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (isBrigadier) {
                            OutlinedButton(
                                onClick = {
                                    userToEditRole = user
                                    selectedNewRole = user.role
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (user.role == UserRole.ADMIN) "Админ ✏️" else "Шерик ✏️",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        } else {
                            Surface(
                                color = if (user.role == UserRole.ADMIN) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = if (user.role == UserRole.ADMIN) "Админ" else "Шерик",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (userToEditRole != null && isBrigadier) {
        val editing = userToEditRole!!
        AlertDialog(
            onDismissRequest = { userToEditRole = null },
            title = { Text("${editing.name} ролини ўзгартириш") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Янги ролни танланг:")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedNewRole == UserRole.WORKER,
                            onClick = { selectedNewRole = UserRole.WORKER },
                            label = { Text("Шерик (Ишчи)") }
                        )
                        FilterChip(
                            selected = selectedNewRole == UserRole.ADMIN,
                            onClick = { selectedNewRole = UserRole.ADMIN },
                            label = { Text("Админ") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setUserRole(editing.loginId, selectedNewRole) { success, _ ->
                            userToEditRole = null
                        }
                    }
                ) {
                    Text("САҚЛАШ")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToEditRole = null }) {
                    Text("БЕКОР ҚИЛИШ")
                }
            }
        )
    }
}
