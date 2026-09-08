package com.example.daftarcha.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit // <-- ИМПОРТ
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.* // <-- ИМПОРТ ДЛЯ remember, mutableStateOf, getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.daftarcha.data.model.Employee
import com.example.daftarcha.data.model.Payment
import com.example.daftarcha.ui.components.AddDialog
import com.example.daftarcha.ui.components.ProjectStatsCard // Используем временно
import com.example.daftarcha.viewmodel.EmployeeDetailViewModel
import com.example.daftarcha.viewmodel.EmployeeDialog // Убедитесь в наличии
import com.example.daftarcha.viewmodel.ProjectStats // Используем временно
import com.example.daftarcha.ui.components.EmployeeStatsCard // <-- ДОБАВЬТЕ ЭТОТ ИМПОРТ
import android.util.Log
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.material.icons.filled.PersonRemove // <-- 1. ДОБАВЬТЕ ИКОНКУ
import com.example.daftarcha.ui.components.ConfirmDialog

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterialCode")
@Composable
fun EmployeeDetailScreen(
    onBackClick: () -> Unit,
    viewModel: EmployeeDetailViewModel = hiltViewModel()
) {
    // 1. Подписываемся на "живые" данные
    val employee by viewModel.employee.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val totalPaid by viewModel.totalPaid.collectAsState()
    val totalEarned by viewModel.totalEarned.collectAsState() // Будет 0
    val balance by viewModel.balance.collectAsState()         // Будет 0 - totalPaid
    val totalWorkdays by viewModel.totalWorkdays.collectAsState() // <-- СОБИРАЕМ ДНИ
    val currentDialog by viewModel.dialogState.collectAsState()

    val currentEmployee = employee // Локальная копия для стабильности UI

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentEmployee?.name ?: "Юкланмоқда...") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Орқага")
                    }
                },
                // --- ДОБАВЛЕНА КНОПКА EDIT ---
                actions = {
                    if (currentEmployee != null) {
                        // --- 3. ДОБАВЬТЕ КНОПКУ УВОЛИТЬ ---
                        IconButton(onClick = {
                            viewModel.openDialog(EmployeeDialog.FIRE_EMPLOYEE)
                        }) {
                            Icon(Icons.Default.PersonRemove, "Ишдан бўшатиш", tint = MaterialTheme.colorScheme.error)
                        }
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
                // --- Инфо о сотруднике ---
                Icon(
                    imageVector = Icons.Outlined.AccountCircle,
                    contentDescription = "Шерик",
                    modifier = Modifier.size(100.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(currentEmployee.name, /* ... */)
                Text(currentEmployee.phone ?: "Телефон кўрсатилмаган", /* ... */)

                // --- Карточка Статистики ---
                EmployeeStatsCard(
                        workdays = totalWorkdays,
                        earned = totalEarned,
                        paid = totalPaid,
                        balance = balance
                )

                // --- Кнопка "Выдать аванс" ---
                Button(
                    onClick = { viewModel.openDialog(EmployeeDialog.ADD_PAYMENT) }, // <-- Передаем ADD_PAYMENT
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ПУЛ БЕРИШ")
                }

                // --- История выплат ---
                Text(
                    text = "Тўловлар тарихи", // <-- ДОБАВЬТЕ ЭТОТ ТЕКСТ
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (payments.isEmpty()) {
                        Text(
                        text = "Тўловлар ҳали амалга оширилмаган.", // <-- ADD THIS text PARAMETER
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    payments.forEach { payment ->
                        ListItem(
                            headlineContent = { Text("Суммаси: ${payment.amount} с", /* ... */) },
                            supportingContent = {
                                Column {
                                Text("Санаси: ${payment.date}")
                                // Показываем описание, если оно есть
                                payment.description?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontStyle = FontStyle.Italic // Делаем описание курсивом
                                    )
                                }
                            }},
                            leadingContent = { Icon(Icons.Default.Payment, contentDescription ="Тўлов") }
                        )
                        Divider()
                    }
                }
            }
        }
    } // --- Конец Scaffold ---

    // --- Логика диалогов ---
    when (currentDialog) {
        EmployeeDialog.ADD_PAYMENT -> {
            AddDialog(
                title = "Пул бериш",
                nameLabel = "Суммаси (с)",
                // Убираем поле Описания
                phoneLabel = "Тавсиф (ихтиёрий)",
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { amountStr, description, _ -> // Игнорируем phone и date
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    viewModel.addPayment(amount, description)
                }
            )
        }
        // --- ДОБАВЛЕН ДИАЛОГ РЕДАКТИРОВАНИЯ ---
        EmployeeDialog.EDIT_EMPLOYEE -> {
            if (currentEmployee != null) {
                AddDialog(
                    title = "Маълумотларни таҳрирлаш",
                    nameLabel = "Шерикнинг исми",
                    phoneLabel = "Телефон",
                    initialName = currentEmployee.name,
                    initialPhone = currentEmployee.phone ?: "",
                    onDismiss = { viewModel.dismissDialog() },
                    onConfirm = { newName, newPhone, _ -> // Игнорируем date
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
                    viewModel.fireEmployee() // Увольняем
                    onBackClick() // Возвращаемся
                },
                onDismiss = { viewModel.dismissDialog() },
                confirmButtonText = "Ҳа, ишдан бўшатиш",
                confirmButtonColor = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            )
        }
        EmployeeDialog.NONE -> { /* Ничего не делаем */ }
    }
}
