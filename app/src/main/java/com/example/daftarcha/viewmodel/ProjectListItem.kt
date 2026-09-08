package com.example.daftarcha.viewmodel // Или com.example.daftarcha.data.model

import com.example.daftarcha.data.model.Project

data class ProjectListItem(
    val project: Project,
    val employeeCount: Int,
    val totalDays: Int
)