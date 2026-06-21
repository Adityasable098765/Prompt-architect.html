package com.example.data

import android.content.Context
import android.net.Uri
import java.io.File
import kotlinx.coroutines.flow.Flow

class PromptRepository(private val promptDao: PromptDao) {
    val allPrompts: Flow<List<SavedPrompt>> = promptDao.getAllPrompts()

    suspend fun insert(prompt: SavedPrompt): Long {
        return promptDao.insertPrompt(prompt)
    }

    suspend fun deleteById(id: Int) {
        promptDao.deletePromptById(id)
    }

    fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val fileName = "prompt_img_${System.currentTimeMillis()}.jpg"
            val file = File(context.filesDir, fileName)
            file.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
