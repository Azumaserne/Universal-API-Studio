package com.universalapi.studio.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class HeaderPair(
    val id: String = UUID.randomUUID().toString(),
    val key: String = "",
    val value: String = "",
    val enabled: Boolean = true,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("key", key)
        put("value", value)
        put("enabled", enabled)
    }

    companion object {
        fun fromJson(obj: JSONObject): HeaderPair = HeaderPair(
            id = obj.optString("id", UUID.randomUUID().toString()),
            key = obj.optString("key", ""),
            value = obj.optString("value", ""),
            enabled = obj.optBoolean("enabled", true),
        )
    }
}

data class ApiProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "OpenAI 兼容接口",
    val baseUrl: String = "https://api.openai.com/v1",
    val useExactUrl: Boolean = false,
    val apiKey: String = "",
    val model: String = "gpt-4o-mini",
    val stream: Boolean = true,
    val systemPrompt: String = "",
    val temperature: Float = 0.7f,
    val topP: Float = 1.0f,
    val maxTokens: Int? = null,
    val customHeaders: List<HeaderPair> = emptyList(),
    val extraBodyJson: String = "",
    val customBodyTemplate: String = "",
    val customResponsePath: String = "",
    val contextWindow: Int = 128000,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("baseUrl", baseUrl)
        put("useExactUrl", useExactUrl)
        put("apiKey", apiKey)
        put("model", model)
        put("stream", stream)
        put("systemPrompt", systemPrompt)
        put("temperature", temperature.toDouble())
        put("topP", topP.toDouble())
        if (maxTokens != null) put("maxTokens", maxTokens) else put("maxTokens", JSONObject.NULL)
        put("customHeaders", JSONArray().apply { customHeaders.forEach { put(it.toJson()) } })
        put("extraBodyJson", extraBodyJson)
        put("customBodyTemplate", customBodyTemplate)
        put("customResponsePath", customResponsePath)
        put("contextWindow", contextWindow)
    }

    companion object {
        fun fromJson(obj: JSONObject): ApiProfile {
            val headersArr = obj.optJSONArray("customHeaders")
            val headers = buildList {
                if (headersArr != null) {
                    for (i in 0 until headersArr.length()) {
                        headersArr.optJSONObject(i)?.let { add(HeaderPair.fromJson(it)) }
                    }
                }
            }
            val maxTokensVal = if (obj.isNull("maxTokens")) null else obj.optInt("maxTokens").takeIf { it > 0 }
            val ctxWindowVal = obj.optInt("contextWindow", 128000).takeIf { it > 0 } ?: 128000
            return ApiProfile(
                id = obj.optString("id", UUID.randomUUID().toString()),
                name = obj.optString("name", "未命名接口"),
                baseUrl = obj.optString("baseUrl", ""),
                useExactUrl = obj.optBoolean("useExactUrl", false),
                apiKey = obj.optString("apiKey", ""),
                model = obj.optString("model", "gpt-4o-mini"),
                stream = obj.optBoolean("stream", true),
                systemPrompt = obj.optString("systemPrompt", ""),
                temperature = obj.optDouble("temperature", 0.7).toFloat(),
                topP = obj.optDouble("topP", 1.0).toFloat(),
                maxTokens = maxTokensVal,
                customHeaders = headers,
                extraBodyJson = obj.optString("extraBodyJson", ""),
                customBodyTemplate = obj.optString("customBodyTemplate", ""),
                customResponsePath = obj.optString("customResponsePath", ""),
                contextWindow = ctxWindowVal,
            )
        }
    }
}

