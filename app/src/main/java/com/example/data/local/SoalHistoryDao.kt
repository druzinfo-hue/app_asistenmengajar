package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SoalHistoryDao {
    @Query("SELECT * FROM soal_history ORDER BY created_at DESC")
    fun getAllFlow(): Flow<List<SoalHistoryEntity>>

    @Query("SELECT * FROM soal_history ORDER BY created_at DESC")
    suspend fun getAll(): List<SoalHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(soal: SoalHistoryEntity): Long

    @Delete
    suspend fun delete(soal: SoalHistoryEntity)

    @Query("DELETE FROM soal_history")
    suspend fun deleteAll()
}
