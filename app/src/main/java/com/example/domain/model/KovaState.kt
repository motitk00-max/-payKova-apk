package com.example.domain.model

/**
  * Sealed hierarchy representing all UI and runtime states of Kova.
  */
sealed class KovaState {
    data class Idle(
        val isServiceRunning: Boolean = false,
        val standbyMessage: String = "Say \"Kova\" or tap to speak"
    ) : KovaState()

    data class Listening(
        val amplitude: Float = 0f,
        val speechDetected: Boolean = false,
        val liveTranscript: String = ""
    ) : KovaState()

    data class Thinking(
        val userPrompt: String = "",
        val isStreaming: Boolean = true
    ) : KovaState()

    data class Speaking(
        val text: String,
        val audioAmplitude: Float = 0.5f,
        val emotion: KovaEmotion = KovaEmotion.DEFAULT
    ) : KovaState()

    data class ExecutingTool(
        val toolName: String,
        val statusMessage: String
    ) : KovaState()

    data class ConfirmationRequired(
        val toolName: String,
        val promptText: String,
        val targetEntity: String = "",
        val payload: Map<String, String> = emptyMap()
    ) : KovaState()

    data class DisambiguationRequired(
        val title: String,
        val options: List<ContactOption>,
        val originalQuery: String
    ) : KovaState()

    data class PermissionRequired(
        val permission: String,
        val rationale: String,
        val isPermanentlyDenied: Boolean = false
    ) : KovaState()

    data class Error(
        val technicalMessage: String,
        val conversationalMessage: String
    ) : KovaState()
}

data class ContactOption(
    val id: String,
    val name: String,
    val detail: String,
    val type: String
)
