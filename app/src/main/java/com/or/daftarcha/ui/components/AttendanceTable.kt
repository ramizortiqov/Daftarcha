package com.or.daftarcha.ui.components

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.or.daftarcha.data.model.Employee
import com.or.daftarcha.viewmodel.AttendanceTable
import java.text.SimpleDateFormat
import java.util.Locale

// Размеры ячеек
private val cellWidth = 48.dp
private val cellHeight = 44.dp
private val nameWidth = 128.dp

/**
 * Главный Composable таблицы — ruled ledger grid matching the Modernist
 * attendance journal (screen 1e): filled ink squares for present days,
 * hollow cells for absent, accent-filled cells for unsaved local edits.
 */
@Composable
fun AttendanceTable(
    employees: List<Employee>,
    table: AttendanceTable,
    onMarkClick: (employeeId: Int, date: String, isPresent: Boolean) -> Unit,
    onEmployeeNameClick: (Int) -> Unit,
    // --- Флаг режима редактирования ---
    isEditing: Boolean,
    localChanges: Map<Pair<Int, String>, Boolean>
) {
    // Единый стейт для горизонтальной прокрутки — "склеивает" прокрутку
    // заголовка и всех рядов.
    val horizontalScrollState = rememberScrollState()
    val borderColor = MaterialTheme.colorScheme.outlineVariant

    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .border(1.dp, MaterialTheme.colorScheme.onSurface)
    ) {
        // --- ЗАГОЛОВОК (ДАТЫ) ---
        Row(Modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
            HeaderCell("Шериклар", nameWidth, borderColor)

            Row(Modifier.horizontalScroll(horizontalScrollState)) {
                table.dates.forEach { dateStr ->
                    // Форматируем "2024-10-29" -> "29.10"
                    val formattedDate = try {
                        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
                        SimpleDateFormat("dd.MM", Locale.getDefault()).format(parsed!!)
                    } catch (e: Exception) { "?" }

                    HeaderCell(formattedDate, cellWidth, borderColor)
                }
            }
        }

        // --- РЯДЫ ДАННЫХ (ШЕРИКЛАР И ОТМЕТКИ) ---
        employees.forEach { employee ->
            val marks = table.employeeMarks[employee.id] ?: emptyList()

            Row {
                DataCell(
                    employee.name,
                    width = nameWidth,
                    borderColor = borderColor,
                    modifier = if (!isEditing) {
                        Modifier.clickable { onEmployeeNameClick(employee.id) }
                    } else {
                        Modifier // В режиме редактирования клик не работает
                    }
                )

                Row(Modifier.horizontalScroll(horizontalScrollState)) {
                    // zip связывает дату и отметку (true/false)
                    table.dates.zip(marks).forEach { (dateStr, isPresentFromTable) ->
                        CheckmarkCell(
                            isPresent = isPresentFromTable,
                            date = dateStr,
                            employeeId = employee.id,
                            localChanges = localChanges,
                            width = cellWidth,
                            borderColor = borderColor,
                            onClick = {
                                if (isEditing) {
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
private fun HeaderCell(text: String, width: Dp, borderColor: Color) {
    Box(
        modifier = Modifier
            .width(width)
            .height(38.dp)
            .border(1.dp, borderColor)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DataCell(
    text: String,
    width: Dp,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(width)
            .height(cellHeight)
            .border(1.dp, borderColor)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2
        )
    }
}

@Composable
private fun CheckmarkCell(
    isPresent: Boolean, // Оригинальное значение из БД/ViewModel
    date: String,
    employeeId: Int,
    localChanges: Map<Pair<Int, String>, Boolean>, // Карта локальных изменений
    width: Dp,
    borderColor: Color,
    onClick: () -> Unit
) {
    val changeKey = Pair(employeeId, date)
    val hasLocalChange = localChanges.containsKey(changeKey)
    val displayPresent = if (hasLocalChange) localChanges[changeKey]!! else isPresent

    val glyph = if (displayPresent) "+" else "–"

    val backgroundColor = when {
        hasLocalChange && displayPresent -> MaterialTheme.colorScheme.primary
        hasLocalChange && !displayPresent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        displayPresent -> MaterialTheme.colorScheme.onSurface
        else -> Color.Transparent
    }

    val glyphColor = when {
        hasLocalChange && displayPresent -> MaterialTheme.colorScheme.onPrimary
        displayPresent -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = Modifier
            .width(width)
            .height(cellHeight)
            .border(1.dp, borderColor)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = glyph, color = glyphColor, style = MaterialTheme.typography.titleLarge)
    }
}
