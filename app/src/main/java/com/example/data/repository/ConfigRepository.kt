package com.example.data.repository

import com.example.data.local.ConfigDao
import com.example.data.local.ConfigEntity
import kotlinx.coroutines.flow.Flow

class ConfigRepository(private val configDao: ConfigDao) {

    fun getConfig(key: String): Flow<String?> = configDao.getConfig(key)

    suspend fun getConfigSync(key: String): String? = configDao.getConfigSync(key)

    fun getAllConfig(): Flow<List<ConfigEntity>> = configDao.getAllConfig()

    suspend fun setConfig(key: String, value: String) {
        configDao.setConfig(ConfigEntity(key = key, value = value))
    }

    suspend fun getAllConfigList(): List<ConfigEntity> = configDao.getAllConfigList()

    suspend fun insertAll(configList: List<ConfigEntity>) {
        configDao.insertAll(configList)
    }

    suspend fun deleteAll() = configDao.deleteAll()
}
