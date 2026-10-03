package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class VoiceOutputManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    @Volatile
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        // Initialize TTS on background thread to prevent main-thread lag
        CoroutineScope(Dispatchers.IO).launch {
            try {
                tts = TextToSpeech(context.applicationContext, this@VoiceOutputManager)
            } catch (e: Exception) {
                Log.e("VoiceOutputManager", "Error initializing TTS on background thread: ${e.message}", e)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val indonesianLocale = Locale("id", "ID")
            val result = tts?.setLanguage(indonesianLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("VoiceOutputManager", "Indonesian language is not fully supported on this device TTS engine, falling back to default")
                tts?.language = Locale.getDefault()
            }
            tts?.setSpeechRate(0.95f) // Slightly slower for crisp clarity in classroom context
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
            isInitialized = true
        } else {
            Log.e("VoiceOutputManager", "Initialization of TextToSpeech failed.")
            isInitialized = false
        }
    }

    fun speak(text: String) {
        if (!isInitialized) return
        stop()
        // Strip markdown asterisks and hash tags for clean natural speech
        val cleanedText = text
            .replace(Regex("[*#_`]"), "")
            .replace(Regex("\\[.*?\\]\\(.*?\\)"), "")
            .trim()

        if (cleanedText.isNotEmpty()) {
            val utteranceId = System.currentTimeMillis().toString()
            tts?.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    fun stop() {
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
        _isSpeaking.value = false
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
