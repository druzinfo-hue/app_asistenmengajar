package com.example.data.repository

import com.example.data.local.SoalHistoryDao
import com.example.data.local.SoalHistoryEntity
import kotlinx.coroutines.flow.Flow

class SoalHistoryRepository(private val soalHistoryDao: SoalHistoryDao) {

    fun getAllFlow(): Flow<List<SoalHistoryEntity>> = soalHistoryDao.getAllFlow()

    suspend fun insert(soal: SoalHistoryEntity): Long = soalHistoryDao.insert(soal)

    suspend fun delete(soal: SoalHistoryEntity) = soalHistoryDao.delete(soal)

    suspend fun deleteAll() = soalHistoryDao.deleteAll()
}
