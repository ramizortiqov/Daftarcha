package com.example.daftarcha.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_users")
data class AppUser(
    @PrimaryKey
    val loginId: String, // ID для входа, выдаваемый бригадиром (например, "admin", "ibragim", "emp_1", "001")
    val password: String, // Пароль для входа
    val name: String, // Отображаемое имя
    val role: UserRole = UserRole.WORKER, // BRIGADIER, ADMIN, WORKER
    val employeeId: Int? = null, // Связка с записью в таблице employees (для WORKER)
    val phone: String? = null
)
