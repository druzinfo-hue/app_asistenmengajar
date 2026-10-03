package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "soal_history")
data class SoalHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val mapel: String,
    val kelas: Int,
    val topik: String,
    val jumlah: Int,
    val tipe: String,
    val hasil: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
