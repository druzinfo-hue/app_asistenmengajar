package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.local.AsistenDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object DataImporter {

    private const val TAG = "DataImporter"

    suspend fun importDataIfNeeded(context: Context, database: AsistenDatabase) = withContext(Dispatchers.IO) {
        val materiDao = database.materiDao()
        val configDao = database.configDao()

        try {
            val jsonString = context.assets.open("data_mengajar.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val exportedAt = root.optString("exported_at", root.optString("version", "1.0"))
            val currentImported = configDao.getConfigSync("data_imported_version")

            if (materiDao.getCount() > 0 && database.kalenderDao().getCount() > 0 && currentImported == exportedAt) {
                Log.d(TAG, "Database already up to date with version $exportedAt. Skipping import.")
                return@withContext
            }

            val result = BackupHelper.restoreFromJson(context, jsonString, database)
            when (result) {
                is BackupHelper.RestoreResult.Success -> {
                    Log.d(TAG, "DataImporter: ${result.message}")
                }
                is BackupHelper.RestoreResult.Error -> {
                    Log.e(TAG, "DataImporter error: ${result.message}")
                }
            }

            seedSampleBukuIfNeeded(database)
        } catch (e: Exception) {
            Log.e(TAG, "Error importing assets data: ${e.message}", e)
        }
    }

    suspend fun seedSampleBukuIfNeeded(database: AsistenDatabase) = withContext(Dispatchers.IO) {
        val bukuDao = database.bukuDao()
        val chunkDao = database.bukuChunkDao()
        if (bukuDao.getCount() > 0) return@withContext

        try {
            // 1. Buku Guru Matematika Kelas 4
            val idGuruMat = bukuDao.insertBuku(
                com.example.data.local.BukuEntity(
                    judul = "Buku Panduan Guru Matematika Kelas 4",
                    kelas = 4,
                    mapel = "Matematika",
                    tipe = "guru",
                    fileName = "Buku_Guru_Matematika_Kelas_4.pdf",
                    totalHalaman = 18,
                    uploadedAt = "2026-09-30 08:00"
                )
            )
            chunkDao.insertChunks(listOf(
                com.example.data.local.BukuChunkEntity(
                    bukuId = idGuruMat,
                    halaman = 12,
                    text = "Bab Pecahan: Pecahan menyatakan bagian dari satu keutuhan benda konkret atau himpunan. Pembilang (angka atas) menunjukkan bagian yang diambil, dan penyebut (angka bawah) menunjukkan total bagian sama besar. Pendekatan pembelajaran berbasis pengalaman nyata menggunakan peraga lipatan kertas."
                ),
                com.example.data.local.BukuChunkEntity(
                    bukuId = idGuruMat,
                    halaman = 14,
                    text = "Konsep Pecahan Senilai: Dua pecahan dikatakan senilai apabila mempunyai nilai atau proporsi yang sama meskipun lambang bilangannya berbeda. Contoh 1/2 senilai dengan 2/4 dan 4/8. Guru membimbing siswa mengamati hubungan perkalian faktor pembilang dan penyebut."
                ),
                com.example.data.local.BukuChunkEntity(
                    bukuId = idGuruMat,
                    halaman = 16,
                    text = "Operasi Pecahan Berpenyebut Sama: Penjumlahan dan pengurangan pecahan hanya dilakukan pada pembilangnya jika penyebut kedua pecahan sudah sama besar. Contoh: a/c + b/c = (a+b)/c."
                )
            ))

            // 2. Buku Siswa Matematika Kelas 4
            val idSiswaMat = bukuDao.insertBuku(
                com.example.data.local.BukuEntity(
                    judul = "Buku Siswa Belajar Bersama Temanmu Matematika Kelas 4",
                    kelas = 4,
                    mapel = "Matematika",
                    tipe = "siswa",
                    fileName = "Buku_Siswa_Matematika_Kelas_4.pdf",
                    totalHalaman = 28,
                    uploadedAt = "2026-09-30 08:05"
                )
            )
            chunkDao.insertChunks(listOf(
                com.example.data.local.BukuChunkEntity(
                    bukuId = idSiswaMat,
                    halaman = 20,
                    text = "Ayo Mengamati! Siti membawa kue bolu ke sekolah. Kue bolu dipotong menjadi 8 bagian sama besar. Dayu memakan 3 potong bolu. Nilai pecahan bagian yang dimakan Dayu adalah 3/8 bagian. Angka 3 disebut pembilang dan angka 8 disebut penyebut."
                ),
                com.example.data.local.BukuChunkEntity(
                    bukuId = idSiswaMat,
                    halaman = 22,
                    text = "Contoh Menentukan Pecahan Senilai: Budi memiliki pizza 1/2 loyang, sedangkan Edo memiliki pizza 2/4 loyang dengan ukuran loyang yang sama. Besar potongan pizza Budi dan Edo adalah sama luasnya. Maka 1/2 senilai dengan 2/4."
                ),
                com.example.data.local.BukuChunkEntity(
                    bukuId = idSiswaMat,
                    halaman = 25,
                    text = "Ayo Berlatih! Kerjakan soal-soal berikut:\n1. Tentukan pecahan senilai dari 2/3 dengan mengalikan pembilang dan penyebutnya dengan angka 2!\n2. Ibu membeli tepung seberat 3/5 kg, lalu bibi memberi 1/5 kg tepung. Berapa total berat tepung sekarang?\n3. Manakah yang lebih besar antara 3/7 dan 5/7?"
                )
            ))

            // 3. Buku Siswa IPAS Kelas 4
            val idSiswaIpas = bukuDao.insertBuku(
                com.example.data.local.BukuEntity(
                    judul = "Buku Siswa Ilmu Pengetahuan Alam dan Sosial (IPAS) Kelas 4",
                    kelas = 4,
                    mapel = "IPAS",
                    tipe = "siswa",
                    fileName = "Buku_Siswa_IPAS_Kelas_4.pdf",
                    totalHalaman = 35,
                    uploadedAt = "2026-09-30 08:10"
                )
            )
            chunkDao.insertChunks(listOf(
                com.example.data.local.BukuChunkEntity(
                    bukuId = idSiswaIpas,
                    halaman = 32,
                    text = "Topik Fotosintesis: Proses Paling Penting di Bumi. Tumbuhan hijau membuat makanannya sendiri melalui fotosintesis. Tumbuhan menyerap air dari akar, menyerap gas karbondioksida dari udara melalui stomata daun, serta menyerap energi cahaya matahari dengan klorofil."
                ),
                com.example.data.local.BukuChunkEntity(
                    bukuId = idSiswaIpas,
                    halaman = 34,
                    text = "Hasil Fotosintesis: Fotosintesis menghasilkan karbohidrat (glukosa) sebagai cadangan makanan tumbuhan dan melepaskan gas oksigen ke udara yang sangat dibutuhkan manusia dan hewan untuk bernapas."
                )
            ))

            Log.d(TAG, "Sample books and chunks successfully seeded.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed seeding sample books: ${e.message}", e)
        }
    }
}
