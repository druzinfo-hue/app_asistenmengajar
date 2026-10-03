package com.example.data.repository

import com.example.data.local.BukuChunkDao
import com.example.data.local.BukuChunkEntity
import com.example.data.local.BukuDao
import com.example.data.local.BukuEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BukuRepository(
    private val bukuDao: BukuDao,
    private val bukuChunkDao: BukuChunkDao
) {

    fun getAllBukuFlow(): Flow<List<BukuEntity>> = bukuDao.getAllBukuFlow()

    suspend fun getAllBuku(): List<BukuEntity> = bukuDao.getAllBuku()

    suspend fun getBuku(kelas: Int, mapel: String, tipe: String): BukuEntity? =
        bukuDao.getBuku(kelas, mapel, tipe)

    suspend fun getBukuById(id: Long): BukuEntity? = bukuDao.getBukuById(id)

    suspend fun searchBuku(keyword: String): List<BukuEntity> = bukuDao.searchBuku(keyword)

    suspend fun searchChunks(bukuId: Long, keyword: String): List<BukuChunkEntity> =
        bukuChunkDao.searchByKeyword(bukuId, keyword)

    suspend fun searchAllChunks(keyword: String): List<BukuChunkEntity> =
        bukuChunkDao.searchAllChunks(keyword)

    suspend fun getChunksByBuku(bukuId: Long): List<BukuChunkEntity> =
        bukuChunkDao.getChunksByBuku(bukuId)

    suspend fun insertBukuWithChunks(
        judul: String,
        kelas: Int,
        mapel: String,
        tipe: String,
        fileName: String,
        pages: List<Pair<Int, String>>
    ): Long {
        val now = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(Date())
        val buku = BukuEntity(
            judul = judul,
            kelas = kelas,
            mapel = mapel,
            tipe = tipe.lowercase().trim(),
            fileName = fileName,
            totalHalaman = pages.size,
            uploadedAt = now
        )
        val bukuId = bukuDao.insertBuku(buku)
        if (pages.isNotEmpty()) {
            val chunks = pages.map { (pageNumber, text) ->
                BukuChunkEntity(
                    bukuId = bukuId,
                    halaman = pageNumber,
                    text = text.trim()
                )
            }
            bukuChunkDao.insertChunks(chunks)
        }
        return bukuId
    }

    suspend fun deleteBuku(id: Long) {
        bukuChunkDao.deleteByBukuId(id)
        bukuDao.deleteBuku(id)
    }

    suspend fun getCount(): Int = bukuDao.getCount()
}
