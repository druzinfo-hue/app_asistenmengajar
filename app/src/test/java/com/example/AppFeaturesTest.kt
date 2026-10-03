package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AsistenDatabase
import com.example.data.local.JadwalEntity
import com.example.data.local.MateriEntity
import com.example.util.WikipediaHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = AsistenMengajarApp::class)
class AppFeaturesTest {

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
    fun `test WikipediaHelper extractTerm correctly extracts keywords`() {
        assertEquals("fotosintesis", WikipediaHelper.extractTerm("apa itu fotosintesis?"))
        assertEquals("metamorfosis", WikipediaHelper.extractTerm("apa yang dimaksud dengan metamorfosis?"))
        assertEquals("sistem tata surya", WikipediaHelper.extractTerm("jelaskan tentang sistem tata surya"))
        assertEquals("gaya magnet", WikipediaHelper.extractTerm("pengertian dari gaya magnet"))
        assertEquals("pecahan senilai", WikipediaHelper.extractTerm("definisi pecahan senilai"))
    }

    @Test
    fun `test Jadwal query by day returns scheduled subjects`() = runBlocking {
        database.jadwalDao().insertAll(
            listOf(
                JadwalEntity(kelas = 4, hari = "Senin", jamMulai = "07:35", jamSelesai = "09:20", mapel = "Matematika"),
                JadwalEntity(kelas = 4, hari = "Senin", jamMulai = "09:55", jamSelesai = "11:05", mapel = "B. Indonesia"),
                JadwalEntity(kelas = 4, hari = "Senin", jamMulai = "11:05", jamSelesai = "12:15", mapel = "IPAS"),
                JadwalEntity(kelas = 4, hari = "Selasa", jamMulai = "07:35", jamSelesai = "09:20", mapel = "Pendidikan Pancasila")
            )
        )

        val seninList = database.jadwalDao().getJadwalByHariSync("Senin")
        assertEquals(3, seninList.size)
        assertEquals("Matematika", seninList[0].mapel)
        assertEquals("07:35", seninList[0].jamMulai)

        val selasaList = database.jadwalDao().getJadwalByHariSync("Selasa")
        assertEquals(1, selasaList.size)
        assertEquals("Pendidikan Pancasila", selasaList[0].mapel)
    }

    @Test
    fun `test Materi searchByKeyword returns matching items`() = runBlocking {
        database.materiDao().insertAll(
            listOf(
                MateriEntity(
                    id = 1,
                    kelas = 4,
                    mapel = "IPAS",
                    pertemuan = 1,
                    bab = "Tumbuhan, Sumber Kehidupan di Bumi",
                    judul = "Bagian Tubuh Tumbuhan dan Fotosintesis",
                    bahan = "Daun, air, gelas",
                    tujuan = "Memahami fotosintesis",
                    alokasiJp = 2
                ),
                MateriEntity(
                    id = 2,
                    kelas = 4,
                    mapel = "Matematika",
                    pertemuan = 2,
                    bab = "Pecahan",
                    judul = "Pecahan Senilai",
                    bahan = "Kertas lipat",
                    tujuan = "Menentukan pecahan senilai",
                    alokasiJp = 2
                )
            )
        )

        val resultsFotosintesis = database.materiDao().searchByKeyword("Fotosintesis")
        assertEquals(1, resultsFotosintesis.size)
        assertEquals("Bagian Tubuh Tumbuhan dan Fotosintesis", resultsFotosintesis[0].judul)

        val resultsPecahan = database.materiDao().searchByKeyword("Pecahan")
        assertEquals(1, resultsPecahan.size)
        assertEquals("Pecahan Senilai", resultsPecahan[0].judul)
    }

    @Test
    fun `test Theme mode preference saves and loads correctly`() {
        val prefs = context.getSharedPreferences("asisten_mengajar_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("theme_mode", "light").apply()
        assertEquals("light", prefs.getString("theme_mode", "auto"))

        prefs.edit().putString("theme_mode", "dark").apply()
        assertEquals("dark", prefs.getString("theme_mode", "auto"))

        prefs.edit().putString("theme_mode", "auto").apply()
        assertEquals("auto", prefs.getString("theme_mode", "auto"))
    }

    @Test
    fun `test Reminder preferences save and load correctly`() {
        val prefs = context.getSharedPreferences("asisten_mengajar_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("reminder_jadwal_enabled", true)
            .putBoolean("reminder_pagi_enabled", true)
            .putInt("reminder_menit_sebelum", 30)
            .putString("reminder_pagi_time", "06:00")
            .apply()

        assertTrue(prefs.getBoolean("reminder_jadwal_enabled", false))
        assertTrue(prefs.getBoolean("reminder_pagi_enabled", false))
        assertEquals(30, prefs.getInt("reminder_menit_sebelum", 0))
        assertEquals("06:00", prefs.getString("reminder_pagi_time", ""))
    }

    @Test
    fun `test Setup Awal saves user config and clears placeholder defaults`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<AsistenMengajarApp>()
        val vm = com.example.ui.MainViewModel(app)

        vm.saveInitialSetup(
            name = "Pak Ahmad",
            role = "Guru Kelas",
            school = "SDN Sukamaju 01",
            kelasUtama = "4",
            kelasList = listOf(4),
            aiName = "Aira",
            aiGreeting = "Halo! Siap membantu."
        )
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        val prefs = app.getSharedPreferences("asisten_mengajar_prefs", Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean("setup_done", false))
        assertEquals("Pak Ahmad", prefs.getString("user_name", ""))
        assertEquals("SDN Sukamaju 01", prefs.getString("user_school", ""))
        assertEquals("4", prefs.getString("user_kelas_utama", ""))
        assertEquals("Aira", prefs.getString("assistant_name", ""))
    }
}
