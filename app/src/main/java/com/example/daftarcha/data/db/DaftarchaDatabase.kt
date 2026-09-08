package com.example.daftarcha.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.daftarcha.data.dao.* // Импортируем все DAO
import com.example.daftarcha.data.model.* // Импортируем все Entities

// 1. Говорим Room, что это класс базы данных
@Database(
    // 2. Перечисляем ВСЕ наши таблицы (Entities)
    entities = [
        Project::class,
        Employee::class,
        ProjectEmployee::class,
        Attendance::class,
        Expense::class,
        Bonus::class,
        Payment::class
    ],
    // 3. Указываем версию. Если вы измените структуру, нужно будет поменять на 2
    version = 7,
    // 4. Отключаем экспорт схемы, чтобы избежать предупреждений при сборке
    exportSchema = false
)
// 5. Класс должен быть абстрактным и наследовать RoomDatabase
abstract class DaftarchaDatabase : RoomDatabase() {

    // 6. Мы должны объявить по одной абстрактной функции для КАЖДОГО DAO.
    //    Room и Hilt сами реализуют их за нас.

    abstract fun projectDao(): ProjectDao
    abstract fun employeeDao(): EmployeeDao
    abstract fun projectEmployeeDao(): ProjectEmployeeDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun bonusDao(): BonusDao
    abstract fun paymentDao(): PaymentDao

}