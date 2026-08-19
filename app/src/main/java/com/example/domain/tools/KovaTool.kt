package com.example.domain.tools

import org.json.JSONArray
import org.json.JSONObject

/**
 * Definition of an Android Tool executable by Gemini.
 */
data class KovaToolDefinition(
    val name: String,
    val description: String,
    val parameters: Map<String, ToolParam>,
    val required: List<String> = emptyList(),
    val requiresPermission: String? = null,
    val isSensitiveAction: Boolean = false
) {
    fun toJsonSchema(): JSONObject {
        val root = JSONObject()
        root.put("name", name)
        root.put("description", description)

        val paramsObj = JSONObject()
        paramsObj.put("type", "OBJECT")

        val propertiesObj = JSONObject()
        parameters.forEach { (key, param) ->
            val p = JSONObject()
            p.put("type", param.type)
            p.put("description", param.description)
            if (param.enum != null) {
                p.put("enum", JSONArray(param.enum))
            }
            propertiesObj.put(key, p)
        }
        paramsObj.put("properties", propertiesObj)

        if (required.isNotEmpty()) {
            paramsObj.put("required", JSONArray(required))
        }
        root.put("parameters", paramsObj)
        return root
    }
}

data class ToolParam(
    val type: String,
    val description: String,
    val enum: List<String>? = null
)

sealed class ToolExecutionResult {
    data class Success(
        val message: String,
        val conversationalResponse: String,
        val details: Map<String, Any> = emptyMap()
    ) : ToolExecutionResult()

    data class DisambiguationNeeded(
        val question: String,
        val options: List<Map<String, String>>
    ) : ToolExecutionResult()

    data class ConfirmationNeeded(
        val prompt: String,
        val toolName: String,
        val target: String,
        val parameters: Map<String, String>
    ) : ToolExecutionResult()

    data class PermissionNeeded(
        val permission: String,
        val explanation: String
    ) : ToolExecutionResult()

    data class Error(
        val errorMessage: String,
        val conversationalExplanation: String
    ) : ToolExecutionResult()
}
