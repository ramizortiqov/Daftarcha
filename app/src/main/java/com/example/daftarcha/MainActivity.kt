package com.example.daftarcha

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.daftarcha.ui.components.AddDialog
import com.example.daftarcha.ui.screens.MainScreen
import com.example.daftarcha.ui.theme.DaftarchaTheme
import com.example.daftarcha.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DaftarchaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showAddProjectDialog by remember { mutableStateOf(false) }
                    var showAddEmployeeDialog by remember { mutableStateOf(false) }

                    MainScreen(
                        onShowAddProjectDialog = { showAddProjectDialog = true },
                        onShowAddEmployeeDialog = { showAddEmployeeDialog = true }
                    )

                    if (showAddProjectDialog) {
                        AddDialog(
                            title = "Янги иш",
                            nameLabel = "Иш номи",
                            showDateField = true,
                            onDismiss = { showAddProjectDialog = false },
                            onConfirm = { name, _, date ->
                                homeViewModel.addProject(name, date)
                                showAddProjectDialog = false
                            }
                        )
                    }

                    if (showAddEmployeeDialog) {
                        AddDialog(
                            title = "Янги шерик",
                            nameLabel = "Шерикнинг исми",
                            phoneLabel = "Телефон (ихтиёрий)",
                            onDismiss = { showAddEmployeeDialog = false },
                            onConfirm = { name, phone, _ ->
                                homeViewModel.addEmployee(name, phone)
                                showAddEmployeeDialog = false
                            }
                        )
                    }
                }
            }
        }
    }
}