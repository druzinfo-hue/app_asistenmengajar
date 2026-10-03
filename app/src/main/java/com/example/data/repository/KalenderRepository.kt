package com.example.data.repository

import com.example.data.local.KalenderDao
import com.example.data.local.KalenderEntity
import kotlinx.coroutines.flow.Flow

class KalenderRepository(private val kalenderDao: KalenderDao) {

    fun getAllFlow(): Flow<List<KalenderEntity>> = kalenderDao.getAllFlow()

    suspend fun getAll(): List<KalenderEntity> = kalenderDao.getAll()

    suspend fun getByJenis(jenis: String): List<KalenderEntity> = kalenderDao.getByJenis(jenis)

    suspend fun insertAll(items: List<KalenderEntity>) = kalenderDao.insertAll(items)

    suspend fun insert(kalender: KalenderEntity): Long = kalenderDao.insert(kalender)

    suspend fun update(kalender: KalenderEntity) = kalenderDao.update(kalender)

    suspend fun delete(kalender: KalenderEntity) = kalenderDao.delete(kalender)

    suspend fun deleteById(id: Long) = kalenderDao.deleteById(id)

    suspend fun deleteAll() = kalenderDao.deleteAll()
}
