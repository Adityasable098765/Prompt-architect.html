package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptDao {
    @Query("SELECT * FROM saved_prompts ORDER BY timestamp DESC")
    fun getAllPrompts(): Flow<List<SavedPrompt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrompt(prompt: SavedPrompt): Long

    @Query("DELETE FROM saved_prompts WHERE id = :id")
    suspend fun deletePromptById(id: Int)
}
