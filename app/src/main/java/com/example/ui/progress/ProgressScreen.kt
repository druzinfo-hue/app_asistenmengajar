package com.example.ui.progress

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.MateriEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.GreenContainer
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OnGreenContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    viewModel: MainViewModel,
    showTopBar: Boolean = true
) {
    val allMateri by viewModel.allMateri.collectAsState()
    val selectedKelas by viewModel.selectedProgressKelas.collectAsState()
    val selectedMapel by viewModel.selectedProgressMapel.collectAsState()

    var selectedDetailMateri by remember { mutableStateOf<MateriEntity?>(null) }
    var showResetDialog by remember { mutableStateOf<String?>(null) } // mapel name to reset
    var showRewindDialog by remember { mutableStateOf<MateriEntity?>(null) }

    // Filter available mapel for selected class
    val materiForKelas = remember(allMateri, selectedKelas) {
        allMateri.filter { it.kelas == selectedKelas }
    }
    val availableMapels = remember(materiForKelas) {
        listOf("Semua") + materiForKelas.map { it.mapel }.distinct().sorted()
    }

    // Filter displayed materi
    val displayedMateri = remember(materiForKelas, selectedMapel) {
        if (selectedMapel == null || selectedMapel == "Semua") {
            materiForKelas.sortedWith(compareBy({ it.mapel }, { it.pertemuan }))
        } else {
            materiForKelas.filter { it.mapel.equals(selectedMapel, ignoreCase = true) }
                .sortedBy { it.pertemuan }
        }
    }

    val totalMeetings = displayedMateri.size
    val completedMeetings = displayedMateri.count { it.status == "selesai" }
    val progressFraction = if (totalMeetings > 0) completedMeetings.toFloat() / totalMeetings else 0f
    val progressPercent = (progressFraction * 100).toInt()

    // Reset Confirmation Dialog
    showResetDialog?.let { mapelToReset ->
        AlertDialog(
            onDismissRequest = { showResetDialog = null },
            title = { Text("Reset Progress Materi?") },
            text = {
                Text(
                    if (mapelToReset == "Semua")
                        "Apakah Anda yakin ingin mereset progress semua mapel untuk Kelas $selectedKelas?"
                    else
                        "Apakah Anda yakin ingin mereset progress $mapelToReset Kelas $selectedKelas menjadi belum selesai?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (mapelToReset == "Semua") {
                            availableMapels.filter { it != "Semua" }.forEach {
                                viewModel.resetAllMateri(selectedKelas, it)
                            }
                        } else {
                            viewModel.resetAllMateri(selectedKelas, mapelToReset)
                        }
                        showResetDialog = null
                    }
                ) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Rewind Confirmation Dialog
    showRewindDialog?.let { targetMateri ->
        AlertDialog(
            onDismissRequest = { showRewindDialog = null },
            title = { Text("Mundur ke Pertemuan ${targetMateri.pertemuan}?") },
            text = {
                Text("Semua pertemuan setelah Pertemuan ${targetMateri.pertemuan} (${targetMateri.mapel}) akan ditandai kembali menjadi belum selesai.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetMateriAfter(targetMateri.kelas, targetMateri.mapel, targetMateri.pertemuan)
                        viewModel.markMateriCompleted(targetMateri.id, true)
                        showRewindDialog = null
                        selectedDetailMateri = null
                    }
                ) {
                    Text("Terapkan", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRewindDialog = null }) {
                    Text("Batal")
                }
            }
        )
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (m.status == "selesai") GreenContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Pertemuan ${m.pertemuan}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (m.status == "selesai") OnGreenContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${m.mapel} • Kls ${m.kelas}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = m.judul,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                if (m.bab.isNotBlank()) {
                    Text(
                        text = m.bab,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                DetailItemRow(label = "Tujuan Pembelajaran", content = m.tujuan.ifBlank { "-" })
                Spacer(modifier = Modifier.height(10.dp))
                DetailItemRow(label = "Bahan & Alat", content = m.bahan.ifBlank { "-" })
                Spacer(modifier = Modifier.height(10.dp))
                DetailItemRow(label = "Alokasi Waktu", content = "${m.alokasiJp} Jam Pelajaran (JP)")
                Spacer(modifier = Modifier.height(10.dp))
                DetailItemRow(
                    label = "Status",
                    content = if (m.status == "selesai") "✅ Selesai ${m.tanggalSelesai?.let { "($it)" } ?: ""}" else "⏳ Belum Dilaksanakan"
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.toggleMateriStatus(m)
                            selectedDetailMateri = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (m.status == "selesai") MaterialTheme.colorScheme.outline else GreenPrimary
                        )
                    ) {
                        Text(if (m.status == "selesai") "Batalkan Selesai" else "Tandai Selesai")
                    }

                    OutlinedButton(
                        onClick = { showRewindDialog = m },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mundur ke Sini", fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Progress Materi Kurikulum",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { showResetDialog = selectedMapel ?: "Semua" },
                            modifier = Modifier.testTag("reset_progress_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Progress",
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
                .testTag("progress_screen")
        ) {
            // 1. FILTER KELAS CHIPS (1-6)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf(1, 2, 3, 4, 5, 6)) { k ->
                    FilterChip(
                        selected = selectedKelas == k,
                        onClick = {
                            viewModel.setSelectedProgressKelas(k)
                            viewModel.setSelectedProgressMapel("Semua")
                        },
                        label = { Text("Kelas $k", fontWeight = if (selectedKelas == k) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // 2. FILTER MAPEL CHIPS
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableMapels) { mapelName ->
                    val isSelected = (selectedMapel == mapelName) || (selectedMapel == null && mapelName == "Semua")
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setSelectedProgressMapel(mapelName) },
                        label = { Text(mapelName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // 3. PROGRESS HEADER CARD
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pencapaian ${selectedMapel ?: "Semua Mapel"} (Kelas $selectedKelas)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$completedMeetings/$totalMeetings Selesai ($progressPercent%)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = GreenPrimary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            // 4. MEETINGS LIST
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 20.dp, top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (displayedMateri.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Text(
                                text = "Belum ada daftar pertemuan untuk kelas dan mata pelajaran ini.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(displayedMateri, key = { it.id }) { materi ->
                    MateriMeetingCard(
                        materi = materi,
                        onToggle = { viewModel.toggleMateriStatus(materi) },
                        onTap = { selectedDetailMateri = materi }
                    )
                }
            }
        }
    }
}

@Composable
fun MateriMeetingCard(
    materi: MateriEntity,
    onToggle: () -> Unit,
    onTap: () -> Unit
) {
    val isCompleted = materi.status == "selesai"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) GreenContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox Icon button
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = if (isCompleted) "Selesai" else "Belum",
                    tint = if (isCompleted) GreenPrimary else Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isCompleted) GreenPrimary else MaterialTheme.colorScheme.secondary
                    ) {
                        Text(
                            text = "P${materi.pertemuan}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "${materi.mapel} • ${materi.alokasiJp} JP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = materi.judul,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (materi.bab.isNotBlank()) {
                    Text(
                        text = materi.bab,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Detail",
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun DetailItemRow(label: String, content: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = com.example.util.LatexRenderer.render(content),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
