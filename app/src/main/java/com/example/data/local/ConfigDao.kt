package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {
    @Query("SELECT value FROM config WHERE `key` = :key LIMIT 1")
    fun getConfig(key: String): Flow<String?>

    @Query("SELECT value FROM config WHERE `key` = :key LIMIT 1")
    suspend fun getConfigSync(key: String): String?

    @Query("SELECT * FROM config")
    fun getAllConfig(): Flow<List<ConfigEntity>>

    @Query("SELECT * FROM config")
    suspend fun getAllConfigList(): List<ConfigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setConfig(config: ConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(configList: List<ConfigEntity>)

    @Query("DELETE FROM config")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM config")
    suspend fun getCount(): Int
}
