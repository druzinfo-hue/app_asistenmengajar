package com.example.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.local.ChatEntity
import com.example.ui.MainViewModel
import com.example.ui.components.UpgradeDialog
import com.example.ui.theme.GreenContainer
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OnGreenContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    showTopBar: Boolean = true
) {
    val context = LocalContext.current
    val chatHistory by viewModel.chatHistory.collectAsState()
    val isLoading by viewModel.isChatLoading.collectAsState()
    val isListening by viewModel.voiceInputManager.isListening.collectAsState()
    val speechResult by viewModel.voiceInputManager.speechResult.collectAsState()
    val voiceError by viewModel.voiceInputManager.errorMessage.collectAsState()
    val isSpeaking by viewModel.voiceOutputManager.isSpeaking.collectAsState()
    val ttsEnabled by viewModel.ttsEnabled.collectAsState()
    val assistantName by viewModel.assistantName.collectAsState()
    val assistantGreeting by viewModel.assistantGreeting.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val showUpgradeDialog by viewModel.showUpgradeDialog.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    var showCameraOptions by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val listState = rememberLazyListState()

    // Camera launcher for OCR
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            coroutineScope.launch {
                try {
                    val text = com.example.util.OcrHelper.recognizeTextFromBitmap(bitmap)
                    if (text.isNotBlank()) {
                        inputText = text.trim()
                        Toast.makeText(context, "Teks berhasil dipindai dari kamera!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Tidak ada teks yang terdeteksi pada foto", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal memindai foto: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Gallery picker launcher for OCR
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                coroutineScope.launch {
                    try {
                        val text = com.example.util.OcrHelper.recognizeTextFromBitmap(bitmap)
                        if (text.isNotBlank()) {
                            inputText = text.trim()
                            Toast.makeText(context, "Teks berhasil dipindai dari galeri!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Tidak ada teks yang terdeteksi pada gambar", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Gagal memindai gambar: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal membuka gambar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Permission launcher for microphone
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.voiceInputManager.startListening()
        }
    }

    // Update input text when voice speech recognizer finishes
    LaunchedEffect(speechResult) {
        speechResult?.let { recognized ->
            inputText = recognized
            viewModel.voiceInputManager.clearResult()
        }
    }

    // Scroll to bottom on new message
    LaunchedEffect(chatHistory.size, isLoading) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size)
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Hapus Riwayat Chat?") },
            text = { Text("Semua percakapan dengan asisten AI akan dihapus permanen dari memori aplikasi.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearChatHistory()
                        showClearDialog = false
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showCameraOptions) {
        AlertDialog(
            onDismissRequest = { showCameraOptions = false },
            title = { Text("Pindai Teks (OCR)") },
            text = { Text("Pilih sumber gambar untuk mengenali teks materi buku atau lembar kerja:") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCameraOptions = false
                        cameraLauncher.launch()
                    }
                ) {
                    Text("Kamera")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showCameraOptions = false }) {
                        Text("Batal")
                    }
                    TextButton(
                        onClick = {
                            showCameraOptions = false
                            galleryLauncher.launch("image/*")
                        }
                    ) {
                        Text("Galeri")
                    }
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

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = GreenPrimary,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "$assistantName (Tanya AI)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isLoading) "Sedang mengetik..." else "Online • Siap Membantu",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isLoading) GreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        // TTS Audio Speaker toggle
                        IconButton(
                            onClick = {
                                if (isSpeaking) {
                                    viewModel.voiceOutputManager.stop()
                                } else {
                                    viewModel.setTtsEnabled(ttsEnabled != "true")
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (ttsEnabled == "true") {
                                    if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp
                                } else Icons.Default.VolumeOff,
                                contentDescription = "Toggle Suara AI",
                                tint = if (isSpeaking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }

                        // Clear chat
                        IconButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier.testTag("clear_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus Riwayat Chat",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("chat_screen")
        ) {
            val remainingChats = viewModel.chatLimitManager.getRemainingChats()
            val totalLimit = viewModel.chatLimitManager.getLimit()
            val isUnlimited = totalLimit == Int.MAX_VALUE
            val hasCustomKey = viewModel.chatLimitManager.hasUserApiKey()
            val isPremium = viewModel.premiumManager.isPremium()

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.openUpgradeDialog() }
                    .testTag("chat_quota_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🤖 Nexotao (DeepSeek V3.2)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (isPremium) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "• PREMIUM",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = GreenPrimary
                            )
                        } else if (hasCustomKey) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "• API PRIBADI",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Text(
                        text = if (isUnlimited) "📊 Unlimited" else "📊 Sisa: $remainingChats/$totalLimit",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (!isUnlimited && remainingChats <= 2) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Voice error alert if any
            AnimatedVisibility(visible = voiceError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = voiceError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // Listening banner
            AnimatedVisibility(visible = isListening) {
                Surface(
                    color = GreenContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = GreenPrimary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Mendengarkan suara Anda (Bahasa Indonesia)...",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = OnGreenContainer
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = { viewModel.voiceInputManager.stopListening() },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Batal", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Chat Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (chatHistory.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (!userName.isNullOrBlank()) "Halo $userName!" else "Halo Guru!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = assistantGreeting?.ifBlank { null } ?: "Halo! Cici siap membantu. Panggil aku kalau butuh sesuatu ya.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                items(chatHistory, key = { it.id }) { chat ->
                    ChatBubbleItem(
                        chat = chat,
                        onSpeakText = { text ->
                            viewModel.voiceOutputManager.speak(text)
                        }
                    )
                }

                if (isLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = GreenPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Asisten sedang menyusun jawaban...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Prompt Suggestions Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val suggestions = listOf(
                    "/jadwal",
                    "/bahan",
                    "/progress",
                    "/materi",
                    "/bahan_ajar",
                    "/help"
                )
                items(suggestions) { tip ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        onClick = {
                            inputText = tip
                            viewModel.sendMessage(tip)
                            inputText = ""
                        }
                    ) {
                        Text(
                            text = tip,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Input Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Voice Mic Button
                    IconButton(
                        onClick = {
                            if (isListening) {
                                viewModel.voiceInputManager.stopListening()
                            } else {
                                val hasAudioPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasAudioPermission) {
                                    viewModel.voiceInputManager.startListening()
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        modifier = Modifier.testTag("voice_input_button")
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Input Suara",
                            tint = if (isListening) MaterialTheme.colorScheme.error else GreenPrimary
                        )
                    }

                    // Text Field
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        placeholder = { Text("Ketik pertanyaan materi...") },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Camera OCR Button
                    IconButton(
                        onClick = { showCameraOptions = true },
                        modifier = Modifier.testTag("chat_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Pindai Teks Kamera (OCR)",
                            tint = GreenPrimary
                        )
                    }

                    // Send Button
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() && !isLoading) {
                                val textToSend = inputText.trim()
                                inputText = ""
                                if (textToSend.equals("/share_wa", ignoreCase = true)) {
                                    val lastAiMsg = chatHistory.lastOrNull { it.role != "user" }?.message
                                    if (!lastAiMsg.isNullOrBlank()) {
                                        val rendered = com.example.util.LatexRenderer.render(lastAiMsg)
                                        com.example.util.WhatsAppShareHelper.shareToWhatsApp(context, rendered)
                                    }
                                }
                                viewModel.sendMessage(textToSend)
                            }
                        },
                        enabled = inputText.isNotBlank() && !isLoading,
                        modifier = Modifier.testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Kirim Pesan",
                            tint = if (inputText.isNotBlank() && !isLoading) GreenPrimary else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubbleItem(
    chat: ChatEntity,
    onSpeakText: (String) -> Unit
) {
    val context = LocalContext.current
    val isUser = chat.role == "user"
    val timeFormatted = remember(chat.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(chat.timestamp))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Surface(
                shape = CircleShape,
                color = GreenPrimary,
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.Bottom)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            ElevatedCard(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                    containerColor = if (isUser) GreenContainer else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Optional illustrated visual image
                    chat.imageUrl?.let { imgUrl ->
                        AsyncImage(
                            model = imgUrl,
                            contentDescription = "Ilustrasi Materi Ajar",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        text = if (!isUser) com.example.util.LatexRenderer.render(chat.message) else chat.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isUser) OnGreenContainer else MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = timeFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = (if (isUser) OnGreenContainer else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.7f)
                        )

                        if (!isUser) {
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { onSpeakText(com.example.util.LatexRenderer.render(chat.message)) },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Bacakan Suara",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    val rendered = com.example.util.LatexRenderer.render(chat.message)
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Pesan Cici", rendered))
                                    android.widget.Toast.makeText(context, "Teks disalin", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Salin Teks",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    val rendered = com.example.util.LatexRenderer.render(chat.message)
                                    com.example.util.WhatsAppShareHelper.shareToWhatsApp(context, rendered)
                                },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Bagikan ke WhatsApp",
                                    tint = GreenPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.Bottom)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
