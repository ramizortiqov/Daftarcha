package com.example.daftarcha.di

import android.content.Context
import androidx.room.Room
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