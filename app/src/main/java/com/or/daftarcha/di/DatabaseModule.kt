package com.or.daftarcha.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.or.daftarcha.data.db.DaftarchaDatabase
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

    // SQLite's ALTER TABLE can't add a FOREIGN KEY constraint, so a plain
    // "ADD COLUMN employeeId" leaves the on-disk table without the FK to
    // employees(id) that the Expense entity declares. Room's schema validation
    // then fails on every app start with "Migration didn't properly handle:
    // expenses(...)". The table has to be recreated to add the FK properly.
    private fun rebuildExpensesTableWithEmployeeForeignKey(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS expenses_new (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                projectId INTEGER NOT NULL,
                amount REAL NOT NULL,
                description TEXT,
                date TEXT NOT NULL,
                employeeId INTEGER,
                FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE,
                FOREIGN KEY(employeeId) REFERENCES employees(id) ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO expenses_new (id, projectId, amount, description, date, employeeId)
            SELECT id, projectId, amount, description, date, employeeId FROM expenses
            """.trimIndent()
        )
        db.execSQL("DROP TABLE expenses")
        db.execSQL("ALTER TABLE expenses_new RENAME TO expenses")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_projectId ON expenses(projectId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_employeeId ON expenses(employeeId)")
    }

    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                db.execSQL("ALTER TABLE expenses ADD COLUMN employeeId INTEGER")
            } catch (e: Exception) {
                // Column might already exist
            }
            rebuildExpensesTableWithEmployeeForeignKey(db)
        }
    }

    private val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                db.execSQL("ALTER TABLE bonuses ADD COLUMN distributionNote TEXT")
            } catch (e: Exception) {
                // Column might already exist
            }
            try {
                db.execSQL("ALTER TABLE bonuses ADD COLUMN unallocatedAmount REAL NOT NULL DEFAULT 0.0")
            } catch (e: Exception) {
                // Column might already exist
            }
            // Devices that already ran the old, broken MIGRATION_9_10 are stuck with an
            // expenses table missing the employeeId foreign key. Rebuilding again here
            // is a no-op if it's already correct, and repairs it otherwise.
            rebuildExpensesTableWithEmployeeForeignKey(db)
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
            .addMigrations(MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
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