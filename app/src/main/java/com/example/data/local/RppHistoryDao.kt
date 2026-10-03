package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RppHistoryDao {
    @Query("SELECT * FROM rpp_history ORDER BY created_at DESC")
    fun getAllFlow(): Flow<List<RppHistoryEntity>>

    @Query("SELECT * FROM rpp_history ORDER BY created_at DESC")
    suspend fun getAll(): List<RppHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rpp: RppHistoryEntity): Long

    @Delete
    suspend fun delete(rpp: RppHistoryEntity)

    @Query("DELETE FROM rpp_history")
    suspend fun deleteAll()
}
