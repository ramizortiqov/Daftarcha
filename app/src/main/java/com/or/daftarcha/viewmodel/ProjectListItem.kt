package com.or.daftarcha.viewmodel // Или com.or.daftarcha.data.model

import com.or.daftarcha.data.model.Project

data class ProjectListItem(
    val project: Project,
    val employeeCount: Int,
    val totalDays: Int
)