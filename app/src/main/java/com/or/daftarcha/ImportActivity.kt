package com.or.daftarcha

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.or.daftarcha.ui.theme.DaftarchaTheme
import com.or.daftarcha.viewmodel.ImportViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.system.exitProcess // <-- ИМПОРТ ДЛЯ ВЫХОДА

@AndroidEntryPoint
class ImportActivity : ComponentActivity() {

    private val viewModel: ImportViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val importUri: Uri? = intent.data

        if (importUri == null) {
            Toast.makeText(this, "Ошибка: Файл не найден", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContent {
            DaftarchaTheme {
                ImportDialog(
                    onConfirm = {
                        // 1. Выполняем импорт
                        viewModel.importDatabase(importUri)

                        // 2. Сообщаем пользователю
                        Toast.makeText(applicationContext, "Импорт завершен. Перезапустите приложение.", Toast.LENGTH_LONG).show()

                        // 3. ПРИНУДИТЕЛЬНО УБИВАЕМ ПРИЛОЖЕНИЕ
                        finishAffinity() // Закрываем все Activity
                        exitProcess(0) // Убиваем процесс
                    },
                    onDismiss = {
                        finish() // Просто закрываем Activity
                    }
                )
            }
        }
    }
}

@Composable
fun ImportDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Импорт базы данных") },
        text = {
            Text("ВНИМАНИЕ!\n\nИмпорт заменит ВСЕ ваши текущие данные на данные из файла. Это действие необратимо.\n\nПродолжить?")
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Восстановить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}