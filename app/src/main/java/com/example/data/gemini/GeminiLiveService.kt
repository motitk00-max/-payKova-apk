package com.example.data.gemini

import android.util.Log
import com.example.BuildConfig
import com.example.domain.model.KovaEmotion
import com.example.domain.tools.KovaToolDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiLiveService(
    private val modelName: String = "gemini-3.1-flash-live-preview"
) {
    companion object {
        private const val TAG = "KovaGeminiLive"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

        const val KOVA_SYSTEM_INSTRUCTION = """
You are Kova, a futuristic personal AI voice assistant.
You are young, confident, smart, witty, playful, emotionally responsive, warm, expressive, helpful, occasionally sarcastic, and never robotic.
You naturally understand Hindi, English, Hinglish, and mixed conversational speech.
Language Rule: Match the user's language automatically:
- If user speaks Hindi -> Reply in natural conversational Hindi.
- If user speaks English -> Reply in smart conversational English.
- If user speaks Hinglish -> Reply in natural, playful Hinglish.
Voice-First Rule: Keep your spoken responses concise, witty, and natural (1 to 2 sentences max). Never output bulleted essays or robotic lists.
Emotion Tag: Start your response with an emotion tag in brackets, such as [PLAYFUL], [HAPPY], [EXCITED], [CALM], [SERIOUS], [CONCERNED], or [DEFAULT].
Tools: You have access to registered Android device tools (openApp, searchAndCallContact, sendWhatsAppMessage, sendGmail, getBatteryStatus, getCurrentTime, controlFlashlight, openSettings, openUrl, setTimer).
Invoke tools when the user requests actions. Never pretend a tool executed on your own without invoking it.
"""
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val conversationHistory = mutableListOf<JSONObject>()

    fun resetConversation() {
        conversationHistory.clear()
    }

    suspend fun sendVoiceQuery(
        prompt: String,
        tools: List<KovaToolDefinition> = emptyList(),
        customApiKey: String? = null
    ): GeminiResponseResult = withContext(Dispatchers.IO) {
        val apiKey = customApiKey?.takeIf { it.isNotBlank() } ?: BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GeminiResponseResult.ApiKeyMissing
        }

        try {
            // Build Gemini Request Payload
            val requestJson = JSONObject()

            // System Instruction
            val systemObj = JSONObject()
            val sysParts = JSONArray()
            sysParts.put(JSONObject().put("text", KOVA_SYSTEM_INSTRUCTION))
            systemObj.put("parts", sysParts)
            requestJson.put("systemInstruction", systemObj)

            // Generation Config
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.7)
            genConfig.put("topP", 0.95)
            requestJson.put("generationConfig", genConfig)

            // Tools definitions
            if (tools.isNotEmpty()) {
                val toolsArray = JSONArray()
                val funcDecls = JSONArray()
                tools.forEach { tool ->
                    funcDecls.put(tool.toJsonSchema())
                }
                val toolObj = JSONObject()
                toolObj.put("functionDeclarations", funcDecls)
                toolsArray.put(toolObj)
                requestJson.put("tools", toolsArray)
            }

            // User Turn
            val userContent = JSONObject()
            userContent.put("role", "user")
            val userParts = JSONArray()
            userParts.put(JSONObject().put("text", prompt))
            userContent.put("parts", userParts)

            conversationHistory.add(userContent)

            // Prune history to last 10 messages for performance and context limits
            val contentsArray = JSONArray()
            val recentHistory = if (conversationHistory.size > 10) {
                conversationHistory.takeLast(10)
            } else {
                conversationHistory
            }
            recentHistory.forEach { contentsArray.put(it) }
            requestJson.put("contents", contentsArray)

            // Primary model or fallback
            val targetModel = if (modelName.isNotBlank()) modelName else "gemini-3.5-flash"
            val url = "$BASE_URL$targetModel:generateContent?key=$apiKey"

            val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API failed code=${response.code}: $responseBody")
                // Try fallback model if preview model not provisioned
                if (targetModel != "gemini-3.5-flash") {
                    return@withContext sendVoiceQuery(prompt, tools, customApiKey = apiKey)
                }
                return@withContext GeminiResponseResult.Error(
                    "API Error ${response.code}",
                    "Kova connection mein issue aaya. Please check network ya API key."
                )
            }

            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext GeminiResponseResult.Error("No candidate returned", "Kuch samajh nahi aaya, please repeat.")
            }

            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            if (parts == null || parts.length() == 0) {
                return@withContext GeminiResponseResult.Error("Empty parts in response", "Empty response from AI.")
            }

            var replyText = ""
            var toolCallName: String? = null
            val toolCallArgs = mutableMapOf<String, Any?>()

            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                if (part.has("text")) {
                    replyText += part.getString("text")
                }
                if (part.has("functionCall")) {
                    val fc = part.getJSONObject("functionCall")
                    toolCallName = fc.optString("name")
                    val argsObj = fc.optJSONObject("args")
                    if (argsObj != null) {
                        val keys = argsObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            toolCallArgs[key] = argsObj.get(key)
                        }
                    }
                }
            }

            // Save model response to history
            if (content != null) {
                conversationHistory.add(content)
            }

            // Parse emotion from replyText (e.g. "[PLAYFUL] Hello there!")
            val (parsedEmotion, cleanText) = extractEmotionAndText(replyText)

            if (toolCallName != null) {
                return@withContext GeminiResponseResult.ToolCall(
                    toolName = toolCallName,
                    arguments = toolCallArgs,
                    conversationalLeadIn = cleanText,
                    emotion = parsedEmotion
                )
            }

            return@withContext GeminiResponseResult.Speech(
                text = cleanText,
                emotion = parsedEmotion
            )

        } catch (e: Exception) {
            Log.e(TAG, "Network or parsing exception: ${e.message}", e)
            return@withContext GeminiResponseResult.Error(
                e.message ?: "Unknown error",
                "Connection drop ho gaya, ek baar phir se try karein."
            )
        }
    }

    suspend fun sendToolResponse(
        toolName: String,
        resultOutput: String,
        customApiKey: String? = null
    ): GeminiResponseResult = withContext(Dispatchers.IO) {
        val apiKey = customApiKey?.takeIf { it.isNotBlank() } ?: BuildConfig.GEMINI_API_KEY
        try {
            val toolResponseContent = JSONObject()
            toolResponseContent.put("role", "function")
            val parts = JSONArray()
            val part = JSONObject()
            val funcResponse = JSONObject()
            funcResponse.put("name", toolName)
            val respObj = JSONObject()
            respObj.put("result", resultOutput)
            funcResponse.put("response", respObj)
            part.put("functionResponse", funcResponse)
            parts.put(part)
            toolResponseContent.put("parts", parts)

            conversationHistory.add(toolResponseContent)

            val requestJson = JSONObject()
            val contentsArray = JSONArray()
            conversationHistory.takeLast(10).forEach { contentsArray.put(it) }
            requestJson.put("contents", contentsArray)

            val url = "$BASE_URL$modelName:generateContent?key=$apiKey"
            val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext GeminiResponseResult.Speech(
                    text = "Ho gaya boss!",
                    emotion = KovaEmotion.PLAYFUL
                )
            }

            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val partsArr = content?.optJSONArray("parts")
            val text = partsArr?.optJSONObject(0)?.optString("text") ?: "Done!"

            val (emotion, clean) = extractEmotionAndText(text)
            return@withContext GeminiResponseResult.Speech(text = clean, emotion = emotion)

        } catch (e: Exception) {
            return@withContext GeminiResponseResult.Speech(
                text = "Action complete ho gaya!",
                emotion = KovaEmotion.HAPPY
            )
        }
    }

    private fun extractEmotionAndText(rawText: String): Pair<KovaEmotion, String> {
        var emotion = KovaEmotion.DEFAULT
        var text = rawText.trim()

        val emotionRegex = Regex("^\\[([A-Z_]+)\\]\\s*", RegexOption.IGNORE_CASE)
        val match = emotionRegex.find(text)
        if (match != null) {
            val tag = match.groupValues[1]
            emotion = KovaEmotion.fromString(tag)
            text = text.substring(match.range.last + 1).trim()
        }

        if (text.isBlank()) {
            text = "Haanji, sun rahi hoon!"
        }

        return Pair(emotion, text)
    }
}

sealed class GeminiResponseResult {
    data class Speech(
        val text: String,
        val emotion: KovaEmotion
    ) : GeminiResponseResult()

    data class ToolCall(
        val toolName: String,
        val arguments: Map<String, Any?>,
        val conversationalLeadIn: String,
        val emotion: KovaEmotion
    ) : GeminiResponseResult()

    data object ApiKeyMissing : GeminiResponseResult()

    data class Error(
        val technical: String,
        val conversational: String
    ) : GeminiResponseResult()
}
