package com.or.daftarcha.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "attendance",
    primaryKeys = ["projectId", "employeeId", "date"], // Составной ключ
    foreignKeys = [
        ForeignKey(entity = Project::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Employee::class, parentColumns = ["id"], childColumns = ["employeeId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["projectId"]), Index(value = ["employeeId"]), Index(value = ["date"])]
)
data class Attendance(
    val projectId: Int,
    val employeeId: Int,
    val date: String, // YYYY-MM-DD
    val present: Boolean // Room конвертирует в 0 или 1
)