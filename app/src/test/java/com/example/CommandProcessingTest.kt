package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AsistenDatabase
import com.example.data.local.ChatEntity
import com.example.data.local.JadwalEntity
import com.example.data.local.KalenderEntity
import com.example.data.local.MateriEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = AsistenMengajarApp::class)
class CommandProcessingTest {

    private lateinit var app: AsistenMengajarApp
    private lateinit var viewModel: com.example.ui.MainViewModel

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        viewModel = com.example.ui.MainViewModel(app)
        viewModel.saveAssistantConfig("Cici", "Halo! Cici siap membantu.")
    }

    @Test
    fun `test help command returns all 14 commands`() = runBlocking {
        val result = viewModel.tanyaAi("/help")
        assertTrue(result.contains("/start"))
        assertTrue(result.contains("/jadwal"))
        assertTrue(result.contains("/besok"))
        assertTrue(result.contains("/bahan"))
        assertTrue(result.contains("/progress"))
        assertTrue(result.contains("/materi"))
        assertTrue(result.contains("/gambar"))
        assertTrue(result.contains("/search"))
        assertTrue(result.contains("/token"))
        assertTrue(result.contains("/libur"))
        assertTrue(result.contains("/backup"))
        assertTrue(result.contains("/reset_tahun"))
        assertTrue(result.contains("/memory"))
        assertTrue(result.contains("/help"))
    }

    @Test
    fun `test start command greets user`() = runBlocking {
        val result = viewModel.tanyaAi("/start")
        assertTrue(result.contains("Halo"))
        assertTrue(result.contains("Cici"))
        assertTrue(result.contains("/help"))

        val resultHalo = viewModel.tanyaAi("halo")
        assertTrue(resultHalo.contains("Halo"))
    }

    @Test
    fun `test token command returns api info`() = runBlocking {
        val result = viewModel.tanyaAi("/token")
        assertTrue(result.contains("Status API"))
        assertTrue(result.contains("Gemini:"))
        assertTrue(result.contains("DeepSeek:"))
    }

    @Test
    fun `test reset_tahun command asks for confirmation first`() = runBlocking {
        val resultPrompt = viewModel.tanyaAi("/reset_tahun")
        assertTrue(resultPrompt.contains("KONFIRMASI RESET TAHUN AJARAN"))

        val resultConfirm = viewModel.tanyaAi("/reset_tahun ya")
        assertTrue(resultConfirm.contains("Tahun ajaran direset"))
    }
}
