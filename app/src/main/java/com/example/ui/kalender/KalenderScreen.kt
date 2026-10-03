package com.example.ui.kalender

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.data.local.KalenderEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.GreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KalenderScreen(
    viewModel: MainViewModel,
    showTopBar: Boolean = true
) {
    val context = LocalContext.current
    val allKalender by viewModel.allKalender.collectAsState()
    val semesterInfo by viewModel.kalenderSemester.collectAsState()

    var selectedFilter by remember { mutableStateOf("Semua") }
    val filterOptions = listOf("Semua", "Libur", "Kegiatan", "Ujian")

    var showFormDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<KalenderEntity?>(null) }
    var deletingEvent by remember { mutableStateOf<KalenderEntity?>(null) }

    val liburNasional = allKalender.filter { it.jenis == "libur_nasional" }
    val liburSemester = allKalender.filter { it.jenis == "libur_semester" || it.jenis == "libur_keagamaan" || it.jenis == "libur_khusus" }
    val kegiatanSekolah = allKalender.filter { it.jenis == "kegiatan" || it.jenis == "peringatan" }
    val ujianDanRapor = allKalender.filter { it.jenis == "ujian" || it.jenis == "rapor" }

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Kalender",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Kalender Akademik",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = GreenPrimary
                    ),
                    modifier = Modifier.testTag("kalender_top_app_bar")
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingEvent = null
                    showFormDialog = true
                },
                containerColor = GreenPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_tambah_event")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah Event",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("kalender_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: "📅 KALENDER AKADEMIK" + Semester Info
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_semester_info"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "📅",
                                fontSize = 22.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "KALENDER AKADEMIK",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                ),
                                color = GreenPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = semesterInfo ?: "Semester Ganjil 2026/2027",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Tahun Pelajaran 2026/2027 • SD Kabupaten Bogor",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Filter Chips Bar
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("kalender_filter_chips"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filterOptions) { opt ->
                        FilterChip(
                            selected = (selectedFilter == opt),
                            onClick = { selectedFilter = opt },
                            label = { Text(opt) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GreenPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = GreenPrimary
                            ),
                            modifier = Modifier.testTag("chip_filter_$opt")
                        )
                    }
                }
            }

            // Section 1: 📌 LIBUR NASIONAL
            if (selectedFilter == "Semua" || selectedFilter == "Libur") {
                item {
                    KalenderSection(
                        title = "📌 LIBUR NASIONAL",
                        items = liburNasional,
                        accentColor = Color(0xFFD32F2F),
                        testTag = "section_libur_nasional",
                        onEdit = { e ->
                            editingEvent = e
                            showFormDialog = true
                        },
                        onDelete = { e ->
                            deletingEvent = e
                        }
                    )
                }
            }

            // Section 2: 📌 LIBUR SEMESTER & KHUSUS
            if (selectedFilter == "Semua" || selectedFilter == "Libur") {
                item {
                    KalenderSection(
                        title = "📌 LIBUR SEMESTER",
                        items = liburSemester,
                        accentColor = Color(0xFFF57C00),
                        testTag = "section_libur_semester",
                        onEdit = { e ->
                            editingEvent = e
                            showFormDialog = true
                        },
                        onDelete = { e ->
                            deletingEvent = e
                        }
                    )
                }
            }

            // Section 3: 📌 KEGIATAN SEKOLAH
            if (selectedFilter == "Semua" || selectedFilter == "Kegiatan") {
                item {
                    KalenderSection(
                        title = "📌 KEGIATAN SEKOLAH",
                        items = kegiatanSekolah,
                        accentColor = Color(0xFF1976D2),
                        testTag = "section_kegiatan_sekolah",
                        onEdit = { e ->
                            editingEvent = e
                            showFormDialog = true
                        },
                        onDelete = { e ->
                            deletingEvent = e
                        }
                    )
                }
            }

            // Section 4: 📌 UJIAN
            if (selectedFilter == "Semua" || selectedFilter == "Ujian") {
                item {
                    KalenderSection(
                        title = "📌 UJIAN",
                        items = ujianDanRapor,
                        accentColor = GreenPrimary,
                        testTag = "section_ujian",
                        onEdit = { e ->
                            editingEvent = e
                            showFormDialog = true
                        },
                        onDelete = { e ->
                            deletingEvent = e
                        }
                    )
                }
            }
        }
    }

    // Dialog Tambah / Edit Event
    if (showFormDialog) {
        TambahEventDialog(
            eventToEdit = editingEvent,
            onDismiss = {
                showFormDialog = false
                editingEvent = null
            },
            onSave = { entity ->
                val isEdit = editingEvent != null
                viewModel.saveKalender(entity, isEdit) {
                    showFormDialog = false
                    editingEvent = null
                }
            }
        )
    }

    // Dialog Konfirmasi Hapus Event
    if (deletingEvent != null) {
        val target = deletingEvent!!
        AlertDialog(
            onDismissRequest = { deletingEvent = null },
            title = { Text("Hapus Agenda Kalender?") },
            text = {
                Text("Apakah Anda yakin ingin menghapus agenda \"${target.keterangan}\"?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteKalender(target) {
                            Toast.makeText(context, "Event \"${target.keterangan}\" dihapus", Toast.LENGTH_SHORT).show()
                        }
                        deletingEvent = null
                    },
                    modifier = Modifier.testTag("btn_confirm_hapus_event")
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { deletingEvent = null },
                    modifier = Modifier.testTag("btn_cancel_hapus_event")
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KalenderSection(
    title: String,
    items: List<KalenderEntity>,
    accentColor: Color,
    testTag: String,
    onEdit: (KalenderEntity) -> Unit,
    onDelete: (KalenderEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada jadwal untuk kategori ini",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    items.forEachIndexed { index, item ->
                        var showContextMenu by remember { mutableStateOf(false) }
                        val dateText = formatKalenderDate(item)

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { onEdit(item) },
                                        onLongClick = { showContextMenu = true }
                                    )
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("event_item_${item.id}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = accentColor,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "$dateText — ${item.keterangan}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Edit icon (pensil) di kanan
                                IconButton(
                                    onClick = { onEdit(item) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("btn_edit_event_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Event",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Context menu popup on long press
                            DropdownMenu(
                                expanded = showContextMenu,
                                onDismissRequest = { showContextMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = GreenPrimary)
                                    },
                                    onClick = {
                                        showContextMenu = false
                                        onEdit(item)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Hapus", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    },
                                    onClick = {
                                        showContextMenu = false
                                        onDelete(item)
                                    }
                                )
                            }
                        }

                        if (index < items.size - 1) {
                            HorizontalDivider(
                                thickness = 0.6.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Format date like:
 * "17 Agu 2026"
 * "15-17 Jul 2026"
 * "30 Nov - 11 Des 2026"
 */
fun formatKalenderDate(entity: KalenderEntity): String {
    if (entity.tanggal.isNotBlank()) {
        return formatSingleDate(entity.tanggal)
    }

    if (entity.tanggalMulai.isNotBlank() && entity.tanggalSelesai.isNotBlank()) {
        return formatRangeDate(entity.tanggalMulai, entity.tanggalSelesai)
    }

    if (entity.tanggalMulai.isNotBlank()) {
        return formatSingleDate(entity.tanggalMulai)
    }

    return ""
}

private val monthNames = listOf(
    "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
    "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
)

private fun formatSingleDate(dateStr: String): String {
    val parts = dateStr.trim().split("-")
    if (parts.size == 3) {
        val year = parts[0]
        val monthIdx = (parts[1].toIntOrNull() ?: 1) - 1
        val day = parts[2].toIntOrNull() ?: parts[2]
        val monthName = if (monthIdx in 0..11) monthNames[monthIdx] else parts[1]
        return "$day $monthName $year"
    }
    return dateStr
}

private fun formatRangeDate(startStr: String, endStr: String): String {
    val pStart = startStr.trim().split("-")
    val pEnd = endStr.trim().split("-")

    if (pStart.size == 3 && pEnd.size == 3) {
        val yStart = pStart[0]
        val mStartIdx = (pStart[1].toIntOrNull() ?: 1) - 1
        val dStart = pStart[2].toIntOrNull() ?: pStart[2]

        val yEnd = pEnd[0]
        val mEndIdx = (pEnd[1].toIntOrNull() ?: 1) - 1
        val dEnd = pEnd[2].toIntOrNull() ?: pEnd[2]

        val mStartName = if (mStartIdx in 0..11) monthNames[mStartIdx] else pStart[1]
        val mEndName = if (mEndIdx in 0..11) monthNames[mEndIdx] else pEnd[1]

        return when {
            yStart == yEnd && mStartIdx == mEndIdx -> "$dStart-$dEnd $mStartName $yStart"
            yStart == yEnd -> "$dStart $mStartName - $dEnd $mEndName $yStart"
            else -> "$dStart $mStartName $yStart - $dEnd $mEndName $yEnd"
        }
    }
    return "$startStr - $endStr"
}
