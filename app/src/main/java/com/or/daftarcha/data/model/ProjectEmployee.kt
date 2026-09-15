package com.or.daftarcha.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "project_employees",
    primaryKeys = ["projectId", "employeeId"], // Составной первичный ключ
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Employee::class,
            parentColumns = ["id"],
            childColumns = ["employeeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    // Индексы ускоряют поиск
    indices = [Index(value = ["projectId"]), Index(value = ["employeeId"])]
)
data class ProjectEmployee(
    val projectId: Int,
    val employeeId: Int
)