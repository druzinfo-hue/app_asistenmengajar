package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "buku_chunk",
    indices = [
        Index(value = ["buku_id"]),
        Index(value = ["buku_id", "halaman"])
    ]
)
data class BukuChunkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "buku_id")
    val bukuId: Long,
    val halaman: Int,
    val text: String
)
