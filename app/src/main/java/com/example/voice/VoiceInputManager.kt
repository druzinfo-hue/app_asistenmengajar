package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceInputManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _speechResult = MutableStateFlow<String?>(null)
    val speechResult: StateFlow<String?> = _speechResult.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _errorMessage.value = "Fitur pengenalan suara tidak tersedia di perangkat ini."
            return
        }

        stopListening()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        _errorMessage.value = null
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        val msg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "Tidak ada suara yang cocok. Coba lagi."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Waktu bicara habis."
                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Gangguan koneksi internet."
                            SpeechRecognizer.ERROR_AUDIO -> "Kesalahan pada audio mikrofon."
                            else -> "Tidak dapat mengenali suara (Kode: $error)"
                        }
                        _errorMessage.value = msg
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            _speechResult.value = matches[0]
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            _speechResult.value = matches[0]
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "id-ID")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "id-ID")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Silakan bicara sekarang...")
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _isListening.value = false
            _errorMessage.value = "Gagal memulai mikrofon: ${e.localizedMessage}"
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        _isListening.value = false
    }

    fun clearResult() {
        _speechResult.value = null
        _errorMessage.value = null
    }
}
