package com.example.daftarcha.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.daftarcha.ui.components.ProjectCard
import com.example.daftarcha.viewmodel.HomeViewModel

@Composable
fun ProjectsTab(
    viewModel: HomeViewModel = hiltViewModel(),
    onProjectClick: (Int) -> Unit // Передаем ID проекта при клике
) {
    // Получаем "живой" список проектов
    val projectItems by viewModel.projectListItems.collectAsState()

    // Аналог MDList + ScrollView, но более эффективный
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // Аналог цикла for p in projects:
        items(projectItems) { item ->
            ProjectCard(
                projectName = item.project.name,
                employeeCount = item.employeeCount, // <-- Теперь реальное значение
                totalDays = item.totalDays,         // <-- Теперь реальное значение
                onClick = { onProjectClick(item.project.id) }
            )
        }
    }
}