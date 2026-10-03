package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ApiKeyManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("api_keys", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "ApiKeyManager"
        private const val PREF_GEMINI_KEYS = "gemini_keys"
        private const val PREF_ACTIVE_GEMINI_KEY = "active_gemini_key"
        private const val PREF_DEEPSEEK_KEYS = "deepseek_keys"
        private const val PREF_ACTIVE_DEEPSEEK_KEY = "active_deepseek_key"
        private const val PREF_NEXOTAO_KEYS = "nexotao_keys"
        private const val PREF_ACTIVE_NEXOTAO_KEY = "active_nexotao_key"

        @Volatile
        private var instance: ApiKeyManager? = null

        fun getInstance(context: Context): ApiKeyManager {
            return instance ?: synchronized(this) {
                instance ?: ApiKeyManager(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        // Auto-seed from BuildConfig if available and no keys stored
        val geminiList = getGeminiKeys()
        if (geminiList.isEmpty()) {
            val buildConfigKey = try {
                BuildConfig.GEMINI_API_KEY
            } catch (e: Exception) {
                ""
            }
            if (buildConfigKey.isNotBlank()) {
                addGeminiKey(buildConfigKey)
            }
        }
    }

    // --- GEMINI KEYS ---

    @Synchronized
    fun getGeminiKeys(): List<String> {
        val json = prefs.getString(PREF_GEMINI_KEYS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val k = arr.optString(i).trim()
                if (k.isNotEmpty() && !list.contains(k)) {
                    list.add(k)
                }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun getActiveGeminiKey(): String? {
        val keys = getGeminiKeys()
        if (keys.isEmpty()) return null
        val active = prefs.getString(PREF_ACTIVE_GEMINI_KEY, null)
        if (active != null && keys.contains(active)) {
            return active
        }
        val first = keys.first()
        setActiveGeminiKey(first)
        return first
    }

    @Synchronized
    fun setActiveGeminiKey(key: String) {
        prefs.edit().putString(PREF_ACTIVE_GEMINI_KEY, key.trim()).apply()
    }

    @Synchronized
    fun rotateGeminiKey() {
        val keys = getGeminiKeys()
        if (keys.size <= 1) return
        val current = getActiveGeminiKey() ?: return
        val currentIndex = keys.indexOf(current)
        val nextIndex = (currentIndex + 1) % keys.size
        setActiveGeminiKey(keys[nextIndex])
        Log.i(TAG, "Rotated Gemini Key to index $nextIndex (${maskKey(keys[nextIndex])})")
    }

    @Synchronized
    fun addGeminiKey(key: String): Boolean {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) return false
        val keys = getGeminiKeys().toMutableList()
        if (!keys.contains(trimmed)) {
            keys.add(trimmed)
            saveGeminiKeys(keys)
            if (getActiveGeminiKey() == null) {
                setActiveGeminiKey(trimmed)
            }
            return true
        }
        return false
    }

    @Synchronized
    fun removeGeminiKey(key: String): Boolean {
        val trimmed = key.trim()
        val keys = getGeminiKeys().toMutableList()
        val removed = keys.remove(trimmed)
        if (removed) {
            saveGeminiKeys(keys)
            val active = getActiveGeminiKey()
            if (active == trimmed) {
                if (keys.isNotEmpty()) {
                    setActiveGeminiKey(keys.first())
                } else {
                    prefs.edit().remove(PREF_ACTIVE_GEMINI_KEY).apply()
                }
            }
        }
        return removed
    }

    private fun saveGeminiKeys(keys: List<String>) {
        val arr = JSONArray()
        keys.forEach { arr.put(it) }
        prefs.edit().putString(PREF_GEMINI_KEYS, arr.toString()).apply()
    }

    // --- DEEPSEEK KEYS ---

    @Synchronized
    fun getDeepSeekKeys(): List<String> {
        val json = prefs.getString(PREF_DEEPSEEK_KEYS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val k = arr.optString(i).trim()
                if (k.isNotEmpty() && !list.contains(k)) {
                    list.add(k)
                }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun getActiveDeepSeekKey(): String? {
        val keys = getDeepSeekKeys()
        if (keys.isEmpty()) return null
        val active = prefs.getString(PREF_ACTIVE_DEEPSEEK_KEY, null)
        if (active != null && keys.contains(active)) {
            return active
        }
        val first = keys.first()
        setActiveDeepSeekKey(first)
        return first
    }

    @Synchronized
    fun setActiveDeepSeekKey(key: String) {
        prefs.edit().putString(PREF_ACTIVE_DEEPSEEK_KEY, key.trim()).apply()
    }

    @Synchronized
    fun rotateDeepSeekKey() {
        val keys = getDeepSeekKeys()
        if (keys.size <= 1) return
        val current = getActiveDeepSeekKey() ?: return
        val currentIndex = keys.indexOf(current)
        val nextIndex = (currentIndex + 1) % keys.size
        setActiveDeepSeekKey(keys[nextIndex])
        Log.i(TAG, "Rotated DeepSeek Key to index $nextIndex (${maskKey(keys[nextIndex])})")
    }

    @Synchronized
    fun addDeepSeekKey(key: String): Boolean {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) return false
        val keys = getDeepSeekKeys().toMutableList()
        if (!keys.contains(trimmed)) {
            keys.add(trimmed)
            saveDeepSeekKeys(keys)
            if (getActiveDeepSeekKey() == null) {
                setActiveDeepSeekKey(trimmed)
            }
            return true
        }
        return false
    }

    @Synchronized
    fun removeDeepSeekKey(key: String): Boolean {
        val trimmed = key.trim()
        val keys = getDeepSeekKeys().toMutableList()
        val removed = keys.remove(trimmed)
        if (removed) {
            saveDeepSeekKeys(keys)
            val active = getActiveDeepSeekKey()
            if (active == trimmed) {
                if (keys.isNotEmpty()) {
                    setActiveDeepSeekKey(keys.first())
                } else {
                    prefs.edit().remove(PREF_ACTIVE_DEEPSEEK_KEY).apply()
                }
            }
        }
        return removed
    }

    private fun saveDeepSeekKeys(keys: List<String>) {
        val arr = JSONArray()
        keys.forEach { arr.put(it) }
        prefs.edit().putString(PREF_DEEPSEEK_KEYS, arr.toString()).apply()
    }

    fun getDeepSeekUsage(): Int {
        return getTodayUsage("deepseek")
    }

    // --- NEXOTAO USER KEYS ---

    @Synchronized
    fun getNexotaoKeys(): List<String> {
        val json = prefs.getString(PREF_NEXOTAO_KEYS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val k = arr.optString(i).trim()
                if (k.isNotEmpty() && !list.contains(k)) {
                    list.add(k)
                }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun getActiveNexotaoKey(): String? {
        val keys = getNexotaoKeys()
        if (keys.isEmpty()) return null
        val active = prefs.getString(PREF_ACTIVE_NEXOTAO_KEY, null)
        if (active != null && keys.contains(active)) {
            return active
        }
        val first = keys.first()
        setActiveNexotaoKey(first)
        return first
    }

    @Synchronized
    fun setActiveNexotaoKey(key: String) {
        prefs.edit().putString(PREF_ACTIVE_NEXOTAO_KEY, key.trim()).apply()
    }

    @Synchronized
    fun rotateNexotaoKey() {
        val keys = getNexotaoKeys()
        if (keys.size <= 1) return
        val current = getActiveNexotaoKey() ?: return
        val currentIndex = keys.indexOf(current)
        val nextIndex = (currentIndex + 1) % keys.size
        setActiveNexotaoKey(keys[nextIndex])
        Log.i(TAG, "Rotated Nexotao Key to index $nextIndex (${maskKey(keys[nextIndex])})")
    }

    @Synchronized
    fun addNexotaoKey(key: String): Boolean {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) return false
        val keys = getNexotaoKeys().toMutableList()
        if (!keys.contains(trimmed)) {
            keys.add(trimmed)
            saveNexotaoKeys(keys)
            if (getActiveNexotaoKey() == null) {
                setActiveNexotaoKey(trimmed)
            }
            return true
        }
        return false
    }

    @Synchronized
    fun removeNexotaoKey(key: String): Boolean {
        val trimmed = key.trim()
        val keys = getNexotaoKeys().toMutableList()
        val removed = keys.remove(trimmed)
        if (removed) {
            saveNexotaoKeys(keys)
            val active = getActiveNexotaoKey()
            if (active == trimmed) {
                if (keys.isNotEmpty()) {
                    setActiveNexotaoKey(keys.first())
                } else {
                    prefs.edit().remove(PREF_ACTIVE_NEXOTAO_KEY).apply()
                }
            }
        }
        return removed
    }

    private fun saveNexotaoKeys(keys: List<String>) {
        val arr = JSONArray()
        keys.forEach { arr.put(it) }
        prefs.edit().putString(PREF_NEXOTAO_KEYS, arr.toString()).apply()
    }

    fun getNexotaoUsage(): Int {
        return getTodayUsage("nexotao")
    }

    // --- USAGE TRACKING ---

    fun recordUsage(provider: String, tokens: Int) {
        if (tokens <= 0) return
        val dateKey = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val prefKey = "usage_${provider.lowercase()}_$dateKey"
        val current = prefs.getInt(prefKey, 0)
        prefs.edit().putInt(prefKey, current + tokens).apply()
    }

    fun getTodayUsage(provider: String): Int {
        val dateKey = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val prefKey = "usage_${provider.lowercase()}_$dateKey"
        return prefs.getInt(prefKey, 0)
    }

    // --- HELPERS ---

    fun maskKey(key: String): String {
        val trimmed = key.trim()
        if (trimmed.length <= 8) return "••••••••"
        val prefix = trimmed.take(4)
        val suffix = trimmed.takeLast(4)
        return "$prefix...$suffix"
    }

    suspend fun testGeminiKey(key: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val req = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = "Halo, respon dengan kata 'OK' untuk tes koneksi."))
                    )
                ),
                generationConfig = GeminiGenerationConfig(temperature = 0.1f, maxOutputTokens = 20)
            )
            val response = GeminiClient.api.generateContent(
                model = "gemini-3.5-flash",
                apiKey = key,
                request = req
            )
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrBlank()) {
                recordUsage("gemini", 10)
                val words = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.take(3).joinToString(" ")
                Result.success("✅ Key valid! Response: $words")
            } else {
                Result.failure(Exception("Respon kosong"))
            }
        } catch (e: HttpException) {
            val code = e.code()
            when (code) {
                503 -> {
                    Result.success("⚠️ Key valid, tapi server Gemini sedang sibuk. Coba lagi nanti.")
                }
                429 -> {
                    Result.failure(Exception("⚠️ Kuota API terlampaui (HTTP 429)"))
                }
                400 -> {
                    Result.failure(Exception("❌ Format request salah (HTTP 400)"))
                }
                401, 403 -> {
                    Result.failure(Exception("❌ Key tidak valid atau otorisasi ditolak (HTTP $code)"))
                }
                else -> {
                    Result.failure(Exception("HTTP $code: ${e.message()}"))
                }
            }
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("503")) {
                Result.success("⚠️ Key valid, tapi server Gemini sedang sibuk. Coba lagi nanti.")
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun testDeepSeekKey(key: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val jsonBody = JSONObject().apply {
                put("model", "deepseek-chat")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", "Kamu Cici, asisten guru.")
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "Halo, jawab 'OK' saja.")
                    })
                })
                put("temperature", 0.7)
                put("max_tokens", 20)
                put("stream", false)
            }

            val request = Request.Builder()
                .url("https://api.deepseek.com/v1/chat/completions")
                .header("Authorization", "Bearer ${key.trim()}")
                .header("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val code = response.code
                val bodyStr = response.body?.string() ?: ""
                when (code) {
                    200 -> {
                        val jsonObj = JSONObject(bodyStr)
                        val choices = jsonObj.optJSONArray("choices")
                        val reply = choices?.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: "OK"
                        recordUsage("deepseek", 10)
                        val words = reply.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.take(3).joinToString(" ")
                        Result.success("✅ Key valid! Response: $words")
                    }
                    401, 403 -> {
                        Result.failure(Exception("❌ Key tidak valid"))
                    }
                    402 -> {
                        Result.failure(Exception("⚠️ Saldo habis. Top up di platform.deepseek.com"))
                    }
                    429 -> {
                        Result.failure(Exception("⚠️ Rate limit, tunggu 1 menit"))
                    }
                    else -> {
                        Result.failure(Exception("HTTP $code: ${bodyStr.take(100)}"))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testNexotaoKey(key: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val jsonBody = JSONObject().apply {
                put("model", "deepseek-v3-2")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", "Kamu Cici, asisten guru.")
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "Halo, jawab 'OK' saja.")
                    })
                })
                put("temperature", 0.7)
                put("max_tokens", 20)
                put("stream", false)
            }

            val request = Request.Builder()
                .url("https://api.nexotao.com/v1/chat/completions")
                .header("Authorization", "Bearer ${key.trim()}")
                .header("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val code = response.code
                val bodyStr = response.body?.string() ?: ""
                when (code) {
                    200 -> {
                        val jsonObj = JSONObject(bodyStr)
                        val choices = jsonObj.optJSONArray("choices")
                        val reply = choices?.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: "OK"
                        recordUsage("nexotao", 10)
                        val words = reply.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.take(3).joinToString(" ")
                        Result.success("✅ Key Nexotao valid! Response: $words")
                    }
                    401, 403 -> {
                        Result.failure(Exception("❌ Key Nexotao tidak valid"))
                    }
                    402 -> {
                        Result.failure(Exception("⚠️ Saldo Nexotao habis. Hubungi admin."))
                    }
                    429 -> {
                        Result.failure(Exception("⚠️ Rate limit, tunggu 1 menit"))
                    }
                    else -> {
                        Result.failure(Exception("HTTP $code: ${bodyStr.take(100)}"))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
