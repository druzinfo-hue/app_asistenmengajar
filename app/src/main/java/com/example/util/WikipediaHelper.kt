package com.example.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

object WikipediaHelper {

    private const val TAG = "WikipediaHelper"
    private const val PREF_NAME = "wikipedia_cache_prefs"

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    fun extractTerm(question: String): String {
        var q = question.trim()
        val removePrefixes = listOf(
            "apa yang dimaksud dengan",
            "apa maksud dari",
            "apakah yang dimaksud dengan",
            "apakah itu",
            "apa itu",
            "jelaskan tentang",
            "jelaskan mengenai",
            "jelaskan",
            "pengertian dari",
            "pengertian",
            "definisi dari",
            "definisi",
            "apa arti dari",
            "apa arti",
            "arti dari",
            "arti"
        )

        for (prefix in removePrefixes) {
            val regex = Regex("^(?i)\\b$prefix\\b")
            if (regex.containsMatchIn(q)) {
                q = q.replaceFirst(regex, "").trim()
                break
            }
        }

        // Clean trailing punctuation
        q = q.replace(Regex("[?!.,:;]+$"), "").trim()
        return q.ifEmpty { question.trim() }
    }

    suspend fun cariWikipedia(context: Context, question: String): String? = withContext(Dispatchers.IO) {
        val term = extractTerm(question)
        if (term.isBlank()) return@withContext null

        val cacheKey = "wiki_" + term.lowercase()
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val cached = prefs.getString(cacheKey, null)
        if (!cached.isNullOrBlank()) {
            Log.d(TAG, "Returning cached summary for $term")
            return@withContext cached
        }

        try {
            val encodedTerm = URLEncoder.encode(term, StandardCharsets.UTF_8.toString())
                .replace("+", "%20")
            val url = "https://id.wikipedia.org/api/rest_v1/page/summary/$encodedTerm"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "AsistenMengajarApp/1.0 (Android; idrusrusdiana98@guru.sd.belajar.id)")
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.d(TAG, "Wikipedia response not successful: ${response.code} for $term")
                    return@withContext null
                }

                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val extract = json.optString("extract", "")

                if (extract.isNotBlank()) {
                    prefs.edit().putString(cacheKey, extract).apply()
                    return@withContext extract
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching from Wikipedia: ${e.message}")
        }
        null
    }
}
