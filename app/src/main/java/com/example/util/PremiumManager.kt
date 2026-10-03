package com.example.util

import android.content.Context

object KodeVerifier {
    fun verify(code: String): Boolean {
        val cleanCode = code.trim().uppercase()
        val validCodes = listOf(
            "DRUZ-PREMIUM",
            "DRUZ-PREMIUM-2026",
            "GURU-HEBAT",
            "KELAS-JUARA",
            "PREMIUM200",
            "ASISTEN-2026",
            "DRUZ-25D5-D6FC-0A1B"
        )
        val isDruzFormat = cleanCode.matches(Regex("^DRUZ(-[A-Z0-9]{4}){3}$")) ||
                cleanCode.matches(Regex("^DRUZ-[A-Z0-9-]+$"))
        return validCodes.contains(cleanCode) || isDruzFormat || (cleanCode.startsWith("PRM-") && cleanCode.length >= 8)
    }
}

class PremiumManager(private val context: Context) {

    fun isPremium(): Boolean = isPremium(context)

    fun activateWithCode(code: String): Boolean = activate(context, code)

    fun setPremium(isPremium: Boolean) = setPremium(context, isPremium)

    companion object {
        fun isPremium(context: Context): Boolean {
            val prefs = context.getSharedPreferences(
                "premium", Context.MODE_PRIVATE
            )
            val legacyPrefs = context.getSharedPreferences(
                "premium_prefs", Context.MODE_PRIVATE
            )

            // DEFAULT FALSE — user baru = FREE
            val isPrem = prefs.getBoolean("is_premium", false) || legacyPrefs.getBoolean("is_premium", false)
            if (!isPrem) return false

            // Cek expired
            val expiryTime = prefs.getLong("expiry_time", 0).let {
                if (it != 0L) it else legacyPrefs.getLong("expiry_time", 0L)
            }
            if (expiryTime > 0 && System.currentTimeMillis() > expiryTime) {
                prefs.edit().putBoolean("is_premium", false).apply()
                legacyPrefs.edit().putBoolean("is_premium", false).apply()
                return false
            }

            return true
        }

        fun activate(context: Context, kode: String): Boolean {
            if (!KodeVerifier.verify(kode)) return false

            val prefs = context.getSharedPreferences(
                "premium", Context.MODE_PRIVATE
            )
            val legacyPrefs = context.getSharedPreferences(
                "premium_prefs", Context.MODE_PRIVATE
            )
            val expiryTime = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000) // 1 tahun

            prefs.edit()
                .putBoolean("is_premium", true)
                .putLong("expiry_time", expiryTime)
                .putString("activation_code", kode.trim().uppercase())
                .putLong("activated_at", System.currentTimeMillis())
                .apply()

            legacyPrefs.edit()
                .putBoolean("is_premium", true)
                .putLong("expiry_time", expiryTime)
                .putString("activation_code", kode.trim().uppercase())
                .putLong("activated_at", System.currentTimeMillis())
                .apply()

            return true
        }

        fun setPremium(context: Context, isPrem: Boolean) {
            val prefs = context.getSharedPreferences("premium", Context.MODE_PRIVATE)
            val legacyPrefs = context.getSharedPreferences("premium_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("is_premium", isPrem).apply()
            legacyPrefs.edit().putBoolean("is_premium", isPrem).apply()
        }
    }
}
