package com.or.daftarcha.data.model

enum class UserRole {
    BRIGADIER, // Полный доступ: создание проектов, добавление сотрудников, назначение админов
    ADMIN,     // Доступ ко всему, кроме назначения админов, добавления новых сотрудников и проектов
    WORKER     // Доступ только к своим данным: рабочие дни, полученные деньги, общий расход
}

data class AuthUser(
    val id: String = "",
    val name: String = "",
    val role: UserRole = UserRole.WORKER,
    val employeeId: Int? = null,
    val phone: String? = null,
    val brigadierId: String = ""
) {
    val effectiveBrigadierId: String
        get() = if (role == UserRole.BRIGADIER) id else brigadierId.ifBlank { id }
}
