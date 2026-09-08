package com.example.daftarcha.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.daftarcha.data.model.Employee
import com.example.daftarcha.viewmodel.AttendanceTable
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.foundation.clickable

// Размеры ячеек
private val cellWidth = 60.dp
private val cellHeight = 48.dp
private val nameWidth = 130.dp
private val borderColor = Color.Gray.copy(alpha = 0.5f)

/**
 * Главный Composable таблицы
 */
@Composable
fun AttendanceTable(
    employees: List<Employee>,
    table: AttendanceTable,
    onMarkClick: (employeeId: Int, date: String, isPresent: Boolean) -> Unit,
    onEmployeeNameClick: (Int) -> Unit,
    // --- НОВОЕ: Флаг режима редактирования ---
    isEditing: Boolean,
    localChanges: Map<Pair<Int, String>, Boolean>
) {
    // 1. Единый стейт для горизонтальной прокрутки.
    //    Это "склеивает" прокрутку заголовка и всех рядов.
    val horizontalScrollState = rememberScrollState()

    Column(Modifier.padding(horizontal = 16.dp)) {
        // --- ЗАГОЛОВОК (ДАТЫ) ---
        Row(Modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
            // Фиксированная ячейка "Сотрудник"
            HeaderCell("Шериклар", nameWidth)

            // Прокручиваемая часть с датами
            Row(Modifier.horizontalScroll(horizontalScrollState)) {
                table.dates.forEach { dateStr ->
                    // Форматируем "2024-10-29" -> "29.10"
                    val formattedDate = try {
                        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
                        SimpleDateFormat("dd.MM", Locale.getDefault()).format(parsed!!)
                    } catch (e: Exception) { "?" }

                    HeaderCell(formattedDate, cellWidth)
                }
            }
        }

        // --- РЯДЫ ДАННЫХ (СОТРУДНИКИ И ОТМЕТКИ) ---
        employees.forEach { employee ->
            val marks = table.employeeMarks[employee.id] ?: emptyList()

            Row {
                // Фиксированная ячейка "Имя"
                DataCell(
                    employee.name,
                    width = nameWidth,
                    modifier = if (!isEditing) {
                        Modifier.clickable { onEmployeeNameClick(employee.id) }
                    } else {
                        Modifier // В режиме редактирования клик не работает
                    }

                )

                // Прокручиваемая часть с отметками
                Row(Modifier.horizontalScroll(horizontalScrollState)) {
                    // Используем zip, чтобы связать дату и отметку (true/false)
                    table.dates.zip(marks).forEach { (dateStr, isPresentFromTable) ->
                        CheckmarkCell(
                            isPresent = isPresentFromTable, // Оригинальное значение
                            date = dateStr,                 // Дата
                            employeeId = employee.id,       // ID сотрудника
                            localChanges = localChanges,    // Карта локальных изменений
                            width = cellWidth,
                            onClick = {
                                if (isEditing) {
                                    // Определяем НОВОЕ значение (инвертируем ТЕКУЩЕЕ визуальное)
                                    val currentVisualState = localChanges[Pair(employee.id, dateStr)] ?: isPresentFromTable
                                    onMarkClick(employee.id, dateStr, !currentVisualState)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// --- Вспомогательные Composable-компоненты для ячеек ---

@Composable
private fun HeaderCell(text: String, width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .height(cellHeight)
            .border(1.dp, borderColor)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun DataCell(text: String, width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .height(cellHeight)
            .border(1.dp, borderColor)
            .padding(8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text, maxLines = 2)
    }
}

@Composable
private fun CheckmarkCell(
    isPresent: Boolean, // Оригинальное значение из БД/ViewModel
    date: String,
    employeeId: Int,
    localChanges: Map<Pair<Int, String>, Boolean>, // Карта локальных изменений
    width: Dp,
    onClick: () -> Unit
) {
    // --- НОВАЯ ЛОГИКА: Определяем, что показывать ---
    // Ключ для поиска в карте локальных изменений
    val changeKey = Pair(employeeId, date)
    // Есть ли локальное изменение для этой ячейки?
    val hasLocalChange = localChanges.containsKey(changeKey)
    // Какое значение показывать? Приоритет у локального.
    val displayPresent = if (hasLocalChange) localChanges[changeKey]!! else isPresent
    // --- КОНЕЦ НОВОЙ ЛОГИКИ ---

    val (text, color) = if (displayPresent) {
        "+" to Color(0xFF009900) // Зеленый
    } else {
        "-" to MaterialTheme.colorScheme.error // Красный
    }

    val backgroundColor = if (hasLocalChange) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        Color.Transparent
    }

    Box(
        modifier = Modifier
            .width(width)
            .height(cellHeight)
            .border(1.dp, borderColor)
            .background(backgroundColor)
            .clickable(onClick = onClick) // <-- КЛИКАБЕЛЬНОСТЬ
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = color, fontSize = MaterialTheme.typography.titleLarge.fontSize, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DataCell(text: String, width: Dp, modifier: Modifier = Modifier) { // <-- Добавили modifier
    Box(
        modifier = modifier // <-- Используем modifier
            .width(width)
            .height(cellHeight)
            .border(1.dp, borderColor)
            .padding(8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text, maxLines = 2)
    }
}