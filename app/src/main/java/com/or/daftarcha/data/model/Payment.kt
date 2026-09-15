package com.or.daftarcha.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(entity = Employee::class, parentColumns = ["id"], childColumns = ["employeeId"], onDelete = ForeignKey.CASCADE),
        // projectId может быть null (не привязан к проекту),
        // поэтому onDelete = SET_NULL (если проект удалят, выплата останется)
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["employeeId"]), Index(value = ["projectId"])]
)
data class Payment(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val employeeId: Int,
    val projectId: Int?, // Может быть null
    val amount: Double,
    val date: String, // YYYY-MM-DD
    val description: String? = null
)