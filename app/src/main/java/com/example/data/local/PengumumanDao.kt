package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PengumumanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PengumumanHistoryEntity): Long

    @Query("SELECT * FROM pengumuman_history ORDER BY timestamp DESC")
    suspend fun getAll(): List<PengumumanHistoryEntity>

    @Query("SELECT * FROM pengumuman_history ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<PengumumanHistoryEntity>>

    @Query("DELETE FROM pengumuman_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pengumuman_history")
    suspend fun deleteAll()
}
