package com.example.presentation

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.audio.AudioPipeline
import com.example.core.audio.KovaAcousticWakeWordDetector
import com.example.core.audio.KovaSpeechSynthesizer
import com.example.core.audio.WakeWordDetector
import com.example.core.permissions.PermissionManager
import com.example.data.gemini.GeminiLiveService
import com.example.data.gemini.GeminiResponseResult
import com.example.domain.model.ContactOption
import com.example.domain.model.KovaEmotion
import com.example.domain.model.KovaState
import com.example.domain.tools.ToolExecutionEngine
import com.example.domain.tools.ToolExecutionResult
import com.example.service.BackgroundAudioService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class KovaViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "KovaViewModel"
    }

    private val context = application.applicationContext

    // Core Managers & Engines
    val permissionManager = PermissionManager(context)
    val toolEngine = ToolExecutionEngine(context)
    private val wakeWordDetector: WakeWordDetector = KovaAcousticWakeWordDetector()
    private var geminiService = GeminiLiveService("gemini-3.1-flash-live-preview")
    private var speechSynthesizer: KovaSpeechSynthesizer? = null
    private var audioPipeline: AudioPipeline? = null
    private var speechRecognizer: SpeechRecognizer? = null

    // UI States
    private val _kovaState = MutableStateFlow<KovaState>(KovaState.Idle())
    val kovaState: StateFlow<KovaState> = _kovaState.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _activeTranscript = MutableStateFlow("")
    val activeTranscript: StateFlow<String> = _activeTranscript.asStateFlow()

    private val _permissionStatus = MutableStateFlow(permissionManager.getPermissionStatus())
    val permissionStatus: StateFlow<PermissionManager.PermissionStatus> = _permissionStatus.asStateFlow()

    // Config Settings
    val selectedModel = MutableStateFlow("gemini-3.1-flash-live-preview")
    val customApiKey = MutableStateFlow("")
    val wakeWordSensitivity = MutableStateFlow(0.65f)
    val voicePitch = MutableStateFlow(1.08f)
    val voiceSpeed = MutableStateFlow(1.05f)
    val languageMode = MutableStateFlow("Auto (Hindi/Eng)")
    val isServiceRunning = MutableStateFlow(false)

    private var sessionJob: Job? = null
    private var autoReturnToIdleJob: Job? = null

    init {
        initSynthesizer()
        initAudioPipeline()
        initSpeechRecognizer()
        observeBackgroundServiceTriggers()
    }

    private fun initSynthesizer() {
        speechSynthesizer = KovaSpeechSynthesizer(
            context = context,
            onSpeechStarted = {
                val current = _kovaState.value
                if (current is KovaState.Speaking) {
                    audioPipeline?.setKovaSpeaking(true)
                }
            },
            onSpeechCompleted = {
                audioPipeline?.setKovaSpeaking(false)
                scheduleReturnToStandby(delayMs = 1200)
            },
            onSpeechError = {
                audioPipeline?.setKovaSpeaking(false)
                scheduleReturnToStandby(delayMs = 500)
            }
        )

        // Bind TTS speaking amplitude to UI amplitude
        viewModelScope.launch {
            speechSynthesizer?.speakingAmplitude?.collect { amp ->
                if (_kovaState.value is KovaState.Speaking) {
                    _audioAmplitude.value = amp
                }
            }
        }
    }

    private fun initAudioPipeline() {
        audioPipeline = AudioPipeline(
            wakeWordDetector = wakeWordDetector,
            onWakeWordDetected = {
                viewModelScope.launch {
                    onWakeWordTriggered()
                }
            },
            onBargeInDetected = {
                viewModelScope.launch {
                    handleBargeIn()
                }
            },
            onPcmChunkCaptured = { /* Streaming PCM hook */ }
        )

        // Bind input mic amplitude
        viewModelScope.launch {
            audioPipeline?.inputAmplitude?.collect { amp ->
                val state = _kovaState.value
                if (state is KovaState.Listening || state is KovaState.Idle) {
                    _audioAmplitude.value = amp
                }
            }
        }
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d(TAG, "SpeechRecognizer onReadyForSpeech")
                    }

                    override fun onBeginningOfSpeech() {
                        _kovaState.value = KovaState.Listening(speechDetected = true, liveTranscript = "Listening...")
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        if (_kovaState.value is KovaState.Listening) {
                            val norm = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                            _audioAmplitude.value = norm
                        }
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _kovaState.value = KovaState.Thinking(userPrompt = _activeTranscript.value)
                    }

                    override fun onError(error: Int) {
                        Log.w(TAG, "SpeechRecognizer error: $error")
                        // If no speech input detected in push-to-talk, gracefully return to idle
                        if (_kovaState.value is KovaState.Listening) {
                            scheduleReturnToStandby(delayMs = 300)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spokenText = matches?.firstOrNull() ?: ""
                        if (spokenText.isNotBlank()) {
                            processSpokenQuery(spokenText)
                        } else {
                            scheduleReturnToStandby(delayMs = 300)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = partials?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            _activeTranscript.value = text
                            _kovaState.value = KovaState.Listening(liveTranscript = text, speechDetected = true)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    private fun observeBackgroundServiceTriggers() {
        viewModelScope.launch {
            BackgroundAudioService.wakeWordTriggerFlow.collect {
                onWakeWordTriggered()
            }
        }
    }

    fun refreshPermissions() {
        val status = permissionManager.getPermissionStatus()
        _permissionStatus.value = status
        if (status.hasRecordAudio) {
            audioPipeline?.startRecording(viewModelScope)
        }
    }

    fun startBackgroundService() {
        if (_permissionStatus.value.hasRecordAudio) {
            BackgroundAudioService.startService(context)
            isServiceRunning.value = true
        }
    }

    fun stopBackgroundService() {
        BackgroundAudioService.stopService(context)
        isServiceRunning.value = false
    }

    fun onWakeWordTriggered() {
        autoReturnToIdleJob?.cancel()
        speechSynthesizer?.stop()
        audioPipeline?.stopPlayback()

        _activeTranscript.value = ""
        _kovaState.value = KovaState.Listening(liveTranscript = "Say your command...")

        // Play subtle wake acknowledgment or start speech capture
        startVoiceCapture()
    }

    fun onOrbClicked() {
        val current = _kovaState.value
        when (current) {
            is KovaState.Idle -> {
                onWakeWordTriggered()
            }
            is KovaState.Speaking -> {
                // Manual tap interruption
                handleBargeIn()
            }
            is KovaState.Listening -> {
                // Stop listening and force process
                speechRecognizer?.stopListening()
            }
            is KovaState.Thinking -> {
                // Cancel current thinking
                sessionJob?.cancel()
                _kovaState.value = KovaState.Idle(isServiceRunning.value)
            }
            else -> {
                onWakeWordTriggered()
            }
        }
    }

    private fun startVoiceCapture() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                // Configure Hinglish / Indian English / Hindi multi-language support
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayListOf("en-IN", "hi-IN", "en-US"))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start speech recognition: ${e.message}")
        }
    }

    fun processSpokenQuery(userQuery: String) {
        autoReturnToIdleJob?.cancel()
        _activeTranscript.value = userQuery
        _kovaState.value = KovaState.Thinking(userPrompt = userQuery)

        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            try {
                // 1. Direct Quick-Command Matcher for instant response
                val directHandled = handleLocalFastPath(userQuery)
                if (directHandled) return@launch

                // 2. Query Gemini Live / API with full Tool Declarations
                val result = geminiService.sendVoiceQuery(
                    prompt = userQuery,
                    tools = toolEngine.registeredTools,
                    customApiKey = customApiKey.value
                )

                handleGeminiResult(result)

            } catch (e: Exception) {
                Log.e(TAG, "Error in processSpokenQuery: ${e.message}", e)
                speakResponse("Oops, thoda issue aaya. Ek baar repeat karein?", KovaEmotion.CONCERNED)
            }
        }
    }

    private fun handleGeminiResult(result: GeminiResponseResult) {
        when (result) {
            is GeminiResponseResult.Speech -> {
                speakResponse(result.text, result.emotion)
            }

            is GeminiResponseResult.ToolCall -> {
                _kovaState.value = KovaState.ExecutingTool(
                    toolName = result.toolName,
                    statusMessage = result.conversationalLeadIn.ifBlank { "Executing ${result.toolName}..." }
                )

                // Execute tool
                val toolResult = toolEngine.executeTool(result.toolName, result.arguments)
                handleToolExecutionResult(result.toolName, toolResult, result.conversationalLeadIn)
            }

            is GeminiResponseResult.ApiKeyMissing -> {
                speakResponse(
                    "Boss, Gemini API Key configure nahi hai. Settings mein check karein!",
                    KovaEmotion.PLAYFUL
                )
            }

            is GeminiResponseResult.Error -> {
                speakResponse(result.conversational, KovaEmotion.CONCERNED)
            }
        }
    }

    private fun handleToolExecutionResult(
        toolName: String,
        result: ToolExecutionResult,
        conversationalLeadIn: String
    ) {
        when (result) {
            is ToolExecutionResult.Success -> {
                val fullSpeech = if (conversationalLeadIn.isNotBlank()) {
                    "$conversationalLeadIn ${result.conversationalResponse}"
                } else {
                    result.conversationalResponse
                }
                speakResponse(fullSpeech, KovaEmotion.HAPPY)
            }

            is ToolExecutionResult.ConfirmationNeeded -> {
                _kovaState.value = KovaState.ConfirmationRequired(
                    toolName = result.toolName,
                    promptText = result.prompt,
                    targetEntity = result.target,
                    payload = result.parameters
                )
                // Speak confirmation prompt
                speechSynthesizer?.speak(result.prompt, KovaEmotion.PLAYFUL)
            }

            is ToolExecutionResult.DisambiguationNeeded -> {
                val options = result.options.map {
                    ContactOption(
                        id = it["id"] ?: "",
                        name = it["name"] ?: "",
                        detail = it["phone"] ?: "",
                        type = "phone"
                    )
                }
                _kovaState.value = KovaState.DisambiguationRequired(
                    title = result.question,
                    options = options,
                    originalQuery = ""
                )
                speechSynthesizer?.speak(result.question, KovaEmotion.CALM)
            }

            is ToolExecutionResult.PermissionNeeded -> {
                _kovaState.value = KovaState.PermissionRequired(
                    permission = result.permission,
                    rationale = result.explanation
                )
                speechSynthesizer?.speak(result.explanation, KovaEmotion.CONCERNED)
            }

            is ToolExecutionResult.Error -> {
                speakResponse(result.conversationalExplanation, KovaEmotion.CONCERNED)
            }
        }
    }

    fun confirmPendingAction() {
        val current = _kovaState.value
        if (current is KovaState.ConfirmationRequired) {
            when (current.toolName) {
                "callPhoneDirect" -> {
                    val phone = current.payload["phoneNumber"] ?: ""
                    val name = current.payload["name"] ?: ""
                    val res = toolEngine.callPhoneDirect(phone, name)
                    if (res is ToolExecutionResult.Success) {
                        speakResponse(res.conversationalResponse, KovaEmotion.HAPPY)
                    } else if (res is ToolExecutionResult.Error) {
                        speakResponse(res.conversationalExplanation, KovaEmotion.CONCERNED)
                    }
                }
                "sendWhatsAppDirect" -> {
                    val phone = current.payload["phoneNumber"] ?: ""
                    val msg = current.payload["message"] ?: ""
                    val res = toolEngine.openWhatsAppChat(phone, msg)
                    if (res is ToolExecutionResult.Success) {
                        speakResponse(res.conversationalResponse, KovaEmotion.HAPPY)
                    } else if (res is ToolExecutionResult.Error) {
                        speakResponse(res.conversationalExplanation, KovaEmotion.CONCERNED)
                    }
                }
                else -> {
                    scheduleReturnToStandby(100)
                }
            }
        }
    }

    fun cancelPendingAction() {
        speakResponse("Cancel kar diya boss.", KovaEmotion.PLAYFUL)
    }

    fun selectDisambiguationOption(option: ContactOption) {
        val res = toolEngine.callPhoneDirect(option.detail, option.name)
        if (res is ToolExecutionResult.Success) {
            speakResponse(res.conversationalResponse, KovaEmotion.HAPPY)
        } else if (res is ToolExecutionResult.Error) {
            speakResponse(res.conversationalExplanation, KovaEmotion.CONCERNED)
        }
    }

    private fun handleLocalFastPath(query: String): Boolean {
        val q = query.lowercase().trim()

        // YouTube
        if (q.contains("youtube kholo") || q.contains("open youtube") || q == "youtube") {
            val res = toolEngine.executeTool("openApp", mapOf("packageNameOrName" to "youtube"))
            speakResponse("Done. YouTube aa gaya, enjoy karo! 🍿", KovaEmotion.PLAYFUL)
            return true
        }

        // Instagram
        if (q.contains("instagram kholo") || q.contains("open instagram") || q == "instagram") {
            toolEngine.executeTool("openApp", mapOf("packageNameOrName" to "instagram"))
            speakResponse("Done. Instagram khol diya — ab productivity ka kya hoga, boss? 😏", KovaEmotion.FUNNY)
            return true
        }

        // Calculator
        if (q.contains("calculator") || q.contains("calc")) {
            toolEngine.executeTool("openApp", mapOf("packageNameOrName" to "calculator"))
            speakResponse("Calculator ready. Maths se dosti karne ka waqt aa gaya.", KovaEmotion.PLAYFUL)
            return true
        }

        // Battery
        if (q.contains("battery") || q.contains("charge kitna")) {
            val res = toolEngine.executeTool("getBatteryStatus", emptyMap())
            if (res is ToolExecutionResult.Success) {
                speakResponse(res.conversationalResponse, KovaEmotion.DEFAULT)
            }
            return true
        }

        // Flashlight / Torch
        if (q.contains("torch") || q.contains("flashlight")) {
            val enable = !q.contains("band") && !q.contains("off")
            val res = toolEngine.executeTool("controlFlashlight", mapOf("enable" to enable))
            if (res is ToolExecutionResult.Success) {
                speakResponse(res.conversationalResponse, KovaEmotion.HAPPY)
            }
            return true
        }

        // Boredom
        if (q.contains("bore") || q.contains("main bore ho raha")) {
            speakResponse(
                "Not on my watch! Batao, gossip chahiye, game chahiye, ya productive banne ka natak karein? 😜",
                KovaEmotion.PLAYFUL
            )
            return true
        }

        return false
    }

    private fun speakResponse(text: String, emotion: KovaEmotion = KovaEmotion.DEFAULT) {
        _activeTranscript.value = text
        _kovaState.value = KovaState.Speaking(
            text = text,
            audioAmplitude = 0.5f,
            emotion = emotion
        )
        speechSynthesizer?.speak(text, emotion)
    }

    private fun handleBargeIn() {
        Log.i(TAG, "Barge-in: Interrupting assistant speech")
        speechSynthesizer?.stop()
        audioPipeline?.stopPlayback()
        _kovaState.value = KovaState.Listening(liveTranscript = "Listening...")
        startVoiceCapture()
    }

    private fun scheduleReturnToStandby(delayMs: Long) {
        autoReturnToIdleJob?.cancel()
        autoReturnToIdleJob = viewModelScope.launch {
            delay(delayMs)
            _kovaState.value = KovaState.Idle(isServiceRunning = isServiceRunning.value)
        }
    }

    fun updateModel(model: String) {
        selectedModel.value = model
        geminiService = GeminiLiveService(model)
    }

    fun updateApiKey(key: String) {
        customApiKey.value = key
    }

    fun updateSensitivity(value: Float) {
        wakeWordSensitivity.value = value
        wakeWordDetector.setSensitivity(value)
    }

    fun updateVoicePitch(pitch: Float) {
        voicePitch.value = pitch
        speechSynthesizer?.setPitch(pitch)
    }

    fun updateVoiceSpeed(speed: Float) {
        voiceSpeed.value = speed
        speechSynthesizer?.setSpeed(speed)
    }

    fun updateLanguageMode(mode: String) {
        languageMode.value = mode
    }

    fun testToolDirectly(action: String) {
        when (action) {
            "youtube" -> toolEngine.executeTool("openApp", mapOf("packageNameOrName" to "youtube"))
            "battery" -> {
                val res = toolEngine.executeTool("getBatteryStatus", emptyMap())
                if (res is ToolExecutionResult.Success) speakResponse(res.conversationalResponse)
            }
            "torch" -> {
                val res = toolEngine.executeTool("controlFlashlight", mapOf("enable" to true))
                if (res is ToolExecutionResult.Success) speakResponse(res.conversationalResponse)
            }
            "calculator" -> toolEngine.executeTool("openApp", mapOf("packageNameOrName" to "calculator"))
            "time" -> {
                val res = toolEngine.executeTool("getCurrentTime", emptyMap())
                if (res is ToolExecutionResult.Success) speakResponse(res.conversationalResponse)
            }
            "settings" -> toolEngine.executeTool("openSettings", emptyMap())
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechSynthesizer?.release()
        audioPipeline?.release()
        speechRecognizer?.destroy()
    }
}
