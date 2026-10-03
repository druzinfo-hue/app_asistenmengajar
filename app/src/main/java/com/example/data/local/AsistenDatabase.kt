package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MateriEntity::class,
        JadwalEntity::class,
        ChatEntity::class,
        ConfigEntity::class,
        KalenderEntity::class,
        SoalHistoryEntity::class,
        RppHistoryEntity::class,
        PengumumanHistoryEntity::class,
        BukuEntity::class,
        BukuChunkEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AsistenDatabase : RoomDatabase() {
    abstract fun materiDao(): MateriDao
    abstract fun jadwalDao(): JadwalDao
    abstract fun chatDao(): ChatDao
    abstract fun configDao(): ConfigDao
    abstract fun kalenderDao(): KalenderDao
    abstract fun soalHistoryDao(): SoalHistoryDao
    abstract fun rppHistoryDao(): RppHistoryDao
    abstract fun pengumumanDao(): PengumumanDao
    abstract fun bukuDao(): BukuDao
    abstract fun bukuChunkDao(): BukuChunkDao

    companion object {
        @Volatile
        private var INSTANCE: AsistenDatabase? = null

        fun getDatabase(context: Context): AsistenDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AsistenDatabase::class.java,
                    "asisten_mengajar_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
