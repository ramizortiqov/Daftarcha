package com.or.daftarcha.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.or.daftarcha.ui.theme.PositiveGreen
import com.or.daftarcha.viewmodel.ProjectStats

@Composable
fun ProjectStatsCard(
    stats: ProjectStats
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            StatItem(
                title = "Кунлар",
                value = "${stats.totalWorkdays}",
                modifier = Modifier.weight(1f)
            )
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.fillMaxHeight())
            StatItem(
                title = "Нарх",
                value = "%.0f с".format(stats.netCost),
                modifier = Modifier.weight(1f)
            )
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.fillMaxHeight())
            StatItem(
                title = "Ставка/кун",
                value = "%.0f с".format(stats.dailyRate),
                modifier = Modifier.weight(1f)
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        StatItem(
            title = "Касса",
            value = "%.0f с".format(stats.cashInHand),
            modifier = Modifier.fillMaxWidth(),
            valueColor = if (stats.cashInHand < -0.01) MaterialTheme.colorScheme.error else PositiveGreen
        )
    }
}

@Composable
private fun StatItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified
) {
    Column(
        modifier = modifier.padding(12.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = if (valueColor != Color.Unspecified) valueColor else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
