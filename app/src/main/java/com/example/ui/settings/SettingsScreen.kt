package com.example.ui.settings

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import com.example.ui.components.UpgradeDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.GreenPrimary
import com.example.util.BackupHelper
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    showTopBar: Boolean = true,
    onNavigateToDashboard: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val name by viewModel.userName.collectAsState()
    val role by viewModel.userRole.collectAsState()
    val school by viewModel.userSchool.collectAsState()
    val kelasUtama by viewModel.userKelasUtama.collectAsState()
    val kelasString by viewModel.userKelas.collectAsState()
    val savedApiKey by viewModel.customApiKey.collectAsState()
    val briefingEnabled by viewModel.briefingEnabled.collectAsState()
    val briefingTime by viewModel.briefingTime.collectAsState()
    val hydrationEnabled by viewModel.hydrationEnabled.collectAsState()
    val currentThemeMode by viewModel.themeMode.collectAsState()
    val reminderJadwalEnabled by viewModel.reminderJadwalEnabled.collectAsState()
    val reminderPagiEnabled by viewModel.reminderPagiEnabled.collectAsState()
    val reminderMenitSebelum by viewModel.reminderMenitSebelum.collectAsState()
    val reminderPagiTime by viewModel.reminderPagiTime.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editName by remember(name) { mutableStateOf(name ?: "") }
    var editSchool by remember(school) { mutableStateOf(school ?: "") }
    var editRole by remember(role) { mutableStateOf(role ?: "Guru SD") }

    // Assistant AI State
    val assistantName by viewModel.assistantName.collectAsState()
    val assistantGreeting by viewModel.assistantGreeting.collectAsState()
    var editAssistantName by remember(assistantName) { mutableStateOf(assistantName ?: "Cici") }
    var editAssistantGreeting by remember(assistantGreeting) { mutableStateOf(assistantGreeting ?: "Halo! Siap membantu.") }

    // Multi-Token API Keys State
    val apiKeyManager = remember { com.example.data.remote.ApiKeyManager.getInstance(context) }
    var geminiKeys by remember { mutableStateOf(apiKeyManager.getGeminiKeys()) }
    var activeGeminiKey by remember { mutableStateOf(apiKeyManager.getActiveGeminiKey()) }
    var newGeminiKeyInput by remember { mutableStateOf("") }

    var deepSeekKeys by remember { mutableStateOf(apiKeyManager.getDeepSeekKeys()) }
    var activeDeepSeekKey by remember { mutableStateOf(apiKeyManager.getActiveDeepSeekKey()) }
    var newDeepSeekKeyInput by remember { mutableStateOf("") }

    var nexotaoKeys by remember { mutableStateOf(apiKeyManager.getNexotaoKeys()) }
    var activeNexotaoKey by remember { mutableStateOf(apiKeyManager.getActiveNexotaoKey()) }
    var newNexotaoKeyInput by remember { mutableStateOf("") }

    var isAdvancedExpanded by remember { mutableStateOf(false) }
    var isGeminiExpanded by remember { mutableStateOf(false) }
    var isDeepSeekExpanded by remember { mutableStateOf(false) }
    var isNexotaoExpanded by remember { mutableStateOf(false) }

    val showUpgradeDialog by viewModel.showUpgradeDialog.collectAsState()

    var todayGeminiUsage by remember { mutableStateOf(apiKeyManager.getTodayUsage("gemini")) }
    var todayDeepSeekUsage by remember { mutableStateOf(apiKeyManager.getDeepSeekUsage()) }
    var todayNexotaoUsage by remember { mutableStateOf(apiKeyManager.getNexotaoUsage()) }

    var showDeepSeekGuideDialog by remember { mutableStateOf(false) }

    var showTestErrorDialog by remember { mutableStateOf(false) }
    var testErrorTitle by remember { mutableStateOf("❌ Test Gagal") }
    var testErrorDetail by remember { mutableStateOf<String?>(null) }

    fun refreshApiKeys() {
        geminiKeys = apiKeyManager.getGeminiKeys()
        activeGeminiKey = apiKeyManager.getActiveGeminiKey()
        deepSeekKeys = apiKeyManager.getDeepSeekKeys()
        activeDeepSeekKey = apiKeyManager.getActiveDeepSeekKey()
        nexotaoKeys = apiKeyManager.getNexotaoKeys()
        activeNexotaoKey = apiKeyManager.getActiveNexotaoKey()
        todayGeminiUsage = apiKeyManager.getTodayUsage("gemini")
        todayDeepSeekUsage = apiKeyManager.getDeepSeekUsage()
        todayNexotaoUsage = apiKeyManager.getNexotaoUsage()
    }

    // API Key state
    var apiKeyInput by remember(savedApiKey) { mutableStateOf(savedApiKey ?: "") }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    // Restore text dialog state
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonText by remember { mutableStateOf("") }

    // Warning / Error Dialog state
    var showWarningDialog by remember { mutableStateOf(false) }
    var restoreErrorMessage by remember { mutableStateOf("") }

    // Reset Data states (FIX 1, 3, 4)
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var isResetLoading by remember { mutableStateOf(false) }
    var resetLoadingMessage by remember { mutableStateOf("⏳ Mereset data...") }
    var showResetSuccessDialog by remember { mutableStateOf(false) }
    var showResetErrorDialog by remember { mutableStateOf(false) }
    var resetErrorMessage by remember { mutableStateOf("") }

    fun performReset() {
        coroutineScope.launch {
            try {
                isResetLoading = true
                resetLoadingMessage = "⏳ Mereset data..."
                val result = viewModel.resetData(context)
                isResetLoading = false
                if (result.isSuccess) {
                    showResetSuccessDialog = true
                } else {
                    resetErrorMessage = result.exceptionOrNull()?.message ?: "Gagal mereset data."
                    showResetErrorDialog = true
                }
            } catch (e: Exception) {
                isResetLoading = false
                resetErrorMessage = "Gagal reset: ${e.message}"
                showResetErrorDialog = true
            }
        }
    }

    // File Picker for JSON Backup
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    if (jsonString.isNullOrBlank()) {
                        restoreErrorMessage = "Gagal membaca file: File kosong atau tidak dapat diakses."
                        showWarningDialog = true
                        return@launch
                    }
                    val result = viewModel.restoreBackup(context, jsonString)
                    when (result) {
                        is BackupHelper.RestoreResult.Success -> {
                            Toast.makeText(context.applicationContext, result.message, Toast.LENGTH_LONG).show()
                            (context as? Activity)?.recreate()
                        }
                        is BackupHelper.RestoreResult.Error -> {
                            restoreErrorMessage = result.message
                            showWarningDialog = true
                        }
                    }
                } catch (e: Exception) {
                    restoreErrorMessage = "Gagal membaca file: ${e.localizedMessage ?: "Unknown error"}"
                    showWarningDialog = true
                }
            }
        }
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profil Guru") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nama Guru") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editSchool,
                        onValueChange = { editSchool = it },
                        label = { Text("Nama Sekolah") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editRole,
                        onValueChange = { editRole = it },
                        label = { Text("Peran / Jabatan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentKelasList = (kelasString ?: "1,2,3,4,5,6").split(",").mapNotNull { it.trim().toIntOrNull() }
                        viewModel.saveUserProfile(
                            name = editName,
                            role = editRole,
                            school = editSchool,
                            kelasUtama = kelasUtama ?: "4",
                            kelasList = currentKelasList
                        )
                        showEditProfileDialog = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Profil berhasil disimpan")
                        }
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Warning / Error Dialog for Restore
    if (showWarningDialog) {
        AlertDialog(
            onDismissRequest = { showWarningDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Peringatan File Backup") },
            text = {
                Text(
                    text = restoreErrorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(onClick = { showWarningDialog = false }) {
                    Text("Mengerti")
                }
            }
        )
    }

    if (showUpgradeDialog) {
        UpgradeDialog(
            onDismiss = { viewModel.dismissUpgradeDialog() },
            onSuccessActivated = { viewModel.dismissUpgradeDialog() }
        )
    }

    // 1. Reset Data Confirmation Dialog (FIX 1 & 4)
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isResetLoading) showResetConfirmDialog = false
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("⚠️ Yakin reset data?") },
            text = {
                Text(
                    text = "Semua materi, jadwal, dan kalender akan dikembalikan ke default.\n\nRiwayat chat juga akan dibersihkan.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        performReset()
                    },
                    enabled = !isResetLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("btn_confirm_reset")
                ) {
                    Text("Ya, Reset", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetConfirmDialog = false },
                    enabled = !isResetLoading,
                    modifier = Modifier.testTag("btn_cancel_reset")
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // 2. Loading State Dialog (FIX 3 & 4)
    if (isResetLoading) {
        LoadingDialog(message = resetLoadingMessage)
    }

    // 3. Reset Success Dialog (FIX 1 & 4)
    if (showResetSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Sukses",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("✅ Reset Selesai") },
            text = {
                Text(
                    text = "Data berhasil dikembalikan ke default.\n\nAplikasi akan kembali ke Beranda.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetSuccessDialog = false
                        // Navigasi ke Beranda & Refresh UI
                        onNavigateToDashboard()
                    },
                    modifier = Modifier.testTag("btn_reset_success_ok")
                ) {
                    Text("OK")
                }
            }
        )
    }

    // 4. Reset Error Dialog (FIX 4)
    if (showResetErrorDialog) {
        AlertDialog(
            onDismissRequest = { showResetErrorDialog = false },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            icon = {
                Icon(
                    Icons.Default.Error,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("❌ Gagal Reset Data") },
            text = {
                Text(
                    text = resetErrorMessage.ifBlank { "Terjadi kesalahan saat mereset data." },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetErrorDialog = false
                        performReset()
                    },
                    modifier = Modifier.testTag("btn_reset_retry")
                ) {
                    Text("Coba Lagi")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetErrorDialog = false },
                    modifier = Modifier.testTag("btn_reset_error_dismiss")
                ) {
                    Text("Tutup")
                }
            }
        )
    }

    // Manual Paste JSON Restore Dialog
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore dari Teks JSON") },
            text = {
                Column {
                    Text(
                        text = "Tempel teks JSON lengkap yang memuat objek 'data' (materi, jadwal, config, kalender, visual_catalog).",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = restoreJsonText,
                        onValueChange = { restoreJsonText = it },
                        placeholder = { Text("{\"version\": \"1.0\", \"data\": {...}}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        maxLines = 8
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreJsonText.isNotBlank()) {
                            coroutineScope.launch {
                                val result = viewModel.restoreBackup(context, restoreJsonText)
                                showRestoreDialog = false
                                restoreJsonText = ""
                                when (result) {
                                    is BackupHelper.RestoreResult.Success -> {
                                        Toast.makeText(context.applicationContext, result.message, Toast.LENGTH_LONG).show()
                                        (context as? Activity)?.recreate()
                                    }
                                    is BackupHelper.RestoreResult.Error -> {
                                        restoreErrorMessage = result.message
                                        showWarningDialog = true
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text("Restore Sekarang")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Panduan Cara Dapatkan DeepSeek API Key
    if (showDeepSeekGuideDialog) {
        AlertDialog(
            onDismissRequest = { showDeepSeekGuideDialog = false },
            icon = { Text("🐋", fontSize = 28.sp) },
            title = { Text("Cara Dapatkan DeepSeek Key") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "1. Buka https://platform.deepseek.com\n" +
                               "2. Daftar dengan Google/email\n" +
                               "3. Top up minimal \$2 (~Rp 30.000)\n" +
                               "   - Bayar via kartu kredit / crypto\n" +
                               "4. Buka https://platform.deepseek.com/api_keys\n" +
                               "5. Klik 'Create new API key'\n" +
                               "6. Copy key (format: sk-...)\n" +
                               "7. Paste di aplikasi ini",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Keunggulan DeepSeek:\n" +
                                       "✅ Sangat murah (\$0.14/1M token)\n" +
                                       "✅ Kualitas setara GPT-4\n" +
                                       "✅ Support Bahasa Indonesia\n\n" +
                                       "Estimasi: \$2 = ~14 juta token = ribuan chat!",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://platform.deepseek.com/api_keys"))
                        context.startActivity(intent)
                    }
                ) {
                    Text("Buka Platform DeepSeek")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeepSeekGuideDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Dialog Detail Error Pengujian API Key
    if (showTestErrorDialog && testErrorDetail != null) {
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = {
                showTestErrorDialog = false
                testErrorDetail = null
            },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = testErrorTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Detail:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = testErrorDetail ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(testErrorDetail ?: ""))
                        Toast.makeText(context, "Log berhasil disalin", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Copy Log")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showTestErrorDialog = false
                        testErrorDetail = null
                    }
                ) {
                    Text("Tutup")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Pengaturan Aplikasi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("settings_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. PROFIL PENDIDIK
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = GreenPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Profil Guru",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(onClick = {
                                editName = name ?: ""
                                editSchool = school ?: ""
                                editRole = role ?: "Guru SD"
                                showEditProfileDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Profil", tint = GreenPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = name.orEmpty().ifBlank { "Guru" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${role ?: "Guru SD"}${if (!school.isNullOrBlank()) " • $school" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Kelas Utama Dropdown
                        Text(
                            text = "Kelas Utama (Prioritas Dashboard):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        var expandedKelasDropdown by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expandedKelasDropdown,
                            onExpandedChange = { expandedKelasDropdown = !expandedKelasDropdown }
                        ) {
                            OutlinedTextField(
                                value = "Kelas ${kelasUtama ?: "4"}",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedKelasDropdown) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedKelasDropdown,
                                onDismissRequest = { expandedKelasDropdown = false }
                            ) {
                                (1..6).forEach { k ->
                                    DropdownMenuItem(
                                        text = { Text("Kelas $k") },
                                        onClick = {
                                            val currentKelasList = (kelasString ?: "1,2,3,4,5,6").split(",").mapNotNull { it.trim().toIntOrNull() }
                                            viewModel.saveUserProfile(
                                                name = name ?: "",
                                                role = role ?: "Guru SD",
                                                school = school ?: "",
                                                kelasUtama = k.toString(),
                                                kelasList = currentKelasList
                                            )
                                            expandedKelasDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Kelas Yang Diampu (Multi-Select)
                        Text(
                            text = "Kelas yang Diampu (Multi-select 1-6):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val activeKelasList = remember(kelasString) {
                            (kelasString ?: "1,2,3,4,5,6").split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf(1, 2, 3, 4, 5, 6)) { k ->
                                val isSelected = activeKelasList.contains(k)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val newSet = if (isSelected) {
                                            if (activeKelasList.size > 1) activeKelasList - k else activeKelasList
                                        } else {
                                            activeKelasList + k
                                        }
                                        viewModel.saveUserProfile(
                                            name = name ?: "",
                                            role = role ?: "Guru SD",
                                            school = school ?: "",
                                            kelasUtama = kelasUtama ?: "4",
                                            kelasList = newSet.toList()
                                        )
                                    },
                                    label = { Text("Kls $k") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GreenPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 2. KUSTOMISASI ASISTEN AI (TASK 5)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GreenPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Kustomisasi Asisten AI",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Ubah nama panggilan asisten dan pesan sapaan awal saat membuka Tanya AI:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = editAssistantName,
                            onValueChange = { editAssistantName = it },
                            label = { Text("Nama Asisten AI") },
                            placeholder = { Text("Contoh: Cici") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_nama_asisten_ai"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = editAssistantGreeting,
                            onValueChange = { editAssistantGreeting = it },
                            label = { Text("Sapaan Pembuka AI") },
                            placeholder = { Text("Contoh: Halo! Cici siap membantu.") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_greeting_asisten_ai")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                viewModel.saveAssistantConfig(editAssistantName, editAssistantGreeting)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Pengaturan Asisten AI berhasil disimpan!")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                            modifier = Modifier
                                .align(Alignment.End)
                                .testTag("btn_simpan_asisten_ai")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan Pengaturan AI")
                        }
                    }
                }
            }

            // 3. LAYANAN AI BAWAAN & PENGATURAN LANJUTAN (TASK 3)
            item {
                val chatLimitManager = remember { com.example.util.ChatLimitManager(context) }
                val remainingChats = chatLimitManager.getRemainingChats()
                val chatLimit = chatLimitManager.getLimit()
                val hasUserKey = chatLimitManager.hasUserApiKey()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_layanan_ai"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GreenPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Layanan Asisten AI",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Status AI Bawaan (Bundled)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GreenPrimary.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(GreenPrimary, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "🟢 AI Bawaan Aktif (DeepSeek V3.2)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = GreenPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Siap digunakan tanpa konfigurasi untuk tanya jawab jadwal, pembuatan RPP/Modul, dan bank soal.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (hasUserKey) {
                                        "⚡ Kuota Hari Ini: Unlimited (Menggunakan API Key Pribadi)"
                                    } else {
                                        "📊 Kuota Hari Ini: Sisa chat $remainingChats / $chatLimit"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Tombol Expandable: ⚙️ Pengaturan Lanjutan (Advanced)
                        Surface(
                            onClick = { isAdvancedExpanded = !isAdvancedExpanded },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_toggle_advanced_settings")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "⚙️ Pengaturan Lanjutan (Advanced)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = if (isAdvancedExpanded) "Sembunyikan menu kunci API pribadi" else "Gunakan API key sendiri (Opsional)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (isAdvancedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isAdvancedExpanded) "Tutup" else "Buka",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Isi Menu Advanced (Tersembunyi secara default)
                        if (isAdvancedExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "🔑 Gunakan API Key Sendiri (Opsional)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Jika Anda menambahkan kunci API sendiri, kuota chat menjadi Unlimited dan sistem akan memprioritaskan kunci Anda sebelum menggunakan kuota bawaan.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Switch / Info Urutan Prioritas
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "⚡ Prioritas Pemanggilan API:",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "1. Nexotao Bawaan (DeepSeek V3.2)\n2. Gemini User (jika ada kunci)\n3. DeepSeek User (jika ada kunci)\n4. Nexotao User (jika ada kunci)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // 1. NEXOTAO USER KEYS
                                    Text(
                                        text = "Nexotao API Keys",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (nexotaoKeys.isNotEmpty()) GreenPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(if (nexotaoKeys.isNotEmpty()) GreenPrimary else Color.Gray, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (nexotaoKeys.isNotEmpty()) {
                                                    "Status: 🟢 ${nexotaoKeys.size} Kunci Terdaftar • Aktif: ${apiKeyManager.maskKey(activeNexotaoKey ?: nexotaoKeys.first())}"
                                                } else {
                                                    "Status: Belum ada kunci Nexotao kustom (Menggunakan Bawaan)"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (nexotaoKeys.isNotEmpty()) GreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (nexotaoKeys.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            nexotaoKeys.forEachIndexed { index, k ->
                                                val isActive = k == activeNexotaoKey
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = "Key ${index + 1}: ${apiKeyManager.maskKey(k)}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                                            )
                                                            if (isActive) {
                                                                Text("Aktif", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                                            }
                                                        }
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            if (!isActive) {
                                                                TextButton(onClick = {
                                                                    apiKeyManager.setActiveNexotaoKey(k)
                                                                    refreshApiKeys()
                                                                }) {
                                                                    Text("Pilih", fontSize = 12.sp)
                                                                }
                                                            }
                                                            TextButton(onClick = {
                                                                coroutineScope.launch {
                                                                    val res = apiKeyManager.testNexotaoKey(k)
                                                                    val msg = res.getOrElse { it.message ?: "Gagal tes" }
                                                                    snackbarHostState.showSnackbar(msg)
                                                                    refreshApiKeys()
                                                                }
                                                            }) {
                                                                Text("Test", fontSize = 12.sp)
                                                            }
                                                            IconButton(onClick = {
                                                                apiKeyManager.removeNexotaoKey(k)
                                                                refreshApiKeys()
                                                                coroutineScope.launch {
                                                                    snackbarHostState.showSnackbar("Kunci Nexotao dihapus")
                                                                }
                                                            }) {
                                                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = newNexotaoKeyInput,
                                            onValueChange = { newNexotaoKeyInput = it },
                                            label = { Text("Tambah Kunci Nexotao") },
                                            placeholder = { Text("sk-nexo-...") },
                                            modifier = Modifier.weight(1f).testTag("input_new_nexotao_key"),
                                            singleLine = true
                                        )
                                        Button(
                                            onClick = {
                                                val added = apiKeyManager.addNexotaoKey(newNexotaoKeyInput)
                                                if (added) {
                                                    newNexotaoKeyInput = ""
                                                    refreshApiKeys()
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("Kunci Nexotao berhasil ditambahkan!")
                                                    }
                                                }
                                            },
                                            enabled = newNexotaoKeyInput.isNotBlank(),
                                            modifier = Modifier.testTag("btn_tambah_nexotao_key")
                                        ) {
                                            Text("+ Tambah")
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                    Spacer(modifier = Modifier.height(14.dp))

                                    // 2. GOOGLE GEMINI API KEYS
                                    Text(
                                        text = "Google Gemini API Keys",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (geminiKeys.isNotEmpty()) GreenPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(if (geminiKeys.isNotEmpty()) GreenPrimary else MaterialTheme.colorScheme.error, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (geminiKeys.isNotEmpty()) {
                                                    "Status: ${geminiKeys.size} Kunci Terdaftar • Aktif: ${apiKeyManager.maskKey(activeGeminiKey ?: geminiKeys.first())}"
                                                } else {
                                                    "Status: Belum ada kunci Gemini tersimpan"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (geminiKeys.isNotEmpty()) GreenPrimary else MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }

                                    if (geminiKeys.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            geminiKeys.forEachIndexed { index, k ->
                                                val isActive = k == activeGeminiKey
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = "Key ${index + 1}: ${apiKeyManager.maskKey(k)}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                                            )
                                                            if (isActive) {
                                                                Text("Aktif", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                                            }
                                                        }
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            if (!isActive) {
                                                                TextButton(onClick = {
                                                                    apiKeyManager.setActiveGeminiKey(k)
                                                                    refreshApiKeys()
                                                                }) {
                                                                    Text("Pilih", fontSize = 12.sp)
                                                                }
                                                            }
                                                            TextButton(onClick = {
                                                                coroutineScope.launch {
                                                                    val testResult = apiKeyManager.testGeminiKey(k)
                                                                    val msg = testResult.getOrElse { it.message ?: "Gagal tes" }
                                                                    snackbarHostState.showSnackbar(msg)
                                                                    refreshApiKeys()
                                                                }
                                                            }) {
                                                                Text("Test", fontSize = 12.sp)
                                                            }
                                                            IconButton(onClick = {
                                                                apiKeyManager.removeGeminiKey(k)
                                                                refreshApiKeys()
                                                                coroutineScope.launch {
                                                                    snackbarHostState.showSnackbar("Kunci Gemini dihapus")
                                                                }
                                                            }) {
                                                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = newGeminiKeyInput,
                                            onValueChange = { newGeminiKeyInput = it },
                                            label = { Text("Tambah Kunci Gemini") },
                                            placeholder = { Text("AIzaSy...") },
                                            modifier = Modifier.weight(1f).testTag("input_new_gemini_key"),
                                            singleLine = true
                                        )
                                        Button(
                                            onClick = {
                                                val added = apiKeyManager.addGeminiKey(newGeminiKeyInput)
                                                if (added) {
                                                    viewModel.saveApiKey(newGeminiKeyInput)
                                                    newGeminiKeyInput = ""
                                                    refreshApiKeys()
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("Kunci Gemini berhasil ditambahkan!")
                                                    }
                                                }
                                            },
                                            enabled = newGeminiKeyInput.isNotBlank(),
                                            modifier = Modifier.testTag("btn_tambah_gemini_key")
                                        ) {
                                            Text("+ Tambah")
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                    Spacer(modifier = Modifier.height(14.dp))

                                    // 3. DEEPSEEK API KEYS
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "DeepSeek API Keys",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        TextButton(onClick = { showDeepSeekGuideDialog = true }) {
                                            Text("Cara Dapatkan", fontSize = 12.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (deepSeekKeys.isNotEmpty()) GreenPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(if (deepSeekKeys.isNotEmpty()) GreenPrimary else Color.Gray, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (deepSeekKeys.isNotEmpty()) {
                                                    "Status: 🟢 ${deepSeekKeys.size} Kunci Terdaftar • Aktif: ${apiKeyManager.maskKey(activeDeepSeekKey ?: deepSeekKeys.first())}"
                                                } else {
                                                    "Status: Belum ada kunci DeepSeek kustom tersimpan"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (deepSeekKeys.isNotEmpty()) GreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (deepSeekKeys.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            deepSeekKeys.forEachIndexed { index, k ->
                                                val isActive = k == activeDeepSeekKey
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = "Key ${index + 1}: ${apiKeyManager.maskKey(k)}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                                            )
                                                            if (isActive) {
                                                                Text("Aktif", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                                            }
                                                        }
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            if (!isActive) {
                                                                TextButton(onClick = {
                                                                    apiKeyManager.setActiveDeepSeekKey(k)
                                                                    refreshApiKeys()
                                                                }) {
                                                                    Text("Pilih", fontSize = 12.sp)
                                                                }
                                                            }
                                                            TextButton(onClick = {
                                                                coroutineScope.launch {
                                                                    val testResult = apiKeyManager.testDeepSeekKey(k)
                                                                    if (testResult.isSuccess) {
                                                                        val msg = testResult.getOrNull() ?: "✅ Key valid!"
                                                                        snackbarHostState.showSnackbar(msg)
                                                                    } else {
                                                                        val err = testResult.exceptionOrNull()?.message ?: "Gagal tes koneksi"
                                                                        if (err.length > 50 || err.contains("\n")) {
                                                                            testErrorTitle = "❌ Test Gagal (DeepSeek)"
                                                                            testErrorDetail = err
                                                                            showTestErrorDialog = true
                                                                        } else {
                                                                            snackbarHostState.showSnackbar("❌ Test Gagal: $err")
                                                                        }
                                                                    }
                                                                    refreshApiKeys()
                                                                }
                                                            }) {
                                                                Text("Test", fontSize = 12.sp)
                                                            }
                                                            IconButton(onClick = {
                                                                apiKeyManager.removeDeepSeekKey(k)
                                                                refreshApiKeys()
                                                                coroutineScope.launch {
                                                                    snackbarHostState.showSnackbar("Kunci DeepSeek dihapus")
                                                                }
                                                            }) {
                                                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = newDeepSeekKeyInput,
                                            onValueChange = { newDeepSeekKeyInput = it },
                                            label = { Text("Tambah Kunci DeepSeek") },
                                            placeholder = { Text("sk-...") },
                                            modifier = Modifier.weight(1f).testTag("input_new_deepseek_key"),
                                            singleLine = true
                                        )
                                        Button(
                                            onClick = {
                                                val added = apiKeyManager.addDeepSeekKey(newDeepSeekKeyInput)
                                                if (added) {
                                                    newDeepSeekKeyInput = ""
                                                    refreshApiKeys()
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("Kunci DeepSeek berhasil ditambahkan!")
                                                    }
                                                }
                                            },
                                            enabled = newDeepSeekKeyInput.isNotBlank(),
                                            modifier = Modifier.testTag("btn_tambah_deepseek_key")
                                        ) {
                                            Text("+ Tambah")
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Cara Dapatkan DeepSeek Key button
                                    OutlinedButton(
                                        onClick = { showDeepSeekGuideDialog = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("btn_cara_dapat_deepseek_key")
                                    ) {
                                        Text("🐋 Cara Dapatkan DeepSeek Key")
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Usage Summary Card
                                    val formatter = remember { java.text.NumberFormat.getIntegerInstance(Locale("id", "ID")) }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "📊 Penggunaan Token Hari Ini",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "• Google Gemini: ${formatter.format(todayGeminiUsage)} token",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Text(
                                                text = "• DeepSeek: ${formatter.format(todayDeepSeekUsage)} token",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Text(
                                                text = "• Nexotao: ${formatter.format(todayNexotaoUsage)} token",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. TAMPILAN
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_tampilan_theme"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = GreenPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tampilan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Pilih tema aplikasi yang nyaman untuk mata saat mengajar:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val themeOptions = listOf(
                            Triple("light", "Terang (Light)", "Tema warna terang dengan kontras tinggi"),
                            Triple("dark", "Gelap (Dark)", "Tema warna gelap hemat baterai"),
                            Triple("auto", "Otomatis (Auto - ikut sistem)", "Mengikuti pengaturan sistem Android")
                        )

                        themeOptions.forEach { (mode, label, desc) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setThemeMode(mode) }
                                    .padding(vertical = 6.dp)
                                    .testTag("theme_option_$mode"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentThemeMode == mode,
                                    onClick = { viewModel.setThemeMode(mode) },
                                    colors = RadioButtonDefaults.colors(selectedColor = GreenPrimary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (currentThemeMode == mode) FontWeight.Bold else FontWeight.Normal,
                                        color = if (currentThemeMode == mode) GreenPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. NOTIFIKASI & PENGINGAT
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Notifikasi & Pengingat",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Toggle Pengingat Jadwal
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pengingat Jadwal",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Alarm peringatan sebelum jam mengajar kelas dimulai ($reminderMenitSebelum menit sebelumnya)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = reminderJadwalEnabled,
                                onCheckedChange = { checked ->
                                    viewModel.setReminderJadwalEnabled(checked)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = GreenPrimary),
                                modifier = Modifier.testTag("switch_pengingat_jadwal")
                            )
                        }

                        if (reminderJadwalEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Ingatkan berapa menit sebelum ngajar?",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(15, 30, 45, 60).forEach { mins ->
                                    FilterChip(
                                        selected = reminderMenitSebelum == mins,
                                        onClick = { viewModel.setReminderMenitSebelum(mins) },
                                        label = { Text("$mins menit") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = GreenPrimary,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Toggle Pengingat Pagi
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pengingat Pagi",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Notifikasi daftar jadwal mengajar hari ini setiap jam $reminderPagiTime",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = reminderPagiEnabled,
                                onCheckedChange = { checked ->
                                    viewModel.setReminderPagiEnabled(checked)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = GreenPrimary),
                                modifier = Modifier.testTag("switch_pengingat_pagi")
                            )
                        }

                        if (reminderPagiEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val parts = reminderPagiTime.split(":")
                                        val h = parts.getOrNull(0)?.toIntOrNull() ?: 6
                                        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                        TimePickerDialog(
                                            context,
                                            { _, selectedH, selectedM ->
                                                viewModel.setReminderPagiTime(selectedH, selectedM)
                                            },
                                            h,
                                            m,
                                            true
                                        ).show()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Jam Pagi: $reminderPagiTime")
                                }

                                Button(
                                    onClick = {
                                        viewModel.triggerTestReminder()
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Notifikasi pengingat pagi & jadwal terkirim!")
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Uji Reminder", fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Briefing Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Briefing Otomatis Malam",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Notifikasi persiapan esok hari setiap jam $briefingTime",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = briefingEnabled == "true",
                                onCheckedChange = { checked ->
                                    viewModel.setBriefingEnabled(checked)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = GreenPrimary)
                            )
                        }

                        // Time Picker Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val parts = (briefingTime ?: "20:00").split(":")
                                    val h = parts.getOrNull(0)?.toIntOrNull() ?: 20
                                    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0

                                    TimePickerDialog(
                                        context,
                                        { _, selectedH, selectedM ->
                                            viewModel.setBriefingTime(selectedH, selectedM)
                                        },
                                        h,
                                        m,
                                        true
                                    ).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Set Jam: $briefingTime")
                            }

                            Button(
                                onClick = {
                                    viewModel.triggerTestNotification()
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Notifikasi briefing terkirim!")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Uji Notifikasi", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Reminder Minum Air
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Pengingat Minum & Istirahat",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = "Mengingatkan minum air putih dan relaksasi saat mengajar",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = hydrationEnabled == "true",
                                onCheckedChange = { checked ->
                                    viewModel.setHydrationEnabled(checked)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = GreenPrimary)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedButton(
                            onClick = {
                                viewModel.triggerTestHydration()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Notifikasi minum air terkirim!")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Uji Pengingat Minum")
                        }
                    }
                }
            }

            // 4. CADANGKAN, PULIHKAN & RESET DATA
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cadangkan & Pulihkan Data",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Ekspor data kurikulum, jadwal, dan preferensi mengajar ke format JSON, atau pulihkan dari file backup eksternal (Kelas 1 s/d 6).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Row 1: Backup & Restore File JSON
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val backupJson = viewModel.getBackupJson(context)
                                        BackupHelper.shareBackup(context, backupJson)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("backup_button")
                            ) {
                                Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Backup JSON", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    filePickerLauncher.launch("*/*")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("restore_file_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GreenPrimary
                                )
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pilih File JSON", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Row 2: Tempel Teks Manual
                        OutlinedButton(
                            onClick = { showRestoreDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("paste_json_button")
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tempel Teks JSON Secara Manual", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Row 3: Reset Data ke Kondisi Default (Requirement 4)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Reset Data Aplikasi",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Kembalikan seluruh materi dan jadwal ke kondisi awal default Kurikulum Merdeka.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { showResetConfirmDialog = true },
                            enabled = !isResetLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reset_data_button"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset Data ke Default", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 5. TENTANG APLIKASI & FOOTER
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_tentang_aplikasi"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = GreenPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Asisten Mengajar v1.0",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Aplikasi Native Android untuk Guru SD Indonesia • Kurikulum Merdeka",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Dirancang khusus untuk mendukung pendidik Indonesia dalam administrasi kelas, materi ajar, dan asistensi kecerdasan buatan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Dibuat dengan ❤️ oleh DRUZ.ID",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("footer_druz_id"),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun LoadingDialog(message: String) {
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .testTag("loading_dialog"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
