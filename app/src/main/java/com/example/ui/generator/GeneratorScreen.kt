package com.example.ui.generator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.InputKodeDialog
import com.example.ui.components.PremiumLockDialog
import com.example.ui.theme.GreenPrimary
import com.example.util.OcrHelper
import com.example.util.PdfHelper
import com.example.util.PremiumManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
    viewModel: MainViewModel,
    showTopBar: Boolean = true,
    onNavigateToDonasi: () -> Unit = {},
    onDismissOrBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var isPremium by remember {
        mutableStateOf(PremiumManager.isPremium(context))
    }
    var showPremiumDialog by remember {
        mutableStateOf(!isPremium)
    }
    var showInputKodeDialog by remember {
        mutableStateOf(false)
    }

    // JIKA BUKAN PREMIUM → TAMPILKAN DIALOG DAN STOP
    if (!isPremium) {
        if (showPremiumDialog) {
            PremiumLockDialog(
                featureName = "Generator & Alat Guru",
                onDismiss = {
                    showPremiumDialog = false
                    onDismissOrBack()
                },
                onDonasi = {
                    showPremiumDialog = false
                    onNavigateToDonasi()
                },
                onInputKode = {
                    showPremiumDialog = false
                    showInputKodeDialog = true
                }
            )
        }

        if (showInputKodeDialog) {
            InputKodeDialog(
                onDismiss = {
                    showInputKodeDialog = false
                    onDismissOrBack()
                },
                onSuccess = {
                    isPremium = true
                    showInputKodeDialog = false
                }
            )
        }

        return // STOP — jangan tampilkan UI Generator
    }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Soal", "RPP", "Bahan Ajar", "Pengumuman", "OCR")

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Generator",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Generator & Alat Guru",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = GreenPrimary
                    ),
                    modifier = Modifier.testTag("generator_top_app_bar")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = GreenPrimary,
                modifier = Modifier.testTag("generator_tab_row")
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = {
                            when (index) {
                                0 -> Icon(Icons.Default.EditNote, contentDescription = null)
                                1 -> Icon(Icons.Default.Article, contentDescription = null)
                                2 -> Icon(Icons.Default.MenuBook, contentDescription = null)
                                3 -> Icon(Icons.Default.Notifications, contentDescription = null)
                                else -> Icon(Icons.Default.CameraAlt, contentDescription = null)
                            }
                        },
                        modifier = Modifier.testTag("tab_generator_${title.lowercase().replace(" ", "_")}")
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> GeneratorSoalContent(viewModel, snackbarHostState)
                1 -> GeneratorRppContent(viewModel, snackbarHostState)
                2 -> GeneratorBahanAjarContent(viewModel, snackbarHostState)
                3 -> GeneratorPengumumanContent(viewModel, snackbarHostState)
                4 -> OcrScanContent(viewModel, snackbarHostState)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorSoalContent(
    viewModel: MainViewModel,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val soalHistory by viewModel.soalHistory.collectAsState()

    val mapelOptions = listOf("Bahasa Indonesia", "Matematika", "IPAS", "Pendidikan Pancasila", "Bahasa Inggris", "Seni Rupa", "PAI")
    val kelasOptions = listOf(1, 2, 3, 4, 5, 6)
    val tipeOptions = listOf("Pilihan Ganda", "Isian Singkat", "Esai", "Campuran")

    val allBuku by viewModel.allBuku.collectAsState()
    var selectedMapel by remember { mutableStateOf(mapelOptions[1]) } // Matematika
    var selectedKelas by remember { mutableIntStateOf(4) }
    var topikText by remember { mutableStateOf("Pecahan") }
    var jumlahSoalText by remember { mutableStateOf("5") }
    var selectedTipe by remember { mutableStateOf(tipeOptions[0]) } // Pilihan Ganda
    var pakaiBuku by remember { mutableStateOf(true) }

    val hasBukuSiswa = allBuku.any { it.kelas == selectedKelas && it.mapel.equals(selectedMapel, ignoreCase = true) && it.tipe.equals("siswa", ignoreCase = true) }

    var isGenerating by remember { mutableStateOf(false) }
    var generatedResult by remember { mutableStateOf<String?>(null) }
    var showHistory by remember { mutableStateOf(false) }

    var expandedMapel by remember { mutableStateOf(false) }
    var expandedKelas by remember { mutableStateOf(false) }
    var expandedTipe by remember { mutableStateOf(false) }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Soal Ajar", text))
        Toast.makeText(context, "Soal berhasil disalin ke papan klip!", Toast.LENGTH_SHORT).show()
    }

    fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "Soal $selectedMapel Kelas $selectedKelas - $topikText")
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Soal"))
    }

    fun saveAsPdf(text: String) {
        coroutineScope.launch {
            val result = PdfHelper.saveTextAsPdf(
                context = context,
                title = "SOAL LATIHAN: $selectedMapel (KELAS $selectedKelas)\nTOPIK: $topikText",
                bodyText = text,
                fileNamePrefix = "Soal_${selectedMapel}_Kls$selectedKelas"
            )
            result.onSuccess { path ->
                snackbarHostState.showSnackbar("PDF berhasil disimpan: $path")
            }.onFailure { e ->
                snackbarHostState.showSnackbar("Gagal menyimpan PDF: ${e.message}")
            }
        }
    }

    fun doGenerate() {
        val jumlah = jumlahSoalText.toIntOrNull() ?: 10
        if (topikText.isBlank()) {
            Toast.makeText(context, "Silakan isi topik soal terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }
        isGenerating = true
        coroutineScope.launch {
            val result = viewModel.generateSoal(
                mapel = selectedMapel,
                kelas = selectedKelas,
                topik = topikText.trim(),
                jumlah = jumlah.coerceIn(1, 50),
                tipe = selectedTipe,
                pakaiBuku = pakaiBuku
            )
            isGenerating = false
            result.onSuccess { text ->
                generatedResult = text
                snackbarHostState.showSnackbar("Soal berhasil dibuat!")
            }.onFailure { e ->
                snackbarHostState.showSnackbar("Gagal membuat soal: ${e.message}")
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("generator_soal_page"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // FORM CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Parameter Soal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                        IconButton(onClick = { showHistory = !showHistory }) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = "Riwayat Soal",
                                tint = if (showHistory) GreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Mapel Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedMapel,
                        onExpandedChange = { expandedMapel = !expandedMapel },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedMapel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Mata Pelajaran") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMapel) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .testTag("dropdown_soal_mapel")
                        )
                        ExposedDropdownMenu(
                            expanded = expandedMapel,
                            onDismissRequest = { expandedMapel = false }
                        ) {
                            mapelOptions.forEach { mapel ->
                                DropdownMenuItem(
                                    text = { Text(mapel) },
                                    onClick = {
                                        selectedMapel = mapel
                                        expandedMapel = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Kelas Dropdown
                        ExposedDropdownMenuBox(
                            expanded = expandedKelas,
                            onExpandedChange = { expandedKelas = !expandedKelas },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = "Kelas $selectedKelas",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Kelas") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedKelas) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .testTag("dropdown_soal_kelas")
                            )
                            ExposedDropdownMenu(
                                expanded = expandedKelas,
                                onDismissRequest = { expandedKelas = false }
                            ) {
                                kelasOptions.forEach { k ->
                                    DropdownMenuItem(
                                        text = { Text("Kelas $k") },
                                        onClick = {
                                            selectedKelas = k
                                            expandedKelas = false
                                        }
                                    )
                                }
                            }
                        }

                        // Jumlah Soal
                        OutlinedTextField(
                            value = jumlahSoalText,
                            onValueChange = { jumlahSoalText = it.filter { ch -> ch.isDigit() }.take(2) },
                            label = { Text("Jumlah") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_soal_jumlah"),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Topik Text Field
                    OutlinedTextField(
                        value = topikText,
                        onValueChange = { topikText = it },
                        label = { Text("Topik / Materi Pembelajaran") },
                        placeholder = { Text("Contoh: Pecahan, Siklus Air, dsb") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_soal_topik"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tipe Soal Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedTipe,
                        onExpandedChange = { expandedTipe = !expandedTipe },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedTipe,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipe Soal") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipe) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .testTag("dropdown_soal_tipe")
                        )
                        ExposedDropdownMenu(
                            expanded = expandedTipe,
                            onDismissRequest = { expandedTipe = false }
                        ) {
                            tipeOptions.forEach { tipe ->
                                DropdownMenuItem(
                                    text = { Text(tipe) },
                                    onClick = {
                                        selectedTipe = tipe
                                        expandedTipe = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (hasBukuSiswa && pakaiBuku) GreenPrimary.copy(alpha = 0.08f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pakai Buku Siswa (RAG)",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = if (hasBukuSiswa) "Buku Siswa tersedia, soal dibuat dari materi buku"
                                    else "Buku belum di-upload (pakai pengetahuan umum)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (hasBukuSiswa) GreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = pakaiBuku,
                                onCheckedChange = { pakaiBuku = it },
                                modifier = Modifier.testTag("switch_pakai_buku_soal")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { doGenerate() },
                        enabled = !isGenerating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("button_generate_soal"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sedang Membuat Soal...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Soal", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // HASIL GENERATE CARD
        generatedResult?.let { resultText ->
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_hasil_soal"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Hasil Soal ($selectedMapel Kls $selectedKelas)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GreenPrimary
                            )
                            IconButton(onClick = { doGenerate() }, enabled = !isGenerating) {
                                Icon(Icons.Default.Refresh, contentDescription = "Regenerate Soal", tint = GreenPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = com.example.util.LatexRenderer.render(resultText),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Buttons: Copy, Share, Save PDF
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { copyToClipboard(resultText) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("button_copy_soal")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { shareText(resultText) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("button_share_soal")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { saveAsPdf(resultText) },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("button_save_pdf_soal"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save PDF", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // RIWAYAT SOAL SECTION
        if (showHistory) {
            item {
                Text(
                    text = "Riwayat Pembuatan Soal (${soalHistory.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (soalHistory.isEmpty()) {
                item {
                    Text(
                        text = "Belum ada riwayat soal tersimpan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(soalHistory) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${item.mapel} Kelas ${item.kelas} • ${item.topik}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                IconButton(onClick = { viewModel.deleteSoalHistory(item) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            Text(
                                text = "${item.jumlah} soal ${item.tipe}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { generatedResult = item.hasil }) {
                                    Text("Buka Soal", fontSize = 11.sp)
                                }
                                OutlinedButton(onClick = { shareText(item.hasil) }) {
                                    Text("Share", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorRppContent(
    viewModel: MainViewModel,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val rppHistory by viewModel.rppHistory.collectAsState()

    val mapelOptions = listOf("Bahasa Indonesia", "Matematika", "IPAS", "Pendidikan Pancasila", "Bahasa Inggris", "Seni Rupa", "PAI")
    val kelasOptions = listOf(1, 2, 3, 4, 5, 6)
    val semesterOptions = listOf("Ganjil", "Genap")

    val allBuku by viewModel.allBuku.collectAsState()
    var selectedMapel by remember { mutableStateOf(mapelOptions[1]) }
    var selectedKelas by remember { mutableIntStateOf(4) }
    var topikText by remember { mutableStateOf("Pecahan") }
    var jpText by remember { mutableStateOf("2") }
    var selectedSemester by remember { mutableStateOf(semesterOptions[0]) }
    var pakaiBuku by remember { mutableStateOf(true) }

    val hasBukuGuru = allBuku.any { it.kelas == selectedKelas && it.mapel.equals(selectedMapel, ignoreCase = true) && it.tipe.equals("guru", ignoreCase = true) }

    var isGenerating by remember { mutableStateOf(false) }
    var generatedResult by remember { mutableStateOf<String?>(null) }
    var showHistory by remember { mutableStateOf(false) }

    var expandedMapel by remember { mutableStateOf(false) }
    var expandedKelas by remember { mutableStateOf(false) }
    var expandedSemester by remember { mutableStateOf(false) }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("RPP Kemendikbud", text))
        Toast.makeText(context, "RPP berhasil disalin ke papan klip!", Toast.LENGTH_SHORT).show()
    }

    fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "RPP $selectedMapel Kelas $selectedKelas - $topikText")
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan RPP"))
    }

    fun saveAsPdf(text: String) {
        coroutineScope.launch {
            val result = PdfHelper.saveTextAsPdf(
                context = context,
                title = "RENCANA PELAKSANAAN PEMBELAJARAN (RPP)\n$selectedMapel - KELAS $selectedKelas ($topikText)",
                bodyText = text,
                fileNamePrefix = "RPP_${selectedMapel}_Kls$selectedKelas"
            )
            result.onSuccess { path ->
                snackbarHostState.showSnackbar("PDF RPP berhasil disimpan: $path")
            }.onFailure { e ->
                snackbarHostState.showSnackbar("Gagal menyimpan PDF: ${e.message}")
            }
        }
    }

    fun doGenerate() {
        val jp = jpText.toIntOrNull() ?: 2
        if (topikText.isBlank()) {
            Toast.makeText(context, "Silakan isi topik RPP terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }
        isGenerating = true
        coroutineScope.launch {
            val result = viewModel.generateRpp(
                mapel = selectedMapel,
                kelas = selectedKelas,
                topik = topikText.trim(),
                jp = jp,
                semester = selectedSemester,
                pakaiBuku = pakaiBuku
            )
            isGenerating = false
            result.onSuccess { text ->
                generatedResult = text
                snackbarHostState.showSnackbar("RPP berhasil dibuat!")
            }.onFailure { e ->
                snackbarHostState.showSnackbar("Gagal membuat RPP: ${e.message}")
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("generator_rpp_page"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // FORM CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Parameter RPP Merdeka Belajar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                        IconButton(onClick = { showHistory = !showHistory }) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = "Riwayat RPP",
                                tint = if (showHistory) GreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Mapel
                    ExposedDropdownMenuBox(
                        expanded = expandedMapel,
                        onExpandedChange = { expandedMapel = !expandedMapel },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedMapel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Mata Pelajaran") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMapel) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .testTag("dropdown_rpp_mapel")
                        )
                        ExposedDropdownMenu(
                            expanded = expandedMapel,
                            onDismissRequest = { expandedMapel = false }
                        ) {
                            mapelOptions.forEach { mapel ->
                                DropdownMenuItem(
                                    text = { Text(mapel) },
                                    onClick = {
                                        selectedMapel = mapel
                                        expandedMapel = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Kelas
                        ExposedDropdownMenuBox(
                            expanded = expandedKelas,
                            onExpandedChange = { expandedKelas = !expandedKelas },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = "Kelas $selectedKelas",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Kelas") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedKelas) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .testTag("dropdown_rpp_kelas")
                            )
                            ExposedDropdownMenu(
                                expanded = expandedKelas,
                                onDismissRequest = { expandedKelas = false }
                            ) {
                                kelasOptions.forEach { k ->
                                    DropdownMenuItem(
                                        text = { Text("Kelas $k") },
                                        onClick = {
                                            selectedKelas = k
                                            expandedKelas = false
                                        }
                                    )
                                }
                            }
                        }

                        // Alokasi JP
                        OutlinedTextField(
                            value = jpText,
                            onValueChange = { jpText = it.filter { ch -> ch.isDigit() }.take(2) },
                            label = { Text("Alokasi (JP)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_rpp_jp"),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Topik Text Field
                    OutlinedTextField(
                        value = topikText,
                        onValueChange = { topikText = it },
                        label = { Text("Topik Pembelajaran") },
                        placeholder = { Text("Contoh: Pecahan, Ekosistem, dsb") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_rpp_topik"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Semester Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedSemester,
                        onExpandedChange = { expandedSemester = !expandedSemester },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = "Semester $selectedSemester",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Semester") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSemester) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .testTag("dropdown_rpp_semester")
                        )
                        ExposedDropdownMenu(
                            expanded = expandedSemester,
                            onDismissRequest = { expandedSemester = false }
                        ) {
                            semesterOptions.forEach { sem ->
                                DropdownMenuItem(
                                    text = { Text("Semester $sem") },
                                    onClick = {
                                        selectedSemester = sem
                                        expandedSemester = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (hasBukuGuru && pakaiBuku) GreenPrimary.copy(alpha = 0.08f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pakai Buku Guru (RAG)",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = if (hasBukuGuru) "Buku Guru tersedia, modul ajar mengacu pada buku resmi"
                                    else "Buku belum di-upload (pakai pengetahuan umum)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (hasBukuGuru) GreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = pakaiBuku,
                                onCheckedChange = { pakaiBuku = it },
                                modifier = Modifier.testTag("switch_pakai_buku_rpp")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { doGenerate() },
                        enabled = !isGenerating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("button_generate_rpp"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Menyusun RPP Lengkap...")
                        } else {
                            Icon(Icons.Default.Article, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate RPP", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // HASIL RPP CARD
        generatedResult?.let { resultText ->
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_hasil_rpp"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RPP: $selectedMapel (Kelas $selectedKelas)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GreenPrimary
                            )
                            IconButton(onClick = { doGenerate() }, enabled = !isGenerating) {
                                Icon(Icons.Default.Refresh, contentDescription = "Regenerate RPP", tint = GreenPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = com.example.util.LatexRenderer.render(resultText),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Actions: Copy, Share, Save PDF, Save ke Bank
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { copyToClipboard(resultText) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("button_copy_rpp")
                            ) {
                                Text("Copy", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = { shareText(resultText) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("button_share_rpp")
                            ) {
                                Text("Share", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { saveAsPdf(resultText) },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("button_save_pdf_rpp"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("Save PDF", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val saveResult = viewModel.saveRppToBank(
                                            mapel = selectedMapel,
                                            kelas = selectedKelas,
                                            topik = topikText,
                                            rppText = resultText
                                        )
                                        saveResult.onSuccess {
                                            snackbarHostState.showSnackbar("RPP berhasil disimpan ke Bank Materi!")
                                        }.onFailure { e ->
                                            snackbarHostState.showSnackbar("Gagal menyimpan ke Bank: ${e.message}")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("button_save_bank_rpp"),
                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                            ) {
                                Text("Simpan", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // RIWAYAT RPP
        if (showHistory) {
            item {
                Text(
                    text = "Riwayat Pembuatan RPP (${rppHistory.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (rppHistory.isEmpty()) {
                item {
                    Text(
                        text = "Belum ada riwayat RPP tersimpan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(rppHistory) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "RPP: ${item.mapel} Kelas ${item.kelas} • ${item.topik}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                IconButton(onClick = { viewModel.deleteRppHistory(item) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            Text(
                                text = "${item.jp} JP • Semester ${item.semester}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { generatedResult = item.hasil }) {
                                    Text("Buka RPP", fontSize = 11.sp)
                                }
                                OutlinedButton(onClick = { shareText(item.hasil) }) {
                                    Text("Share", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScanContent(
    viewModel: MainViewModel,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val mapelOptions = listOf("Bahasa Indonesia", "Matematika", "IPAS", "Pendidikan Pancasila", "Bahasa Inggris", "Seni Rupa", "PAI")
    val kelasOptions = listOf(1, 2, 3, 4, 5, 6)

    var selectedMapel by remember { mutableStateOf(mapelOptions[0]) }
    var selectedKelas by remember { mutableIntStateOf(4) }
    var babText by remember { mutableStateOf("Materi Pindai Buku") }

    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var recognizedText by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    var expandedMapel by remember { mutableStateOf(false) }
    var expandedKelas by remember { mutableStateOf(false) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            selectedBitmap = bitmap
            recognizedText = ""
        }
    }

    // Gallery picker launcher
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
                selectedBitmap = bitmap
                recognizedText = ""
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal memuat gambar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // PDF picker launcher for Upload Buku PDF
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isScanning = true
            coroutineScope.launch {
                try {
                    val result = com.example.util.PdfExtractorHelper.extractTextFromPdfUri(
                        context = context,
                        uri = uri,
                        maxPages = 15
                    )
                    isScanning = false
                    result.onSuccess { text ->
                        recognizedText = com.example.util.LatexRenderer.render(text)
                        selectedBitmap = null
                        Toast.makeText(context, "Buku PDF berhasil dipindai!", Toast.LENGTH_SHORT).show()
                    }.onFailure { e ->
                        Toast.makeText(context, "Gagal membaca PDF: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    isScanning = false
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun doScan() {
        val bitmap = selectedBitmap ?: return
        isScanning = true
        coroutineScope.launch {
            try {
                val text = OcrHelper.recognizeTextFromBitmap(bitmap)
                recognizedText = com.example.util.LatexRenderer.render(text.ifBlank { "Tidak ditemukan teks terbaca pada gambar." })
                snackbarHostState.showSnackbar("Pindai OCR selesai!")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Gagal memindai teks: ${e.message}")
            } finally {
                isScanning = false
            }
        }
    }

    fun saveMateri() {
        if (recognizedText.isBlank()) {
            Toast.makeText(context, "Teks hasil OCR masih kosong", Toast.LENGTH_SHORT).show()
            return
        }
        isSaving = true
        coroutineScope.launch {
            val result = viewModel.saveMateriFromOcr(
                kelas = selectedKelas,
                mapel = selectedMapel,
                bab = babText.trim(),
                textOcr = recognizedText.trim()
            )
            isSaving = false
            result.onSuccess {
                Toast.makeText(context, "Materi tersimpan: Kelas $selectedKelas, Mapel $selectedMapel", Toast.LENGTH_LONG).show()
                snackbarHostState.showSnackbar("Materi tersimpan: Kelas $selectedKelas, Mapel $selectedMapel")
            }.onFailure { e ->
                snackbarHostState.showSnackbar("Gagal menyimpan materi: ${e.message}")
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ocr_scan_page"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // SELECT IMAGE CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Pindai Teks Buku (OCR)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ambil foto halaman buku pelajaran atau dokumen untuk langsung diubah menjadi materi ajar:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { cameraLauncher.launch() },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("button_take_photo_camera"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buka Kamera")
                        }

                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("button_pick_image_gallery"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pilih Galeri")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { pdfLauncher.launch("application/pdf") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("button_upload_buku_pdf"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload Buku PDF", fontWeight = FontWeight.Bold)
                    }

                    // PREVIEW IMAGE
                    selectedBitmap?.let { bmp ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Preview Foto Buku",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { doScan() },
                            enabled = !isScanning,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("button_scan_ocr"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sedang Memindai Teks...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scan Teks (OCR)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // HASIL SCAN & SIMPAN CARD
        if (recognizedText.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_hasil_ocr"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Hasil Pindai Teks",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = recognizedText,
                            onValueChange = { recognizedText = it },
                            label = { Text("Teks Hasil Pindai (Bisa diedit)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .testTag("input_ocr_result_text")
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Simpan ke Bank Materi",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Kelas
                            ExposedDropdownMenuBox(
                                expanded = expandedKelas,
                                onExpandedChange = { expandedKelas = !expandedKelas },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = "Kelas $selectedKelas",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Kelas") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedKelas) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .testTag("dropdown_ocr_kelas")
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedKelas,
                                    onDismissRequest = { expandedKelas = false }
                                ) {
                                    kelasOptions.forEach { k ->
                                        DropdownMenuItem(
                                            text = { Text("Kelas $k") },
                                            onClick = {
                                                selectedKelas = k
                                                expandedKelas = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Mapel
                            ExposedDropdownMenuBox(
                                expanded = expandedMapel,
                                onExpandedChange = { expandedMapel = !expandedMapel },
                                modifier = Modifier.weight(1.5f)
                            ) {
                                OutlinedTextField(
                                    value = selectedMapel,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Mapel") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMapel) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .testTag("dropdown_ocr_mapel")
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedMapel,
                                    onDismissRequest = { expandedMapel = false }
                                ) {
                                    mapelOptions.forEach { mapel ->
                                        DropdownMenuItem(
                                            text = { Text(mapel) },
                                            onClick = {
                                                selectedMapel = mapel
                                                expandedMapel = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = babText,
                            onValueChange = { babText = it },
                            label = { Text("Bab / Judul Materi") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_ocr_bab"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { saveMateri() },
                            enabled = !isSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("button_simpan_materi_ocr"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Menyimpan...")
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Simpan ke Bank Materi", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorBahanAjarContent(
    viewModel: MainViewModel,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val allBuku by viewModel.allBuku.collectAsState()
    val mapelOptions = listOf("Bahasa Indonesia", "Matematika", "IPAS", "Pendidikan Pancasila", "Bahasa Inggris", "Seni Rupa", "PAI")
    val kelasOptions = listOf(1, 2, 3, 4, 5, 6)

    var selectedMapel by remember { mutableStateOf(mapelOptions[1]) }
    var selectedKelas by remember { mutableIntStateOf(4) }
    var topikText by remember { mutableStateOf("Pecahan dan Operasi Hitung") }
    var pakaiBuku by remember { mutableStateOf(true) }

    val hasBuku = allBuku.any { it.kelas == selectedKelas && it.mapel.equals(selectedMapel, ignoreCase = true) }

    var isGenerating by remember { mutableStateOf(false) }
    var isSavingToBank by remember { mutableStateOf(false) }
    var generatedResult by remember { mutableStateOf<String?>(null) }
    var expandedMapel by remember { mutableStateOf(false) }
    var expandedKelas by remember { mutableStateOf(false) }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Bahan Ajar", text))
        Toast.makeText(context, "Bahan ajar disalin ke clipboard", Toast.LENGTH_SHORT).show()
    }

    fun saveAsPdf(text: String) {
        coroutineScope.launch {
            val result = PdfHelper.saveTextAsPdf(
                context = context,
                title = "BAHAN AJAR $selectedMapel KELAS $selectedKelas\nTOPIK: $topikText",
                bodyText = text,
                fileNamePrefix = "BahanAjar_${selectedMapel}_Kls$selectedKelas"
            )
            result.onSuccess { path ->
                snackbarHostState.showSnackbar("PDF Bahan Ajar berhasil disimpan: $path")
            }.onFailure { e ->
                snackbarHostState.showSnackbar("Gagal menyimpan PDF: ${e.message}")
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = GreenPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Generator Bahan Ajar Multi-Sumber (RAG)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = expandedMapel,
                            onExpandedChange = { expandedMapel = !expandedMapel },
                            modifier = Modifier.weight(1.5f)
                        ) {
                            OutlinedTextField(
                                value = selectedMapel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Mapel") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMapel) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedMapel,
                                onDismissRequest = { expandedMapel = false }
                            ) {
                                mapelOptions.forEach { mapel ->
                                    DropdownMenuItem(
                                        text = { Text(mapel) },
                                        onClick = {
                                            selectedMapel = mapel
                                            expandedMapel = false
                                        }
                                    )
                                }
                            }
                        }

                        ExposedDropdownMenuBox(
                            expanded = expandedKelas,
                            onExpandedChange = { expandedKelas = !expandedKelas },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = "Kelas $selectedKelas",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Kelas") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedKelas) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedKelas,
                                onDismissRequest = { expandedKelas = false }
                            ) {
                                kelasOptions.forEach { k ->
                                    DropdownMenuItem(
                                        text = { Text("Kelas $k") },
                                        onClick = {
                                            selectedKelas = k
                                            expandedKelas = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = topikText,
                        onValueChange = { topikText = it },
                        label = { Text("Topik / Bab Materi") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // TOGGLE PAKAI BUKU
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (hasBuku && pakaiBuku) GreenPrimary.copy(alpha = 0.08f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pakai Buku Pelajaran (RAG)",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = if (hasBuku) "Buku tersedia, materi diambil dari buku guru/siswa & internet"
                                    else "Buku belum di-upload (akan dirangkum dari internet & kurikulum)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (hasBuku) GreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = pakaiBuku,
                                onCheckedChange = { pakaiBuku = it },
                                modifier = Modifier.testTag("switch_pakai_buku_bahan_ajar")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            isGenerating = true
                            generatedResult = null
                            coroutineScope.launch {
                                val res = viewModel.generateBahanAjar(selectedMapel, selectedKelas, topikText, pakaiBuku)
                                isGenerating = false
                                if (res.isSuccess) {
                                    generatedResult = res.getOrNull()
                                    snackbarHostState.showSnackbar("Bahan ajar berhasil dibuat!")
                                } else {
                                    snackbarHostState.showSnackbar("Gagal membuat bahan ajar: ${res.exceptionOrNull()?.message}")
                                }
                            }
                        },
                        enabled = !isGenerating && topikText.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Menyusun Bahan Ajar...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Bahan Ajar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (generatedResult != null) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Hasil Bahan Ajar",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Row {
                                IconButton(onClick = { copyToClipboard(generatedResult ?: "") }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Salin", tint = GreenPrimary)
                                }
                                IconButton(onClick = { com.example.util.WhatsAppShareHelper.shareToWhatsApp(context, generatedResult ?: "") }) {
                                    Icon(Icons.Default.Share, contentDescription = "Bagikan", tint = GreenPrimary)
                                }
                                IconButton(onClick = { saveAsPdf(generatedResult ?: "") }) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Download PDF", tint = GreenPrimary)
                                }
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = generatedResult ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = {
                                isSavingToBank = true
                                coroutineScope.launch {
                                    val saveRes = viewModel.saveBahanAjarToBank(
                                        mapel = selectedMapel,
                                        kelas = selectedKelas,
                                        topik = topikText,
                                        bahanAjarText = generatedResult ?: ""
                                    )
                                    isSavingToBank = false
                                    if (saveRes.isSuccess) {
                                        snackbarHostState.showSnackbar("Bahan ajar berhasil disimpan ke Bank Materi!")
                                    } else {
                                        snackbarHostState.showSnackbar("Gagal menyimpan: ${saveRes.exceptionOrNull()?.message}")
                                    }
                                }
                            },
                            enabled = !isSavingToBank,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isSavingToBank) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = GreenPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Menyimpan ke Bank Materi...")
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Simpan ke Bank Materi")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorPengumumanContent(
    viewModel: MainViewModel,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pengumumanHistory by viewModel.pengumumanHistory.collectAsState()

    val jenisOptions = listOf(
        "Ulangan/UTS",
        "PR/Tugas",
        "Bahan Bawaan",
        "Kegiatan Sekolah",
        "Libur",
        "Rapat Wali Murid",
        "Custom"
    )
    val kelasOptions = listOf(1, 2, 3, 4, 5, 6)
    val bahasaOptions = listOf("Indonesia", "Sunda", "Inggris")

    var selectedJenis by remember { mutableStateOf(jenisOptions[0]) }
    var infoTambahanText by remember { mutableStateOf("ulangan matematika bab pecahan besok selasa") }
    var selectedKelas by remember { mutableIntStateOf(4) }
    var selectedBahasa by remember { mutableStateOf(bahasaOptions[0]) }
    var isFormal by remember { mutableStateOf(true) }

    var isGenerating by remember { mutableStateOf(false) }
    var generatedResult by remember { mutableStateOf<String?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editedText by remember { mutableStateOf("") }

    var expandedJenis by remember { mutableStateOf(false) }
    var expandedKelas by remember { mutableStateOf(false) }
    var expandedBahasa by remember { mutableStateOf(false) }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Pengumuman WhatsApp", text))
        Toast.makeText(context, "Pengumuman disalin ke clipboard", Toast.LENGTH_SHORT).show()
    }

    fun applyQuickTemplate(jenis: String) {
        selectedJenis = jenis
        when (jenis) {
            "Ulangan/UTS" -> infoTambahanText = "ulangan matematika bab pecahan besok selasa, bawa pensil 2B & penggaris"
            "PR/Tugas" -> infoTambahanText = "PR matematika halaman 45 nomor 1-5 dikumpulkan hari rabu"
            "Bahan Bawaan" -> infoTambahanText = "membawa kertas origami, lem, dan gunting untuk prakarya hari kamis"
            "Kegiatan Sekolah" -> infoTambahanText = "kegiatan jalan santai hari jumat, pakai kaos olahraga dari rumah"
            "Libur" -> infoTambahanText = "libur awal puasa tanggal 1-3 maret, masuk kembali hari senin tanggal 4 maret"
            "Rapat Wali Murid" -> infoTambahanText = "pertemuan parenting dan pembagian rapot semester ganjil sabtu jam 09.00 di aula"
            else -> infoTambahanText = "pengumuman kegiatan untuk seluruh wali murid"
        }
    }

    fun doGenerate() {
        if (infoTambahanText.isBlank()) return
        isGenerating = true
        generatedResult = null
        coroutineScope.launch {
            val res = viewModel.generatePengumuman(
                jenis = selectedJenis,
                infoTambahan = infoTambahanText,
                kelas = selectedKelas,
                formal = isFormal,
                bahasa = selectedBahasa
            )
            isGenerating = false
            if (res.isSuccess) {
                generatedResult = res.getOrNull()
                snackbarHostState.showSnackbar("Pengumuman WhatsApp berhasil dibuat!")
            } else {
                snackbarHostState.showSnackbar("Gagal membuat pengumuman: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    // Dialog Edit Manual
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = "Edit Teks Pengumuman",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    label = { Text("Sesuaikan Teks") },
                    maxLines = 15
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        generatedResult = editedText
                        showEditDialog = false
                        viewModel.savePengumumanManual(
                            jenis = selectedJenis,
                            kelas = selectedKelas,
                            konten = editedText,
                            infoTambahan = infoTambahanText
                        )
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Perubahan pengumuman disimpan")
                        }
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // QUICK TEMPLATES (TASK 7)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = GreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "📢 Template Cepat Pengumuman WA",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Ulangan/UTS", "PR/Tugas", "Bahan Bawaan").forEach { t ->
                            FilterChip(
                                selected = selectedJenis == t,
                                onClick = { applyQuickTemplate(t) },
                                label = { Text(t.substringBefore("/"), fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Kegiatan Sekolah", "Libur", "Rapat Wali Murid").forEach { t ->
                            FilterChip(
                                selected = selectedJenis == t,
                                onClick = { applyQuickTemplate(t) },
                                label = {
                                    val shortLabel = when (t) {
                                        "Kegiatan Sekolah" -> "Kegiatan"
                                        "Rapat Wali Murid" -> "Rapat"
                                        else -> t
                                    }
                                    Text(shortLabel, fontSize = 12.sp)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // FORM PENGUMUMAN (TASK 1)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Form Pengumuman WhatsApp",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Jenis Pengumuman Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedJenis,
                        onExpandedChange = { expandedJenis = !expandedJenis },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedJenis,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Jenis Pengumuman") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedJenis) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .testTag("dropdown_jenis_pengumuman")
                        )
                        ExposedDropdownMenu(
                            expanded = expandedJenis,
                            onDismissRequest = { expandedJenis = false }
                        ) {
                            jenisOptions.forEach { jenis ->
                                DropdownMenuItem(
                                    text = { Text(jenis) },
                                    onClick = {
                                        selectedJenis = jenis
                                        expandedJenis = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Kelas & Bahasa
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = expandedKelas,
                            onExpandedChange = { expandedKelas = !expandedKelas },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = "Kelas $selectedKelas",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Kelas") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedKelas) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .testTag("dropdown_kelas_pengumuman")
                            )
                            ExposedDropdownMenu(
                                expanded = expandedKelas,
                                onDismissRequest = { expandedKelas = false }
                            ) {
                                kelasOptions.forEach { k ->
                                    DropdownMenuItem(
                                        text = { Text("Kelas $k") },
                                        onClick = {
                                            selectedKelas = k
                                            expandedKelas = false
                                        }
                                    )
                                }
                            }
                        }

                        ExposedDropdownMenuBox(
                            expanded = expandedBahasa,
                            onExpandedChange = { expandedBahasa = !expandedBahasa },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedBahasa,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Bahasa") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBahasa) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedBahasa,
                                onDismissRequest = { expandedBahasa = false }
                            ) {
                                bahasaOptions.forEach { b ->
                                    DropdownMenuItem(
                                        text = { Text(b) },
                                        onClick = {
                                            selectedBahasa = b
                                            expandedBahasa = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Info Tambahan Multi-line
                    OutlinedTextField(
                        value = infoTambahanText,
                        onValueChange = { infoTambahanText = it },
                        label = { Text("Info Tambahan") },
                        placeholder = { Text("Contoh: ulangan matematika bab pecahan besok selasa") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("input_info_tambahan_pengumuman"),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Toggle Formal / Santai
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isFormal) "Gaya: Formal (Salam Lengkap)" else "Gaya: Santai (Langsung Info)",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = if (isFormal) "Cocok untuk rapat dan surat resmi" else "Cocok untuk pengingat harian",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isFormal,
                                onCheckedChange = { isFormal = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = GreenPrimary)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tombol Generate
                    Button(
                        onClick = { doGenerate() },
                        enabled = !isGenerating && infoTambahanText.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_generate_pengumuman"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Menyusun Pengumuman...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Pengumuman", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // PREVIEW PENGUMUMAN & AKSI
        if (generatedResult != null) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_preview_pengumuman")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = GreenPrimary
                                ) {
                                    Text(
                                        text = "WhatsApp Ready",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Preview Pengumuman",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }

                            Row {
                                IconButton(onClick = {
                                    editedText = generatedResult ?: ""
                                    showEditDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Manual", tint = GreenPrimary)
                                }
                                IconButton(onClick = { doGenerate() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Regenerate", tint = GreenPrimary)
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Box Pesan WhatsApp
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = generatedResult ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 22.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Tombol Aksi: Copy, Share WA, Share WA Grup, Save
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { copyToClipboard(generatedResult ?: "") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_copy_pengumuman"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Salin", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    com.example.util.WhatsAppShareHelper.shareToWhatsApp(
                                        context,
                                        generatedResult ?: ""
                                    )
                                },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("btn_share_wa_pengumuman"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share WA", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.savePengumumanManual(
                                        jenis = selectedJenis,
                                        kelas = selectedKelas,
                                        konten = generatedResult ?: "",
                                        infoTambahan = infoTambahanText
                                    )
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Pengumuman tersimpan di Riwayat!")
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.1f)
                                    .testTag("btn_save_pengumuman"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Simpan", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // RIWAYAT PENGUMUMAN (TASK 6)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Riwayat Pengumuman (${pengumumanHistory.size})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }

        if (pengumumanHistory.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Belum ada riwayat pengumuman. Buat pengumuman pertama Anda di atas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(pengumumanHistory) { item ->
                val timeStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(item.timestamp))

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_history_pengumuman_${item.id}"),
                    onClick = {
                        selectedJenis = item.jenis
                        selectedKelas = item.kelas
                        infoTambahanText = item.info_tambahan
                        generatedResult = item.konten
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Pengumuman dimuat ke preview")
                        }
                    }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = GreenPrimary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = item.jenis,
                                        color = GreenPrimary,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "Kelas ${item.kelas}",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = timeStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { viewModel.deletePengumumanHistory(item.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Hapus Riwayat",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = item.konten.take(120).replace("\n", " ") + if (item.konten.length > 120) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}
