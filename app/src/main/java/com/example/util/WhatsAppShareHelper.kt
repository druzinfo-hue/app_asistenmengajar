package com.example.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast

object WhatsAppShareHelper {
    fun shareToWhatsApp(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage("com.whatsapp")
        }

        try {
            context.startActivity(
                Intent.createChooser(intent, "Bagikan ke WhatsApp")
            )
        } catch (e: ActivityNotFoundException) {
            // WhatsApp tidak terinstall, fallback ke share sheet umum
            val generalIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            try {
                context.startActivity(
                    Intent.createChooser(generalIntent, "Bagikan Pengumuman")
                )
            } catch (err: Exception) {
                Toast.makeText(context, "Gagal membagikan pengumuman: ${err.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openWhatsAppSupport(context: Context) {
        val phoneNumber = "6285894071160"
        val message = "Halo Admin, saya butuh bantuan dengan aplikasi Asisten Mengajar."

        try {
            val uri = android.net.Uri.parse(
                "https://wa.me/$phoneNumber?text=" +
                java.net.URLEncoder.encode(message, "UTF-8")
            )
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "WhatsApp tidak terinstall",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
