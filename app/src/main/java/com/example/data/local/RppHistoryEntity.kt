package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rpp_history")
data class RppHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val mapel: String,
    val kelas: Int,
    val topik: String,
    val jp: Int = 2,
    val semester: String = "Ganjil",
    val hasil: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
