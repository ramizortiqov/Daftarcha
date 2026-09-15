package com.or.daftarcha.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bonuses",
    foreignKeys = [
        ForeignKey(entity = Project::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["projectId"])]
)
data class Bonus(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val projectId: Int,
    val amount: Double,
    val description: String?,
    val date: String, // YYYY-MM-DD
    // Ким қанча олгани (ва тарқатилмай қолган қисми, агар бўлса) — тарихда кўрсатиш учун.
    val distributionNote: String? = null,
    // Тарқатилмай, устада (кассада) қолган қисми.
    val unallocatedAmount: Double = 0.0
)