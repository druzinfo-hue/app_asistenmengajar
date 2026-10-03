package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kalender")
data class KalenderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tanggal: String,        // "2026-08-17" atau "" kalau range
    val tanggalMulai: String,   // untuk range
    val tanggalSelesai: String, // untuk range
    val keterangan: String,
    val jenis: String           // "libur_nasional", "libur_semester", "libur_keagamaan", "kegiatan", "ujian", "rapor", "peringatan"
)
