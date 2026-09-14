package com.example.daftarcha.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.daftarcha.ui.theme.PositiveGreen

@Composable
fun EmployeeStatsCard(
    workdays: Int,
    earned: Double,
    paid: Double,
    balance: Double,
    personalExpenses: Double = 0.0,
    showEarningsAndDebt: Boolean = true
) {
    val balanceColor = when {
        balance > 0.01 -> PositiveGreen
        balance < -0.01 -> MaterialTheme.colorScheme.error
        else -> LocalContentColor.current
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface))
    ) {
        if (showEarningsAndDebt) {
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                StatItem(
                    title = "Иш кунлари",
                    value = "$workdays",
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.fillMaxHeight())
                StatItem(
                    title = "Ишлади",
                    value = "%.0f с".format(earned),
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                StatItem(
                    title = "Олди",
                    value = "%.0f с".format(paid),
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.fillMaxHeight())
                StatItem(
                    title = "Харажатлар",
                    value = "%.0f с".format(personalExpenses),
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                StatItem(
                    title = "Тўланиши керак",
                    value = "%.0f с".format(balance),
                    valueColor = balanceColor,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                StatItem(
                    title = "Иш кунлари",
                    value = "$workdays кун",
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.fillMaxHeight())
                StatItem(
                    title = "Олди (тўланган)",
                    value = "%.0f с".format(paid),
                    valueColor = PositiveGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Иш ҳақи ва қарз ҳисоб-китоби усто томонидан ёпилган",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = LocalContentColor.current
) {
    Column(
        modifier = modifier.padding(14.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = valueColor
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
