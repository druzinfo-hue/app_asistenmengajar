package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GreenPrimary
import com.example.util.PremiumManager

@Composable
fun PremiumLockDialog(
    featureName: String = "Fitur Premium",
    onDismiss: () -> Unit,
    onDonasi: () -> Unit,
    onInputKode: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("premium_lock_dialog"),
        title = { 
            Text("🔒 Fitur Premium")  // BUKAN "Limit Chat Habis"
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(
                    rememberScrollState()
                )
            ) {
                Text("Fitur \"$featureName\" tersedia di versi Premium.")
                Spacer(Modifier.height(12.dp))
                
                Text(
                    "Dukung pengembangan app ini:",
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text("✅ 200 chat AI/hari")
                Text("✅ Semua generator unlimited")
                Text("✅ Upload buku & OCR")
                Text("✅ Backup/Restore")
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    "Donasi sukarela:",
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text("📱 Saweria (otomatis):")
                Text("   saweria.co/druzid")
                Spacer(Modifier.height(4.dp))
                Text("📷 QRIS (semua e-wallet):")
                Text("   Bisa scan di halaman Donasi")
                Spacer(Modifier.height(4.dp))
                Text("💬 WhatsApp:")
                Text("   0858-9407-1160")
                
                Spacer(Modifier.height(12.dp))
                Text(
                    "Setelah donasi, kirim bukti ke WA untuk dapat kode aktivasi Premium.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDonasi,
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                modifier = Modifier.testTag("btn_donasi_sekarang")
            ) {
                Text("Donasi Sekarang")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onInputKode,
                    modifier = Modifier.testTag("btn_punya_kode")
                ) {
                    Text("Punya kode?")
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_tutup_dialog")
                ) {
                    Text("Nanti")
                }
            }
        }
    )
}

@Composable
fun InputKodeDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var kode by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("input_kode_dialog"),
        title = { Text("🔑 Input Kode Aktivasi") },
        text = {
            Column {
                Text("Masukkan kode aktivasi Premium:")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = kode,
                    onValueChange = { 
                        kode = it.uppercase()
                        errorMsg = ""
                    },
                    placeholder = { Text("DRUZ-XXXX-XXXX-XXXX") },
                    isError = errorMsg.isNotEmpty(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("activation_code_text_field")
                )
                if (errorMsg.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(errorMsg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (PremiumManager.activate(context, kode)) {
                        onSuccess()
                    } else {
                        errorMsg = "Kode tidak valid"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                modifier = Modifier.testTag("btn_submit_activation_code")
            ) {
                Text("Aktivasi")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cancel_activation")
            ) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun InputActivationCodeDialog(
    onDismiss: () -> Unit,
    onSuccessActivated: () -> Unit
) {
    InputKodeDialog(
        onDismiss = onDismiss,
        onSuccess = onSuccessActivated
    )
}
