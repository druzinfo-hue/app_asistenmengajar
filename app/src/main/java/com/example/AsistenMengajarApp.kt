package com.example

import android.app.Application
import com.example.data.local.AsistenDatabase
import com.example.data.repository.ChatRepository
import com.example.data.repository.ConfigRepository
import com.example.data.repository.JadwalRepository
import com.example.data.repository.MateriRepository
import com.example.notification.BriefingWorker
import com.example.notification.NotificationHelper
import com.example.util.DataImporter
import com.example.voice.VoiceInputManager
import com.example.voice.VoiceOutputManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AsistenMengajarApp : Application() {

    companion object {
        lateinit var instance: AsistenMengajarApp
            private set
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { AsistenDatabase.getDatabase(this) }
    val materiRepository by lazy { MateriRepository(database.materiDao()) }
    val jadwalRepository by lazy { JadwalRepository(database.jadwalDao()) }
    val kalenderRepository by lazy { com.example.data.repository.KalenderRepository(database.kalenderDao()) }
    val soalHistoryRepository by lazy { com.example.data.repository.SoalHistoryRepository(database.soalHistoryDao()) }
    val rppHistoryRepository by lazy { com.example.data.repository.RppHistoryRepository(database.rppHistoryDao()) }
    val pengumumanRepository by lazy { com.example.data.repository.PengumumanRepository(database.pengumumanDao()) }
    val chatRepository by lazy { ChatRepository(database.chatDao()) }
    val configRepository by lazy { ConfigRepository(database.configDao()) }
    val bukuRepository by lazy { com.example.data.repository.BukuRepository(database.bukuDao(), database.bukuChunkDao()) }

    val voiceInputManager by lazy { VoiceInputManager(this) }
    val voiceOutputManager by lazy { VoiceOutputManager(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Setup notification channels
        NotificationHelper.createNotificationChannels(this)
    }
}
