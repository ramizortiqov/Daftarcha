package com.or.daftarcha.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.or.daftarcha.data.db.DaftarchaDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val db: DaftarchaDatabase, // Hilt предоставит экземпляр БД
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val DB_NAME = "daftarcha_db"

    fun importDatabase(uri: Uri) {
        Log.d("ImportDB", "Начало импорта...")

        val targetDbFile = context.getDatabasePath(DB_NAME)
        val walFile = File(targetDbFile.path + "-wal")
        val shmFile = File(targetDbFile.path + "-shm")

        try {
            // 1. ЗАКРЫВАЕМ текущее соединение с БД
            db.close()
            Log.d("ImportDB", "База данных закрыта.")

            // 2. Удаляем старые файлы (WAL и SHM в первую очередь)
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()
            if (targetDbFile.exists()) targetDbFile.delete()
            Log.d("ImportDB", "Старые файлы БД удалены.")

            // 3. Копируем новый файл из Uri в targetDbFile
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(targetDbFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Log.d("ImportDB", "Новая БД скопирована.")

        } catch (e: Exception) {
            Log.e("ImportDB", "Ошибка импорта: ${e.message}")
            e.printStackTrace()
        }
    }
}