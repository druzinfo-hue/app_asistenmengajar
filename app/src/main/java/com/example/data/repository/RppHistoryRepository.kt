package com.example.data.repository

import com.example.data.local.RppHistoryDao
import com.example.data.local.RppHistoryEntity
import kotlinx.coroutines.flow.Flow

class RppHistoryRepository(private val rppHistoryDao: RppHistoryDao) {

    fun getAllFlow(): Flow<List<RppHistoryEntity>> = rppHistoryDao.getAllFlow()

    suspend fun insert(rpp: RppHistoryEntity): Long = rppHistoryDao.insert(rpp)

    suspend fun delete(rpp: RppHistoryEntity) = rppHistoryDao.delete(rpp)

    suspend fun deleteAll() = rppHistoryDao.deleteAll()
}
