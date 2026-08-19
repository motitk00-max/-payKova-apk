package com.example.domain.model

enum class KovaEmotion(val label: String, val promptDescriptor: String) {
    DEFAULT("Normal", "Natural, warm, witty"),
    HAPPY("Happy", "Upbeat, delighted, warm"),
    EXCITED("Excited", "High energy, enthusiastic"),
    FUNNY("Playful/Funny", "Teasing, humorous, playful smirk"),
    CALM("Calm", "Relaxed, reassuring, soft"),
    SERIOUS("Serious", "Focused, professional, crisp"),
    CONCERNED("Concerned", "Empathetic, caring, attentive"),
    EMPATHETIC("Empathetic", "Understanding, gentle"),
    PLAYFUL("Playful", "Slightly teasing, clever, cheerful");

    companion object {
        fun fromString(value: String?): KovaEmotion {
            if (value.isNullOrBlank()) return DEFAULT
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } ?: DEFAULT
        }
    }
}
