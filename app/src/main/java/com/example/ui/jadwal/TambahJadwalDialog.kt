package com.example.ui.jadwal

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.JadwalEntity
import com.example.ui.theme.GreenPrimary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TambahJadwalDialog(
    jadwalToEdit: JadwalEntity? = null,
    onDismiss: () -> Unit,
    onSave: (JadwalEntity) -> Unit
) {
    val context = LocalContext.current
    val isEdit = jadwalToEdit != null

    val hariList = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
    val kelasList = listOf("Kelas 1", "Kelas 2", "Kelas 3", "Kelas 4", "Kelas 5", "Kelas 6")

    var hari by remember { mutableStateOf(jadwalToEdit?.hari ?: "Senin") }
    var kelas by remember { mutableStateOf("Kelas ${jadwalToEdit?.kelas ?: 4}") }

    var jamMulai by remember { mutableStateOf(jadwalToEdit?.jamMulai ?: "07:35") }
    var jamSelesai by remember { mutableStateOf(jadwalToEdit?.jamSelesai ?: "09:20") }

    var mapelText by remember { mutableStateOf(jadwalToEdit?.mapel ?: "") }
    var jpText by remember { mutableStateOf("2") }

    val commonMapelList = listOf(
        "Bahasa Indonesia",
        "Matematika",
        "IPAS",
        "Pendidikan Pancasila",
        "Bahasa Inggris",
        "Seni Rupa",
        "PJOK",
        "PAI & BP",
        "Bahasa Sunda"
    )

    fun showTimePicker(initialTime: String, onTimeSelected: (String) -> Unit) {
        val parts = initialTime.split(":")
        val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 7
        val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 30

        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val formatted = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
                onTimeSelected(formatted)
            },
            initialHour,
            initialMinute,
            true
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("tambah_jadwal_dialog"),
        title = {
            Text(
                text = if (isEdit) "Edit Jadwal Mengajar" else "Tambah Jadwal Mengajar",
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
                // Dropdown Hari
                DropdownField(
                    label = "Hari *",
                    value = hari,
                    options = hariList,
                    onValueChange = { hari = it },
                    modifier = Modifier.testTag("dropdown_hari_field")
                )

                // Baris Waktu Mulai & Selesai
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Jam Mulai
                    OutlinedTextField(
                        value = jamMulai,
                        onValueChange = { jamMulai = it },
                        label = { Text("Mulai *") },
                        leadingIcon = {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.clickable {
                                showTimePicker(jamMulai) { jamMulai = it }
                            })
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_jam_mulai"),
                        singleLine = true
                    )

                    // Jam Selesai
                    OutlinedTextField(
                        value = jamSelesai,
                        onValueChange = { jamSelesai = it },
                        label = { Text("Selesai *") },
                        leadingIcon = {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.clickable {
                                showTimePicker(jamSelesai) { jamSelesai = it }
                            })
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_jam_selesai"),
                        singleLine = true
                    )
                }

                // Dropdown Kelas & Field JP
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Kelas Dropdown
                    DropdownField(
                        label = "Kelas *",
                        value = kelas,
                        options = kelasList,
                        onValueChange = { kelas = it },
                        modifier = Modifier
                            .weight(2f)
                            .testTag("dropdown_kelas_field")
                    )

                    // JP
                    OutlinedTextField(
                        value = jpText,
                        onValueChange = { jpText = it },
                        label = { Text("Alokasi JP") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_alokasi_jp"),
                        singleLine = true
                    )
                }

                // Text field Mapel
                OutlinedTextField(
                    value = mapelText,
                    onValueChange = { mapelText = it },
                    label = { Text("Mata Pelajaran *") },
                    leadingIcon = {
                        Icon(Icons.Default.Book, contentDescription = null, tint = GreenPrimary)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_mapel"),
                    placeholder = { Text("Contoh: Matematika") },
                    singleLine = true
                )

                // Autocomplete suggestion chips
                Text(
                    text = "Pilihan Cepat Mapel:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    commonMapelList.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (mapelText.equals(item, ignoreCase = true)) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.clickable {
                                mapelText = item
                            }
                        ) {
                            Text(
                                text = item,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                color = if (mapelText.equals(item, ignoreCase = true)) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedMapel = mapelText.trim()
                    val trimmedMulai = jamMulai.trim()
                    val trimmedSelesai = jamSelesai.trim()

                    // Validasi kelengkapan
                    if (trimmedMapel.isEmpty()) {
                        Toast.makeText(context, "Mata Pelajaran wajib diisi!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (trimmedMulai.isEmpty() || trimmedSelesai.isEmpty()) {
                        Toast.makeText(context, "Jam mulai dan jam selesai wajib diisi!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // Validasi jam selesai > jam mulai
                    if (trimmedSelesai <= trimmedMulai) {
                        Toast.makeText(context, "Jam selesai harus lebih akhir dari jam mulai!", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    val selectedKelasInt = kelas.replace("Kelas ", "").trim().toIntOrNull() ?: 4

                    val resultEntity = JadwalEntity(
                        id = jadwalToEdit?.id ?: 0,
                        kelas = selectedKelasInt,
                        hari = hari,
                        jamMulai = trimmedMulai,
                        jamSelesai = trimmedSelesai,
                        mapel = trimmedMapel
                    )

                    onSave(resultEntity)
                    Toast.makeText(context, "Jadwal $hari $trimmedMapel tersimpan!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                modifier = Modifier.testTag("btn_simpan_jadwal")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_batal_jadwal")
            ) {
                Text("Batal")
            }
        }
    )
}
