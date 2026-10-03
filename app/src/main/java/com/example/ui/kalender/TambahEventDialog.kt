package com.example.ui.kalender

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.KalenderEntity
import com.example.ui.theme.GreenPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDropdownField(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TambahEventDialog(
    eventToEdit: KalenderEntity? = null,
    onDismiss: () -> Unit,
    onSave: (KalenderEntity) -> Unit
) {
    val context = LocalContext.current
    val isEdit = eventToEdit != null

    // Kategori: "Hari Libur", "Kegiatan Sekolah", "Ujian"
    val kategoriOptions = listOf("Hari Libur", "Kegiatan Sekolah", "Ujian")
    val initialKategori = when {
        eventToEdit?.jenis?.startsWith("libur") == true -> "Hari Libur"
        eventToEdit?.jenis == "ujian" || eventToEdit?.jenis == "rapor" -> "Ujian"
        else -> "Kegiatan Sekolah"
    }
    var selectedKategori by remember { mutableStateOf(if (isEdit) initialKategori else "Hari Libur") }

    var keteranganText by remember { mutableStateOf(eventToEdit?.keterangan ?: "") }

    // Rentang tanggal
    val isRangeInitial = eventToEdit?.tanggalMulai?.isNotBlank() == true && eventToEdit.tanggalSelesai.isNotBlank()
    var isRange by remember { mutableStateOf(isRangeInitial) }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var singleTanggal by remember { mutableStateOf(eventToEdit?.tanggal?.ifBlank { todayStr } ?: todayStr) }
    var tanggalMulai by remember { mutableStateOf(eventToEdit?.tanggalMulai?.ifBlank { todayStr } ?: todayStr) }
    var tanggalSelesai by remember { mutableStateOf(eventToEdit?.tanggalSelesai?.ifBlank { todayStr } ?: todayStr) }

    // Jenis Libur: Libur Nasional, Libur Semester, Libur Keagamaan, Libur Khusus
    val jenisLiburMap = listOf(
        "Libur Nasional" to "libur_nasional",
        "Libur Semester" to "libur_semester",
        "Libur Keagamaan" to "libur_keagamaan",
        "Libur Khusus" to "libur_khusus"
    )
    val jenisLiburOptions = jenisLiburMap.map { it.first }
    val initialJenisLiburLabel = jenisLiburMap.firstOrNull { it.second == eventToEdit?.jenis }?.first ?: "Libur Nasional"
    var selectedJenisLiburLabel by remember { mutableStateOf(initialJenisLiburLabel) }

    fun showDatePicker(initialDate: String, onDateSelected: (String) -> Unit) {
        val parts = initialDate.split("-")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
        val m = (parts.getOrNull(1)?.toIntOrNull() ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)) - 1
        val d = parts.getOrNull(2)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formatted = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                onDateSelected(formatted)
            },
            y,
            m,
            d
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("tambah_event_dialog"),
        title = {
            Text(
                text = if (isEdit) "Edit Agenda Kalender" else "Tambah Agenda Kalender",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Dropdown Kategori
                EventDropdownField(
                    label = "Kategori Agenda *",
                    value = selectedKategori,
                    options = kategoriOptions,
                    onValueChange = { selectedKategori = it },
                    modifier = Modifier.testTag("dropdown_kategori_agenda")
                )

                // Sub-Dropdown Jenis Libur jika kategori = "Hari Libur"
                if (selectedKategori == "Hari Libur") {
                    EventDropdownField(
                        label = "Jenis Libur *",
                        value = selectedJenisLiburLabel,
                        options = jenisLiburOptions,
                        onValueChange = { selectedJenisLiburLabel = it },
                        modifier = Modifier.testTag("dropdown_jenis_libur")
                    )
                }

                // Text field Keterangan
                OutlinedTextField(
                    value = keteranganText,
                    onValueChange = { keteranganText = it },
                    label = { Text("Keterangan Agenda *") },
                    leadingIcon = {
                        Icon(Icons.Default.Description, contentDescription = null, tint = GreenPrimary)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_keterangan_event"),
                    placeholder = { Text("Contoh: Libur Hari Guru Nasional") }
                )

                // Checkbox Rentang Tanggal
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isRange = !isRange },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isRange,
                        onCheckedChange = { isRange = it },
                        modifier = Modifier.testTag("checkbox_rentang_tanggal")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Rentang beberapa hari (Mulai s/d Selesai)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Date Picker Fields
                if (!isRange) {
                    // Single Date
                    OutlinedTextField(
                        value = singleTanggal,
                        onValueChange = { singleTanggal = it },
                        label = { Text("Tanggal *") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = "Pilih Tanggal",
                                tint = GreenPrimary,
                                modifier = Modifier.clickable {
                                    showDatePicker(singleTanggal) { singleTanggal = it }
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showDatePicker(singleTanggal) { singleTanggal = it }
                            }
                            .testTag("input_tanggal_single"),
                        singleLine = true
                    )
                } else {
                    // Range Date (Mulai + Selesai)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = tanggalMulai,
                            onValueChange = { tanggalMulai = it },
                            label = { Text("Mulai *") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = GreenPrimary,
                                    modifier = Modifier.clickable {
                                        showDatePicker(tanggalMulai) { tanggalMulai = it }
                                    }
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_tanggal_mulai"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = tanggalSelesai,
                            onValueChange = { tanggalSelesai = it },
                            label = { Text("Selesai *") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = GreenPrimary,
                                    modifier = Modifier.clickable {
                                        showDatePicker(tanggalSelesai) { tanggalSelesai = it }
                                    }
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_tanggal_selesai"),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedKet = keteranganText.trim()
                    if (trimmedKet.isEmpty()) {
                        Toast.makeText(context, "Keterangan agenda wajib diisi!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val finalJenis = when (selectedKategori) {
                        "Hari Libur" -> jenisLiburMap.firstOrNull { it.first == selectedJenisLiburLabel }?.second ?: "libur_nasional"
                        "Ujian" -> "ujian"
                        else -> "kegiatan"
                    }

                    val entity = if (!isRange) {
                        KalenderEntity(
                            id = eventToEdit?.id ?: 0L,
                            tanggal = singleTanggal.trim(),
                            tanggalMulai = "",
                            tanggalSelesai = "",
                            keterangan = trimmedKet,
                            jenis = finalJenis
                        )
                    } else {
                        KalenderEntity(
                            id = eventToEdit?.id ?: 0L,
                            tanggal = "",
                            tanggalMulai = tanggalMulai.trim(),
                            tanggalSelesai = tanggalSelesai.trim(),
                            keterangan = trimmedKet,
                            jenis = finalJenis
                        )
                    }

                    onSave(entity)
                    Toast.makeText(context, "Event tersimpan!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                modifier = Modifier.testTag("btn_simpan_event")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_batal_event")
            ) {
                Text("Batal")
            }
        }
    )
}
