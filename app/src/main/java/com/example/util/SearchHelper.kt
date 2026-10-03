package com.example.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

object SearchHelper {
    private const val TAG = "SearchHelper"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun searchDuckDuckGo(query: String): String? = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext null

        try {
            val encodedQuery = URLEncoder.encode(q, StandardCharsets.UTF_8.toString())
            val url = "https://api.duckduckgo.com/?q=$encodedQuery&format=json&no_html=1&skip_disambig=1"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "AsistenMengajarApp/1.0 (Android; idrusrusdiana98@guru.sd.belajar.id)")
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "DuckDuckGo response unsuccessful: ${response.code}")
                    return@withContext null
                }

                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)

                val abstractText = json.optString("AbstractText", "").ifBlank {
                    json.optString("Abstract", "")
                }
                val heading = json.optString("Heading", "")
                val sourceUrl = json.optString("AbstractURL", "")

                if (abstractText.isNotBlank()) {
                    val sb = StringBuilder()
                    if (heading.isNotBlank()) sb.append("📌 $heading\n\n")
                    sb.append(abstractText)
                    if (sourceUrl.isNotBlank()) sb.append("\n\n🔗 Sumber: $sourceUrl")
                    return@withContext sb.toString()
                }

                // Check RelatedTopics array
                val relatedTopics = json.optJSONArray("RelatedTopics")
                if (relatedTopics != null && relatedTopics.length() > 0) {
                    for (i in 0 until relatedTopics.length()) {
                        val topicObj = relatedTopics.optJSONObject(i) ?: continue
                        val text = topicObj.optString("Text", "")
                        if (text.isNotBlank()) {
                            val topicUrl = topicObj.optString("FirstURL", "")
                            val sb = StringBuilder(text)
                            if (topicUrl.isNotBlank()) sb.append("\n\n🔗 Sumber: $topicUrl")
                            return@withContext sb.toString()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Search error: ${e.message}")
        }
        null
    }
}
