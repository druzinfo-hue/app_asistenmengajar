package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AsistenMengajarApp
import com.example.BuildConfig
import com.example.data.local.ChatEntity
import com.example.data.local.JadwalEntity
import com.example.data.local.KalenderEntity
import com.example.data.local.MateriEntity
import com.example.data.remote.ApiCaller
import com.example.data.remote.ApiKeyManager
import com.example.data.remote.GeminiClient
import com.example.data.remote.GeminiContent
import com.example.data.remote.GeminiGenerationConfig
import com.example.data.remote.GeminiPart
import com.example.data.remote.GeminiRequest
import com.example.notification.BriefingWorker
import com.example.notification.NotificationHelper
import com.example.util.BackupHelper
import com.example.util.ImageSaverHelper
import com.example.util.SearchHelper
import android.os.Environment
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.util.DataImporter
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CommandResult(
    val success: Boolean,
    val message: String,
    val action: String? = null,
    val imageUrl: String? = null
)

data class MapelProgress(
    val mapel: String,
    val total: Int,
    val completed: Int,
    val percentage: Float
)

data class TodayScheduleItem(
    val jadwal: JadwalEntity,
    val nextMateri: MateriEntity?
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AsistenMengajarApp
    private val materiRepo = app.materiRepository
    private val jadwalRepo = app.jadwalRepository
    private val kalenderRepo = app.kalenderRepository
    private val chatRepo = app.chatRepository
    private val configRepo = app.configRepository
    val voiceInputManager = app.voiceInputManager
    val voiceOutputManager = app.voiceOutputManager

    // SharedPreferences Cache for fast startup and zero lag
    private val appPrefs = application.getSharedPreferences("asisten_mengajar_prefs", Context.MODE_PRIVATE)

    fun getCachedConfig(key: String, default: String): String {
        return appPrefs.getString("cached_$key", default) ?: default
    }

    fun cacheConfig(key: String, value: String) {
        appPrefs.edit().putString("cached_$key", value).apply()
    }

    // Configuration / Profile states with SharedPreferences caching for instantaneous access
    val userName = configRepo.getConfig("user_name").stateIn(viewModelScope, SharingStarted.Eagerly, getCachedConfig("user_name", ""))
    val userRole = configRepo.getConfig("user_role").stateIn(viewModelScope, SharingStarted.Eagerly, getCachedConfig("user_role", ""))
    val userSchool = configRepo.getConfig("user_school").stateIn(viewModelScope, SharingStarted.Eagerly, getCachedConfig("user_school", ""))
    val userKelas = configRepo.getConfig("user_kelas").stateIn(viewModelScope, SharingStarted.Eagerly, getCachedConfig("user_kelas", ""))
    val userKelasUtama = configRepo.getConfig("user_kelas_utama").stateIn(viewModelScope, SharingStarted.Eagerly, getCachedConfig("user_kelas_utama", ""))
    val customApiKey = configRepo.getConfig("gemini_api_key").stateIn(viewModelScope, SharingStarted.Eagerly, getCachedConfig("gemini_api_key", ""))
    val briefingEnabled = configRepo.getConfig("notification_briefing_enabled").stateIn(viewModelScope, SharingStarted.Eagerly, getCachedConfig("notification_briefing_enabled", "true"))
    val briefingTime = configRepo.getConfig("notification_briefing_time").stateIn(viewModelScope, SharingStarted.Eagerly, getCachedConfig("notification_briefing_time", "20:00"))
    val hydrationEnabled = configRepo.getConfig("notification_hydration_enabled").stateIn(viewModelScope, SharingStarted.Eagerly, getCachedConfig("notification_hydration_enabled", "true"))
    val ttsEnabled = configRepo.getConfig("voice_tts_enabled").stateIn(viewModelScope, SharingStarted.Eagerly, getCachedConfig("voice_tts_enabled", "true"))
    val kalenderSemester = configRepo.getConfig("kalender_semester").stateIn(viewModelScope, SharingStarted.Eagerly, getCachedConfig("kalender_semester", "Semester Ganjil 2026/2027"))
    val kalenderTahunAjaran = configRepo.getConfig("kalender_tahun_ajaran").stateIn(viewModelScope, SharingStarted.Eagerly, getCachedConfig("kalender_tahun_ajaran", "2026/2027"))
    val assistantName = configRepo.getConfig("assistant_name").stateIn(viewModelScope, SharingStarted.Eagerly, getCachedConfig("assistant_name", "Cici"))
    val assistantGreeting = configRepo.getConfig("assistant_greeting").stateIn(viewModelScope, SharingStarted.Eagerly, getCachedConfig("assistant_greeting", "Halo! Cici siap membantu. Panggil aku kalau butuh sesuatu ya."))

    val chatLimitManager by lazy { com.example.util.ChatLimitManager(app) }
    val premiumManager by lazy { com.example.util.PremiumManager(app) }

    private val _showUpgradeDialog = MutableStateFlow(false)
    val showUpgradeDialog: StateFlow<Boolean> = _showUpgradeDialog.asStateFlow()

    fun dismissUpgradeDialog() {
        _showUpgradeDialog.value = false
    }

    fun openUpgradeDialog() {
        _showUpgradeDialog.value = true
    }

    fun saveAssistantConfig(name: String, greeting: String) {
        val n = name.trim().ifBlank { "Cici" }
        val g = greeting.trim().ifBlank { "Halo! Cici siap membantu. Panggil aku kalau butuh sesuatu ya." }
        cacheConfig("assistant_name", n)
        cacheConfig("assistant_greeting", g)
        appPrefs.edit()
            .putString("assistant_name", n)
            .putString("assistant_greeting", g)
            .apply()
        viewModelScope.launch {
            configRepo.setConfig("assistant_name", n)
            configRepo.setConfig("assistant_greeting", g)
        }
    }

    // Theme Mode Preference
    private val _themeMode = MutableStateFlow(appPrefs.getString("theme_mode", "auto") ?: "auto")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        appPrefs.edit().putString("theme_mode", mode).apply()
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            when (mode) {
                "light" -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                else -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    // Reminder Notification States
    private val _reminderJadwalEnabled = MutableStateFlow(appPrefs.getBoolean("reminder_jadwal_enabled", true))
    val reminderJadwalEnabled: StateFlow<Boolean> = _reminderJadwalEnabled.asStateFlow()

    private val _reminderPagiEnabled = MutableStateFlow(appPrefs.getBoolean("reminder_pagi_enabled", true))
    val reminderPagiEnabled: StateFlow<Boolean> = _reminderPagiEnabled.asStateFlow()

    private val _reminderMenitSebelum = MutableStateFlow(appPrefs.getInt("reminder_menit_sebelum", 30))
    val reminderMenitSebelum: StateFlow<Int> = _reminderMenitSebelum.asStateFlow()

    private val _reminderPagiTime = MutableStateFlow(appPrefs.getString("reminder_pagi_time", "06:00") ?: "06:00")
    val reminderPagiTime: StateFlow<String> = _reminderPagiTime.asStateFlow()

    fun setReminderJadwalEnabled(enabled: Boolean) {
        _reminderJadwalEnabled.value = enabled
        appPrefs.edit().putBoolean("reminder_jadwal_enabled", enabled).apply()
        com.example.notification.ReminderScheduler.rescheduleAll(app)
    }

    fun setReminderPagiEnabled(enabled: Boolean) {
        _reminderPagiEnabled.value = enabled
        appPrefs.edit().putBoolean("reminder_pagi_enabled", enabled).apply()
        com.example.notification.ReminderScheduler.rescheduleAll(app)
    }

    fun setReminderMenitSebelum(minutes: Int) {
        _reminderMenitSebelum.value = minutes
        appPrefs.edit().putInt("reminder_menit_sebelum", minutes).apply()
        com.example.notification.ReminderScheduler.rescheduleAll(app)
    }

    fun setReminderPagiTime(hour: Int, minute: Int) {
        val formatted = String.format(java.util.Locale.US, "%02d:%02d", hour, minute)
        _reminderPagiTime.value = formatted
        appPrefs.edit().putString("reminder_pagi_time", formatted).apply()
        com.example.notification.ReminderScheduler.scheduleMorningAlarm(app)
    }

    fun triggerTestReminder() {
        com.example.notification.ReminderScheduler.triggerImmediateTest(app)
    }

    // Progress Screen Filters
    private val _selectedProgressKelas = MutableStateFlow(4)
    val selectedProgressKelas: StateFlow<Int> = _selectedProgressKelas.asStateFlow()

    private val _selectedProgressMapel = MutableStateFlow<String?>("Semua")
    val selectedProgressMapel: StateFlow<String?> = _selectedProgressMapel.asStateFlow()

    // Materi Bank Filter
    private val _selectedMateriBankKelas = MutableStateFlow(4)
    val selectedMateriBankKelas: StateFlow<Int> = _selectedMateriBankKelas.asStateFlow()

    private val _materiBankSearchQuery = MutableStateFlow("")
    val materiBankSearchQuery: StateFlow<String> = _materiBankSearchQuery.asStateFlow()

    // Chat AI State
    val chatHistory: StateFlow<List<ChatEntity>> = chatRepo.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    // All Materi Flow
    val allMateri: StateFlow<List<MateriEntity>> = materiRepo.getAllMateri()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Jadwal Flow
    val allJadwal: StateFlow<List<JadwalEntity>> = jadwalRepo.getAllJadwal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Kalender Flow
    val allKalender: StateFlow<List<com.example.data.local.KalenderEntity>> = kalenderRepo.getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Soal & RPP History Flows
    val soalHistory: StateFlow<List<com.example.data.local.SoalHistoryEntity>> = app.soalHistoryRepository.getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rppHistory: StateFlow<List<com.example.data.local.RppHistoryEntity>> = app.rppHistoryRepository.getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pengumumanHistory: StateFlow<List<com.example.data.local.PengumumanHistoryEntity>> = app.pengumumanRepository.allPengumuman
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBuku: StateFlow<List<com.example.data.local.BukuEntity>> = app.bukuRepository.getAllBukuFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Today's Day Name
    fun getTodayDayName(): String {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Senin"
            Calendar.TUESDAY -> "Selasa"
            Calendar.WEDNESDAY -> "Rabu"
            Calendar.THURSDAY -> "Kamis"
            Calendar.FRIDAY -> "Jumat"
            Calendar.SATURDAY -> "Sabtu"
            else -> "Minggu"
        }
    }

    fun getTimeGreeting(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 4..10 -> "Selamat Pagi"
            in 11..14 -> "Selamat Siang"
            in 15..17 -> "Selamat Sore"
            else -> "Selamat Malam"
        }
    }

    // Cached today's schedule in ViewModel for high-performance rendering without lag
    private val _todaySchedule = MutableStateFlow<List<TodayScheduleItem>>(emptyList())
    val todaySchedule: StateFlow<List<TodayScheduleItem>> = _todaySchedule.asStateFlow()

    init {
        // Automatically sync & cache all Room configurations into SharedPreferences in background
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            configRepo.getAllConfig().collect { configs ->
                configs.forEach { entity ->
                    cacheConfig(entity.key, entity.value)
                }
            }
        }

        // Cache today's schedule in ViewModel asynchronously on background dispatcher
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            allJadwal.collect { jadwals ->
                updateTodayScheduleCache(jadwals)
            }
        }
    }

    private suspend fun updateTodayScheduleCache(jadwals: List<JadwalEntity>) {
        val today = getTodayDayName()
        val todayJadwals = jadwals.filter { it.hari.equals(today, ignoreCase = true) }
            .sortedBy { it.jamMulai }

        val items = todayJadwals.map { j ->
            val nextM = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                materiRepo.getNextMateri(j.kelas, j.mapel)
            }
            TodayScheduleItem(j, nextM)
        }
        _todaySchedule.value = items
    }

    fun refreshTodaySchedule() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            updateTodayScheduleCache(allJadwal.value)
        }
    }

    // Progress list for teacher's active class (kelas utama)
    val subjectProgressList: StateFlow<List<MapelProgress>> = combine(
        userKelasUtama,
        allMateri
    ) { kUtamaStr, materis ->
        val kUtama = kUtamaStr?.toIntOrNull() ?: 4
        val filtered = materis.filter { it.kelas == kUtama }
        val grouped = filtered.groupBy { it.mapel }
        grouped.map { (mapel, list) ->
            val total = list.size
            val completed = list.count { it.status == "selesai" }
            val pct = if (total > 0) (completed.toFloat() / total) else 0f
            MapelProgress(mapel, total, completed, pct)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedProgressKelas(kelas: Int) {
        _selectedProgressKelas.value = kelas
    }

    fun setSelectedProgressMapel(mapel: String?) {
        _selectedProgressMapel.value = mapel
    }

    fun setSelectedMateriBankKelas(kelas: Int) {
        _selectedMateriBankKelas.value = kelas
    }

    fun setMateriBankSearchQuery(query: String) {
        _materiBankSearchQuery.value = query
    }

    // Toggle materi completed status
    fun toggleMateriStatus(materi: MateriEntity) {
        viewModelScope.launch {
            val isCompleted = materi.status == "selesai"
            materiRepo.markCompleted(materi.id, !isCompleted)
            refreshTodaySchedule()
        }
    }

    fun markMateriCompleted(materiId: Int, completed: Boolean) {
        viewModelScope.launch {
            materiRepo.markCompleted(materiId, completed)
            refreshTodaySchedule()
        }
    }

    fun resetMateriAfter(kelas: Int, mapel: String, pertemuan: Int) {
        viewModelScope.launch {
            materiRepo.resetProgressAfter(kelas, mapel, pertemuan)
        }
    }

    fun resetAllMateri(kelas: Int, mapel: String) {
        viewModelScope.launch {
            materiRepo.resetAllForMapel(kelas, mapel)
        }
    }

    // Send message to Gemini AI or Command Processor
    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            // 1. Insert user message to Room
            chatRepo.insertMessage(role = "user", message = trimmed)

            _isChatLoading.value = true

            try {
                val cmdResult = processCommand(trimmed)
                val replyText: String
                val imageUrl: String?

                if (cmdResult != null) {
                    replyText = cmdResult.message
                    imageUrl = cmdResult.imageUrl
                } else {
                    if (!chatLimitManager.canChat()) {
                        _showUpgradeDialog.value = true
                        val limit = chatLimitManager.getLimit()
                        replyText = """
                        🔒 Limit Chat Hari Ini Habis ($limit/$limit)
                        
                        Limit harian gratis Anda sudah tercapai.
                        
                        Pilihan:
                        1. Upgrade ke Premium (200 chat/hari & generator unlimited)
                        2. Tambah API Key sendiri di menu Pengaturan > Advanced (Unlimited)
                        3. Tunggu kuota direset besok jam 00:00
                        """.trimIndent()
                        imageUrl = null
                    } else {
                        chatLimitManager.recordChat()
                        // Visual topic auto-illustrate keyword detector
                        val visualKeywords = listOf("tumbuhan", "tata surya", "pecahan", "fotosintesis", "rangka", "daur air", "peta", "gunung", "metamorfosis", "magnet", "bintang", "anatomi")
                        val hasVisualKeyword = visualKeywords.any { trimmed.contains(it, ignoreCase = true) }
                        val detectedKeyword = visualKeywords.firstOrNull { trimmed.contains(it, ignoreCase = true) }

                        imageUrl = if (hasVisualKeyword && detectedKeyword != null) {
                            val encodedPrompt = URLEncoder.encode("education illustration for primary school children: $detectedKeyword, clean vibrant colors, educational diagram style", StandardCharsets.UTF_8.toString())
                            "https://image.pollinations.ai/prompt/$encodedPrompt"
                        } else null

                        replyText = handleSmartFallback(trimmed)
                    }
                }

                chatRepo.insertMessage(role = "ai", message = replyText, imageUrl = imageUrl)

                // Read with TTS if enabled
                if (ttsEnabled.value == "true") {
                    val spokenText = if (replyText.length > 250) {
                        replyText.lines().firstOrNull { it.isNotBlank() }?.take(180) ?: replyText.take(180)
                    } else {
                        replyText
                    }
                    voiceOutputManager.speak(spokenText)
                }

            } catch (e: Exception) {
                val errorReply = "Maaf, koneksi sedang bermasalah atau API Key tidak valid (${e.localizedMessage ?: "Network error"}). Silakan periksa koneksi internet atau kunci API di Pengaturan."
                chatRepo.insertMessage(role = "ai", message = errorReply)
                if (ttsEnabled.value == "true") {
                    voiceOutputManager.speak("Maaf, koneksi sedang bermasalah.")
                }
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    suspend fun tanyaAi(question: String): String {
        val cmd = processCommand(question)
        if (cmd != null) {
            return cmd.message
        }
        if (!chatLimitManager.canChat()) {
            _showUpgradeDialog.value = true
            return """
            🔒 Limit chat gratis habis (10/hari).
            
            Dukung pengembangan app untuk 200 chat/hari:
            
            📱 Saweria: saweria.co/druzid
            💬 WA: 0858-9407-1160
            
            [Donasi Sekarang] [Punya Kode?]
            """.trimIndent()
        }
        chatLimitManager.recordChat()
        return handleSmartFallback(question)
    }

    suspend fun processCommand(question: String): CommandResult? {
        val trimmed = question.trim()
        val lower = trimmed.lowercase()
        val firstWord = lower.split(" ").firstOrNull()?.removePrefix("/") ?: ""

        return when {
            // 1. /start atau "mulai" atau "halo" atau "hai"
            firstWord == "start" || lower == "mulai" || lower == "halo" || lower == "hai" || lower.startsWith("halo cici") || lower.startsWith("hai cici") -> {
                CommandResult(success = true, message = handleStart())
            }

            // 2. /jadwal atau "jadwal hari ini"
            firstWord == "jadwal" || lower == "jadwal hari ini" || lower == "jadwal sekarang" -> {
                CommandResult(success = true, message = formatJadwal("hari_ini"))
            }

            // 3. /besok atau "jadwal besok"
            firstWord == "besok" || lower == "jadwal besok" -> {
                CommandResult(success = true, message = formatJadwal("besok"))
            }

            // 4. /bahan atau "bahan besok"
            firstWord == "bahan" || lower == "bahan besok" || lower.startsWith("bahan besok") -> {
                CommandResult(success = true, message = handleBahanBesok())
            }

            // 5. /progress atau "progress materi"
            firstWord == "progress" || lower.startsWith("progress materi") || lower.startsWith("progres") -> {
                CommandResult(success = true, message = handleProgress(trimmed))
            }

            // 6. /materi atau "materi kelas X"
            firstWord == "materi" || lower.startsWith("materi kelas") || lower == "daftar materi" -> {
                CommandResult(success = true, message = handleMateri(trimmed))
            }

            // 7. /gambar [prompt]
            firstWord == "gambar" || lower.startsWith("generate gambar") || lower.startsWith("buat gambar") -> {
                handleGambar(trimmed)
            }

            // 8. /search [query]
            firstWord == "search" || lower.startsWith("cari di internet") || lower.startsWith("cari info") || (firstWord == "cari" && lower.split(" ").size > 1 && !lower.contains("materi") && !lower.contains("buku")) -> {
                handleSearch(trimmed)
            }

            // 9. /token atau "sisa token"
            firstWord == "token" || lower == "sisa token" || lower == "cek token" -> {
                CommandResult(success = true, message = handleToken())
            }

            // 10. /libur atau "daftar libur"
            firstWord == "libur" || lower == "daftar libur" || lower == "jadwal libur" || lower == "hari libur" -> {
                CommandResult(success = true, message = handleLibur())
            }

            // 11. /backup atau "backup materi"
            firstWord == "backup" || lower == "backup materi" || lower == "backup data" -> {
                handleBackup()
            }

            // 12. /reset_tahun
            firstWord == "reset_tahun" || lower.startsWith("reset_tahun") || lower.startsWith("reset tahun") -> {
                handleResetTahun(trimmed)
            }

            // 13. /memory atau "lihat memory"
            firstWord == "memory" || lower == "lihat memory" || lower == "riwayat memory" || lower == "riwayat chat" -> {
                CommandResult(success = true, message = handleMemory())
            }

            // 14. /help atau "bantuan"
            firstWord == "help" || lower == "bantuan" || lower == "menu" || lower == "/menu" -> {
                CommandResult(success = true, message = handleHelp())
            }

            // 15. /buku → List buku yang di-upload
            firstWord == "buku" || lower == "daftar buku" || lower == "list buku" -> {
                CommandResult(success = true, message = handleListBuku())
            }

            // 16. /upload_buku → Panduan upload buku PDF
            firstWord == "upload_buku" || lower.startsWith("/upload_buku") || lower.startsWith("upload buku") -> {
                CommandResult(success = true, message = handleUploadBukuInfo())
            }

            // 17. /bahan_ajar [mapel] [kelas] [topik] atau "buat bahan ajar X"
            firstWord == "bahan_ajar" || lower.startsWith("/bahan_ajar") || lower.startsWith("buat bahan ajar") || lower.startsWith("bahan ajar ") -> {
                CommandResult(success = true, message = handleBahanAjarCommand(trimmed))
            }

            // 18. /soal_buku [mapel] [kelas] [topik] [jumlah]
            firstWord == "soal_buku" || lower.startsWith("/soal_buku") || lower.startsWith("soal buku") -> {
                CommandResult(success = true, message = handleSoalBukuCommand(trimmed))
            }

            // 19. /modul_buku [mapel] [kelas] [topik]
            firstWord == "modul_buku" || firstWord == "rpp_buku" || lower.startsWith("/modul_buku") || lower.startsWith("/rpp_buku") || lower.startsWith("modul buku") || lower.startsWith("rpp buku") -> {
                CommandResult(success = true, message = handleModulBukuCommand(trimmed))
            }

            // 20. /cari_buku [keyword]
            firstWord == "cari_buku" || lower.startsWith("/cari_buku") || lower.startsWith("cari di buku") || lower.startsWith("cari buku ") -> {
                CommandResult(success = true, message = handleCariBukuCommand(trimmed))
            }

            // 21. /ringkas_buku [kelas] [mapel] [bab]
            firstWord == "ringkas_buku" || lower.startsWith("/ringkas_buku") || lower.startsWith("ringkas buku") || lower.startsWith("rangkum buku") -> {
                CommandResult(success = true, message = handleRingkasBukuCommand(trimmed))
            }

            // 22. /pengumuman [info]
            firstWord == "pengumuman" || lower.startsWith("buat pengumuman") || lower.startsWith("pengumuman ") -> {
                val info = trimmed.removePrefix("/pengumuman").removePrefix("pengumuman").trim()
                val pengumumanResult = generatePengumuman(
                    jenis = "Custom",
                    infoTambahan = info.ifBlank { "Pengumuman kegiatan untuk siswa dan wali murid" },
                    kelas = userKelasUtama.value?.toIntOrNull() ?: 4,
                    formal = true
                )
                val reply = pengumumanResult.getOrElse { "Gagal membuat pengumuman: ${it.message}" }
                CommandResult(success = true, message = reply)
            }

            // 23. /share_wa
            firstWord == "share_wa" || lower == "share wa" || lower == "share whatsapp" -> {
                val recentAi = app.database.chatDao().getRecentMessages(5).firstOrNull { it.role != "user" }
                val textToShare = recentAi?.message ?: "Belum ada pesan untuk dibagikan."
                CommandResult(
                    success = true,
                    message = "📱 *Siap Dibagikan ke WhatsApp:*\n\n$textToShare\n\n_Gunakan tombol WhatsApp di bawah pesan percakapan untuk langsung mengirim._"
                )
            }

            else -> null
        }
    }

    private suspend fun handleStart(): String {
        val nama = configRepo.getConfigSync("user_name")?.ifBlank { null } ?: userName.value.orEmpty().ifBlank { null } ?: "Guru"
        val sekolah = configRepo.getConfigSync("user_school")?.ifBlank { null } ?: userSchool.value.orEmpty().ifBlank { null } ?: "Sekolah Dasar"
        val asstName = configRepo.getConfigSync("assistant_name") ?: assistantName.value ?: "Cici"
        return "Halo $nama! 👋\n\n" +
                "Saya $asstName, asisten profesional Anda di $sekolah.\n\n" +
                "Saya bisa bantu:\n" +
                "• 📅 Cek jadwal & bahan\n" +
                "• 📚 Cari materi kelas 1-6\n" +
                "• 🎨 Generate gambar\n" +
                "• 🔍 Cari di internet\n" +
                "• 📊 Cek progress\n\n" +
                "Ketik /help untuk semua fitur."
    }

    private suspend fun formatJadwal(hari: String = "hari_ini"): String {
        val qLower = hari.lowercase()
        val targetDay = when {
            qLower == "hari_ini" || qLower == "hari ini" -> getTodayDayName()
            qLower == "besok" -> getTomorrowDayName()
            qLower.contains("senin") -> "Senin"
            qLower.contains("selasa") -> "Selasa"
            qLower.contains("rabu") -> "Rabu"
            qLower.contains("kamis") -> "Kamis"
            qLower.contains("jumat") -> "Jumat"
            qLower.contains("sabtu") -> "Sabtu"
            qLower.contains("minggu") -> "Minggu"
            qLower.contains("besok") -> getTomorrowDayName()
            else -> getTodayDayName()
        }

        val list = app.database.jadwalDao().getJadwalByHariSync(targetDay)
        if (list.isEmpty()) {
            return "📅 Hari $targetDay:\n\nTidak ada jadwal mengajar pada hari $targetDay."
        }

        val formattedList = list.sortedBy { it.jamMulai }.joinToString("\n") {
            "• ${it.jamMulai}-${it.jamSelesai}: ${it.mapel} (Kelas ${it.kelas})"
        }
        return "📅 Jadwal Mengajar Hari $targetDay:\n\n$formattedList"
    }

    private suspend fun handleBahanBesok(): String {
        val besok = getTomorrowDayName()
        val jadwalBesok = app.database.jadwalDao().getJadwalByHariSync(besok)
        if (jadwalBesok.isEmpty()) {
            return "Besok tidak ada jadwal mengajar ($besok)."
        }

        val sb = StringBuilder("🛠 Bahan untuk besok ($besok):\n\n")
        jadwalBesok.forEach { j ->
            val materi = app.database.materiDao().getNextMateri(j.kelas, j.mapel)
                ?: app.database.materiDao().getAllMateriList().firstOrNull { it.kelas == j.kelas && it.mapel == j.mapel }
            sb.append("📖 ${j.mapel} (${j.jamMulai}):\n")
            if (materi != null && materi.bahan.isNotBlank()) {
                val bList = materi.bahan.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (bList.isNotEmpty()) {
                    bList.forEach { b -> sb.append("  • $b\n") }
                } else {
                    sb.append("  • ${materi.bahan}\n")
                }
            } else {
                sb.append("  • Buku teks & modul ajar standar\n")
            }
            sb.append("\n")
        }
        return sb.toString().trimEnd()
    }

    private suspend fun handleProgress(question: String): String {
        val kelasRegex = Regex("(?:kelas|k)\\s*([1-6])", RegexOption.IGNORE_CASE)
        val match = kelasRegex.find(question)
        val targetKelas = match?.groupValues?.get(1)?.toIntOrNull()
            ?: question.split(" ").mapNotNull { it.toIntOrNull() }.firstOrNull { it in 1..6 }
            ?: userKelasUtama.value?.toIntOrNull()
            ?: 4

        val allMateri = app.database.materiDao().getAllMateriList().filter { it.kelas == targetKelas }
        if (allMateri.isEmpty()) {
            return "Belum ada data materi untuk Kelas $targetKelas."
        }

        val grouped = allMateri.groupBy { it.mapel }
        val sb = StringBuilder("📊 Progress Kelas $targetKelas:\n\n")
        grouped.forEach { (mapel, pertemuans) ->
            val completed = pertemuans.count { it.status == "selesai" }
            val total = pertemuans.size
            val pct = if (total > 0) (completed * 100 / total) else 0
            sb.append("• $mapel: $completed/$total pertemuan ($pct%)\n")
        }
        return sb.toString().trimEnd()
    }

    private suspend fun handleMateri(question: String): String {
        val kelasRegex = Regex("(?:kelas|k)\\s*([1-6])", RegexOption.IGNORE_CASE)
        val match = kelasRegex.find(question)
        val targetKelas = match?.groupValues?.get(1)?.toIntOrNull()
            ?: question.split(" ").mapNotNull { it.toIntOrNull() }.firstOrNull { it in 1..6 }
            ?: userKelasUtama.value?.toIntOrNull()
            ?: 4

        val allMateri = app.database.materiDao().getAllMateriList().filter { it.kelas == targetKelas }
        if (allMateri.isEmpty()) {
            return "Belum ada materi untuk Kelas $targetKelas."
        }

        val grouped = allMateri.groupBy { it.mapel }
        val sb = StringBuilder("📚 Kelas $targetKelas:\n\n")
        grouped.forEach { (mapel, pertemuans) ->
            sb.append("• $mapel (${pertemuans.size} pertemuan)\n")
        }
        sb.append("\n💡 Ketik topik tertentu (contoh: 'pecahan', 'fotosintesis') untuk melihat rincian bab dan materi.")
        return sb.toString()
    }

    private suspend fun handleGambar(question: String): CommandResult {
        val cleanPrompt = question.replaceFirst(Regex("^/(gambar|generate\\s+gambar)\\s*", RegexOption.IGNORE_CASE), "")
            .replaceFirst(Regex("^(gambar|generate gambar)\\s*", RegexOption.IGNORE_CASE), "")
            .trim()

        if (cleanPrompt.isBlank()) {
            return CommandResult(
                success = false,
                message = "Silakan tentukan gambar apa yang ingin dibuat.\nContoh: `/gambar daur hidup kupu-kupu` atau `/gambar sistem tata surya`"
            )
        }

        return try {
            val encodedPrompt = URLEncoder.encode("education illustration for primary school: $cleanPrompt, clear vibrant educational diagram", StandardCharsets.UTF_8.toString())
            val imageUrl = "https://image.pollinations.ai/prompt/$encodedPrompt"

            val saved = ImageSaverHelper.saveImageFromUrlToGallery(app, imageUrl, cleanPrompt)
            val msg = if (saved) {
                "Gambar dibuat! Cek galeri HP.\n\nIlustrasi: \"$cleanPrompt\"\nFile tersimpan di folder Pictures/AsistenMengajar."
            } else {
                "Gambar dibuat!\n\nIlustrasi: \"$cleanPrompt\"."
            }
            CommandResult(success = true, message = msg, imageUrl = imageUrl)
        } catch (e: Exception) {
            CommandResult(success = false, message = "Maaf, generate gambar error.")
        }
    }

    private suspend fun handleSearch(question: String): CommandResult {
        val cleanQuery = question.replaceFirst(Regex("^/(search|cari)\\s*", RegexOption.IGNORE_CASE), "")
            .replaceFirst(Regex("^(search|cari|cari di internet)\\s*", RegexOption.IGNORE_CASE), "")
            .trim()

        if (cleanQuery.isBlank()) {
            return CommandResult(
                success = false,
                message = "Silakan tentukan kata kunci pencarian.\nContoh: `/search Ki Hajar Dewantara`"
            )
        }

        val searchResult = SearchHelper.searchDuckDuckGo(cleanQuery)
        if (!searchResult.isNullOrBlank()) {
            return CommandResult(
                success = true,
                message = "Hasil pencarian [$cleanQuery]:\n\n$searchResult"
            )
        }

        // Fallback to Gemini
        val geminiResult = askGemini("Jelaskan informasi penting dan akurat mengenai: $cleanQuery")
        return CommandResult(
            success = true,
            message = "Hasil pencarian [$cleanQuery] (via Gemini AI):\n\n$geminiResult"
        )
    }

    private fun handleToken(): String {
        val used = chatLimitManager.getTodayChatCount()
        val limit = chatLimitManager.getLimit()
        val isUnlimited = limit == Int.MAX_VALUE
        val chatStat = if (isUnlimited) {
            "$used (Unlimited • Pakai API Sendiri)"
        } else {
            "$used/$limit"
        }
        val apiKeyManager = com.example.data.remote.ApiKeyManager.getInstance(app)
        val geminiCount = apiKeyManager.getGeminiKeys().size
        val deepSeekCount = apiKeyManager.getDeepSeekKeys().size

        return """
        Status API:
        🤖 Provider Utama: Nexotao Bawaan (DeepSeek V3.2)
        📊 Hari ini: $chatStat chat
        • Gemini: $geminiCount key terdaftar
        • DeepSeek: $deepSeekCount key terdaftar
        ⏰ Reset: besok jam 00:00

        💡 Upgrade Premium untuk 200 chat/hari
        📱 Atau tambah API sendiri di Pengaturan > Advanced
        """.trimIndent()
    }

    private suspend fun handleLibur(): String {
        val allKalender = app.database.kalenderDao().getAll()
        val liburList = allKalender.filter {
            it.jenis.contains("libur", ignoreCase = true) ||
            it.jenis in listOf("libur_nasional", "libur_semester", "libur_keagamaan", "libur_khusus")
        }

        if (liburList.isEmpty()) {
            return "Belum ada agenda libur tercatat di kalender pendidikan."
        }

        val sb = StringBuilder("🏖 Libur mendatang:\n\n")
        liburList.take(15).forEach { k ->
            val tgl = when {
                k.tanggal.isNotBlank() -> k.tanggal
                k.tanggalMulai.isNotBlank() && k.tanggalSelesai.isNotBlank() -> "${k.tanggalMulai} s/d ${k.tanggalSelesai}"
                else -> k.tanggalMulai.ifBlank { k.tanggalSelesai }
            }
            sb.append("• $tgl - ${k.keterangan}\n")
        }
        return sb.toString().trimEnd()
    }

    private suspend fun handleBackup(): CommandResult {
        return try {
            val jsonString = BackupHelper.createBackupJson(app, app.database)
            val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val fileName = "backup_$dateStr.json"

            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = File(downloadsDir, fileName)
                file.writeText(jsonString)
            } catch (_: Exception) {
                val fallbackFile = File(app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                fallbackFile.writeText(jsonString)
            }

            CommandResult(
                success = true,
                message = "Backup selesai! File: $fileName\n\nTersimpan di folder Downloads perangkat."
            )
        } catch (e: Exception) {
            CommandResult(success = false, message = "Gagal membuat backup: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    private suspend fun handleResetTahun(question: String): CommandResult {
        val qLower = question.lowercase().trim()
        val isConfirmed = qLower.contains("ya") || qLower.contains("yes") || qLower.contains("konfirmasi")

        if (!isConfirmed) {
            return CommandResult(
                success = false,
                message = "⚠️ KONFIRMASI RESET TAHUN AJARAN\n\n" +
                        "Perintah ini akan:\n" +
                        "1. Menghapus seluruh riwayat chat (memory asisten).\n" +
                        "2. Mereset status semua materi pembelajaran ke 'belum'.\n\n" +
                        "Untuk konfirmasi, ketik: `/reset_tahun ya`"
            )
        }

        app.database.chatDao().clearHistory()
        app.database.materiDao().resetAllStatus()

        return CommandResult(
            success = true,
            message = "Tahun ajaran direset.\n\nRiwayat chat telah dibersihkan dan status materi telah diatur ulang ke 'belum'."
        )
    }

    private suspend fun handleMemory(): String {
        val recent = app.database.chatDao().getRecentMessages(10)
        if (recent.isEmpty()) {
            return "Memory percakapan masih kosong."
        }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val sb = StringBuilder("🧠 Memory:\n\n")
        recent.forEach { c ->
            val time = timeFormat.format(Date(c.timestamp))
            val sender = if (c.role == "user") "User" else "Cici"
            val snippet = if (c.message.length > 50) c.message.take(50).replace("\n", " ") + "..." else c.message.replace("\n", " ")
            sb.append("• [$time] $sender: $snippet\n")
        }
        return sb.toString().trimEnd()
    }

    private fun handleHelp(): String {
        return """
        📋 Command yang tersedia:
        /start - Sambutan
        /jadwal - Jadwal hari ini
        /besok - Jadwal besok
        /bahan - Bahan besok
        /progress - Progress materi
        /materi - Lihat materi
        /gambar - Generate gambar
        /search - Cari di internet
        /token - Sisa token API
        /libur - Daftar libur
        /backup - Backup materi
        /reset_tahun - Reset tahun
        /memory - Lihat memory
        /help - Bantuan

        📚 BUKU & RAG (Retrieval Augmented Generation):
        /buku - List buku yang di-upload
        /upload_buku - Panduan upload buku PDF
        /bahan_ajar [mapel] [kelas] [topik] - Bahan ajar multi-sumber
        /soal_buku [mapel] [kelas] [topik] [jumlah] - Buat soal dari buku
        /modul_buku [mapel] [kelas] [topik] - Buat modul ajar dari buku guru
        /cari_buku [keyword] - Cari materi di semua buku
        /ringkas_buku [kelas] [mapel] [bab] - Ringkas materi bab dari buku

        📢 PENGUMUMAN & SHARE:
        /pengumuman - Generate pengumuman WA
        /share_wa - Share ke WhatsApp
        """.trimIndent()
    }

    // === FITUR RAG: COMMAND HANDLERS UNTUK BUKU & MATERI ===

    private suspend fun handleListBuku(): String {
        val allBuku = app.database.bukuDao().getAllBuku()
        if (allBuku.isEmpty()) {
            return "📚 *Daftar Buku Pelajaran*\n\n" +
                    "Belum ada buku yang di-upload.\n\n" +
                    "💡 Buka menu *Bank Materi* > Tab *Buku Pelajaran* untuk mengunggah PDF Buku Guru atau Buku Siswa, atau ketik */upload_buku* untuk panduan lengkap."
        }

        val sb = StringBuilder("📚 *Daftar Buku Pelajaran (${allBuku.size} buku):*\n\n")
        allBuku.forEachIndexed { idx, b ->
            val icon = if (b.tipe.equals("guru", true)) "👨‍🏫" else "🎒"
            sb.append("${idx + 1}. $icon *${b.judul}*\n")
            sb.append("   • Kelas ${b.kelas} | ${b.mapel}\n")
            sb.append("   • Tipe: Buku ${b.tipe.replaceFirstChar { it.uppercase() }} (${b.totalHalaman} hal)\n\n")
        }
        sb.append("💡 _Perintah Buku:_\n")
        sb.append("• `/cari_buku [kata kunci]` - Menelusuri isi materi buku\n")
        sb.append("• `/bahan_ajar [mapel] [kelas] [topik]` - Buat lembar ajar RAG\n")
        sb.append("• `/soal_buku [mapel] [kelas] [topik]` - Buat soal dari buku siswa\n")
        sb.append("• `/modul_buku [mapel] [kelas] [topik]` - Buat modul dari buku guru")
        return sb.toString().trim()
    }

    private fun handleUploadBukuInfo(): String {
        return "📖 *Panduan Unggah Buku Pelajaran (PDF)*\n\n" +
                "Untuk mengunggah buku PDF:\n" +
                "1. Buka menu navigasi bawah **Bank Materi**.\n" +
                "2. Pilih tab **Buku Pelajaran**.\n" +
                "3. Klik tombol **+ Upload Buku (PDF)**.\n" +
                "4. Pilih file PDF Buku Guru atau Siswa dari perangkat Anda.\n" +
                "5. Masukkan judul, kelas (1-6), mapel, dan tipe (Guru/Siswa).\n\n" +
                "✨ *Fitur RAG Cici:*\n" +
                "Setiap halaman buku akan diindeks otomatis sehingga Cici dapat mengutip halaman buku nyata saat menyusun Bahan Ajar, Soal Latihan, dan Modul Pembelajaran!"
    }

    private suspend fun handleBahanAjarCommand(input: String): String {
        val clean = input.removePrefix("/bahan_ajar")
            .removePrefix("bahan_ajar")
            .removePrefix("buat bahan ajar")
            .removePrefix("bahan ajar")
            .trim()

        if (clean.isBlank()) {
            return "📚 *Generator Bahan Ajar Multi-Sumber*\n\n" +
                    "Gunakan format:\n" +
                    "*/bahan_ajar [mapel] [kelas] [topik]*\n\n" +
                    "Contoh:\n" +
                    "• `/bahan_ajar IPAS 4 Bagian Tumbuhan`\n" +
                    "• `/bahan_ajar Matematika 4 Pecahan Senilai`\n" +
                    "• `buat bahan ajar IPAS 5 Sistem Pencernaan`"
        }

        val parsed = parseMapelKelasTopik(clean)
        return generateBahanAjar(
            parsed.kelas,
            parsed.mapel,
            parsed.topik,
            true
        )
    }

    private suspend fun handleSoalBukuCommand(input: String): String {
        val clean = input.removePrefix("/soal_buku")
            .removePrefix("soal_buku")
            .removePrefix("soal buku")
            .trim()

        if (clean.isBlank()) {
            return "📝 *Buat Soal Berdasarkan Buku Siswa (RAG)*\n\n" +
                    "Format:\n" +
                    "*/soal_buku [mapel] [kelas] [topik] [jumlah]*\n\n" +
                    "Contoh:\n" +
                    "• `/soal_buku Matematika 4 Pecahan 5`\n" +
                    "• `/soal_buku IPAS 4 Tumbuhan 10`"
        }

        val tokens = clean.split(" ").filter { it.isNotBlank() }.toMutableList()
        var jumlah = 5
        val lastNum = tokens.lastOrNull()?.toIntOrNull()
        if (lastNum != null && lastNum in 1..50) {
            jumlah = lastNum
            tokens.removeAt(tokens.lastIndex)
        }

        val parsed = parseMapelKelasTopik(tokens.joinToString(" "))
        val result = generateSoal(
            mapel = parsed.mapel,
            kelas = parsed.kelas,
            topik = parsed.topik,
            jumlah = jumlah,
            tipe = "Pilihan Ganda",
            pakaiBuku = true
        )
        return result.getOrElse { "Gagal membuat soal dari buku: ${it.message}" }
    }

    private suspend fun handleModulBukuCommand(input: String): String {
        val clean = input.removePrefix("/modul_buku")
            .removePrefix("/rpp_buku")
            .removePrefix("modul_buku")
            .removePrefix("rpp_buku")
            .removePrefix("modul buku")
            .removePrefix("rpp buku")
            .trim()

        if (clean.isBlank()) {
            return "📑 *Buat Modul Ajar Berdasarkan Buku Guru (RAG)*\n\n" +
                    "Format:\n" +
                    "*/modul_buku [mapel] [kelas] [topik]*\n\n" +
                    "Contoh:\n" +
                    "• `/modul_buku IPAS 4 Bagian Tumbuhan`\n" +
                    "• `/modul_buku Matematika 4 Pecahan`"
        }

        val parsed = parseMapelKelasTopik(clean)
        val result = generateModulAjar(
            mapel = parsed.mapel,
            kelas = parsed.kelas,
            topik = parsed.topik,
            jp = 2,
            semester = "Semester 1",
            pakaiBuku = true
        )
        return result.getOrElse { "Gagal menyusun modul ajar dari buku: ${it.message}" }
    }

    private suspend fun handleCariBukuCommand(input: String): String {
        val keyword = input.removePrefix("/cari_buku")
            .removePrefix("cari_buku")
            .removePrefix("cari di buku")
            .removePrefix("cari buku")
            .trim()

        if (keyword.isBlank()) {
            return "🔍 *Pencarian Buku Pelajaran*\n\n" +
                    "Gunakan format:\n" +
                    "*/cari_buku [kata kunci]*\n\n" +
                    "Contoh:\n" +
                    "• `/cari_buku fotosintesis`\n" +
                    "• `/cari_buku pecahan campuran`"
        }

        val chunks = app.database.bukuChunkDao().searchAllChunks(keyword)
        if (chunks.isEmpty()) {
            return "🔍 Tidak ditemukan kutipan materi dengan kata kunci \"$keyword\" pada buku yang sudah di-upload.\n\n" +
                    "💡 Pastikan buku pelajaran telah diunggah di menu *Bank Materi* atau gunakan `/upload_buku`."
        }

        val allBukuMap = app.database.bukuDao().getAllBuku().associateBy { it.id }
        val sb = StringBuilder("🔍 *Hasil Pencarian Buku untuk \"$keyword\":*\n\n")

        chunks.take(8).forEachIndexed { index, chunk ->
            val buku = allBukuMap[chunk.bukuId]
            val bukuName = if (buku != null) "${buku.judul} (${buku.tipe.uppercase()}, Kls ${buku.kelas})" else "Buku #${chunk.bukuId}"
            sb.append("${index + 1}. *📖 $bukuName - Hal. ${chunk.halaman}*\n")
            val preview = chunk.text.trim().take(220)
            sb.append("\"$preview${if (chunk.text.length > 220) "..." else ""}\"\n\n")
        }

        sb.append("💡 _Gunakan /bahan_ajar untuk merangkum materi ini ke dalam lembar ajar._")
        return sb.toString().trim()
    }

    private suspend fun handleRingkasBukuCommand(input: String): String {
        val clean = input.removePrefix("/ringkas_buku")
            .removePrefix("ringkas_buku")
            .removePrefix("ringkas buku")
            .removePrefix("rangkum buku")
            .trim()

        if (clean.isBlank()) {
            return "📖 *Ringkas Bab / Materi Buku*\n\n" +
                    "Format:\n" +
                    "*/ringkas_buku [kelas] [mapel] [bab]*\n\n" +
                    "Contoh:\n" +
                    "• `/ringkas_buku 4 IPAS Tumbuhan`\n" +
                    "• `/ringkas_buku 4 Matematika Pecahan`"
        }

        val parsed = parseMapelKelasTopik(clean)
        val bukuGuru = app.database.bukuDao().getBuku(parsed.kelas, parsed.mapel, "guru")
        val bukuSiswa = app.database.bukuDao().getBuku(parsed.kelas, parsed.mapel, "siswa")

        val chunks = mutableListOf<com.example.data.local.BukuChunkEntity>()
        if (bukuSiswa != null) {
            chunks.addAll(app.database.bukuChunkDao().searchByKeyword(bukuSiswa.id, parsed.topik))
        }
        if (bukuGuru != null && chunks.size < 5) {
            chunks.addAll(app.database.bukuChunkDao().searchByKeyword(bukuGuru.id, parsed.topik))
        }

        val contextText = if (chunks.isNotEmpty()) {
            "Kutipan dari Buku ${parsed.mapel} Kelas ${parsed.kelas} (Hal ${chunks.map { it.halaman }.distinct().joinToString(", ")}):\n\n" +
            chunks.take(10).joinToString("\n\n") { "Hal ${it.halaman}: ${it.text}" }
        } else {
            "(Belum ada buku untuk ${parsed.mapel} Kelas ${parsed.kelas} yang di-upload)"
        }

        val prompt = """
        Kamu guru SD profesional. Buat ringkasan intisari materi yang jelas, padat, dan mudah dipahami siswa SD untuk:
        - Mapel: ${parsed.mapel}
        - Kelas: ${parsed.kelas}
        - Bab/Topik: ${parsed.topik}

        Referensi:
        $contextText

        Format:
        📖 Ringkasan: [Topik] (${parsed.mapel} Kelas ${parsed.kelas})
        
        1. Konsep Kunci
        2. Poin-Poin Penting
        3. Kata Kunci & Istilah Baru
        4. Kesimpulan Ringkas
        """.trimIndent()

        val response = ApiCaller.callAiWithFallback(context = app, prompt = prompt)
        return com.example.util.LatexRenderer.render(response)
    }

    private data class ParsedMapelKelasTopik(
        val mapel: String,
        val kelas: Int,
        val topik: String
    )

    private fun parseMapelKelasTopik(raw: String): ParsedMapelKelasTopik {
        val tokens = raw.split(" ").filter { it.isNotBlank() }.toMutableList()
        var detectedKelas = userKelasUtama.value?.toIntOrNull() ?: 4
        var detectedMapel: String? = null

        val kelasIndex = tokens.indexOfFirst { token ->
            token.toIntOrNull() in 1..6
        }
        if (kelasIndex != -1) {
            detectedKelas = tokens[kelasIndex].toInt()
            tokens.removeAt(kelasIndex)
            if (kelasIndex > 0 && (tokens[kelasIndex - 1].equals("kelas", true) || tokens[kelasIndex - 1].equals("kls", true))) {
                tokens.removeAt(kelasIndex - 1)
            }
        }

        val knownMapel = listOf(
            "pendidikan pancasila",
            "bahasa indonesia",
            "bahasa inggris",
            "seni rupa",
            "matematika",
            "pancasila",
            "ipas",
            "ipa",
            "ips",
            "pai",
            "pjok"
        )

        val remainingJoined = tokens.joinToString(" ")
        for (km in knownMapel) {
            val regex = Regex("^(?i)\\b$km\\b")
            if (regex.containsMatchIn(remainingJoined)) {
                detectedMapel = when (km.lowercase()) {
                    "ipa", "ips", "ipas" -> "IPAS"
                    "pancasila", "pendidikan pancasila" -> "Pendidikan Pancasila"
                    "pai" -> "PAI"
                    "pjok" -> "PJOK"
                    "matematika" -> "Matematika"
                    "bahasa indonesia" -> "Bahasa Indonesia"
                    "bahasa inggris" -> "Bahasa Inggris"
                    "seni rupa" -> "Seni Rupa"
                    else -> km.replaceFirstChar { it.uppercase() }
                }
                val mapelWordCount = km.split(" ").size
                for (i in 0 until mapelWordCount.coerceAtMost(tokens.size)) {
                    tokens.removeAt(0)
                }
                break
            }
        }

        val finalMapel = detectedMapel ?: (todaySchedule.value.firstOrNull()?.jadwal?.mapel ?: "IPAS")
        val finalTopik = tokens.joinToString(" ").trim().ifEmpty { "Materi Umum $finalMapel" }
        return ParsedMapelKelasTopik(finalMapel, detectedKelas, finalTopik)
    }

    private suspend fun handleSmartFallback(question: String): String {
        val q = question.trim()
        val qLower = q.lowercase()

        // 1. Cek intent: apakah tanya jadwal?
        if (isTanyaJadwal(qLower)) {
            return formatJadwal(qLower)
        }

        // 2. Cek bank materi — cari keyword jika menanyakan materi/bab/pelajaran
        if (qLower.contains("materi") || qLower.contains("bab") || qLower.contains("pertemuan") || qLower.contains("ajar")) {
            val cleanKw = qLower.replace("cari", "")
                .replace("materi", "")
                .replace("tentang", "")
                .replace("apakah", "")
                .replace("ada", "")
                .replace("pelajaran", "")
                .replace("bab", "")
                .trim()
            if (cleanKw.length >= 3) {
                val materi = app.database.materiDao().searchByKeyword(cleanKw)
                if (materi.isNotEmpty()) {
                    return formatMateri(materi)
                }
            }
        }

        // 3. Cek konteks: apakah tanya definisi/umum?
        // ("apa itu", "jelaskan", "pengertian", "definisi")
        if (isTanyaDefinisi(qLower)) {
            val wiki = com.example.util.WikipediaHelper.cariWikipedia(app, q)
            if (!wiki.isNullOrBlank()) {
                return "📖 Menurut Wikipedia:\n\n$wiki\n\nSumber: Wikipedia Indonesia"
            }
        }

        // 4. Fallback: Gemini
        return askGemini(q)
    }

    private fun isTanyaJadwal(qLower: String): Boolean {
        return qLower.contains("jadwal") ||
               qLower.contains("ngajar apa") ||
               qLower.contains("mengajar apa") ||
               qLower.contains("pelajaran hari") ||
               qLower.contains("ada jam") ||
               (qLower.contains("pelajaran") && (qLower.contains("senin") || qLower.contains("selasa") || qLower.contains("rabu") || qLower.contains("kamis") || qLower.contains("jumat") || qLower.contains("sabtu") || qLower.contains("hari ini") || qLower.contains("besok")))
    }

    fun getTomorrowDayName(): String {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Senin"
            Calendar.TUESDAY -> "Selasa"
            Calendar.WEDNESDAY -> "Rabu"
            Calendar.THURSDAY -> "Kamis"
            Calendar.FRIDAY -> "Jumat"
            Calendar.SATURDAY -> "Sabtu"
            else -> "Minggu"
        }
    }

    private fun isTanyaDefinisi(qLower: String): Boolean {
        return qLower.contains("apa itu") ||
               qLower.contains("apakah itu") ||
               qLower.contains("jelaskan") ||
               qLower.contains("pengertian") ||
               qLower.contains("definisi") ||
               qLower.contains("apa yang dimaksud") ||
               qLower.contains("arti dari") ||
               qLower.contains("apa arti")
    }

    private fun formatMateri(materiList: List<MateriEntity>): String {
        val sb = StringBuilder("📚 Ditemukan di Bank Materi Kurikulum Merdeka:\n\n")
        materiList.take(3).forEachIndexed { idx, m ->
            sb.append("${idx + 1}. [Kelas ${m.kelas}] ${m.mapel} (Pertemuan ${m.pertemuan})\n")
            sb.append("   📖 Materi: ${m.judul}\n")
            if (m.bab.isNotBlank()) sb.append("   Bab: ${m.bab}\n")
            if (m.tujuan.isNotBlank()) sb.append("   🎯 Tujuan: ${m.tujuan}\n")
            if (m.bahan.isNotBlank()) sb.append("   🛠 Bahan: ${m.bahan}\n")
            sb.append("\n")
        }
        return sb.toString().trimEnd()
    }

    private suspend fun askGemini(question: String): String {
        val jadwalList = app.database.jadwalDao().getAllJadwalList().ifEmpty { allJadwal.value }
        val nama = configRepo.getConfigSync("user_name")?.ifBlank { null } ?: userName.value.orEmpty().ifBlank { null } ?: "Guru"
        val sekolah = configRepo.getConfigSync("user_school")?.ifBlank { null } ?: userSchool.value.orEmpty().ifBlank { null } ?: "Sekolah Dasar"
        val kelas = configRepo.getConfigSync("user_kelas") ?: userKelas.value ?: "1,2,3,4,5,6"
        val asstName = configRepo.getConfigSync("assistant_name") ?: assistantName.value ?: "Cici"

        val jadwalText = jadwalList
            .groupBy { it.hari }
            .entries.joinToString("\n\n") { (hari, list) ->
                "$hari:\n" + list.joinToString("\n") {
                    "  - ${it.jamMulai}-${it.jamSelesai}: ${it.mapel} (kelas ${it.kelas})"
                }
            }

        val systemPrompt = """
        Kamu adalah $asstName, asisten pribadi $nama di $sekolah. Kelas yang diampu: $kelas.

        === JADWAL MENGAJAR ===
        $jadwalText

        === ATURAN ===
        - Jawab HANYA berdasarkan data di atas jika relevan.
        - Jawab ringkas, ramah, dan membantu guru SD dalam bahasa Indonesia.
        - PENTING: Jangan gunakan format LaTeX seperti \frac{}{} atau ${'$'}...${'$'}. Untuk pecahan, tulis sebagai 3/8 (bukan \frac{3}{8}). Pakai simbol Unicode untuk operasi: ×, ÷, ≤, ≥. Format plain text yang mudah dibaca di HP.
        """.trimIndent()

        val answer = ApiCaller.callAiWithFallback(
            context = app,
            prompt = question,
            systemPrompt = systemPrompt
        )
        return com.example.util.LatexRenderer.render(answer)
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatRepo.clearHistory()
            voiceOutputManager.stop()
        }
    }

    // Jadwal CRUD
    fun saveJadwal(jadwal: JadwalEntity, isEdit: Boolean, onComplete: () -> Unit) {
        viewModelScope.launch {
            if (isEdit && jadwal.id != 0) {
                jadwalRepo.update(jadwal)
            } else {
                jadwalRepo.insert(jadwal)
            }
            com.example.notification.ReminderScheduler.rescheduleAll(app)
            onComplete()
        }
    }

    fun deleteJadwal(jadwal: JadwalEntity, onComplete: () -> Unit) {
        viewModelScope.launch {
            jadwalRepo.delete(jadwal)
            com.example.notification.ReminderScheduler.rescheduleAll(app)
            onComplete()
        }
    }

    // Kalender CRUD
    fun saveKalender(kalender: KalenderEntity, isEdit: Boolean, onComplete: () -> Unit) {
        viewModelScope.launch {
            if (isEdit && kalender.id != 0L) {
                kalenderRepo.update(kalender)
            } else {
                kalenderRepo.insert(kalender)
            }
            onComplete()
        }
    }

    fun deleteKalender(kalender: KalenderEntity, onComplete: () -> Unit) {
        viewModelScope.launch {
            kalenderRepo.delete(kalender)
            onComplete()
        }
    }

    // Settings actions
    fun saveUserProfile(name: String, role: String, school: String, kelasUtama: String, kelasList: List<Int>) {
        val sortedKelas = kelasList.sorted().joinToString(",")
        cacheConfig("user_name", name)
        cacheConfig("user_role", role)
        cacheConfig("user_school", school)
        cacheConfig("user_kelas_utama", kelasUtama)
        cacheConfig("user_kelas", sortedKelas)
        appPrefs.edit()
            .putString("user_name", name)
            .putString("user_role", role)
            .putString("user_school", school)
            .putString("user_kelas_utama", kelasUtama)
            .putString("user_kelas", sortedKelas)
            .apply()
        viewModelScope.launch {
            configRepo.setConfig("user_name", name)
            configRepo.setConfig("user_role", role)
            configRepo.setConfig("user_school", school)
            configRepo.setConfig("user_kelas_utama", kelasUtama)
            configRepo.setConfig("user_kelas", sortedKelas)
        }
    }

    fun saveInitialSetup(
        name: String,
        role: String,
        school: String,
        kelasUtama: String,
        kelasList: List<Int>,
        aiName: String,
        aiGreeting: String
    ) {
        val sortedKelas = kelasList.sorted().joinToString(",")
        val cleanAiName = aiName.trim().ifBlank { "Cici" }
        val cleanGreeting = aiGreeting.trim().ifBlank { "Halo! Siap membantu." }

        cacheConfig("user_name", name)
        cacheConfig("user_role", role)
        cacheConfig("user_school", school)
        cacheConfig("user_kelas_utama", kelasUtama)
        cacheConfig("user_kelas", sortedKelas)
        cacheConfig("assistant_name", cleanAiName)
        cacheConfig("assistant_greeting", cleanGreeting)

        appPrefs.edit()
            .putString("user_name", name)
            .putString("user_role", role)
            .putString("user_school", school)
            .putString("user_kelas_utama", kelasUtama)
            .putString("user_kelas", sortedKelas)
            .putString("assistant_name", cleanAiName)
            .putString("assistant_greeting", cleanGreeting)
            .putBoolean("setup_done", true)
            .apply()

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            configRepo.setConfig("user_name", name)
            configRepo.setConfig("user_role", role)
            configRepo.setConfig("user_school", school)
            configRepo.setConfig("user_kelas_utama", kelasUtama)
            configRepo.setConfig("user_kelas", sortedKelas)
            configRepo.setConfig("assistant_name", cleanAiName)
            configRepo.setConfig("assistant_greeting", cleanGreeting)
        }
    }

    fun saveApiKey(apiKey: String) {
        viewModelScope.launch {
            configRepo.setConfig("gemini_api_key", apiKey.trim())
        }
    }

    fun setBriefingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            configRepo.setConfig("notification_briefing_enabled", enabled.toString())
            if (enabled) {
                val timeStr = briefingTime.value ?: "20:00"
                val parts = timeStr.split(":")
                val hour = parts.getOrNull(0)?.toIntOrNull() ?: 20
                val min = parts.getOrNull(1)?.toIntOrNull() ?: 0
                BriefingWorker.scheduleDailyBriefing(getApplication(), hour, min)
            } else {
                BriefingWorker.cancelDailyBriefing(getApplication())
            }
        }
    }

    fun setBriefingTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            val formatted = String.format("%02d:%02d", hour, minute)
            configRepo.setConfig("notification_briefing_time", formatted)
            if (briefingEnabled.value == "true") {
                BriefingWorker.scheduleDailyBriefing(getApplication(), hour, minute)
            }
        }
    }

    fun setHydrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            configRepo.setConfig("notification_hydration_enabled", enabled.toString())
        }
    }

    fun setTtsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            configRepo.setConfig("voice_tts_enabled", enabled.toString())
            if (!enabled) {
                voiceOutputManager.stop()
            }
        }
    }

    fun triggerTestNotification() {
        val today = getTodayDayName()
        val teacher = userName.value.orEmpty().ifBlank { "Guru" }
        NotificationHelper.showBriefingNotification(
            getApplication(),
            "Uji Coba Notifikasi Briefing ($today)",
            "Jadwal mengajar dan bahan ajar siap dicek. Semangat mengajar hari ini, $teacher!"
        )
    }

    fun triggerTestHydration() {
        val teacher = userName.value.orEmpty().ifBlank { "Guru" }
        NotificationHelper.showHydrationReminder(getApplication(), teacher)
    }

    suspend fun getBackupJson(context: android.content.Context): String {
        return BackupHelper.createBackupJson(context, app.database)
    }

    suspend fun restoreBackup(context: android.content.Context, jsonString: String): BackupHelper.RestoreResult {
        return BackupHelper.restoreFromJson(context, jsonString, app.database)
    }

    suspend fun resetToDefault(context: android.content.Context): BackupHelper.RestoreResult {
        return BackupHelper.resetToDefaultData(context, app.database)
    }

    suspend fun resetData(context: android.content.Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val db = app.database
            // Hapus data
            db.materiDao().deleteAll()
            db.jadwalDao().deleteAll()
            db.kalenderDao().deleteAll()
            db.chatDao().deleteAll()
            db.configDao().deleteAll()

            // Re-import dari assets
            val result = BackupHelper.resetToDefaultData(context, db)
            when (result) {
                is BackupHelper.RestoreResult.Success -> {
                    DataImporter.seedSampleBukuIfNeeded(db)
                    Result.success(result.message)
                }
                is BackupHelper.RestoreResult.Error -> {
                    Result.failure(Exception(result.message))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // === FITUR 5: GENERATOR SOAL (DENGAN DUKUNGAN RAG BUKU SISWA) ===
    suspend fun generateSoal(
        mapel: String,
        kelas: Int,
        topik: String,
        jumlah: Int,
        tipe: String,
        pakaiBuku: Boolean = true
    ): Result<String> {
        val bukuSiswa = if (pakaiBuku) app.database.bukuDao().getBuku(kelas, mapel, "siswa") else null
        val chunks = if (bukuSiswa != null) {
            app.database.bukuChunkDao().searchByKeyword(bukuSiswa.id, topik)
        } else emptyList()

        val sumberText = if (chunks.isNotEmpty()) {
            "Buku Siswa Kelas $kelas, hal " +
            chunks.map { it.halaman }.distinct().joinToString(", ") +
            ":\n" +
            chunks.joinToString("\n\n") { "Hal ${it.halaman}: ${it.text}" }
        } else null

        val prompt = if (sumberText != null) {
            """
            Kamu guru SD. Berdasarkan buku:

            $sumberText

            Buat $jumlah soal $tipe tentang $topik untuk kelas $kelas mata pelajaran $mapel.

            PENTING: Jangan gunakan LaTeX seperti \frac{}{} atau ${'$'}...${'$'}. Untuk pecahan, tulis 3/8 bukan \frac{3}{8}.
            Untuk operasi matematika, pakai simbol Unicode:
            - Kali: ×
            - Bagi: ÷
            - Kurang dari sama dengan: ≤
            - Lebih dari sama dengan: ≥

            Format:
            1. [Soal]
               A. ...
               B. ...
               C. ...
               D. ...

            2. [Soal]
               ...

            Kunci Jawaban:
            1. A
            2. B
            """.trimIndent()
        } else {
            """
            Kamu guru SD. Buat $jumlah soal $tipe tentang $topik untuk kelas $kelas mata pelajaran $mapel. (Buku belum di-upload, pakai pengetahuan umum)

            PENTING: Jangan gunakan format LaTeX seperti \frac{}{} atau ${'$'}...${'$'}.
            Untuk pecahan, tulis sebagai 3/8 (bukan \frac{3}{8}).
            Untuk operasi matematika, pakai simbol Unicode:
            - Kali: ×
            - Bagi: ÷
            - Kurang dari sama dengan: ≤
            - Lebih dari sama dengan: ≥

            Tulis soal dan penjelasan dalam format plain text yang mudah dibaca di HP.

            Format output:
            1. [Soal]
               A. ...
               B. ...
               C. ...
               D. ...

            2. [Soal]
               ...

            Kunci Jawaban:
            1. A
            2. B
            ...
            """.trimIndent()
        }

        val result = callGeminiRaw(prompt)
        return if (result.isSuccess) {
            val text = com.example.util.LatexRenderer.render(result.getOrNull() ?: "")
            app.soalHistoryRepository.insert(
                com.example.data.local.SoalHistoryEntity(
                    mapel = mapel,
                    kelas = kelas,
                    topik = topik,
                    jumlah = jumlah,
                    tipe = tipe,
                    hasil = text
                )
            )
            Result.success(text)
        } else {
            result
        }
    }

    fun deleteSoalHistory(item: com.example.data.local.SoalHistoryEntity) {
        viewModelScope.launch {
            app.soalHistoryRepository.delete(item)
        }
    }

    // === FITUR 6: GENERATOR RPP / MODUL AJAR (DENGAN DUKUNGAN RAG BUKU GURU) ===
    suspend fun generateModulAjar(
        mapel: String,
        kelas: Int,
        topik: String,
        jp: Int,
        semester: String,
        pakaiBuku: Boolean = true
    ): Result<String> {
        val bukuGuru = if (pakaiBuku) app.database.bukuDao().getBuku(kelas, mapel, "guru") else null
        val chunks = if (bukuGuru != null) {
            app.database.bukuChunkDao().searchByKeyword(bukuGuru.id, topik)
        } else emptyList()

        val sumberText = if (chunks.isNotEmpty()) {
            "Buku Guru Kelas $kelas, hal " +
            chunks.map { it.halaman }.distinct().joinToString(", ") +
            ":\n" +
            chunks.joinToString("\n\n") { "Hal ${it.halaman}: ${it.text}" }
        } else null

        val prompt = if (sumberText != null) {
            """
            Kamu guru SD. Berdasarkan buku guru:

            $sumberText

            Buat Modul Ajar lengkap untuk:
            - Mapel: $mapel
            - Kelas: $kelas
            - Topik: $topik
            - Alokasi: $jp JP
            - Semester: $semester

            PENTING: Jangan gunakan format LaTeX seperti \frac{}{} atau ${'$'}...${'$'}.
            Untuk pecahan, tulis sebagai 3/8 (bukan \frac{3}{8}).

            Format: (10 komponen Kemendikbud)
            1. Identitas (Sekolah: ${userSchool.value.orEmpty().ifBlank { "Sekolah Dasar" }}, Mapel: $mapel, Kelas: $kelas, Semester: $semester, Alokasi Waktu: $jp JP)
            2. Kompetensi Inti (KI 1-4) / Profil Pelajar Pancasila
            3. Kompetensi Dasar & Indikator
            4. Tujuan Pembelajaran
            5. Materi Pembelajaran
            6. Metode Pembelajaran
            7. Media & Sumber Belajar
            8. Langkah-langkah Pembelajaran (Pendahuluan, Inti, Penutup)
            9. Penilaian (Sikap, Pengetahuan, Keterampilan)
            10. Lampiran
            """.trimIndent()
        } else {
            """
            Kamu guru SD. Buat Modul Ajar lengkap untuk:
            - Mapel: $mapel
            - Kelas: $kelas
            - Topik: $topik
            - Alokasi: $jp JP
            - Semester: $semester

            (Buku belum di-upload, pakai pengetahuan umum)

            PENTING: Jangan gunakan format LaTeX seperti \frac{}{} atau ${'$'}...${'$'}.
            Untuk pecahan, tulis sebagai 3/8 (bukan \frac{3}{8}).

            Format: (10 komponen Kemendikbud)
            1. Identitas (Sekolah: ${userSchool.value.orEmpty().ifBlank { "Sekolah Dasar" }}, Mapel: $mapel, Kelas: $kelas, Semester: $semester, Alokasi Waktu: $jp JP)
            2. Kompetensi Inti (KI 1-4) / Profil Pelajar Pancasila
            3. Kompetensi Dasar & Indikator
            4. Tujuan Pembelajaran
            5. Materi Pembelajaran
            6. Metode Pembelajaran
            7. Media & Sumber Belajar
            8. Langkah-langkah Pembelajaran (Pendahuluan, Inti, Penutup)
            9. Penilaian (Sikap, Pengetahuan, Keterampilan)
            10. Lampiran
            """.trimIndent()
        }

        val result = callGeminiRaw(prompt)
        return if (result.isSuccess) {
            val text = com.example.util.LatexRenderer.render(result.getOrNull() ?: "")
            app.rppHistoryRepository.insert(
                com.example.data.local.RppHistoryEntity(
                    mapel = mapel,
                    kelas = kelas,
                    topik = topik,
                    jp = jp,
                    semester = semester,
                    hasil = text
                )
            )
            Result.success(text)
        } else {
            result
        }
    }

    suspend fun generateRpp(
        mapel: String,
        kelas: Int,
        topik: String,
        jp: Int,
        semester: String,
        pakaiBuku: Boolean = true
    ): Result<String> = generateModulAjar(mapel, kelas, topik, jp, semester, pakaiBuku)

    fun deleteRppHistory(item: com.example.data.local.RppHistoryEntity) {
        viewModelScope.launch {
            app.rppHistoryRepository.delete(item)
        }
    }

    // === FITUR: GENERATOR PENGUMUMAN WHATSAPP ===
    suspend fun generatePengumuman(
        jenis: String,
        infoTambahan: String,
        kelas: Int,
        formal: Boolean,
        bahasa: String = "Indonesia"
    ): Result<String> {
        val school = userSchool.value.orEmpty().ifBlank { "Sekolah Dasar" }
        val guru = userName.value.orEmpty().ifBlank { "Guru Kelas" }
        val templateBase = com.example.util.PengumumanTemplates.getTemplate(jenis)

        val prompt = """
        Kamu guru SD yang menulis pengumuman untuk grup WhatsApp wali murid.

        DATA:
        - Sekolah: $school
        - Guru: $guru
        - Kelas: $kelas
        - Jenis: $jenis
        - Info tambahan: $infoTambahan
        - Bahasa yang digunakan: $bahasa
        - Gaya: ${if (formal) "Formal (gunakan salam lengkap & tutur kata santun resmi)" else "Santai tapi sopan (langsung ke info utama, akrab & hangat)"}

        REFERENSI TEMPLATE:
        $templateBase

        ATURAN:
        1. Tulis pengumuman SINGKAT (maks 200 kata)
        2. Format WhatsApp:
           - Pakai *bold* untuk judul dan label
           - Pakai _italic_ untuk info tambahan/catatan
           - Pakai emoji untuk visual (📢, 📅, 📝, 🎒, 🙏, dll)
           - Pakai bullet (•) untuk list
        3. Struktur:
           📢 *JUDUL PENGUMUMAN*

           Salam pembuka

           📅 Info (hari, tanggal, waktu/mapel jika relevan)

           📝 Detail pengumuman

           📚 Yang perlu disiapkan (kalau ada)

           Terima kasih atas perhatian dan kerja samanya

           Salam penutup
           *$guru*
           _Guru Kelas $kelas - ${school}_

        4. JANGAN pakai LaTeX atau markdown heading (*bold* sudah format WA)
        5. Pakai Bahasa $bahasa yang ramah & sopan.

        Buat pengumuman sekarang.
        """.trimIndent()

        val result = callGeminiRaw(prompt)
        return if (result.isSuccess) {
            val text = com.example.util.LatexRenderer.render(result.getOrNull() ?: "")
            app.pengumumanRepository.insert(
                com.example.data.local.PengumumanHistoryEntity(
                    timestamp = System.currentTimeMillis(),
                    jenis = jenis,
                    kelas = kelas,
                    konten = text,
                    info_tambahan = infoTambahan
                )
            )
            Result.success(text)
        } else {
            result
        }
    }

    fun savePengumumanManual(
        jenis: String,
        kelas: Int,
        konten: String,
        infoTambahan: String
    ) {
        viewModelScope.launch {
            app.pengumumanRepository.insert(
                com.example.data.local.PengumumanHistoryEntity(
                    timestamp = System.currentTimeMillis(),
                    jenis = jenis,
                    kelas = kelas,
                    konten = konten,
                    info_tambahan = infoTambahan
                )
            )
        }
    }

    fun deletePengumumanHistory(id: Long) {
        viewModelScope.launch {
            app.pengumumanRepository.deleteById(id)
        }
    }

    // === FITUR: GENERATOR BAHAN AJAR MULTI-SOURCE (RAG: PROGRES + BUKU + INTERNET) ===
    suspend fun generateBahanAjar(
        kelas: Int,
        mapel: String,
        topik: String,
        useBuku: Boolean = true
    ): String {
        // 1. Ambil dari progres
        val materiProgres = app.database.materiDao()
            .searchByKelasMapelTopik(kelas, mapel, topik)

        // 2. Ambil dari buku guru & siswa
        val bukuGuru = if (useBuku) app.database.bukuDao().getBuku(kelas, mapel, "guru") else null
        val bukuSiswa = if (useBuku) app.database.bukuDao().getBuku(kelas, mapel, "siswa") else null

        val chunkGuru = if (bukuGuru != null) {
            app.database.bukuChunkDao().searchByKeyword(bukuGuru.id, topik)
        } else emptyList()

        val chunkSiswa = if (bukuSiswa != null) {
            app.database.bukuChunkDao().searchByKeyword(bukuSiswa.id, topik)
        } else emptyList()

        // 3. Fallback ke internet (Wikipedia + DuckDuckGo)
        val internet = if (chunkGuru.isEmpty() && chunkSiswa.isEmpty()) {
            com.example.util.WikipediaHelper.cariWikipedia(app, topik) ?: com.example.util.SearchHelper.searchDuckDuckGo(topik)
        } else null

        // 4. Generate dengan Gemini via prompt builder terstruktur
        val prompt = buildBahanAjarPrompt(
            progres = materiProgres,
            bukuGuru = chunkGuru,
            bukuSiswa = chunkSiswa,
            internet = internet,
            kelas = kelas,
            mapel = mapel,
            topik = topik
        )

        val response = ApiCaller.callAiWithFallback(context = app, prompt = prompt)
        return com.example.util.LatexRenderer.render(response)
    }

    suspend fun generateBahanAjar(
        mapel: String,
        kelas: Int,
        topik: String,
        useBuku: Boolean = true
    ): Result<String> {
        return try {
            val result = generateBahanAjar(kelas, mapel, topik, useBuku)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildBahanAjarPrompt(
        progres: List<com.example.data.local.MateriEntity>,
        bukuGuru: List<com.example.data.local.BukuChunkEntity>,
        bukuSiswa: List<com.example.data.local.BukuChunkEntity>,
        internet: String?,
        kelas: Int,
        mapel: String,
        topik: String
    ): String {
        val halGuru = bukuGuru.map { it.halaman }.distinct().joinToString(", ")
        val halSiswa = bukuSiswa.map { it.halaman }.distinct().joinToString(", ")

        val progresInfo = if (progres.isNotEmpty()) {
            progres.joinToString("\n") { "• Pertemuan ${it.pertemuan}: ${it.bab} - ${it.judul} (${it.status ?: "belum selesai"})" }
        } else "Belum ada progres materi terkait."

        val guruText = if (bukuGuru.isNotEmpty()) {
            bukuGuru.joinToString("\n\n") { "Hal ${it.halaman}: ${it.text}" }
        } else null

        val siswaText = if (bukuSiswa.isNotEmpty()) {
            bukuSiswa.joinToString("\n\n") { "Hal ${it.halaman}: ${it.text}" }
        } else null

        return """
        Kamu asisten guru SD profesional ("Cici").
        Susun Bahan Ajar terstruktur untuk:
        - Mata Pelajaran: $mapel
        - Kelas: $kelas
        - Topik: $topik

        === SUMBER DATA REFERENSI ===
        [Data Progres Belajar]:
        $progresInfo

        ${if (guruText != null) "[Kutipan Buku Guru Kelas $kelas, Hal $halGuru]:\n$guruText\n" else ""}
        ${if (siswaText != null) "[Kutipan Buku Siswa Kelas $kelas, Hal $halSiswa]:\n$siswaText\n" else ""}
        ${if (internet != null) "[Informasi Internet / Ensiklopedia]:\n$internet\n" else ""}

        PENTING:
        - Jangan gunakan notasi LaTeX seperti \frac{}{} atau ${'$'}...${'$'}. Untuk pecahan tulis 3/8.
        - Gunakan bahasa yang mudah dipahami anak SD dan mudah diajarkan guru.
        - Wajib ikuti format output persis seperti berikut:

        📚 BAHAN AJAR - $mapel Kelas $kelas
        📖 Topik: $topik

        ━━━━━━━━━━━━━━━
        📌 PENGERTIAN
        ${if (guruText != null) "(Dari Buku Guru Kelas $kelas, hal $halGuru)\n" else ""}[Penjelasan konsep materi dengan bahasa sederhana]

        📌 CONTOH
        ${if (siswaText != null) "(Dari Buku Siswa Kelas $kelas, hal $halSiswa)\n" else ""}[Contoh nyata dalam kehidupan sehari-hari]

        📌 LATIHAN
        ${if (siswaText != null) "(Dari Buku Siswa Kelas $kelas, hal $halSiswa)\n" else ""}[Latihan soal / aktivitas pemahaman interaktif]

        ${if (internet != null || (guruText == null && siswaText == null)) """📌 TAMBAHAN
        (Dari Wikipedia / Internet)
        [Fakta menarik atau wawasan tambahan seputar materi]
        """ else ""}
        ━━━━━━━━━━━━━━━
        Sumber:
        ${if (guruText != null) "• Buku Guru Kelas $kelas, hal $halGuru\n" else ""}${if (siswaText != null) "• Buku Siswa Kelas $kelas, hal $halSiswa\n" else ""}${if (internet != null) "• Wikipedia\n" else ""}${if (guruText == null && siswaText == null && internet == null) "• Kurikulum Merdeka SD\n" else ""}
        """.trimIndent()
    }

    suspend fun saveBahanAjarToBank(mapel: String, kelas: Int, topik: String, bahanAjarText: String): Result<MateriEntity> {
        return try {
            val allCurrent = app.database.materiDao().getAllMateriList()
            val nextPertemuan = (allCurrent.filter { it.kelas == kelas && it.mapel.equals(mapel, ignoreCase = true) }
                .maxOfOrNull { it.pertemuan } ?: 0) + 1

            val newMateri = MateriEntity(
                kelas = kelas,
                mapel = mapel,
                pertemuan = nextPertemuan,
                bab = "Bahan Ajar: $topik",
                judul = "Bahan Ajar: $topik",
                bahan = "Rangkuman & Lembar Ajar RAG",
                tujuan = bahanAjarText,
                alokasiJp = 2,
                status = null,
                tanggalSelesai = null
            )
            app.materiRepository.insertAll(listOf(newMateri))
            Result.success(newMateri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveRppToBank(mapel: String, kelas: Int, topik: String, rppText: String): Result<MateriEntity> {
        return try {
            val allCurrent = app.database.materiDao().getAllMateriList()
            val nextPertemuan = (allCurrent.filter { it.kelas == kelas && it.mapel.equals(mapel, ignoreCase = true) }
                .maxOfOrNull { it.pertemuan } ?: 0) + 1

            val newMateri = MateriEntity(
                kelas = kelas,
                mapel = mapel,
                pertemuan = nextPertemuan,
                bab = "RPP: $topik",
                judul = "Pelaksanaan Pembelajaran: $topik",
                bahan = "RPP & Modul Pembelajaran",
                tujuan = rppText,
                alokasiJp = 2,
                status = null,
                tanggalSelesai = null
            )
            app.materiRepository.insertAll(listOf(newMateri))
            Result.success(newMateri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // === FITUR 7: MATERI DARI FOTO (OCR) ===
    suspend fun saveMateriFromOcr(
        kelas: Int,
        mapel: String,
        bab: String,
        textOcr: String,
        fotoPath: String = ""
    ): Result<MateriEntity> {
        return try {
            val allCurrent = app.database.materiDao().getAllMateriList()
            val nextPertemuan = (allCurrent.filter { it.kelas == kelas && it.mapel.equals(mapel, ignoreCase = true) }
                .maxOfOrNull { it.pertemuan } ?: 0) + 1

            val dateFormatted = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date())
            val firstLine = textOcr.lines().firstOrNull { it.isNotBlank() } ?: "Catatan Materi"
            val judul = if (firstLine.length > 50) firstLine.take(50) + "..." else firstLine

            val newMateri = MateriEntity(
                kelas = kelas,
                mapel = mapel,
                pertemuan = nextPertemuan,
                bab = bab.ifBlank { "Materi Tambahan Foto" },
                judul = judul,
                bahan = if (fotoPath.isNotBlank()) "Foto: $fotoPath" else "Foto - $dateFormatted",
                tujuan = textOcr,
                alokasiJp = 2,
                status = null,
                tanggalSelesai = null
            )
            app.materiRepository.insertAll(listOf(newMateri))
            Result.success(newMateri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveMateriFromPdf(
        kelas: Int,
        mapel: String,
        bab: String,
        textPdf: String,
        fileName: String = ""
    ): Result<MateriEntity> {
        return try {
            val allCurrent = app.database.materiDao().getAllMateriList()
            val nextPertemuan = (allCurrent.filter { it.kelas == kelas && it.mapel.equals(mapel, ignoreCase = true) }
                .maxOfOrNull { it.pertemuan } ?: 0) + 1

            val dateFormatted = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date())
            val firstLine = textPdf.lines().firstOrNull { it.isNotBlank() && !it.startsWith("===") } ?: "Materi Buku PDF"
            val judul = if (firstLine.length > 50) firstLine.take(50) + "..." else firstLine

            val newMateri = MateriEntity(
                kelas = kelas,
                mapel = mapel,
                pertemuan = nextPertemuan,
                bab = bab.ifBlank { "Buku PDF: ${fileName.ifBlank { "Dokumen Materi" }}" },
                judul = judul,
                bahan = if (fileName.isNotBlank()) "PDF: $fileName" else "Buku PDF - $dateFormatted",
                tujuan = textPdf.take(500),
                alokasiJp = 2,
                status = null,
                tanggalSelesai = null
            )
            app.materiRepository.insertAll(listOf(newMateri))
            Result.success(newMateri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun callGeminiRaw(prompt: String): Result<String> {
        return try {
            val resultText = ApiCaller.callAiWithFallback(context = app, prompt = prompt)
            if (resultText.startsWith("⚠️ Maaf") ||
                resultText.startsWith("⚠️ Server AI sedang sibuk") ||
                resultText.startsWith("Semua DeepSeek model gagal") ||
                resultText.startsWith("⚠️ Saldo DeepSeek habis")) {
                Result.failure(Exception(resultText))
            } else if (resultText.isNotBlank()) {
                Result.success(resultText)
            } else {
                Result.failure(Exception("Respon AI kosong"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceInputManager.stopListening()
        voiceOutputManager.shutdown()
    }
}
