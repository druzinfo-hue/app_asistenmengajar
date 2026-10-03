package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JadwalDao {
    @Query("SELECT * FROM jadwal ORDER BY id ASC")
    fun getAllJadwal(): Flow<List<JadwalEntity>>

    @Query("SELECT * FROM jadwal WHERE hari = :hari ORDER BY jam_mulai ASC")
    fun getJadwalByHari(hari: String): Flow<List<JadwalEntity>>

    @Query("SELECT * FROM jadwal WHERE hari = :hari AND kelas = :kelas ORDER BY jam_mulai ASC")
    fun getJadwalByHariAndKelas(hari: String, kelas: Int): Flow<List<JadwalEntity>>

    @Query("SELECT * FROM jadwal WHERE hari = :hari ORDER BY jam_mulai ASC")
    suspend fun getJadwalByHariSync(hari: String): List<JadwalEntity>

    @Query("SELECT * FROM jadwal")
    suspend fun getAllJadwalList(): List<JadwalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(jadwal: JadwalEntity): Long

    @Update
    suspend fun update(jadwal: JadwalEntity)

    @Delete
    suspend fun delete(jadwal: JadwalEntity)

    @Query("DELETE FROM jadwal WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM jadwal WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(jadwalList: List<JadwalEntity>)

    @Query("DELETE FROM jadwal")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM jadwal")
    suspend fun getCount(): Int
}
