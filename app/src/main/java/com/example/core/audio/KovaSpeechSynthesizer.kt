package com.example.core.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.domain.model.KovaEmotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import kotlin.math.sin

class KovaSpeechSynthesizer(
    private val context: Context,
    private val onSpeechStarted: () -> Unit = {},
    private val onSpeechCompleted: () -> Unit = {},
    private val onSpeechError: (String) -> Unit = {}
) {
    companion object {
        private const val TAG = "KovaTTS"
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var speechScope = CoroutineScope(Dispatchers.Main)
    private var amplitudeSimulationJob: Job? = null

    private val _speakingAmplitude = MutableStateFlow(0f)
    val speakingAmplitude: StateFlow<Float> = _speakingAmplitude.asStateFlow()

    private var preferredPitch = 1.08f // Slightly higher, youthful, vibrant assistant voice
    private var preferredSpeed = 1.05f

    init {
        initializeTts()
    }

    private fun initializeTts() {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                setupUtteranceListener()
                // Default to Hinglish/Indian English or Hindi
                val inLocale = Locale("en", "IN")
                val hiLocale = Locale("hi", "IN")
                val inAvail = tts?.isLanguageAvailable(inLocale) ?: TextToSpeech.LANG_NOT_SUPPORTED
                val hiAvail = tts?.isLanguageAvailable(hiLocale) ?: TextToSpeech.LANG_NOT_SUPPORTED

                if (inAvail >= TextToSpeech.LANG_AVAILABLE) {
                    tts?.language = inLocale
                } else if (hiAvail >= TextToSpeech.LANG_AVAILABLE) {
                    tts?.language = hiLocale
                } else {
                    tts?.language = Locale.US
                }
                tts?.setPitch(preferredPitch)
                tts?.setSpeechRate(preferredSpeed)
                Log.d(TAG, "TTS initialized successfully with voice locale ${tts?.voice?.locale}")
            } else {
                Log.e(TAG, "TTS initialization failed with status $status")
            }
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                startAmplitudeAnimation()
                speechScope.launch { onSpeechStarted() }
            }

            override fun onDone(utteranceId: String?) {
                stopAmplitudeAnimation()
                speechScope.launch { onSpeechCompleted() }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                stopAmplitudeAnimation()
                speechScope.launch { onSpeechError("TTS audio playback error") }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                stopAmplitudeAnimation()
                speechScope.launch { onSpeechError("TTS audio playback error code: $errorCode") }
            }
        })
    }

    fun speak(text: String, emotion: KovaEmotion = KovaEmotion.DEFAULT) {
        if (!isInitialized || tts == null) {
            initializeTts()
        }

        // Clean markdown and emojis that might sound strange in TTS
        val cleanedText = sanitizeTextForSpeech(text)
        if (cleanedText.isBlank()) return

        // 1. Detect language context: Hindi / Hinglish / English
        detectAndApplyLocale(cleanedText)

        // 2. Modulate voice pitch and speed dynamically based on emotion
        applyEmotionalModulation(emotion)

        val utteranceId = UUID.randomUUID().toString()
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        tts?.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    private fun detectAndApplyLocale(text: String) {
        // Check if string contains Devanagari script
        val hasDevanagari = text.any { it in '\u0900'..'\u097F' }
        if (hasDevanagari) {
            val hiLocale = Locale("hi", "IN")
            val hiAvail = tts?.isLanguageAvailable(hiLocale) ?: TextToSpeech.LANG_NOT_SUPPORTED
            if (hiAvail >= TextToSpeech.LANG_AVAILABLE) {
                tts?.language = hiLocale
                return
            }
        }

        // Default to Indian English / Hinglish phonetic pronunciation
        val inLocale = Locale("en", "IN")
        val inAvail = tts?.isLanguageAvailable(inLocale) ?: TextToSpeech.LANG_NOT_SUPPORTED
        if (inAvail >= TextToSpeech.LANG_AVAILABLE) {
            tts?.language = inLocale
        } else {
            tts?.language = Locale.US
        }
    }

    private fun applyEmotionalModulation(emotion: KovaEmotion) {
        when (emotion) {
            KovaEmotion.HAPPY -> {
                tts?.setPitch(preferredPitch * 1.15f)
                tts?.setSpeechRate(preferredSpeed * 1.1f)
            }
            KovaEmotion.EXCITED -> {
                tts?.setPitch(preferredPitch * 1.25f)
                tts?.setSpeechRate(preferredSpeed * 1.2f)
            }
            KovaEmotion.FUNNY, KovaEmotion.PLAYFUL -> {
                tts?.setPitch(preferredPitch * 1.1f)
                tts?.setSpeechRate(preferredSpeed * 1.05f)
            }
            KovaEmotion.CALM -> {
                tts?.setPitch(preferredPitch * 0.95f)
                tts?.setSpeechRate(preferredSpeed * 0.9f)
            }
            KovaEmotion.SERIOUS -> {
                tts?.setPitch(preferredPitch * 0.92f)
                tts?.setSpeechRate(preferredSpeed * 0.95f)
            }
            KovaEmotion.CONCERNED, KovaEmotion.EMPATHETIC -> {
                tts?.setPitch(preferredPitch * 0.98f)
                tts?.setSpeechRate(preferredSpeed * 0.88f)
            }
            KovaEmotion.DEFAULT -> {
                tts?.setPitch(preferredPitch)
                tts?.setSpeechRate(preferredSpeed)
            }
        }
    }

    private fun startAmplitudeAnimation() {
        amplitudeSimulationJob?.cancel()
        amplitudeSimulationJob = CoroutineScope(Dispatchers.Default).launch {
            var step = 0f
            while (isActive) {
                step += 0.3f
                // Generate realistic fluctuating speech envelope
                val base = (sin(step.toDouble()) * 0.35 + 0.45).toFloat()
                val jitter = ((step.hashCode() % 100) / 500f)
                val amp = (base + jitter).coerceIn(0.15f, 0.95f)
                _speakingAmplitude.value = amp
                delay(40)
            }
        }
    }

    private fun stopAmplitudeAnimation() {
        amplitudeSimulationJob?.cancel()
        amplitudeSimulationJob = null
        _speakingAmplitude.value = 0f
    }

    fun stop() {
        tts?.stop()
        stopAmplitudeAnimation()
    }

    fun release() {
        stop()
        tts?.shutdown()
        tts = null
    }

    fun setPitch(pitch: Float) {
        preferredPitch = pitch.coerceIn(0.5f, 2.0f)
        tts?.setPitch(preferredPitch)
    }

    fun setSpeed(speed: Float) {
        preferredSpeed = speed.coerceIn(0.5f, 2.0f)
        tts?.setSpeechRate(preferredSpeed)
    }

    private fun sanitizeTextForSpeech(text: String): String {
        return text
            .replace(Regex("[*#_`~]"), "") // Remove markdown
            .replace(Regex("[\\p{So}\\p{Cn}]"), "") // Remove emojis/symbols
            .trim()
    }
}
