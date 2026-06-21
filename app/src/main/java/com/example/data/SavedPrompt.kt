package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_prompts")
data class SavedPrompt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val prompt: String,
    val negativePrompt: String,
    val style: String,
    val lighting: String,
    val composition: String,
    val stepByStepGuide: String,
    val imagePath: String? = null,
    val isAnalyzed: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
