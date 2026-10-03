package com.example.ui.setup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.GreenPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupAwalScreen(
    viewModel: MainViewModel,
    onSetupComplete: () -> Unit
) {
    // Kalau user back button: tidak bisa kembali ke onboarding, tetap di Setup Awal
    BackHandler(enabled = true) {
        // Tetap di Setup Awal (do nothing on back)
    }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var nama by remember { mutableStateOf("") }
    var namaError by remember { mutableStateOf(false) }

    val jabatanOptions = listOf("Guru Kelas", "Guru Mapel", "Kepala Sekolah")
    var selectedJabatan by remember { mutableStateOf("Guru Kelas") }

    var namaSekolah by remember { mutableStateOf("") }

    val allKelas = listOf(1, 2, 3, 4, 5, 6)
    var selectedKelas by remember { mutableStateOf(emptySet<Int>()) }
    var kelasError by remember { mutableStateOf(false) }

    var selectedKelasUtama by remember { mutableStateOf<Int?>(null) }
    var isKelasDropdownExpanded by remember { mutableStateOf(false) }

    var namaAi by remember { mutableStateOf("Cici") }
    var namaAiError by remember { mutableStateOf(false) }

    var sapaanPembuka by remember { mutableStateOf("Halo! Siap membantu.") }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier
            .fillMaxSize()
            .testTag("setup_awal_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            // Header Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = GreenPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "🎓 Selamat Datang!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Isi data berikut untuk memulai:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Form Content Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // 1. Nama Anda
                    Text(
                        text = "Nama Anda:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = nama,
                        onValueChange = {
                            nama = it
                            if (namaError && it.isNotBlank()) namaError = false
                        },
                        placeholder = { Text("Contoh: Bu Sari") },
                        singleLine = true,
                        isError = namaError,
                        supportingText = {
                            if (namaError) {
                                Text("Nama tidak boleh kosong", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_setup_nama")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Jabatan
                    Text(
                        text = "Jabatan:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column {
                        jabatanOptions.forEach { jabatan ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedJabatan = jabatan }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = (selectedJabatan == jabatan),
                                    onClick = { selectedJabatan = jabatan },
                                    colors = RadioButtonDefaults.colors(selectedColor = GreenPrimary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = jabatan, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. Nama Sekolah
                    Text(
                        text = "Nama Sekolah:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = namaSekolah,
                        onValueChange = { namaSekolah = it },
                        placeholder = { Text("Contoh: SDN Sukamaju 01") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_setup_sekolah")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. Kelas yang Diampu
                    Text(
                        text = "Kelas yang Diampu:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (kelasError) {
                        Text(
                            text = "Minimal 1 kelas harus dipilih",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        allKelas.forEach { k ->
                            val isChecked = selectedKelas.contains(k)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val updated = selectedKelas.toMutableSet()
                                        if (isChecked) {
                                            updated.remove(k)
                                        } else {
                                            updated.add(k)
                                        }
                                        selectedKelas = updated
                                        if (updated.isNotEmpty()) kelasError = false
                                        if (selectedKelasUtama == null && updated.isNotEmpty()) {
                                            selectedKelasUtama = updated.firstOrNull()
                                        } else if (!updated.contains(selectedKelasUtama)) {
                                            selectedKelasUtama = updated.firstOrNull()
                                        }
                                    }
                                    .padding(end = 8.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        val updated = selectedKelas.toMutableSet()
                                        if (checked) {
                                            updated.add(k)
                                        } else {
                                            updated.remove(k)
                                        }
                                        selectedKelas = updated
                                        if (updated.isNotEmpty()) kelasError = false
                                        if (selectedKelasUtama == null && updated.isNotEmpty()) {
                                            selectedKelasUtama = updated.firstOrNull()
                                        } else if (!updated.contains(selectedKelasUtama)) {
                                            selectedKelasUtama = updated.firstOrNull()
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = GreenPrimary)
                                )
                                Text(text = "Kelas $k", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. Kelas Utama (prioritas dashboard)
                    Text(
                        text = "Kelas Utama (prioritas dashboard):",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            onClick = { isKelasDropdownExpanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dropdown_setup_kelas_utama")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (selectedKelasUtama != null) "Kelas $selectedKelasUtama" else "Pilih Kelas Utama",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = if (selectedKelasUtama != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(
                            expanded = isKelasDropdownExpanded,
                            onDismissRequest = { isKelasDropdownExpanded = false }
                        ) {
                            val availableKelas = if (selectedKelas.isNotEmpty()) selectedKelas.sorted() else allKelas
                            availableKelas.forEach { k ->
                                DropdownMenuItem(
                                    text = { Text("Kelas $k") },
                                    onClick = {
                                        selectedKelasUtama = k
                                        if (!selectedKelas.contains(k)) {
                                            selectedKelas = selectedKelas + k
                                            kelasError = false
                                        }
                                        isKelasDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 6. Nama Asisten AI
                    Text(
                        text = "Nama Asisten AI:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = namaAi,
                        onValueChange = {
                            namaAi = it
                            if (namaAiError && it.isNotBlank()) namaAiError = false
                        },
                        placeholder = { Text("Cici") },
                        singleLine = true,
                        isError = namaAiError,
                        supportingText = {
                            if (namaAiError) {
                                Text("Nama Asisten AI tidak boleh kosong", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_setup_nama_ai")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 7. Sapaan Pembuka
                    Text(
                        text = "Sapaan Pembuka:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = sapaanPembuka,
                        onValueChange = { sapaanPembuka = it },
                        placeholder = { Text("Halo! Siap membantu.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_setup_greeting")
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Submit Button: [Mulai Gunakan]
                    Button(
                        onClick = {
                            val trimmedName = nama.trim()
                            val trimmedAiName = namaAi.trim()

                            var isValid = true
                            if (trimmedName.isEmpty()) {
                                namaError = true
                                isValid = false
                            }
                            if (selectedKelas.isEmpty()) {
                                kelasError = true
                                isValid = false
                            }
                            if (trimmedAiName.isEmpty()) {
                                namaAiError = true
                                isValid = false
                            }

                            if (!isValid) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Harap isi nama Anda, pilih minimal 1 kelas, dan isi nama asisten AI.")
                                }
                                return@Button
                            }

                            val cleanGreeting = sapaanPembuka.trim().ifBlank { "Halo! Siap membantu." }
                            val cleanSchool = namaSekolah.trim().ifBlank { "SD" }
                            val finalKelasUtama = selectedKelasUtama ?: selectedKelas.firstOrNull() ?: 4

                            // 1. Simpan ke Room DB & SharedPreferences via ViewModel
                            viewModel.saveInitialSetup(
                                name = trimmedName,
                                role = selectedJabatan,
                                school = cleanSchool,
                                kelasUtama = finalKelasUtama.toString(),
                                kelasList = selectedKelas.toList(),
                                aiName = trimmedAiName,
                                aiGreeting = cleanGreeting
                            )

                            // 4. Navigasi ke Beranda (TIDAK ada halaman donasi di sini)
                            onSetupComplete()
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_mulai_gunakan")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mulai Gunakan",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
