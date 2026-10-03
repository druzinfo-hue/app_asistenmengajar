package com.example.data.repository

import com.example.data.local.JadwalDao
import com.example.data.local.JadwalEntity
import kotlinx.coroutines.flow.Flow

class JadwalRepository(private val jadwalDao: JadwalDao) {

    fun getAllJadwal(): Flow<List<JadwalEntity>> = jadwalDao.getAllJadwal()

    fun getJadwalByHari(hari: String): Flow<List<JadwalEntity>> = jadwalDao.getJadwalByHari(hari)

    fun getJadwalByHariAndKelas(hari: String, kelas: Int): Flow<List<JadwalEntity>> =
        jadwalDao.getJadwalByHariAndKelas(hari, kelas)

    suspend fun getJadwalByHariSync(hari: String): List<JadwalEntity> =
        jadwalDao.getJadwalByHariSync(hari)

    suspend fun insertAll(jadwalList: List<JadwalEntity>) {
        jadwalDao.insertAll(jadwalList)
    }

    suspend fun insert(jadwal: JadwalEntity): Long = jadwalDao.insert(jadwal)

    suspend fun update(jadwal: JadwalEntity) = jadwalDao.update(jadwal)

    suspend fun delete(jadwal: JadwalEntity) = jadwalDao.delete(jadwal)

    suspend fun deleteById(id: Int) = jadwalDao.deleteById(id)

    suspend fun getAllJadwalList(): List<JadwalEntity> = jadwalDao.getAllJadwalList()

    suspend fun deleteAll() = jadwalDao.deleteAll()
}
