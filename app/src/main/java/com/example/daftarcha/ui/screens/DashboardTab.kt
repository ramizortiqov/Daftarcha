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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.daftarcha.ui.components.StatsCard
import com.example.daftarcha.viewmodel.HomeViewModel
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
        Divider(modifier = Modifier.padding(vertical = 16.dp))

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