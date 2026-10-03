package com.example.util

import android.content.Context
import com.example.data.remote.ApiKeyManager
import java.time.LocalDate

class ChatLimitManager(private val context: Context) {
    private val FREE_LIMIT = BundledApiKey.FREE_LIMIT
    private val PREMIUM_LIMIT = BundledApiKey.PREMIUM_LIMIT
    private val apiKeyManager = ApiKeyManager.getInstance(context)

    fun getLimit(): Int {
        // Kalau user punya API sendiri → unlimited
        if (hasUserApiKey()) return Int.MAX_VALUE

        return if (PremiumManager(context).isPremium()) {
            PREMIUM_LIMIT
        } else {
            FREE_LIMIT
        }
    }

    fun hasUserApiKey(): Boolean {
        return apiKeyManager.getGeminiKeys().isNotEmpty() ||
                apiKeyManager.getDeepSeekKeys().isNotEmpty() ||
                apiKeyManager.getNexotaoKeys().isNotEmpty()
    }

    fun canChat(): Boolean {
        val today = LocalDate.now().toString()
        val prefs = context.getSharedPreferences(
            "chat_limit", Context.MODE_PRIVATE
        )
        val lastDate = prefs.getString("last_date", "")
        var count = prefs.getInt("count", 0)

        if (lastDate != today) {
            count = 0
            prefs.edit()
                .putString("last_date", today)
                .putInt("count", 0)
                .apply()
        }

        return count < getLimit()
    }

    fun recordChat() {
        val today = LocalDate.now().toString()
        val prefs = context.getSharedPreferences(
            "chat_limit", Context.MODE_PRIVATE
        )
        val lastDate = prefs.getString("last_date", "")
        val count = if (lastDate == today) prefs.getInt("count", 0) else 0
        prefs.edit()
            .putString("last_date", today)
            .putInt("count", count + 1)
            .apply()
    }

    fun getRemainingChats(): Int {
        val limit = getLimit()
        if (limit == Int.MAX_VALUE) return -1  // unlimited

        val today = LocalDate.now().toString()
        val prefs = context.getSharedPreferences(
            "chat_limit", Context.MODE_PRIVATE
        )
        val lastDate = prefs.getString("last_date", "")
        val count = if (lastDate == today) prefs.getInt("count", 0) else 0
        return (limit - count).coerceAtLeast(0)
    }

    fun getTodayChatCount(): Int {
        val today = LocalDate.now().toString()
        val prefs = context.getSharedPreferences(
            "chat_limit", Context.MODE_PRIVATE
        )
        val lastDate = prefs.getString("last_date", "")
        return if (lastDate == today) prefs.getInt("count", 0) else 0
    }
}
