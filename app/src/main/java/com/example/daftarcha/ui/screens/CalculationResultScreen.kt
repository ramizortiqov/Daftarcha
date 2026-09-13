package com.example.daftarcha.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.daftarcha.ui.theme.PositiveGreen
import android.content.Context
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Download
import java.io.OutputStreamWriter
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterialCode")
@Composable
fun CalculationResultScreen(
    onBackClick: () -> Unit,
    viewModel: CalculatorViewModel
) {
    val result by viewModel.calculationResult.collectAsState()
    val context = LocalContext.current
    val csvFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri ->
            if (uri != null) {
                try {
                    val csvText = viewModel.generateCsvText(result!!)
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        OutputStreamWriter(outputStream, "UTF-8").use { writer ->
                            writer.write("\uFEFF")
                            writer.write(csvText)
                        }
                    }
                    Toast.makeText(context, "Ҳисобот сақланди", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Сақлашда хатолик: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(context, "Сақлаш бекор қилинди", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ҳисоб-китоб натижаси") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Орқага")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        result?.let { summary ->
                            val shareText = viewModel.generateShareableText(summary)
                            val csvText = viewModel.generateCsvText(summary)
                            val csvUri = saveCsvToCacheAndGetUri(context, csvText)

                            if (csvUri == null) {
                                Toast.makeText(context, "Файл яратишда хатолик", Toast.LENGTH_SHORT).show()
                                return@let
                            }

                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "message/rfc822"
                                putExtra(Intent.EXTRA_SUBJECT, "Ҳисоб-китоб бўйича ҳисобот")
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                putExtra(Intent.EXTRA_STREAM, csvUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }

                            val shareIntent = Intent.createChooser(sendIntent, "Ҳисоботни улашиш (Матн + CSV)")
                            context.startActivity(shareIntent)
                        }
                    }) {
                        Icon(Icons.Default.Share, "Улашиш")
                    }
                    IconButton(onClick = {
                        result?.let {
                            val fileName = "hisobot.csv"
                            csvFileLauncher.launch(fileName)
                        }
                    }) {
                        Icon(Icons.Default.Download, "CSV'га юклаб олиш")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (result == null) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val summary = result!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.onSurface)
                            .padding(16.dp)
                    ) {
                        Text(
                            "КУНЛИК СТАВКА",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        )
                        Text(
                            "%.0f с".format(summary.dailyRate),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Column {
                                Text(
                                    "${summary.totalWorkdays}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.surface
                                )
                                Text(
                                    "ЖАМИ КУНЛАР",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                )
                            }
                            Column {
                                Text(
                                    "%.0f с".format(summary.netCost),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.surface
                                )
                                Text(
                                    "СОФ ФОЙДА",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant))
                    ) {
                        StatRow("Умумий пул берилди", "%.0f с".format(summary.totalBonuses), modifier = Modifier.padding(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        StatRow("Умумий харажатлар", "%.0f с".format(summary.totalExpenses), modifier = Modifier.padding(12.dp))
                    }
                }

                item {
                    Text(
                        "ШЕРИКЛАР БЎЙИЧА (${summary.employeeStats.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }

                items(summary.employeeStats) { emp ->
                    val balanceColor = when {
                        emp.balance > 0.01 -> PositiveGreen
                        emp.balance < -0.01 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                emp.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "${emp.workdays} кун",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MiniStat("Ишлади", "%.0f".format(emp.earned))
                            MiniStat("Олди", "%.0f".format(emp.paid))
                            MiniStat("Тўланиши", "%.0f".format(emp.balance), valueColor = balanceColor)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun RowScope.MiniStat(title: String, value: String, valueColor: Color = Color.Unspecified) {
    Column(modifier = Modifier.weight(1f)) {
        Text(
            title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            color = if (valueColor != Color.Unspecified) valueColor else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
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
private fun saveCsvToCacheAndGetUri(context: Context, csvText: String): Uri? {
    return try {
        val cacheDir = File(context.cacheDir, "reports")
        cacheDir.mkdirs()

        val file = File(cacheDir, "hisobot.csv")

        FileOutputStream(file).use { outputStream ->
            OutputStreamWriter(outputStream, "UTF-8").use { writer ->
                writer.write("\uFEFF")
                writer.write(csvText)
            }
        }

        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
