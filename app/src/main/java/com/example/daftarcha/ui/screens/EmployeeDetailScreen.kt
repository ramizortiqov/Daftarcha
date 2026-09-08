package com.example.daftarcha.ui.screens

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.daftarcha.data.model.Employee
import com.example.daftarcha.data.model.Payment
import com.example.daftarcha.data.model.UserRole
import com.example.daftarcha.ui.components.AddDialog
import com.example.daftarcha.ui.components.ConfirmDialog
import com.example.daftarcha.ui.components.EmployeeAccountDialog
import com.example.daftarcha.ui.components.EmployeeStatsCard
import com.example.daftarcha.viewmodel.EmployeeDetailViewModel
import com.example.daftarcha.viewmodel.EmployeeDialog

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterialCode")
@Composable
fun EmployeeDetailScreen(
    onBackClick: () -> Unit,
    viewModel: EmployeeDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val employee by viewModel.employee.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val totalPaid by viewModel.totalPaid.collectAsState()
    val totalEarned by viewModel.totalEarned.collectAsState()
    val balance by viewModel.balance.collectAsState()
    val totalWorkdays by viewModel.totalWorkdays.collectAsState()
    val currentDialog by viewModel.dialogState.collectAsState()
    val employeeAccount by viewModel.employeeAccount.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val currentEmployee = employee
    val isBrigadier = currentUser?.role == UserRole.BRIGADIER

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentEmployee?.name ?: "Юкланмоқда...") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Орқага")
                    }
                },
                actions = {
                    if (currentEmployee != null) {
                        // Key icon for setting ID / Password / Role
                        IconButton(onClick = { viewModel.openDialog(EmployeeDialog.ACCOUNT_CREDENTIALS) }) {
                            Icon(
                                Icons.Default.Key,
                                contentDescription = "ID ва парол бериш",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Fire employee
                        IconButton(onClick = {
                            viewModel.openDialog(EmployeeDialog.FIRE_EMPLOYEE)
                        }) {
                            Icon(
                                Icons.Default.PersonRemove,
                                "Ишдан бўшатиш",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }

                        // Edit employee
                        IconButton(onClick = { viewModel.openDialog(EmployeeDialog.EDIT_EMPLOYEE) }) {
                            Icon(Icons.Default.Edit, "Таҳрирлаш")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (currentEmployee == null) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info icon & Name
                Icon(
                    imageVector = Icons.Outlined.AccountCircle,
                    contentDescription = "Шерик",
                    modifier = Modifier.size(90.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = currentEmployee.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentEmployee.phone ?: "Телефон кўрсатилмаган",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Login credentials card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.openDialog(EmployeeDialog.ACCOUNT_CREDENTIALS) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (employeeAccount != null) {
                            if (employeeAccount?.role == UserRole.ADMIN) MaterialTheme.colorScheme.tertiaryContainer
                            else MaterialTheme.colorScheme.secondaryContainer
                        } else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (employeeAccount?.role == UserRole.ADMIN) Icons.Default.Shield else Icons.Default.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (employeeAccount != null) "Кириш ID: ${employeeAccount?.loginId}" else "Кириш маълумотлари берилмаган",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (employeeAccount != null) {
                                        val roleLabel = if (employeeAccount?.role == UserRole.ADMIN) "Админ" else "Шерик"
                                        "Парол: •••• | Рол: $roleLabel"
                                    } else {
                                        "ID ва парол тайинлаш учун босинг"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Ўзгартириш",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Stats card
                EmployeeStatsCard(
                    workdays = totalWorkdays,
                    earned = totalEarned,
                    paid = totalPaid,
                    balance = balance
                )

                // Pay button
                Button(
                    onClick = { viewModel.openDialog(EmployeeDialog.ADD_PAYMENT) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ПУЛ БЕРИШ")
                }

                // Payment history
                Text(
                    text = "Тўловлар тарихи",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    if (payments.isEmpty()) {
                        Text(
                            text = "Тўловлар ҳали амалга оширилмаган.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    payments.forEach { payment ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = "Суммаси: ${payment.amount} с",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF009900)
                                )
                            },
                            supportingContent = {
                                Column {
                                    Text("Санаси: ${payment.date}")
                                    payment.description?.let { desc ->
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontStyle = FontStyle.Italic
                                        )
                                    }
                                }
                            },
                            leadingContent = {
                                Icon(Icons.Default.Payment, contentDescription = "Тўлов", tint = MaterialTheme.colorScheme.primary)
                            }
                        )
                        Divider()
                    }
                }
            }
        }
    }

    // Dialogs
    when (currentDialog) {
        EmployeeDialog.ADD_PAYMENT -> {
            AddDialog(
                title = "Пул бериш",
                nameLabel = "Суммаси (с)",
                phoneLabel = "Тавсиф (ихтиёрий)",
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { amountStr, description, _ ->
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    viewModel.addPayment(amount, description)
                }
            )
        }
        EmployeeDialog.EDIT_EMPLOYEE -> {
            if (currentEmployee != null) {
                AddDialog(
                    title = "Маълумотларни таҳрирлаш",
                    nameLabel = "Шерикнинг исми",
                    phoneLabel = "Телефон",
                    initialName = currentEmployee.name,
                    initialPhone = currentEmployee.phone ?: "",
                    onDismiss = { viewModel.dismissDialog() },
                    onConfirm = { newName, newPhone, _ ->
                        viewModel.updateEmployee(newName, newPhone)
                    }
                )
            }
        }
        EmployeeDialog.FIRE_EMPLOYEE -> {
            ConfirmDialog(
                title = "Шерикни ишдан бўшатишми?",
                text = "'${currentEmployee?.name}' умумий рўйхатдан яширилади ва 'Собиқ шериклар'га ўтказилади.",
                onConfirm = {
                    viewModel.fireEmployee()
                    onBackClick()
                },
                onDismiss = { viewModel.dismissDialog() },
                confirmButtonText = "Ҳа, ишдан бўшатиш",
                confirmButtonColor = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            )
        }
        EmployeeDialog.ACCOUNT_CREDENTIALS -> {
            if (currentEmployee != null) {
                EmployeeAccountDialog(
                    employee = currentEmployee,
                    currentAccount = employeeAccount,
                    canManageRoles = isBrigadier,
                    onDismiss = { viewModel.dismissDialog() },
                    onSave = { loginId, password, role ->
                        viewModel.saveAccountCredentials(loginId, password, role) { success, errMsg ->
                            if (success) {
                                Toast.makeText(context, "ID ва парол сақланди", Toast.LENGTH_SHORT).show()
                                viewModel.dismissDialog()
                            } else {
                                Toast.makeText(context, errMsg ?: "Хатолик юз берди", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }
        }
        EmployeeDialog.NONE -> {}
    }
}
