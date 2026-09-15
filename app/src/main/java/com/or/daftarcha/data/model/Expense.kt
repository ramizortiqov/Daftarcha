package com.or.daftarcha.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(entity = Project::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Employee::class, parentColumns = ["id"], childColumns = ["employeeId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index(value = ["projectId"]), Index(value = ["employeeId"])]
)
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val projectId: Int,
    val amount: Double,
    val description: String?,
    val date: String, // YYYY-MM-DD
    // Кимга тегишли харажат улуши. null = эски (тарқатилмаган) ёзув.
    val employeeId: Int? = null
)