data class TokenUsage(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
    val cacheHitTokens: Int = 0,
    val cacheMissTokens: Int = 0,
    val isEstimated: Boolean = false,
) {
    val cacheHitRate: Float
        get() = if (promptTokens > 0) {
            (cacheHitTokens.toFloat() / promptTokens.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}

object TokenEstimator {
    fun estimateTokens(text: String): Int {
        if (text.isBlank()) return 0
        var cjk = 0
        var ascii = 0
        for (ch in text) {
            if (ch.code >= 0x2E80) cjk++ else ascii++
        }
        return kotlin.math.ceil(cjk * 1.2 + ascii * 0.28).toInt().coerceAtLeast(1)
    }

    fun estimateConversationContext(
        systemPrompt: String,
        messages: List<ChatMessage>,
    ): Int {
        var total = 0
        if (systemPrompt.isNotBlank()) {
            total += estimateTokens(systemPrompt) + 4
        }
        messages.filter { !it.isError && it.content.isNotBlank() }.forEach { m ->
            total += estimateTokens(m.content) + 4
        }
        return total
    }

    fun formatCompact(tokens: Int): String = when {
        tokens >= 1_000_000 -> {
            val m = tokens / 1_000_000.0
            if (m >= 10.0 || tokens % 1_000_000 == 0) "${m.toInt()}M"
            else String.format(java.util.Locale.US, "%.1fM", m)
        }
        tokens >= 1_000 -> {
            val k = tokens / 1_000.0
            if (k >= 100.0 || tokens % 1_000 == 0) "${k.toInt()}k"
            else String.format(java.util.Locale.US, "%.1fk", k)
        }
        else -> tokens.toString()
    }
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String,
    val content: String,
    val reasoningContent: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val rawRequestPreview: String = "",
    val rawResponsePreview: String = "",
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
    val cacheHitTokens: Int = 0,
    val cacheMissTokens: Int = 0,
    val usageIsEstimated: Boolean = false,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("role", role)
        put("content", content)
        put("reasoningContent", reasoningContent)
        put("timestamp", timestamp)
        put("isError", isError)
        put("rawRequestPreview", rawRequestPreview)
        put("rawResponsePreview", rawResponsePreview)
        put("promptTokens", promptTokens)
        put("completionTokens", completionTokens)
        put("totalTokens", totalTokens)
        put("cacheHitTokens", cacheHitTokens)
        put("cacheMissTokens", cacheMissTokens)
        put("usageIsEstimated", usageIsEstimated)
    }

    companion object {
        fun fromJson(obj: JSONObject): ChatMessage = ChatMessage(
            id = obj.optString("id", UUID.randomUUID().toString()),
            role = obj.optString("role", "user"),
            content = obj.optString("content", ""),
            reasoningContent = obj.optString("reasoningContent", ""),
            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
            isError = obj.optBoolean("isError", false),
            rawRequestPreview = obj.optString("rawRequestPreview", ""),
            rawResponsePreview = obj.optString("rawResponsePreview", ""),
            promptTokens = obj.optInt("promptTokens", 0),
            completionTokens = obj.optInt("completionTokens", 0),
            totalTokens = obj.optInt("totalTokens", 0),
            cacheHitTokens = obj.optInt("cacheHitTokens", 0),
            cacheMissTokens = obj.optInt("cacheMissTokens", 0),
            usageIsEstimated = obj.optBoolean("usageIsEstimated", false),
        )
    }
}

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "新对话",
    val profileId: String = "",
    val sessionSystemPrompt: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("profileId", profileId)
        put("sessionSystemPrompt", sessionSystemPrompt)
        put("messages", JSONArray().apply { messages.forEach { put(it.toJson()) } })
        put("updatedAt", updatedAt)
    }

    companion object {
        fun fromJson(obj: JSONObject): ChatSession {
            val msgsArr = obj.optJSONArray("messages")
            val msgs = buildList {
                if (msgsArr != null) {
                    for (i in 0 until msgsArr.length()) {
                        msgsArr.optJSONObject(i)?.let { add(ChatMessage.fromJson(it)) }
                    }
                }
            }
            return ChatSession(
                id = obj.optString("id", UUID.randomUUID().toString()),
                title = obj.optString("title", "新对话"),
                profileId = obj.optString("profileId", ""),
                sessionSystemPrompt = obj.optString("sessionSystemPrompt", ""),
                messages = msgs,
                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
            )
        }
    }
}
