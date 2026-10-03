package com.example.data.repository

import com.example.data.local.PengumumanDao
import com.example.data.local.PengumumanHistoryEntity
import kotlinx.coroutines.flow.Flow

class PengumumanRepository(private val dao: PengumumanDao) {
    val allPengumuman: Flow<List<PengumumanHistoryEntity>> = dao.getAllFlow()

    suspend fun insert(item: PengumumanHistoryEntity): Long = dao.insert(item)

    suspend fun getAll(): List<PengumumanHistoryEntity> = dao.getAll()

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun deleteAll() = dao.deleteAll()
}
