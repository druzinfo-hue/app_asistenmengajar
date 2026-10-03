package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.AsistenMengajarApp
import com.example.util.BundledApiKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException
import java.util.concurrent.TimeUnit
import kotlin.math.pow

object ApiCaller {

    private const val TAG = "ApiCaller"

    private val delays = listOf(1, 2, 4, 8, 16) // detik untuk retry agresif 503

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Fallback Chain Utama (Prioritas Bundled):
     * 1. Nexotao BUNDLED (bawaan, DeepSeek V3.2, tanpa setting)
     * 2. Gemini user (kalau ada key sendiri di Advanced)
     * 3. DeepSeek user (kalau ada key sendiri di Advanced)
     * 4. Nexotao user (kalau ada key sendiri di Advanced)
     */
    suspend fun callAiWithFallback(
        context: Context,
        prompt: String,
        systemPrompt: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKeyManager = ApiKeyManager.getInstance(context)
        val errors = mutableListOf<String>()

        // 1. Nexotao BUNDLED (bawaan, prioritas utama)
        val nexotaoBundled = tryCallNexotaoBundled(prompt, systemPrompt)
        if (nexotaoBundled.isSuccess && !nexotaoBundled.getOrNull().isNullOrBlank()) {
            return@withContext nexotaoBundled.getOrNull()!!
        }
        val nexotaoMsg = nexotaoBundled.exceptionOrNull()?.message ?: "Gagal terhubung"
        errors.add("Nexotao: $nexotaoMsg")
        Log.w(TAG, "Nexotao Bundled gagal ($nexotaoMsg), cek fallback...")

        // 2. Gemini user (kalau ada key sendiri)
        if (apiKeyManager.getGeminiKeys().isNotEmpty()) {
            val gemini = tryCallGemini(context, prompt, systemPrompt)
            if (gemini.isSuccess && !gemini.getOrNull().isNullOrBlank()) {
                return@withContext gemini.getOrNull()!!
            }
            errors.add("Gemini: ${gemini.exceptionOrNull()?.message}")
        }

        // 3. DeepSeek user (kalau ada key sendiri)
        if (apiKeyManager.getDeepSeekKeys().isNotEmpty()) {
            val deepseek = tryCallDeepSeek(context, prompt, systemPrompt)
            if (deepseek.isSuccess && !deepseek.getOrNull().isNullOrBlank()) {
                return@withContext deepseek.getOrNull()!!
            }
            errors.add("DeepSeek: ${deepseek.exceptionOrNull()?.message}")
        }

        // 4. Nexotao user (kalau ada key sendiri)
        if (apiKeyManager.getNexotaoKeys().isNotEmpty()) {
            val nexotaoUser = tryCallNexotao(context, prompt, systemPrompt)
            if (nexotaoUser.isSuccess && !nexotaoUser.getOrNull().isNullOrBlank()) {
                return@withContext nexotaoUser.getOrNull()!!
            }
            errors.add("Nexotao User: ${nexotaoUser.exceptionOrNull()?.message}")
        }

        // Semua gagal
        return@withContext """
        ⚠️ Maaf, AI sedang tidak bisa diakses.

        Detail:
        ${errors.joinToString("\n")}

        Coba:
        1. Tunggu 30 detik, lalu coba lagi
        2. Cek koneksi internet
        3. Hubungi admin: 0858-9407-1160
        """.trimIndent()
    }

    suspend fun callAiWithFallback(prompt: String): String {
        val ctx = try { AsistenMengajarApp.instance } catch (e: Exception) { null }
        return if (ctx != null) {
            callAiWithFallback(ctx, prompt, null)
        } else {
            // Coba panggil Nexotao bundled langsung tanpa context
            val bundled = tryCallNexotaoBundled(prompt)
            bundled.getOrElse {
                """
                ⚠️ Maaf, AI sedang tidak bisa diakses.
                
                Coba:
                1. Tunggu 30 detik, lalu coba lagi
                2. Cek koneksi internet
                3. Hubungi admin: 0858-9407-1160
                """.trimIndent()
            }
        }
    }

