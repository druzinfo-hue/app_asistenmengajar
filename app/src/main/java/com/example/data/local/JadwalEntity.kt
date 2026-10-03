package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jadwal")
data class JadwalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val kelas: Int,
    val hari: String,
    @ColumnInfo(name = "jam_mulai")
    val jamMulai: String,
    @ColumnInfo(name = "jam_selesai")
    val jamSelesai: String,
    val mapel: String
)
