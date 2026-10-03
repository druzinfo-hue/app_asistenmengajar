package com.example.data.repository

import com.example.data.local.MateriDao
import com.example.data.local.MateriEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MateriRepository(private val materiDao: MateriDao) {

    fun getAllMateri(): Flow<List<MateriEntity>> = materiDao.getAllMateri()

    fun getMateriByKelas(kelas: Int): Flow<List<MateriEntity>> = materiDao.getMateriByKelas(kelas)

    fun getMateriByKelasAndMapel(kelas: Int, mapel: String): Flow<List<MateriEntity>> =
        materiDao.getMateriByKelasAndMapel(kelas, mapel)

    suspend fun getNextMateri(kelas: Int, mapel: String): MateriEntity? =
        materiDao.getNextMateri(kelas, mapel)

    suspend fun markCompleted(id: Int, isCompleted: Boolean) {
        val dateString = if (isCompleted) {
            SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID")).format(Date())
        } else null
        val statusString = if (isCompleted) "selesai" else null
        materiDao.updateStatus(id, statusString, dateString)
    }

    suspend fun resetProgressAfter(kelas: Int, mapel: String, pertemuan: Int) {
        materiDao.resetProgressAfter(kelas, mapel, pertemuan)
    }

    suspend fun resetAllForMapel(kelas: Int, mapel: String) {
        materiDao.resetAllForMapel(kelas, mapel)
    }

    suspend fun insertAll(materiList: List<MateriEntity>) {
        materiDao.insertAll(materiList)
    }

    suspend fun getAllMateriList(): List<MateriEntity> = materiDao.getAllMateriList()

    suspend fun deleteAll() = materiDao.deleteAll()
}
