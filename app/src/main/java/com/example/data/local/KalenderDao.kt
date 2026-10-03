package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface KalenderDao {
    @Query("SELECT * FROM kalender ORDER BY id ASC")
    fun getAllFlow(): Flow<List<KalenderEntity>>

    @Query("SELECT * FROM kalender ORDER BY id ASC")
    suspend fun getAll(): List<KalenderEntity>

    @Query("SELECT * FROM kalender WHERE jenis = :jenis ORDER BY id ASC")
    suspend fun getByJenis(jenis: String): List<KalenderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(kalender: KalenderEntity): Long

    @Update
    suspend fun update(kalender: KalenderEntity)

    @Delete
    suspend fun delete(kalender: KalenderEntity)

    @Query("DELETE FROM kalender WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<KalenderEntity>)

    @Query("DELETE FROM kalender")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM kalender")
    suspend fun getCount(): Int
}
