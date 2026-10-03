package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "buku")
data class BukuEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val judul: String,
    val kelas: Int,
    val mapel: String,
    val tipe: String, // "guru" atau "siswa"
    @ColumnInfo(name = "file_name")
    val fileName: String = "",
    @ColumnInfo(name = "file_path")
    val filePath: String? = null,
    @ColumnInfo(name = "total_halaman")
    val totalHalaman: Int = 0,
    @ColumnInfo(name = "uploaded_at")
    val uploadedAt: String = ""
)
