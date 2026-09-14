package com.example.daftarcha.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.daftarcha.data.db.DaftarchaDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// 1. Объявляем, что это "Модуль Hilt"
@Module
// 2. Говорим, что этот модуль будет "жить" пока "живет" приложение
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                db.execSQL("ALTER TABLE employees ADD COLUMN brigadierId TEXT NOT NULL DEFAULT ''")
            } catch (e: Exception) {
                // Column might already exist
            }
            try {
                db.execSQL("ALTER TABLE projects ADD COLUMN brigadierId TEXT NOT NULL DEFAULT ''")
            } catch (e: Exception) {
                // Column might already exist
            }
            try {
                db.execSQL("ALTER TABLE app_users ADD COLUMN brigadierId TEXT NOT NULL DEFAULT ''")
            } catch (e: Exception) {
                // Column might already exist
            }
        }
    }

    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                db.execSQL("ALTER TABLE expenses ADD COLUMN employeeId INTEGER")
            } catch (e: Exception) {
                // Column might already exist
            }
            db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_employeeId ON expenses(employeeId)")
        }
    }

    // 3. "ИНСТРУКЦИЯ №1": Как создавать саму базу данных
    @Provides
    @Singleton // @Singleton = Создать ОДИН раз и использовать этот экземпляр везде
    fun provideDaftarchaDatabase(
        @ApplicationContext context: Context // Hilt сам "вставит" сюда контекст
    ): DaftarchaDatabase {
        return Room.databaseBuilder(
            context,
            DaftarchaDatabase::class.java,
            "daftarcha_db" // Имя файла вашей базы данных на телефоне
        )
            .addMigrations(MIGRATION_8_9, MIGRATION_9_10)
            .fallbackToDestructiveMigration()
            .build()
    }

    // 4. "ИНСТРУКЦИИ №2-8": Как "раздавать" все DAO (инструменты)
    //    Hilt видит, что им нужен DaftarchaDatabase, берет его из
    //    инструкции №1 и автоматически передает.

    @Provides
    fun provideProjectDao(db: DaftarchaDatabase) = db.projectDao()

    @Provides
    fun provideEmployeeDao(db: DaftarchaDatabase) = db.employeeDao()

    @Provides
    fun provideProjectEmployeeDao(db: DaftarchaDatabase) = db.projectEmployeeDao()

    @Provides
    fun provideAttendanceDao(db: DaftarchaDatabase) = db.attendanceDao()

    @Provides
    fun provideExpenseDao(db: DaftarchaDatabase) = db.expenseDao()

    @Provides
    fun provideBonusDao(db: DaftarchaDatabase) = db.bonusDao()

    @Provides
    fun providePaymentDao(db: DaftarchaDatabase) = db.paymentDao()

    @Provides
    fun provideAppUserDao(db: DaftarchaDatabase) = db.appUserDao()
}