    /**
     * Panggil Nexotao BUNDLED (Bawaan aplikasi, DeepSeek V3.2)
     */
    suspend fun tryCallNexotaoBundled(
        prompt: String,
        systemPrompt: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt?.ifBlank { null } ?: "Kamu Cici, asisten guru SD.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }

            val requestBody = JSONObject().apply {
                put("model", BundledApiKey.NEXOTAO_MODEL)
                put("messages", messages)
                put("temperature", 0.7)
                put("max_tokens", 2048)
                put("stream", false)
            }

            val response = okHttpClient.newCall(
                Request.Builder()
                    .url("${BundledApiKey.NEXOTAO_URL}/chat/completions")
                    .header("Authorization", "Bearer ${BundledApiKey.NEXOTAO_KEY}")
                    .header("Content-Type", "application/json")
                    .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()
            ).execute()

            val code = response.code
            val bodyStr = response.body?.string() ?: ""
            response.close()

            when (code) {
                200 -> {
                    val jsonObj = JSONObject(bodyStr)
                    val choices = jsonObj.optJSONArray("choices")
                    val content = choices?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
                    if (!content.isNullOrBlank()) {
                        Result.success(content)
                    } else {
                        Result.failure(Exception("Respon Nexotao kosong"))
                    }
                }
                402 -> Result.failure(Exception("Saldo AI bawaan habis. Hubungi admin."))
                429 -> Result.failure(Exception("Rate limit. Tunggu 1 menit."))
                else -> Result.failure(Exception("HTTP $code: ${bodyStr.take(100)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Panggil Nexotao dengan Key User Sendiri (dari menu Advanced)
     */
    suspend fun tryCallNexotao(
        context: Context,
        prompt: String,
        systemPrompt: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKeyManager = ApiKeyManager.getInstance(context)
            val key = apiKeyManager.getActiveNexotaoKey()
            if (key.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Tidak ada Nexotao key kustom."))
            }

            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt?.ifBlank { null } ?: "Kamu Cici, asisten guru SD.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }

            val requestBody = JSONObject().apply {
                put("model", BundledApiKey.NEXOTAO_MODEL)
                put("messages", messages)
                put("temperature", 0.7)
                put("max_tokens", 2048)
                put("stream", false)
            }

            val response = okHttpClient.newCall(
                Request.Builder()
                    .url("${BundledApiKey.NEXOTAO_URL}/chat/completions")
                    .header("Authorization", "Bearer ${key.trim()}")
                    .header("Content-Type", "application/json")
                    .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()
            ).execute()

            val code = response.code
            val bodyStr = response.body?.string() ?: ""
            response.close()

            when (code) {
                200 -> {
                    val jsonObj = JSONObject(bodyStr)
                    val choices = jsonObj.optJSONArray("choices")
                    val content = choices?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
                    if (!content.isNullOrBlank()) {
                        apiKeyManager.recordUsage("nexotao", (prompt.length + content.length) / 4)
                        Result.success(content)
                    } else {
                        Result.failure(Exception("Respon Nexotao kosong"))
                    }
                }
                401, 403 -> {
                    apiKeyManager.rotateNexotaoKey()
                    Result.failure(Exception("Nexotao key tidak valid"))
                }
                402 -> Result.failure(Exception("Saldo Nexotao habis. Hubungi admin."))
                429 -> {
                    apiKeyManager.rotateNexotaoKey()
                    Result.failure(Exception("Rate limit Nexotao. Tunggu 1 menit."))
                }
                else -> Result.failure(Exception("HTTP $code: ${bodyStr.take(100)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Panggil Gemini dengan retry sabar & eksponensial (up to 5 retries untuk 503)
     */
    suspend fun tryCallGemini(
        context: Context,
        prompt: String,
        systemPrompt: String? = null,
        maxRetries: Int = 5
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKeyManager = ApiKeyManager.getInstance(context)
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                val key = apiKeyManager.getActiveGeminiKey()
                if (key.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("Tidak ada Gemini key."))
                }

                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            role = "user",
                            parts = listOf(GeminiPart(text = prompt))
                        )
                    ),
                    systemInstruction = systemPrompt?.let {
                        GeminiContent(parts = listOf(GeminiPart(text = it)))
                    },
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.3f,
                        maxOutputTokens = 3000
                    )
                )

                val response = GeminiClient.api.generateContent(
                    model = "gemini-3.5-flash",
                    apiKey = key,
                    request = request
                )

                val resultText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!resultText.isNullOrBlank()) {
                    val estimatedTokens = (prompt.length + resultText.length) / 4
                    apiKeyManager.recordUsage("gemini", estimatedTokens)
                    return@withContext Result.success(resultText)
                }
            } catch (e: HttpException) {
                lastException = e
                val code = e.code()
                Log.w(TAG, "Gemini attempt $attempt: HTTP $code")
                when (code) {
                    429 -> {
                        apiKeyManager.rotateGeminiKey()
                        delay(1500)
                    }
                    503 -> {
                        val delaySec = delays.getOrElse(attempt) { 16 }
                        Log.w(TAG, "Gemini 503 overload. Menunggu ${delaySec} detik sebelum coba lagi...")
                        delay(delaySec * 1000L)
                    }
                    401, 403 -> {
                        apiKeyManager.rotateGeminiKey()
                        delay(500)
                    }
                    else -> {
                        delay(1000)
                    }
                }
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Gemini Exception attempt $attempt: ${e.message}")
                delay(1000)
            }
        }

        Result.failure(lastException ?: Exception("Gemini gagal setelah $maxRetries percobaan."))
    }

