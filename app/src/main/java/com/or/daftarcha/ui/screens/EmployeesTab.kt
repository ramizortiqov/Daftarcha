package com.or.daftarcha.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.or.daftarcha.data.model.Employee
import com.or.daftarcha.ui.components.EmployeeCard
import com.or.daftarcha.viewmodel.HomeViewModel

@Composable
fun EmployeesTab(
    viewModel: HomeViewModel = hiltViewModel(),
    onEmployeeClick: (Int) -> Unit,
    onNavigateToFiredEmployees: () -> Unit
) {
    val employees by viewModel.employees.collectAsState()

    // 1. Используем ЕДИНЫЙ LazyColumn для всего
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            // Отступы применяем ко всему списку
            .padding(horizontal = 16.dp)
    ) {

        // --- 2. Элементы списка (Карточки сотрудников) ---
        items(employees) { employee ->
            EmployeeCard(
                employeeName = employee.name,
                employeePhone = employee.phone ?: "Телефон не указан",
                balance = 0.0,
                onClick = { onEmployeeClick(employee.id) },
                showBalance = false
            )
        }

        // --- 3. Последний элемент (Кнопка) ---
        // 'item' добавляет один элемент в конец списка
        item {
            Divider(modifier = Modifier.padding(vertical = 8.dp)) // Разделитель
            OutlinedButton(
                onClick = onNavigateToFiredEmployees,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp), // Отступ кнопки снизу
                shape = androidx.compose.ui.graphics.RectangleShape
            ) {
                Text("СОБИҚ ШЕРИКЛАР")
            }
        }
    }
}