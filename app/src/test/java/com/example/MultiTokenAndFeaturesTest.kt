package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.ApiKeyManager
import com.example.ui.MainViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = AsistenMengajarApp::class)
class MultiTokenAndFeaturesTest {

    private lateinit var app: AsistenMengajarApp
    private lateinit var apiKeyManager: ApiKeyManager
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        val prefs = app.getSharedPreferences("api_keys", android.content.Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        apiKeyManager = ApiKeyManager.getInstance(app)
        apiKeyManager.getGeminiKeys().forEach { apiKeyManager.removeGeminiKey(it) }
        apiKeyManager.getDeepSeekKeys().forEach { apiKeyManager.removeDeepSeekKey(it) }
        viewModel = MainViewModel(app)
    }

    @Test
    fun testApiKeyManagerGeminiKeys() {
        val testKey1 = "AIzaSyTestKey12345678"
        val testKey2 = "AIzaSyTestKey87654321"

        assertTrue(apiKeyManager.addGeminiKey(testKey1))
        assertTrue(apiKeyManager.getGeminiKeys().contains(testKey1))

        apiKeyManager.setActiveGeminiKey(testKey1)
        assertEquals(testKey1, apiKeyManager.getActiveGeminiKey())

        apiKeyManager.addGeminiKey(testKey2)
        apiKeyManager.rotateGeminiKey()
        assertEquals(testKey2, apiKeyManager.getActiveGeminiKey())

        val masked = apiKeyManager.maskKey(testKey1)
        assertTrue(masked.startsWith("AIza"))
        assertTrue(masked.endsWith("5678"))

        apiKeyManager.removeGeminiKey(testKey1)
        assertFalse(apiKeyManager.getGeminiKeys().contains(testKey1))
    }

    @Test
    fun testApiKeyManagerDeepSeekKeys() {
        val testDeepSeekKey1 = "sk-deepseek1234567890"
        val testDeepSeekKey2 = "sk-deepseek0987654321"

        assertTrue(apiKeyManager.addDeepSeekKey(testDeepSeekKey1))
        assertTrue(apiKeyManager.getDeepSeekKeys().contains(testDeepSeekKey1))
        assertEquals(testDeepSeekKey1, apiKeyManager.getActiveDeepSeekKey())

        apiKeyManager.addDeepSeekKey(testDeepSeekKey2)
        assertEquals(2, apiKeyManager.getDeepSeekKeys().size)

        apiKeyManager.rotateDeepSeekKey()
        assertEquals(testDeepSeekKey2, apiKeyManager.getActiveDeepSeekKey())

        apiKeyManager.recordUsage("deepseek", 200)
        assertEquals(200, apiKeyManager.getDeepSeekUsage())

        apiKeyManager.removeDeepSeekKey(testDeepSeekKey1)
        assertFalse(apiKeyManager.getDeepSeekKeys().contains(testDeepSeekKey1))
        assertEquals(testDeepSeekKey2, apiKeyManager.getActiveDeepSeekKey())
    }

    @Test
    fun testAssistantConfigCustomization() = runBlocking {
        viewModel.saveAssistantConfig("Bu Guru AI", "Halo siswa dan guru hebat!")
        val resultStart = viewModel.tanyaAi("/start")
        assertTrue(resultStart.contains("Bu Guru AI") || resultStart.contains("Halo"))
    }

    @Test
    fun testTokenCommandOutputFormat() = runBlocking {
        val result = viewModel.tanyaAi("/token")
        assertTrue(result.contains("Status API:"))
        assertTrue(result.contains("Gemini:"))
        assertTrue(result.contains("DeepSeek:"))
        assertTrue(result.contains("Hari ini:"))
    }

    @Test
    fun testPengumumanTemplates() {
        val templateUlangan = com.example.util.PengumumanTemplates.getTemplate("Ulangan/UTS")
        assertTrue(templateUlangan.contains("PENGUMUMAN ULANGAN"))
        assertTrue(templateUlangan.contains("Hari/Tanggal"))

        val templatePr = com.example.util.PengumumanTemplates.getTemplate("PR/Tugas")
        assertTrue(templatePr.contains("PR / TUGAS"))

        val templateRapat = com.example.util.PengumumanTemplates.getTemplate("Rapat Wali Murid")
        assertTrue(templateRapat.contains("WALI MURID"))
    }

    @Test
    fun testPengumumanDatabaseOperations() = runBlocking {
        val dao = app.database.pengumumanDao()
        val item = com.example.data.local.PengumumanHistoryEntity(
            timestamp = System.currentTimeMillis(),
            jenis = "Ulangan/UTS",
            kelas = 4,
            konten = "📢 *PENGUMUMAN ULANGAN*",
            info_tambahan = "ulangan matematika besok"
        )
        val id = dao.insert(item)
        assertTrue(id > 0L)

        val list = dao.getAll()
        assertTrue(list.isNotEmpty())
        assertEquals("Ulangan/UTS", list.first().jenis)
        assertEquals(4, list.first().kelas)

        dao.deleteById(id)
        val listAfter = dao.getAll()
        assertTrue(listAfter.none { it.id == id })
    }

    @Test
    fun testNavigationStructure() {
        assertEquals(4, com.example.ui.navigation.Screen.bottomNavItems.size)
        assertEquals("dashboard", com.example.ui.navigation.Screen.bottomNavItems[0].route)
        assertEquals("jadwal", com.example.ui.navigation.Screen.bottomNavItems[1].route)
        assertEquals("chat", com.example.ui.navigation.Screen.bottomNavItems[2].route)
        assertEquals("materi", com.example.ui.navigation.Screen.bottomNavItems[3].route)

        assertEquals(7, com.example.ui.navigation.Screen.drawerNavItems.size)
    }
}

