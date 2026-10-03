package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GreenPrimary
import com.example.util.PremiumManager
import java.net.URLEncoder

@Composable
fun UpgradeDialog(
    onDismiss: () -> Unit,
    onSuccessActivated: () -> Unit = {}
) {
    val context = LocalContext.current
    var showCodeInput by remember { mutableStateOf(false) }
    var inputCode by remember { mutableStateOf("") }
    var codeError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "🔒 Fitur Premium",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Fitur ini tersedia di versi Premium.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Dukung pengembangan app ini:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "✅ 200 chat AI/hari", style = MaterialTheme.typography.bodySmall)
                        Text(text = "✅ Semua generator unlimited", style = MaterialTheme.typography.bodySmall)
                        Text(text = "✅ Upload buku & OCR", style = MaterialTheme.typography.bodySmall)
                        Text(text = "✅ Backup/Restore", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = GreenPrimary.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Donasi sukarela:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "📱 Saweria (otomatis):\n   saweria.co/druzid",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "📷 QRIS (semua e-wallet):\n   Bisa scan di halaman Donasi",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "💬 WhatsApp:\n   0858-9407-1160",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Saweria", "https://saweria.co/druzid"))
                                    Toast.makeText(context, "Link Saweria berhasil disalin!", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Salin Link",
                                tint = GreenPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Salin Link Saweria",
                                style = MaterialTheme.typography.labelMedium,
                                color = GreenPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    text = "Setelah donasi, kirim bukti ke WA untuk dapat kode aktivasi Premium.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (showCodeInput) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = inputCode,
                            onValueChange = {
                                inputCode = it.uppercase()
                                codeError = null
                            },
                            label = { Text("Kode Aktivasi") },
                            placeholder = { Text("DRUZ-XXXX-XXXX-XXXX") },
                            singleLine = true,
                            isError = codeError != null,
                            supportingText = {
                                if (codeError != null) {
                                    Text(codeError!!, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("activation_code_input")
                        )
                        Button(
                            onClick = {
                                if (PremiumManager.activate(context, inputCode)) {
                                    Toast.makeText(context, "🎉 Selamat! Akun Premium Berhasil Diaktifkan!", Toast.LENGTH_LONG).show()
                                    onSuccessActivated()
                                    onDismiss()
                                } else {
                                    codeError = "Kode tidak valid. Hubungi WhatsApp: 0858-9407-1160"
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_verify_code"),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                        ) {
                            Text("Aktivasi Sekarang")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val message = "Halo Admin Asisten Mengajar, saya ingin konfirmasi donasi & minta kode aktivasi Premium."
                        val encoded = URLEncoder.encode(message, "UTF-8")
                        val uri = Uri.parse("https://wa.me/6285894071160?text=$encoded")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Hubungi WhatsApp: 0858-9407-1160", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_whatsapp_confirm"),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Donasi / Konfirmasi WhatsApp")
                }

                if (!showCodeInput) {
                    OutlinedButton(
                        onClick = { showCodeInput = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_have_code")
                    ) {
                        Text("Punya kode?")
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_dismiss_upgrade")
                ) {
                    Text("Nanti")
                }
            }
        },
        dismissButton = {}
    )
}
