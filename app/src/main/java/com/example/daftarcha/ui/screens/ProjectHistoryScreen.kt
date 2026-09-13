package com.example.daftarcha.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.daftarcha.viewmodel.ProjectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterialCode")
@Composable
fun ProjectHistoryScreen(
    onBackClick: () -> Unit,
    viewModel: ProjectViewModel = hiltViewModel()
) {
    val history by viewModel.combinedHistory.collectAsState()
    val project by viewModel.project.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project?.name ?: "Тарих") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Орқага")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            items(history) { item ->
                val (icon, tint) = when (item.type) {
                    "Харажат" -> Icons.Default.ArrowDownward to MaterialTheme.colorScheme.error
                    "Пул берди" -> Icons.Default.ArrowUpward to com.example.daftarcha.ui.theme.PositiveGreen
                    else -> Icons.Default.Payment to MaterialTheme.colorScheme.primary
                }

                ListItem(
                    headlineContent = { Text("%.2f с - ${item.type}".format(item.amount), fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(item.details) },
                    overlineContent = { Text(item.date) },
                    leadingContent = { Icon(icon, contentDescription = item.type, tint = tint) }
                )
                Divider()
            }
        }
    }
}