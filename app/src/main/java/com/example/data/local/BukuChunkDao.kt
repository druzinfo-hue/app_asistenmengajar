package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BukuChunkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<BukuChunkEntity>)

    @Query("SELECT * FROM buku_chunk WHERE buku_id = :bukuId AND (LOWER(text) LIKE '%' || LOWER(:keyword) || '%') ORDER BY halaman ASC LIMIT 10")
    suspend fun searchByKeyword(bukuId: Long, keyword: String): List<BukuChunkEntity>

    @Query("SELECT * FROM buku_chunk WHERE buku_id = :bukuId ORDER BY halaman ASC")
    suspend fun getChunksByBuku(bukuId: Long): List<BukuChunkEntity>

    @Query("SELECT * FROM buku_chunk WHERE buku_id = :bukuId AND halaman = :halaman LIMIT 1")
    suspend fun getChunkByHalaman(bukuId: Long, halaman: Int): BukuChunkEntity?

    @Query("SELECT * FROM buku_chunk WHERE (LOWER(text) LIKE '%' || LOWER(:keyword) || '%') ORDER BY buku_id ASC, halaman ASC LIMIT 15")
    suspend fun searchAllChunks(keyword: String): List<BukuChunkEntity>

    @Query("DELETE FROM buku_chunk WHERE buku_id = :bukuId")
    suspend fun deleteByBukuId(bukuId: Long)

    @Query("SELECT COUNT(*) FROM buku_chunk")
    suspend fun getCount(): Int
}
