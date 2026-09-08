package com.example.daftarcha.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val cost: Double = 0.0,
    val startDate: String?, // YYYY-MM-DD
    val endDate: String? = null,
    val isArchived: Boolean = false,
    val brigadierId: String = ""
)