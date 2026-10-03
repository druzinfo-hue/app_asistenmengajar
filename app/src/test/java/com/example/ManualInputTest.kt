package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AsistenDatabase
import com.example.data.local.JadwalEntity
import com.example.data.local.KalenderEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ManualInputTest {

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
    fun `test Jadwal insert, update, and delete`() = runBlocking {
        val newJadwal = JadwalEntity(
            id = 0,
            kelas = 4,
            hari = "Sabtu",
            jamMulai = "08:00",
            jamSelesai = "09:00",
            mapel = "Bahasa Sunda"
        )
        val insertedId = database.jadwalDao().insert(newJadwal)
        assertTrue(insertedId > 0)

        // Verify inserted
        val listSabtu = database.jadwalDao().getJadwalByHariSync("Sabtu")
        assertEquals(1, listSabtu.size)
        assertEquals("Bahasa Sunda", listSabtu[0].mapel)
        assertEquals("08:00", listSabtu[0].jamMulai)

        // Update
        val toUpdate = listSabtu[0].copy(jamMulai = "08:30", jamSelesai = "09:30")
        database.jadwalDao().update(toUpdate)

        val updatedList = database.jadwalDao().getJadwalByHariSync("Sabtu")
        assertEquals("08:30", updatedList[0].jamMulai)

        // Delete
        database.jadwalDao().delete(updatedList[0])
        val emptyList = database.jadwalDao().getJadwalByHariSync("Sabtu")
        assertEquals(0, emptyList.size)
    }

    @Test
    fun `test Kalender insert, update, and delete`() = runBlocking {
        val newEvent = KalenderEntity(
            id = 0L,
            tanggal = "2026-10-15",
            tanggalMulai = "",
            tanggalSelesai = "",
            keterangan = "Libur Test",
            jenis = "libur_nasional"
        )
        val id = database.kalenderDao().insert(newEvent)
        assertTrue(id > 0)

        // Verify
        val list = database.kalenderDao().getByJenis("libur_nasional")
        val found = list.find { it.keterangan == "Libur Test" }
        assertNotNull(found)
        assertEquals("2026-10-15", found?.tanggal)

        // Update
        val updatedEvent = found!!.copy(keterangan = "Libur Test Updated")
        database.kalenderDao().update(updatedEvent)

        val updatedList = database.kalenderDao().getByJenis("libur_nasional")
        assertTrue(updatedList.any { it.keterangan == "Libur Test Updated" })

        // Delete
        database.kalenderDao().delete(updatedEvent)
        val finalList = database.kalenderDao().getByJenis("libur_nasional")
        assertFalse(finalList.any { it.keterangan == "Libur Test Updated" })
    }
}