    /**
     * Panggil DeepSeek (V3 deepseek-chat & R1 deepseek-reasoner) dengan format OpenAI-compatible
     */
    suspend fun tryCallDeepSeek(
        context: Context,
        prompt: String,
        systemPrompt: String? = null,
        maxRetries: Int = 3
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKeyManager = ApiKeyManager.getInstance(context)
        val DEEPSEEK_MODELS = listOf(
            "deepseek-chat",       // V3 (chat biasa)
            "deepseek-reasoner"    // R1 (reasoning)
        )
        var lastException: Exception? = null

        for (model in DEEPSEEK_MODELS) {
            run attemptLoop@{
                repeat(maxRetries) { attempt ->
                    try {
                        val key = apiKeyManager.getActiveDeepSeekKey()
                        if (key.isNullOrBlank()) {
                            return@withContext Result.failure(Exception("Tidak ada DeepSeek key."))
                        }

                        val messages = JSONArray().apply {
                            put(JSONObject().apply {
                                put("role", "system")
                                put("content", systemPrompt?.ifBlank { null } ?: "Kamu Cici, asisten guru.")
                            })
                            put(JSONObject().apply {
                                put("role", "user")
                                put("content", prompt)
                            })
                        }

                        val requestBody = JSONObject().apply {
                            put("model", model)
                            put("messages", messages)
                            put("temperature", 0.7)
                            put("max_tokens", 2048)
                            put("stream", false)
                        }

                        val response = okHttpClient.newCall(
                            Request.Builder()
                                .url("https://api.deepseek.com/v1/chat/completions")
                                .header("Authorization", "Bearer ${key.trim()}")
                                .header("Content-Type", "application/json")
                                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                                .build()
                        ).execute()

                        val code = response.code
                        val bodyStr = response.body?.string() ?: ""
                        response.close()

                        when (code) {
                            200 -> {
                                val jsonObj = JSONObject(bodyStr)
                                val choices = jsonObj.optJSONArray("choices")
                                val reply = choices?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
                                if (!reply.isNullOrBlank()) {
                                    val usageObj = jsonObj.optJSONObject("usage")
                                    val tokens = usageObj?.optInt("total_tokens", (prompt.length + reply.length) / 4) ?: ((prompt.length + reply.length) / 4)
                                    apiKeyManager.recordUsage("deepseek", tokens)
                                    return@withContext Result.success(reply)
                                }
                            }
                            400 -> {
                                Log.e("DEEPSEEK", "400: $bodyStr")
                                lastException = Exception("DeepSeek 400: $bodyStr")
                                return@attemptLoop // coba model berikutnya
                            }
                            402 -> {
                                // Insufficient balance
                                return@withContext Result.failure(Exception("⚠️ Saldo DeepSeek habis. Top up di platform.deepseek.com"))
                            }
                            429 -> {
                                apiKeyManager.rotateDeepSeekKey()
                                delay(2000)
                            }
                            401 -> {
                                apiKeyManager.rotateDeepSeekKey()
                                delay(500)
                            }
                            else -> {
                                val backoff = (2.0.pow(attempt) * 1000).toLong()
                                delay(backoff)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("DEEPSEEK", "Exception", e)
                        lastException = e
                        delay(1000)
                    }
                }
            }
        }

        Result.failure(lastException ?: Exception("Semua DeepSeek model gagal."))
    }

    /**
     * Backward-compatible helper methods
     */
    suspend fun callGeminiWithRetry(
        context: Context,
        prompt: String,
        systemPrompt: String? = null,
        maxRetries: Int = 5
    ): String = withContext(Dispatchers.IO) {
        val geminiResult = tryCallGemini(context, prompt, systemPrompt, maxRetries)
        if (geminiResult.isSuccess) {
            return@withContext geminiResult.getOrNull()!!
        }

        // Fallback ke DeepSeek
        return@withContext callAiWithFallback(context, prompt, systemPrompt)
    }

    suspend fun callDeepSeekWithRetry(
        context: Context,
        prompt: String,
        systemPrompt: String? = null,
        maxRetries: Int = 3
    ): String = withContext(Dispatchers.IO) {
        val result = tryCallDeepSeek(context, prompt, systemPrompt, maxRetries)
        result.getOrElse { it.message ?: "Semua DeepSeek model gagal." }
    }

    suspend fun callDeepSeekWithRetry(
        prompt: String,
        maxRetries: Int = 3
    ): String {
        val ctx = try { AsistenMengajarApp.instance } catch (e: Exception) { null }
        return if (ctx != null) {
            callDeepSeekWithRetry(ctx, prompt, null, maxRetries)
        } else {
            "Semua DeepSeek model gagal."
        }
    }
}
