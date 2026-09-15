package com.or.daftarcha.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.or.daftarcha.ui.components.StatsCard
import com.or.daftarcha.viewmodel.HomeViewModel
import com.or.daftarcha.viewmodel.SyncViewModel
import com.or.daftarcha.data.sync.SyncStatus
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.navigation.NavHostController
import com.or.daftarcha.data.model.UserRole

@Composable
fun DashboardTab(
    viewModel: HomeViewModel = hiltViewModel(),
    syncViewModel: SyncViewModel = hiltViewModel(),
    onAddProjectClick: () -> Unit,
    onAddEmployeeClick: () -> Unit,
    navController: NavHostController,
    onProjectsCardClick: () -> Unit,
    onEmployeesCardClick: () -> Unit
) {
    val projectCount by viewModel.projectCount.collectAsState()
    val employeeCount by viewModel.employeeCount.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isBrigadier = currentUser?.role == UserRole.BRIGADIER
    val syncStatus by syncViewModel.syncStatus.collectAsState()
    val lastSyncText by syncViewModel.lastSyncTimeFormatted.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val ink = MaterialTheme.colorScheme.onSurface
    val divider = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SectionLabel("Статистика", modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            StatsCard(
                title = "Ишлар",
                count = "$projectCount",
                modifier = Modifier
                    .weight(1f)
                    .clickable { onProjectsCardClick() }
            )
            StatsCard(
                title = "Шериклар",
                count = "$employeeCount",
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEmployeesCardClick() }
            )
        }

        SectionLabel("Тезкор амаллар", modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp))

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isBrigadier) {
                LedgerButton(
                    text = "ЯНГИ ИШ ҚЎШИШ",
                    primary = true,
                    onClick = onAddProjectClick
                )
                LedgerButton(
                    text = "ШЕРИК ҚЎШИШ",
                    onClick = onAddEmployeeClick
                )
            }
            LedgerButton(
                text = "АРХИВ ИШЛАР",
                onClick = { navController.navigate(Screen.Archive.route) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(thickness = 2.dp, color = ink)

        // --- FIREBASE CLOUD SYNC ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Булутли синхронизация",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Охирги: $lastSyncText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Text(
                    text = "СИНХРОН",
                    style = MaterialTheme.typography.labelSmall,
                    color = ink,
                    modifier = Modifier
                        .border(BorderStroke(2.dp, ink))
                        .clickable(enabled = syncStatus !is SyncStatus.InProgress) { syncViewModel.triggerSync() }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }

            if (syncStatus is SyncStatus.Error) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = (syncStatus as SyncStatus.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else if (syncStatus is SyncStatus.Success) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = (syncStatus as SyncStatus.Success).message,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else if (syncStatus is SyncStatus.InProgress) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(16.dp).width(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Синхронизация қилинмоқда...", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        HorizontalDivider(color = divider)

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            LedgerButton(
                text = "БД БЭКАПИНИ ЮБОРИШ (WHATSAPP)",
                onClick = {
                    coroutineScope.launch {
                        viewModel.checkpointDatabase()

                        val dbName = "daftarcha_db"
                        val sourceFile = context.getDatabasePath(dbName)

                        if (!sourceFile.exists()) {
                            Toast.makeText(context, "Маълумотлар базаси ҳали яратилмаган", Toast.LENGTH_SHORT).show()
                        } else {
                            try {
                                val cacheDir = File(context.cacheDir, "backups")
                                cacheDir.mkdirs()
                                val destFile = File(cacheDir, "daftarcha_backup.db")
                                sourceFile.copyTo(destFile, overwrite = true)
                                val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", destFile)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/octet-stream"
                                    putExtra(Intent.EXTRA_STREAM, fileUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    setPackage("com.whatsapp")
                                }
                                context.startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(context, "WhatsApp ўрнатилмаган", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Хатолик: ${e.message}", Toast.LENGTH_LONG).show()
                                e.printStackTrace()
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
private fun LedgerButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean = false
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val bg = if (primary) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent
    val fg = if (primary) MaterialTheme.colorScheme.onPrimary else ink
    val border = if (primary) MaterialTheme.colorScheme.primary else ink

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(2.dp, border))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = fg
        )
    }
}
