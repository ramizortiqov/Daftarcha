package com.example.daftarcha.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Карточка для отображения сотрудника в списке.
 * Аналог <EmployeeCard> из .kv
 */
@Composable
fun EmployeeCard(
    employeeName: String,
    employeePhone: String,
    balance: Double,
    onClick: () -> Unit, // Аналог on_release
    showBalance: Boolean = true
) {
    val balanceColor = if (balance < 0) MaterialTheme.colorScheme.error else Color(0xFF009900)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Аналог MDIcon
            Icon(
                imageVector = Icons.Outlined.AccountCircle,
                contentDescription = "Сотрудник",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier.padding(start = 16.dp)) {
                // Аналог MDLabel (Subtitle1)
                Text(
                    text = employeeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                // Аналог MDLabel (Caption)
                Text(
                    text = employeePhone.ifBlank { "Телефон не указан" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Аналог MDLabel (H6)
            if (showBalance) {
                Text(
                text = "%.2f ₽".format(balance),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = balanceColor
                )
            }
        }
    }
}