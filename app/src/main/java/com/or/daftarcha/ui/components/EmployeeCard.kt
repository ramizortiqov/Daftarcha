package com.or.daftarcha.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.or.daftarcha.ui.theme.PositiveGreen

/**
 * Row for a partner in a list. Аналог <EmployeeCard> из .kv
 */
@Composable
fun EmployeeCard(
    employeeName: String,
    employeePhone: String,
    balance: Double,
    onClick: () -> Unit, // Аналог on_release
    showBalance: Boolean = true
) {
    val balanceColor = if (balance < 0) MaterialTheme.colorScheme.error else PositiveGreen

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = employeeName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = employeePhone.ifBlank { "Телефон кўрсатилмаган" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (showBalance) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "%.0f с".format(kotlin.math.abs(balance)),
                        style = MaterialTheme.typography.titleMedium,
                        color = balanceColor
                    )
                    Text(
                        text = if (balance < 0) "Қарз" else "Тўланган",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
