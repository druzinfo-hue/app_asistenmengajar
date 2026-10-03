package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "materi")
data class MateriEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val kelas: Int,
    val mapel: String,
    val pertemuan: Int,
    val bab: String,
    val judul: String,
    val bahan: String, // Stored as JSON or comma-separated string
    val tujuan: String,
    @ColumnInfo(name = "alokasi_jp")
    val alokasiJp: Int,
    val status: String? = null, // "selesai" or null
    @ColumnInfo(name = "tanggal_selesai")
    val tanggalSelesai: String? = null
)
