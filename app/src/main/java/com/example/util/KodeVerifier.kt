package com.example.util

object KodeVerifier {
    private val validCodes = setOf(
        "DRUZ-PREMIUM",
        "DRUZ-PREMIUM-2026",
        "GURU-HEBAT",
        "KELAS-JUARA",
        "PREMIUM200",
        "ASISTEN-2026",
        "DRUZ-25D5-D6FC-0A1B"
    )

    fun verify(kode: String): Boolean {
        val clean = kode.trim().uppercase()
        if (clean.isBlank()) return false
        if (validCodes.contains(clean)) return true
        if (clean.matches(Regex("^DRUZ(-[A-Z0-9]{4}){3}$"))) return true
        if (clean.matches(Regex("^DRUZ-[A-Z0-9-]+$")) && clean.length >= 8) return true
        if (clean.startsWith("PRM-") && clean.length >= 8) return true
        return false
    }
}
