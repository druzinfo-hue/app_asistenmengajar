package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BukuDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuku(buku: BukuEntity): Long

    @Query("SELECT * FROM buku WHERE kelas = :kelas AND LOWER(mapel) LIKE '%' || LOWER(:mapel) || '%' AND LOWER(tipe) = LOWER(:tipe) ORDER BY id DESC LIMIT 1")
    suspend fun getBuku(kelas: Int, mapel: String, tipe: String): BukuEntity?

    @Query("SELECT * FROM buku WHERE id = :id LIMIT 1")
    suspend fun getBukuById(id: Long): BukuEntity?

    @Query("SELECT * FROM buku ORDER BY kelas ASC, mapel ASC, tipe ASC")
    fun getAllBukuFlow(): Flow<List<BukuEntity>>

    @Query("SELECT * FROM buku ORDER BY kelas ASC, mapel ASC, tipe ASC")
    suspend fun getAllBuku(): List<BukuEntity>

    @Query("SELECT * FROM buku WHERE LOWER(judul) LIKE '%' || LOWER(:keyword) || '%' OR LOWER(mapel) LIKE '%' || LOWER(:keyword) || '%'")
    suspend fun searchBuku(keyword: String): List<BukuEntity>

    @Query("DELETE FROM buku WHERE id = :id")
    suspend fun deleteBuku(id: Long)

    @Delete
    suspend fun delete(buku: BukuEntity)

    @Query("SELECT COUNT(*) FROM buku")
    suspend fun getCount(): Int
}
