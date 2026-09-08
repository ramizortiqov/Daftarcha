package com.example.daftarcha.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.daftarcha.data.model.CalculationSummary
import com.example.daftarcha.viewmodel.CalculatorViewModel
import androidx.navigation.NavHostController

@Composable
fun CalculatorTab(
    navController: NavHostController, 
    viewModel: CalculatorViewModel
) {
    val projects by viewModel.allProjects.collectAsState()
    val selectedIds by viewModel.selectedProjectIds.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Ҳисоб-китоб учун ишларни танланг",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth()
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            items(projects) { project ->
                val isSelected = selectedIds.contains(project.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleProjectSelection(project.id) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { viewModel.toggleProjectSelection(project.id) }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(project.name, style = MaterialTheme.typography.bodyLarge)
                }
                Divider()
            }
        }

        Button(
            onClick = {
                viewModel.calculateSelectedProjects()
                navController.navigate(Screen.CalculationResult.route)
            },
            enabled = selectedIds.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text("ТАНЛАНГАНЛАРНИ ҲИСОБЛАШ")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalculationResultDialog(
    summary: CalculationSummary,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ҳисоб-китоб натижаси") },

        text = {
            LazyColumn(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("УМУМИЙ МАЪЛУМОТ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            StatRow("Умумий пул берилди:", "%.2f с".format(summary.totalBonuses))
                            StatRow("Умумий харажатлар:", "%.2f с".format(summary.totalExpenses))
                            StatRow("Жами (соф фойда):", "%.2f с".format(summary.netCost), isTotal = true)
                            StatRow("Жами кунлар:", "${summary.totalWorkdays}")
                            StatRow("Кунлик ставка:", "%.2f с".format(summary.dailyRate), isTotal = true)
                        }
                    }
                }

                item {
                    Text(
                        "ШЕРИКЛАР БЎЙИЧА (${summary.employeeStats.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }

                items(summary.employeeStats) { emp ->
                    val balanceColor = when {
                        emp.balance > 0.01 -> Color(0xFF009900)
                        emp.balance < -0.01 -> MaterialTheme.colorScheme.error
                        else -> Color.Unspecified
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "${emp.name} (Кунлар: ${emp.workdays})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            StatRow("Ишлади:", "%.2f с".format(emp.earned))
                            StatRow("Олди:", "%.2f с".format(emp.paid))
                            StatRow(
                                "ТЎЛАНИШИ КЕРАК:",
                                "%.2f с".format(emp.balance),
                                valueColor = balanceColor,
                                isTotal = true
                            )
                        }
                    }
                }
            }
        },

        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ЁПИШ")
            }
        },
        dismissButton = null
    )
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isTotal: Boolean = false,
    valueColor: Color = Color.Unspecified
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = if (valueColor != Color.Unspecified) valueColor else LocalContentColor.current
        )
    }
}
