package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.domain.AiCoreState
import com.example.domain.AssistantRuntimeCoordinator
import java.util.Locale

class NishaVoiceController(
    private val context: Context,
    private val onTranscriptReady: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var currentLanguageTag = "en-IN"
    private var speechRate = 1.0f
    private var pitch = 1.0f

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                applyVoiceConfig(currentLanguageTag, speechRate, pitch)
            }
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                AssistantRuntimeCoordinator.setCoreState(AiCoreState.SPEAKING, "Nisha is speaking...")
            }

            override fun onDone(utteranceId: String?) {
                AssistantRuntimeCoordinator.setCoreState(AiCoreState.IDLE, "Ready")
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                AssistantRuntimeCoordinator.setCoreState(AiCoreState.IDLE, "Ready")
            }
        })
    }

    fun applyVoiceConfig(languageMode: String, rate: Float, voicePitch: Float) {
        currentLanguageTag = languageMode
        speechRate = rate.coerceIn(0.6f, 1.8f)
        pitch = voicePitch.coerceIn(0.7f, 1.5f)
        if (!ttsReady) return
        val locale = when (languageMode) {
            "hi-IN" -> Locale("hi", "IN")
            "hinglish" -> Locale("en", "IN")
            else -> Locale("en", "IN")
        }
        tts?.language = locale
        tts?.setSpeechRate(speechRate)
        tts?.setPitch(pitch)
    }

    fun startListening(languageMode: String) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition service is not available on this device.")
            return
        }
        stopListening()
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
        speechRecognizer = recognizer
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                AssistantRuntimeCoordinator.setCoreState(AiCoreState.LISTENING, "Listening ($languageMode)...")
            }

            override fun onBeginningOfSpeech() = Unit

            override fun onRmsChanged(rmsdB: Float) {
                AssistantRuntimeCoordinator.updateAmplitude(rmsdB)
            }

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                AssistantRuntimeCoordinator.updateAmplitude(0f)
                AssistantRuntimeCoordinator.setCoreState(AiCoreState.THINKING, "Processing voice input...")
            }

            override fun onError(error: Int) {
                AssistantRuntimeCoordinator.updateAmplitude(0f)
                AssistantRuntimeCoordinator.setCoreState(AiCoreState.IDLE, "Tap Core to try again")
                val msg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Microphone audio error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition"
                    SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that. Tap mic to speak again."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected."
                    else -> "Voice recognizer error ($error)"
                }
                onError(msg)
            }

            override fun onResults(results: Bundle?) {
                AssistantRuntimeCoordinator.updateAmplitude(0f)
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull().orEmpty().trim()
                if (text.isNotBlank()) {
                    onTranscriptReady(text)
                } else {
                    AssistantRuntimeCoordinator.setCoreState(AiCoreState.IDLE, "Ready")
                    onError("No speech recognized.")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        val localeTag = when (languageMode) {
            "hi-IN" -> "hi-IN"
            else -> "en-IN"
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        recognizer.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.let {
            runCatching {
                it.stopListening()
                it.destroy()
            }
        }
        speechRecognizer = null
    }

    fun speak(text: String) {
        if (!ttsReady) return
        val clean = text.replace(Regex("[#*`]+"), " ").take(3500)
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "nisha_utt_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        tts?.stop()
        AssistantRuntimeCoordinator.setCoreState(AiCoreState.IDLE, "Ready")
    }

    fun release() {
        stopListening()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
