package com.example.daftarcha.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.daftarcha.ui.components.StatsCard
import com.example.daftarcha.viewmodel.HomeViewModel
import com.example.daftarcha.viewmodel.SyncViewModel
import com.example.daftarcha.data.sync.SyncStatus
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.navigation.NavHostController
import com.example.daftarcha.data.model.UserRole

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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Статистика",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
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

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Тезкор амаллар",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        if (isBrigadier) {
            Button(
                onClick = onAddProjectClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ЯНГИ ИШ ҚЎШИШ")
            }

            Button(
                onClick = onAddEmployeeClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ШЕРИК ҚЎШИШ")
            }
        }
        OutlinedButton(
            onClick = {
                navController.navigate(Screen.Archive.route)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("АРХИВ ИШЛАР")
        }

        // --- FIREBASE CLOUD SYNC CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = "Firebase синхронизация",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Firebase булутли синхронизация",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Охирги: $lastSyncText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (syncStatus is SyncStatus.Error) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = (syncStatus as SyncStatus.Error).message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                } else if (syncStatus is SyncStatus.Success) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = (syncStatus as SyncStatus.Success).message,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Button(
                    onClick = { syncViewModel.triggerSync() },
                    enabled = syncStatus !is SyncStatus.InProgress,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (syncStatus is SyncStatus.InProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Синхронизация қилинмоқда...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("БУЛУТ БИЛАН СИНХРОНИЗАЦИЯ ҚИЛИШ")
                    }
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 12.dp))

        OutlinedButton(
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
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("БД БЭКАПИНИ ЮБОРИШ (WHATSAPP)")
        }
    }
}