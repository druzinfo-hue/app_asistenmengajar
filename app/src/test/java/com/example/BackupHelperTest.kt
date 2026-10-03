package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AsistenDatabase
import com.example.data.local.MateriEntity
import com.example.util.BackupHelper
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupHelperTest {

    private lateinit var context: Context
    private lateinit var database: AsistenDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AsistenDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `test restore with valid 6 classes JSON succeeds`() = runBlocking {
        // Pre-insert an old dummy record to test that old records are deleted
        database.materiDao().insertAll(
            listOf(
                MateriEntity(
                    id = 1,
                    kelas = 9,
                    mapel = "Old Mapel",
                    pertemuan = 1,
                    bab = "Old Bab",
                    judul = "Old Judul",
                    bahan = "Old Bahan",
                    tujuan = "Old Tujuan",
                    alokasiJp = 2
                )
            )
        )
        assertEquals(1, database.materiDao().getCount())

        val root = JSONObject()
        root.put("version", "1.0")
        val dataObj = JSONObject()
        dataObj.put("jadwal", JSONObject().put("jadwal", JSONArray()))
        dataObj.put("config", JSONObject().apply {
            put("user", JSONObject().apply {
                put("name", "Pak Guru Baru")
                put("role", "Guru Kelas 4")
                put("school", "SDN Merdeka")
                put("kelas", JSONArray(listOf(1, 2, 3, 4, 5, 6)))
                put("kelas_utama", "4")
            })
        })
        dataObj.put("kalender", JSONObject())
        dataObj.put("visual_catalog", JSONObject())

        val materiObj = JSONObject()
        for (k in 1..6) {
            val kObj = JSONObject()
            val mapelObj = JSONObject()
            val pArr = JSONArray()
            pArr.put(
                JSONObject().apply {
                    put("pertemuan", 1)
                    put("bab", "Bab 1")
                    put("materi", "Judul Pertemuan 1 Kelas $k")
                    put("bahan", JSONArray(listOf("Buku")))
                    put("tujuan", "Tujuan Ajar")
                    put("alokasi_jp", 2)
                }
            )
            mapelObj.put("Matematika", pArr)
            kObj.put("mapel", mapelObj)
            materiObj.put("kelas_$k", kObj)
        }
        dataObj.put("materi", materiObj)
        root.put("data", dataObj)

        val result = BackupHelper.restoreFromJson(context, root.toString(), database)
        assertTrue("Expected Success, got $result", result is BackupHelper.RestoreResult.Success)

        val success = result as BackupHelper.RestoreResult.Success
        assertEquals(6, success.kelasCount)
        assertEquals(6, success.pertemuanCount)
        assertEquals("Berhasil import 6 kelas, 6 pertemuan", success.message)

        // Verify old dummy materi was wiped and 6 new entries inserted
        assertEquals(6, database.materiDao().getCount())

        // Verify SharedPreferences updated
        val prefs = context.getSharedPreferences("asisten_mengajar_prefs", Context.MODE_PRIVATE)
        assertEquals("Pak Guru Baru", prefs.getString("user_name", null))
        assertEquals("SDN Merdeka", prefs.getString("user_school", null))
    }

    @Test
    fun `test kalender import populates kalender table`() = runBlocking {
        val root = JSONObject()
        root.put("version", "1.0")
        val dataObj = JSONObject()
        dataObj.put("jadwal", JSONObject().put("jadwal", JSONArray()))
        dataObj.put("config", JSONObject())
        dataObj.put("visual_catalog", JSONObject())

        val kalenderObj = JSONObject().apply {
            put("semester", "Ganjil 2026/2027")
            val liburArr = JSONArray().apply {
                put(JSONObject().apply {
                    put("tanggal", "2026-08-17")
                    put("keterangan", "Proklamasi Kemerdekaan RI")
                    put("jenis", "libur_nasional")
                })
            }
            put("libur", liburArr)
            val kegiatanArr = JSONArray().apply {
                put(JSONObject().apply {
                    put("tanggal_mulai", "2026-07-15")
                    put("tanggal_selesai", "2026-07-17")
                    put("keterangan", "MPLS")
                    put("jenis", "kegiatan")
                })
            }
            put("kegiatan_sekolah", kegiatanArr)
        }
        dataObj.put("kalender", kalenderObj)

        val materiObj = JSONObject()
        for (k in 1..6) {
            val kObj = JSONObject()
            val mapelObj = JSONObject()
            val pArr = JSONArray()
            pArr.put(
                JSONObject().apply {
                    put("pertemuan", 1)
                    put("bab", "Bab 1")
                    put("materi", "Materi $k")
                }
            )
            mapelObj.put("Matematika", pArr)
            kObj.put("mapel", mapelObj)
            materiObj.put("kelas_$k", kObj)
        }
        dataObj.put("materi", materiObj)
        root.put("data", dataObj)

        val result = BackupHelper.restoreFromJson(context, root.toString(), database)
        assertTrue(result is BackupHelper.RestoreResult.Success)

        val kalenderList = database.kalenderDao().getAll()
        assertEquals(2, kalenderList.size)
        val liburItem = kalenderList.find { it.jenis == "libur_nasional" }
        assertEquals("2026-08-17", liburItem?.tanggal)
        assertEquals("Proklamasi Kemerdekaan RI", liburItem?.keterangan)

        val mplsItem = kalenderList.find { it.keterangan == "MPLS" }
        assertEquals("2026-07-15", mplsItem?.tanggalMulai)
        assertEquals("2026-07-17", mplsItem?.tanggalSelesai)
    }

    @Test
    fun `test restore with only 1 class triggers warning error`() = runBlocking {
        val root = JSONObject()
        val dataObj = JSONObject()
        dataObj.put("jadwal", JSONObject().put("jadwal", JSONArray()))
        dataObj.put("config", JSONObject())
        dataObj.put("kalender", JSONObject())
        dataObj.put("visual_catalog", JSONObject())

        val materiObj = JSONObject()
        val k1 = JSONObject()
        val mapelObj = JSONObject()
        val pArr = JSONArray()
        pArr.put(JSONObject().apply {
            put("pertemuan", 1)
            put("bab", "Bab 1")
            put("materi", "Materi 1")
        })
        mapelObj.put("Bahasa Indonesia", pArr)
        k1.put("mapel", mapelObj)
        materiObj.put("kelas_1", k1)

        dataObj.put("materi", materiObj)
        root.put("data", dataObj)

        val result = BackupHelper.restoreFromJson(context, root.toString(), database)
        assertTrue(result is BackupHelper.RestoreResult.Error)
        val error = result as BackupHelper.RestoreResult.Error
        assertTrue(
            "Expected 'File tidak lengkap, hanya berisi 1 kelas', got: ${error.message}",
            error.message.contains("File tidak lengkap, hanya berisi 1 kelas")
        )
    }

    @Test
    fun `test restore with missing required fields in data fails`() = runBlocking {
        val root = JSONObject()
        val dataObj = JSONObject()
        dataObj.put("materi", JSONObject())
        // missing jadwal, config, kalender, visual_catalog
        root.put("data", dataObj)

        val result = BackupHelper.restoreFromJson(context, root.toString(), database)
        assertTrue(result is BackupHelper.RestoreResult.Error)
        val error = result as BackupHelper.RestoreResult.Error
        assertTrue(error.message.contains("tidak ditemukan"))
    }
}
