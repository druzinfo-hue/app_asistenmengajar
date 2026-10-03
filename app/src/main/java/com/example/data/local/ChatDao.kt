package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_history ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(chat: ChatEntity): Long

    @Query("DELETE FROM chat_history")
    suspend fun clearHistory()

    @Query("DELETE FROM chat_history")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM chat_history")
    suspend fun getCount(): Int

    @Query("SELECT * FROM (SELECT * FROM chat_history ORDER BY id DESC LIMIT :limit) ORDER BY id ASC")
    suspend fun getRecentMessages(limit: Int): List<ChatEntity>
}
