package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MateriDao {
    @Query("SELECT * FROM materi ORDER BY kelas ASC, mapel ASC, pertemuan ASC")
    fun getAllMateri(): Flow<List<MateriEntity>>

    @Query("SELECT * FROM materi WHERE kelas = :kelas ORDER BY mapel ASC, pertemuan ASC")
    fun getMateriByKelas(kelas: Int): Flow<List<MateriEntity>>

    @Query("SELECT * FROM materi WHERE kelas = :kelas AND mapel = :mapel ORDER BY pertemuan ASC")
    fun getMateriByKelasAndMapel(kelas: Int, mapel: String): Flow<List<MateriEntity>>

    @Query("SELECT * FROM materi WHERE kelas = :kelas AND mapel = :mapel AND status IS NULL ORDER BY pertemuan ASC LIMIT 1")
    suspend fun getNextMateri(kelas: Int, mapel: String): MateriEntity?

    @Query("SELECT * FROM materi WHERE judul LIKE '%' || :keyword || '%' OR bab LIKE '%' || :keyword || '%' OR mapel LIKE '%' || :keyword || '%' LIMIT 5")
    suspend fun searchByKeyword(keyword: String): List<MateriEntity>

    @Query("SELECT * FROM materi WHERE kelas = :kelas AND LOWER(mapel) LIKE '%' || LOWER(:mapel) || '%' AND (LOWER(bab) LIKE '%' || LOWER(:topik) || '%' OR LOWER(judul) LIKE '%' || LOWER(:topik) || '%') ORDER BY pertemuan ASC")
    suspend fun searchByKelasMapelTopik(kelas: Int, mapel: String, topik: String): List<MateriEntity>

    @Query("SELECT * FROM materi")
    suspend fun getAllMateriList(): List<MateriEntity>

    @Query("UPDATE materi SET status = :status, tanggal_selesai = :tanggalSelesai WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String?, tanggalSelesai: String?)

    @Query("UPDATE materi SET status = NULL, tanggal_selesai = NULL WHERE kelas = :kelas AND mapel = :mapel AND pertemuan > :pertemuan")
    suspend fun resetProgressAfter(kelas: Int, mapel: String, pertemuan: Int)

    @Query("UPDATE materi SET status = NULL, tanggal_selesai = NULL WHERE kelas = :kelas AND mapel = :mapel")
    suspend fun resetAllForMapel(kelas: Int, mapel: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(materiList: List<MateriEntity>)

    @Query("DELETE FROM materi")
    suspend fun deleteAll()

    @Query("UPDATE materi SET status = NULL, tanggal_selesai = NULL")
    suspend fun resetAllStatus()

    @Query("SELECT COUNT(*) FROM materi")
    suspend fun getCount(): Int
}
