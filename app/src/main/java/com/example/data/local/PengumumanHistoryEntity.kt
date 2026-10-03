package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pengumuman_history")
data class PengumumanHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val jenis: String,
    val kelas: Int,
    val konten: String,
    @ColumnInfo(name = "info_tambahan")
    val info_tambahan: String
)
