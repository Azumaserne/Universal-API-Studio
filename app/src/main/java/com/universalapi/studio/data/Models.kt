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
    val reasoningEffort: String = "",
    val speedMode: String = "",
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
        put("reasoningEffort", reasoningEffort)
        put("speedMode", speedMode)
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
                reasoningEffort = obj.optString("reasoningEffort", ""),
                speedMode = obj.optString("speedMode", ""),
            )
        }
    }
}

data class ModelPreset(
    val id: String,
    val displayName: String,
    val contextWindow: Int,
    val tag: String = "",
)

data class OptionItem(
    val value: String,
    val shortLabel: String,
    val label: String,
    val hint: String = "",
)

object ModelCatalog {
    val claudeModels = listOf(
        ModelPreset("anthropic/claude-opus-5.5", "Opus 5.5", 1_000_000, "旗舰"),
        ModelPreset("anthropic/claude-sonnet-5.5", "Sonnet 5.5", 1_000_000, "均衡"),
        ModelPreset("anthropic/claude-haiku-5.5", "Haiku 5.5", 1_000_000, "极速"),
        ModelPreset("anthropic/claude-fable-5.2", "Fable 5.2", 1_000_000, "创作"),
        ModelPreset("anthropic/claude-opus-5", "Opus 5", 1_000_000, "经典"),
        ModelPreset("anthropic/claude-sonnet-5", "Sonnet 5", 1_000_000, "经典"),
    )

    val gptModels = listOf(
        ModelPreset("openai/gpt-6.2-astra", "GPT-6.2 Astra", 1_050_000, "旗舰"),
        ModelPreset("openai/gpt-6.2-astra-pro", "GPT-6.2 Astra Pro", 1_050_000, "Pro"),
        ModelPreset("openai/gpt-6.2-sol", "GPT-6.2 Sol", 1_050_000, "均衡"),
        ModelPreset("openai/gpt-6.2-sol-pro", "GPT-6.2 Sol Pro", 1_050_000, "Pro"),
        ModelPreset("openai/gpt-6.1-terra", "GPT-6.1 Terra", 1_050_000, "高效"),
        ModelPreset("openai/gpt-6.1-terra-pro", "GPT-6.1 Terra Pro", 1_050_000, "Pro"),
        ModelPreset("openai/gpt-6-luna", "GPT-6 Luna", 1_050_000, "轻量"),
    )

    val deepseekModels = listOf(
        ModelPreset("deepseek-flash", "DeepSeek V4.1 Flash", 1_048_576, "极速/视觉"),
        ModelPreset("deepseek-v4-pro", "DeepSeek V4.0 Pro", 1_048_576, "旗舰推理"),
    )

    val openRouterDeepseekModels = listOf(
        ModelPreset("deepseek/deepseek-v4.1-flash", "DeepSeek V4.1 Flash", 1_048_576, "主力"),
        ModelPreset("deepseek/deepseek-v4-pro", "DeepSeek V4.0 Pro", 1_048_576, "旗舰"),
        ModelPreset("deepseek/deepseek-v4.1-flash-1017", "V4.1 Flash 1017", 1_048_576, "快照"),
        ModelPreset("deepseek/deepseek-v4-pro-0813", "V4.0 Pro 0813", 1_048_576, "快照"),
    )

    val reasoningEfforts = listOf(
        OptionItem("", "默认", "默认 (不传)", "沿用模型/服务端默认思考策略"),
        OptionItem("none", "关闭", "关闭 (none)", "跳过思考，最快响应"),
        OptionItem("minimal", "极简", "极简 (minimal)", "极少量思维链"),
        OptionItem("low", "低", "低 (low)", "轻量思考，较快"),
        OptionItem("medium", "中", "中 (medium)", "平衡速度与推理深度"),
        OptionItem("high", "高", "高 (high)", "深度推理，适合编程与复杂分析"),
        OptionItem("xhigh", "超高", "超高 (xhigh)", "超深推理预算"),
        OptionItem("max", "极限", "极限 (max)", "最大推理预算"),
    )

    val speedModes = listOf(
        OptionItem("", "标准", "标准速度", "默认标准路由与队列"),
        OptionItem("fast", "⚡ Fast", "⚡ Fast (快速模式)", "注入 service_tier=\"fast\" 加速"),
        OptionItem("ultrafast", "⚡ Ultrafast", "⚡ Ultrafast (极速档)", "注入 service_tier=\"ultrafast\""),
        OptionItem("nitro", "🚀 Nitro", "🚀 Nitro (高吞吐)", "OpenRouter :nitro + 吞吐优先"),
        OptionItem("latency", "⏱ 低延迟", "⏱ 低延迟优先", "OpenRouter latency 排序 / priority"),
        OptionItem("flex", "💰 Flex", "💰 Flex (省钱降速)", "低优先/低价队列"),
    )

    fun isOpenRouter(profile: ApiProfile): Boolean =
        profile.baseUrl.contains("openrouter.ai", ignoreCase = true)

    fun isDeepSeekDirect(profile: ApiProfile): Boolean =
        profile.baseUrl.contains("api.deepseek.com", ignoreCase = true)

    fun presetsForProfile(profile: ApiProfile): List<ModelPreset> {
        val id = profile.id.lowercase()
        val name = profile.name.lowercase()
        val model = profile.model.lowercase()
        val base = profile.baseUrl.lowercase()

        return when {
            id.contains("deepseek") || model.contains("deepseek") || base.contains("deepseek") ->
                if (isOpenRouter(profile)) openRouterDeepseekModels else deepseekModels
            id.contains("claude") || model.contains("claude") || name.contains("claude") ->
                if (base.contains("api.anthropic.com")) {
                    claudeModels.map { it.copy(id = it.id.removePrefix("anthropic/")) }
                } else claudeModels
            id.contains("gpt") || model.startsWith("openai/") || model.contains("gpt") || name.contains("chatgpt") ->
                if (base.contains("api.openai.com")) {
                    gptModels.map { it.copy(id = it.id.removePrefix("openai/")) }
                } else gptModels
            isOpenRouter(profile) -> claudeModels + gptModels + openRouterDeepseekModels
            else -> emptyList()
        }
    }

    fun displayModelShort(modelId: String): String {
        val cleanId = modelId.substringBefore(":").trim()
        val allPresets = claudeModels + gptModels + deepseekModels + openRouterDeepseekModels
        val preset = allPresets.firstOrNull {
            it.id.equals(cleanId, ignoreCase = true) ||
                it.id.substringAfterLast("/").equals(cleanId, ignoreCase = true)
        }
        return preset?.displayName ?: cleanId.substringAfterLast("/").ifBlank { modelId }
    }

    fun effortOption(value: String): OptionItem =
        reasoningEfforts.firstOrNull { it.value == value } ?: reasoningEfforts.first()

    fun speedOption(value: String): OptionItem =
        speedModes.firstOrNull { it.value == value } ?: speedModes.first()
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
