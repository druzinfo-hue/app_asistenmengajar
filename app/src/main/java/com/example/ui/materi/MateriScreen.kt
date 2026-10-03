package com.example.ui.materi

import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.MateriEntity
import com.example.ui.MainViewModel
import com.example.ui.components.InputKodeDialog
import com.example.ui.components.PremiumLockDialog
import com.example.ui.generator.OcrScanContent
import com.example.ui.progress.DetailItemRow
import com.example.ui.theme.GreenContainer
import com.example.ui.theme.GreenPrimary
import com.example.util.PremiumManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MateriScreen(
    viewModel: MainViewModel,
    showTopBar: Boolean = true
) {
    val context = LocalContext.current
    var isPremium by remember { mutableStateOf(PremiumManager.isPremium(context)) }
    var showPremiumLockDialog by remember { mutableStateOf(false) }
    var showInputKodeDialog by remember { mutableStateOf(false) }

    if (showPremiumLockDialog) {
        PremiumLockDialog(
            featureName = "Upload Buku & OCR",
            onDismiss = { showPremiumLockDialog = false },
            onDonasi = {
                showPremiumLockDialog = false
                viewModel.openUpgradeDialog()
            },
            onInputKode = {
                showPremiumLockDialog = false
                showInputKodeDialog = true
            }
        )
    }

    if (showInputKodeDialog) {
        InputKodeDialog(
            onDismiss = { showInputKodeDialog = false },
            onSuccess = {
                isPremium = true
                showInputKodeDialog = false
            }
        )
    }

    val allMateri by viewModel.allMateri.collectAsState()
    val selectedKelas by viewModel.selectedMateriBankKelas.collectAsState()
    val searchQuery by viewModel.materiBankSearchQuery.collectAsState()

    var selectedDetailMateri by remember { mutableStateOf<MateriEntity?>(null) }
    var showOcrBottomSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Filter by class and search query
    val filteredMateri = remember(allMateri, selectedKelas, searchQuery) {
        allMateri.filter { it.kelas == selectedKelas }
            .filter {
                if (searchQuery.isBlank()) true
                else {
                    it.judul.contains(searchQuery, ignoreCase = true) ||
                    it.bab.contains(searchQuery, ignoreCase = true) ||
                    it.mapel.contains(searchQuery, ignoreCase = true) ||
                    it.bahan.contains(searchQuery, ignoreCase = true) ||
                    it.tujuan.contains(searchQuery, ignoreCase = true)
                }
            }
    }

    // Group by Bab
    val groupedByBab = remember(filteredMateri) {
        filteredMateri.groupBy {
            val babClean = it.bab.ifBlank { "Umum / Pengenalan" }
            "${it.mapel} - $babClean"
        }
    }

    // Detail Bottom Sheet
    selectedDetailMateri?.let { m ->
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { selectedDetailMateri = null },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GreenContainer
                    ) {
                        Text(
                            text = "${m.mapel} • Kls ${m.kelas} • Pertemuan ${m.pertemuan}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val bahanList = m.bahan.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            val bahanFormatted = if (bahanList.isNotEmpty()) {
                                bahanList.joinToString("\n• ", "• ")
                            } else {
                                "• Tidak ada bahan khusus"
                            }
                            val babText = m.bab.ifBlank { m.judul }
                            val formattedText = """
                            📚 MATERI PEMBELAJARAN

                            Kelas: ${m.kelas} - ${m.mapel}
                            Pertemuan: ${m.pertemuan}
                            Bab: $babText

                            📖 Materi:
                            ${m.judul}

                            🎯 Tujuan:
                            ${m.tujuan.ifBlank { "-" }}

                            🛠 Bahan:
                            $bahanFormatted

                            Sumber: Buku Guru & Siswa Kurikulum Merdeka (Kemendikbudristek)

                            — Dikirim dari Asisten Mengajar
                            """.trimIndent()

                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, formattedText)
                                putExtra(Intent.EXTRA_SUBJECT, "Materi $babText")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Bagikan via"))
                        },
                        modifier = Modifier.testTag("share_materi_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Bagikan Materi", tint = GreenPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = com.example.util.LatexRenderer.render(m.judul),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                if (m.bab.isNotBlank()) {
                    Text(
                        text = com.example.util.LatexRenderer.render(m.bab),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                DetailItemRow(label = "Tujuan Pembelajaran", content = m.tujuan.ifBlank { "-" })
                Spacer(modifier = Modifier.height(10.dp))
                DetailItemRow(label = "Bahan & Alat Praktikum", content = m.bahan.ifBlank { "-" })
                Spacer(modifier = Modifier.height(10.dp))
                DetailItemRow(label = "Alokasi Waktu", content = "${m.alokasiJp} Jam Pelajaran (JP)")
                Spacer(modifier = Modifier.height(10.dp))
                DetailItemRow(
                    label = "Status Pelaksanaan",
                    content = if (m.status == "selesai") "✅ Selesai ${m.tanggalSelesai?.let { "($it)" } ?: ""}" else "⏳ Belum Dilaksanakan"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val bahanList = m.bahan.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        val bahanFormatted = if (bahanList.isNotEmpty()) {
                            bahanList.joinToString("\n• ", "• ")
                        } else {
                            "• Tidak ada bahan khusus"
                        }
                        val babText = m.bab.ifBlank { m.judul }
                        val formattedText = """
                        📚 MATERI PEMBELAJARAN

                        Kelas: ${m.kelas} - ${m.mapel}
                        Pertemuan: ${m.pertemuan}
                        Bab: $babText

                        📖 Materi:
                        ${m.judul}

                        🎯 Tujuan:
                        ${m.tujuan.ifBlank { "-" }}

                        🛠 Bahan:
                        $bahanFormatted

                        Sumber: Buku Guru & Siswa Kurikulum Merdeka (Kemendikbudristek)

                        — Dikirim dari Asisten Mengajar
                        """.trimIndent()

                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, formattedText)
                            putExtra(Intent.EXTRA_SUBJECT, "Materi $babText")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Bagikan via"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("button_share_materi_action"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bagikan Materi (Share)", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showOcrBottomSheet) {
        val ocrSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showOcrBottomSheet = false },
            sheetState = ocrSheetState
        ) {
            OcrScanContent(viewModel = viewModel, snackbarHostState = snackbarHostState)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Bank Materi Kurikulum SD",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { showOcrBottomSheet = true },
                            modifier = Modifier.testTag("materi_upload_pdf_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Upload Buku PDF",
                                tint = GreenPrimary
                            )
                        }

                        IconButton(
                            onClick = { showOcrBottomSheet = true },
                            modifier = Modifier.testTag("materi_tambah_foto_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Tambah dari Foto (OCR)",
                                tint = GreenPrimary
                            )
                        }

                        IconButton(
                            onClick = {
                                // Share all current class materials summary
                                val summary = buildString {
                                    appendLine("=== BANK MATERI AJAR KELAS $selectedKelas ===")
                                    groupedByBab.forEach { (babHeader, list) ->
                                        appendLine("\n📁 $babHeader")
                                        list.forEach { m ->
                                            appendLine(" • P${m.pertemuan}: ${m.judul} (${m.alokasiJp} JP)")
                                        }
                                    }
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, summary)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Bagikan Bank Materi Kelas $selectedKelas"))
                            },
                            modifier = Modifier.testTag("share_materi_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Bagikan Semua Materi",
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
                .testTag("materi_screen")
        ) {
            // 1. TABS KELAS 1 - 6
            val tabs = listOf(1, 2, 3, 4, 5, 6)
            PrimaryTabRow(
                selectedTabIndex = tabs.indexOf(selectedKelas).coerceAtLeast(0),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = GreenPrimary
            ) {
                tabs.forEach { k ->
                    Tab(
                        selected = selectedKelas == k,
                        onClick = { viewModel.setSelectedMateriBankKelas(k) },
                        text = {
                            Text(
                                text = "Kls $k",
                                fontWeight = if (selectedKelas == k) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // 2. SEARCH BAR
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setMateriBankSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("materi_search_bar"),
                placeholder = { Text("Cari judul bab, materi, bahan praktikum...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setMateriBankSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Hapus")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            // 3. GROUPED LIST BY BAB
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (groupedByBab.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank())
                                    "Tidak ada materi yang sesuai dengan pencarian \"$searchQuery\" di Kelas $selectedKelas."
                                else
                                    "Belum ada data materi untuk Kelas $selectedKelas.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                groupedByBab.forEach { (babTitle, materiList) ->
                    item {
                        Column {
                            // Section header
                            Text(
                                text = babTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            ElevatedCard(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column {
                                    materiList.forEachIndexed { index, item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedDetailMateri = item }
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (item.status == "selesai") GreenContainer else MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Text(
                                                    text = "P${item.pertemuan}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (item.status == "selesai") GreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = com.example.util.LatexRenderer.render(item.judul),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                if (item.bahan.isNotBlank()) {
                                                    Text(
                                                        text = "Bahan: ${item.bahan}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Text(
                                                text = "${item.alokasiJp} JP",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )

                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.outline
                                            )
                                        }

                                        if (index < materiList.size - 1) {
                                            androidx.compose.material3.HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.padding(horizontal = 14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
