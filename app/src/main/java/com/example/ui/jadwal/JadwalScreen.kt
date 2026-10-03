package com.example.ui.jadwal

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
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
import com.example.data.local.JadwalEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.GreenPrimaryDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JadwalScreen(
    viewModel: MainViewModel,
    showTopBar: Boolean = true
) {
    val context = LocalContext.current
    val isPremium = com.example.util.PremiumManager.isPremium(context)
    val allJadwal by viewModel.allJadwal.collectAsState()
    val todayName = viewModel.getTodayDayName()

    val daysOfWeek = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")

    var showFormDialog by remember { mutableStateOf(false) }
    var editingJadwal by remember { mutableStateOf<JadwalEntity?>(null) }
    var deletingJadwal by remember { mutableStateOf<JadwalEntity?>(null) }

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Jadwal",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Jadwal Mengajar",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GreenPrimary
                ),
                modifier = Modifier.testTag("jadwal_top_app_bar")
            )
            }
        },
        floatingActionButton = {
            if (isPremium) {
                FloatingActionButton(
                    onClick = {
                        editingJadwal = null
                        showFormDialog = true
                    },
                    containerColor = GreenPrimary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("fab_tambah_jadwal")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah Jadwal",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("jadwal_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(daysOfWeek) { day ->
                val isToday = day.equals(todayName, ignoreCase = true)
                val jadwalHari = allJadwal
                    .filter { it.hari.equals(day, ignoreCase = true) }
                    .sortedBy { it.jamMulai }

                DayScheduleSection(
                    day = day,
                    isToday = isToday,
                    jadwalList = jadwalHari,
                    onEdit = { j ->
                        editingJadwal = j
                        showFormDialog = true
                    },
                    onDelete = { j ->
                        deletingJadwal = j
                    }
                )
            }
        }
    }

    // Dialog Tambah / Edit Jadwal
    if (showFormDialog) {
        TambahJadwalDialog(
            jadwalToEdit = editingJadwal,
            onDismiss = {
                showFormDialog = false
                editingJadwal = null
            },
            onSave = { entity ->
                val isEdit = editingJadwal != null
                viewModel.saveJadwal(entity, isEdit) {
                    showFormDialog = false
                    editingJadwal = null
                }
            }
        )
    }

    // Dialog Konfirmasi Hapus Jadwal
    if (deletingJadwal != null) {
        val target = deletingJadwal!!
        AlertDialog(
            onDismissRequest = { deletingJadwal = null },
            title = { Text("Hapus Jadwal?") },
            text = {
                Text("Apakah Anda yakin ingin menghapus jadwal ${target.mapel} (Kelas ${target.kelas}) pada hari ${target.hari}?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteJadwal(target) {
                            Toast.makeText(context, "Jadwal ${target.mapel} dihapus", Toast.LENGTH_SHORT).show()
                        }
                        deletingJadwal = null
                    },
                    modifier = Modifier.testTag("btn_confirm_hapus_jadwal")
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { deletingJadwal = null },
                    modifier = Modifier.testTag("btn_cancel_hapus_jadwal")
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DayScheduleSection(
    day: String,
    isToday: Boolean,
    jadwalList: List<JadwalEntity>,
    onEdit: (JadwalEntity) -> Unit,
    onDelete: (JadwalEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("section_jadwal_$day")
    ) {
        // Header Hari
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = day.uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = if (isToday) GreenPrimary else MaterialTheme.colorScheme.onSurface
            )

            if (isToday) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GreenPrimary,
                    modifier = Modifier.testTag("badge_hari_ini")
                ) {
                    Text(
                        text = "HARI INI",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Card Container
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isToday) {
                        Modifier.border(
                            width = 2.dp,
                            color = GreenPrimary,
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else Modifier
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isToday) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isToday) 4.dp else 1.dp)
        ) {
            if (jadwalList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada jadwal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    jadwalList.forEachIndexed { index, jadwal ->
                        var showContextMenu by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min)
                                    .combinedClickable(
                                        onClick = { onEdit(jadwal) },
                                        onLongClick = { showContextMenu = true }
                                    )
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("jadwal_item_${jadwal.id}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Sisi Kiri: Waktu (07:35 - 09:20)
                                Row(
                                    modifier = Modifier.weight(0.38f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isToday) GreenPrimary else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${jadwal.jamMulai} - ${jadwal.jamSelesai}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Garis Pembatas Vertikal
                                VerticalDivider(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(horizontal = 8.dp),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )

                                // Sisi Tengah: Mapel & Kelas
                                Column(
                                    modifier = Modifier
                                        .weight(0.50f)
                                        .padding(start = 4.dp)
                                ) {
                                    Text(
                                        text = jadwal.mapel,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (isToday) GreenPrimaryDark else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isToday) GreenPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                    ) {
                                        Text(
                                            text = "Kelas ${jadwal.kelas}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = if (isToday) GreenPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                // Sisi Kanan: Edit Icon (Pensil)
                                IconButton(
                                    onClick = { onEdit(jadwal) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("btn_edit_jadwal_${jadwal.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Jadwal",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Context Menu Popup on Long Press
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
                                        onEdit(jadwal)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Hapus", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    },
                                    onClick = {
                                        showContextMenu = false
                                        onDelete(jadwal)
                                    }
                                )
                            }
                        }

                        if (index < jadwalList.size - 1) {
                            HorizontalDivider(
                                thickness = 0.8.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}
