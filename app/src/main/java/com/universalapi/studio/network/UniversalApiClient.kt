package com.universalapi.studio.network

import com.universalapi.studio.data.ApiProfile
import com.universalapi.studio.data.ChatMessage
import com.universalapi.studio.data.ModelCatalog
import com.universalapi.studio.data.TokenEstimator
import com.universalapi.studio.data.TokenUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ApiCallResult(
    val content: String,
    val reasoningContent: String,
    val rawRequestPreview: String,
    val rawResponsePreview: String,
    val isError: Boolean = false,
    val usage: TokenUsage? = null,
)

object UniversalApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun resolveEndpointUrl(profile: ApiProfile): String {
        val base = profile.baseUrl.trim()
        if (base.isEmpty()) return ""
        if (profile.useExactUrl) return base
        val clean = base.trimEnd('/')
        return when {
            clean.endsWith("/chat/completions") -> clean
            Regex(".*/v\\d+$").matches(clean) -> "$clean/chat/completions"
            else -> "$clean/v1/chat/completions"
        }
    }

    fun resolveModelsUrl(profile: ApiProfile): String {
        val clean = profile.baseUrl.trim().trimEnd('/')
        if (clean.isEmpty()) return ""
        return when {
            clean.endsWith("/chat/completions") -> clean.removeSuffix("/chat/completions") + "/models"
            Regex(".*/v\\d+$").matches(clean) -> "$clean/models"
            else -> "$clean/v1/models"
        }
    }

    private fun jsonEscapeInner(text: String): String {
        val quoted = JSONObject.quote(text)
        return if (quoted.length >= 2) quoted.substring(1, quoted.length - 1) else ""
    }

    fun resolveEffectiveModelId(profile: ApiProfile, allowNitro: Boolean = true): String {
        val raw = profile.model.trim()
        if (raw.isEmpty()) return ""
        return if (allowNitro && ModelCatalog.isOpenRouter(profile) && profile.speedMode == "nitro" && !raw.contains(":")) {
            "$raw:nitro"
        } else {
            raw
        }
    }

    private fun buildRequestBodyJson(
        profile: ApiProfile,
        history: List<ChatMessage>,
        sessionSystemPrompt: String,
        excludedFields: Set<String> = emptySet(),
    ): String {
        val effectiveSystem = sessionSystemPrompt.ifBlank { profile.systemPrompt }.trim()
        val messagesArr = JSONArray()
        if (effectiveSystem.isNotBlank()) {
            messagesArr.put(JSONObject().apply {
                put("role", "system")
                put("content", effectiveSystem)
            })
        }
        history.filter { !it.isError && it.content.isNotBlank() }.forEach { msg ->
            messagesArr.put(JSONObject().apply {
                put("role", msg.role)
                put("content", msg.content)
            })
        }

        val latestUserPrompt = history.lastOrNull { it.role == "user" }?.content.orEmpty()
        val effectiveModel = resolveEffectiveModelId(profile, allowNitro = "nitro" !in excludedFields)

        if (profile.customBodyTemplate.isNotBlank()) {
            var rendered = profile.customBodyTemplate
                .replace("{{model_json}}", JSONObject.quote(effectiveModel))
                .replace("{{model}}", jsonEscapeInner(effectiveModel))
                .replace("{{prompt_json}}", JSONObject.quote(latestUserPrompt))
                .replace("{{prompt}}", jsonEscapeInner(latestUserPrompt))
                .replace("{{system_prompt_json}}", JSONObject.quote(effectiveSystem))
                .replace("{{system_prompt}}", jsonEscapeInner(effectiveSystem))
                .replace("{{messages_json}}", messagesArr.toString())
                .replace("{{temperature}}", profile.temperature.toString())
                .replace("{{top_p}}", profile.topP.toString())
                .replace("{{stream}}", profile.stream.toString())
                .replace("{{max_tokens}}", (profile.maxTokens ?: 2048).toString())

            if (profile.extraBodyJson.isNotBlank()) {
                val baseObj = JSONObject(rendered)
                val extraObj = JSONObject(profile.extraBodyJson.trim())
                val keys = extraObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    baseObj.put(k, extraObj.get(k))
                }
                rendered = baseObj.toString(2)
            }
            return rendered
        }

        val isOpenRouter = ModelCatalog.isOpenRouter(profile)
        val root = JSONObject().apply {
            put("model", effectiveModel)
            put("messages", messagesArr)
            put("stream", profile.stream)
            if ("temperature" !in excludedFields) {
                put("temperature", String.format(java.util.Locale.US, "%.2f", profile.temperature).toDouble())
            }
            if ("top_p" !in excludedFields) {
                put("top_p", String.format(java.util.Locale.US, "%.2f", profile.topP).toDouble())
            }
            if (profile.maxTokens != null && profile.maxTokens > 0) {
                put("max_tokens", profile.maxTokens)
            }
        }

        if (!profile.useExactUrl && profile.reasoningEffort.isNotBlank() && "reasoning" !in excludedFields) {
            if (isOpenRouter) {
                root.put("reasoning", JSONObject().apply { put("effort", profile.reasoningEffort) })
            } else if (profile.reasoningEffort != "none") {
                root.put("reasoning_effort", profile.reasoningEffort)
            }
        }

        if (!profile.useExactUrl && profile.speedMode.isNotBlank()) {
            when (profile.speedMode) {
                "fast" -> {
                    if ("service_tier" !in excludedFields) root.put("service_tier", "fast")
                }
                "ultrafast" -> {
                    if ("service_tier" !in excludedFields) root.put("service_tier", "ultrafast")
                }
                "nitro" -> {
                    if (isOpenRouter) {
                        if ("provider" !in excludedFields) {
                            root.put("provider", JSONObject().apply { put("sort", "throughput") })
                        }
                    } else if ("service_tier" !in excludedFields) {
                        root.put("service_tier", "fast")
                    }
                }
                "latency" -> {
                    if (isOpenRouter) {
                        if ("provider" !in excludedFields) {
                            root.put("provider", JSONObject().apply { put("sort", "latency") })
                        }
                    } else if ("service_tier" !in excludedFields) {
                        root.put("service_tier", "priority")
                    }
                }
                "flex" -> {
                    if ("service_tier" !in excludedFields) root.put("service_tier", "flex")
                    if (isOpenRouter && "provider" !in excludedFields) {
                        root.put("provider", JSONObject().apply { put("sort", "price") })
                    }
                }
            }
        }

        if (profile.extraBodyJson.isNotBlank()) {
            val extraObj = runCatching { JSONObject(profile.extraBodyJson.trim()) }
                .getOrElse { throw IllegalArgumentException("附加 JSON 参数格式有误，请检查是否为合法 JSON 对象") }
            val keys = extraObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                root.put(k, extraObj.get(k))
            }
        }

        if (profile.stream && !profile.useExactUrl && "stream_options" !in excludedFields && !root.has("stream_options")) {
            root.put("stream_options", JSONObject().apply { put("include_usage", true) })
        }

        return root.toString()
    }

    private fun buildHeadersMap(profile: ApiProfile): Map<String, String> {
        val map = linkedMapOf<String, String>()
        map["Accept"] = "text/event-stream, application/json"
        if (profile.apiKey.isNotBlank()) {
            map["Authorization"] = "Bearer ${profile.apiKey.trim()}"
        }
        profile.customHeaders
            .filter { it.enabled && it.key.isNotBlank() }
            .forEach { header ->
                // 若用户自定义同名 Header（不区分大小写），先移除旧键再写入新值
                val existingKey = map.keys.firstOrNull { it.equals(header.key.trim(), ignoreCase = true) }
                if (existingKey != null) map.remove(existingKey)
                map[header.key.trim()] = header.value
            }
        return map
    }

    private fun maskHeaderValue(key: String, value: String): String {
        if (!key.equals("Authorization", ignoreCase = true) && !key.contains("key", ignoreCase = true)) {
            return value
        }
        if (value.length <= 14) return "***"
        return value.take(10) + "..." + value.takeLast(4)
    }

    private fun extractByPath(root: Any?, path: String): String? {
        if (root == null || path.isBlank()) return null
        val tokens = path.trim()
            .replace("[", ".")
            .replace("]", "")
            .split(".")
            .filter { it.isNotBlank() }

        var cur: Any? = root
        for (tok in tokens) {
            cur = when (cur) {
                is JSONObject -> if (cur.has(tok)) cur.get(tok) else null
                is JSONArray -> {
                    val idx = tok.toIntOrNull()
                    if (idx != null && idx in 0 until cur.length()) cur.get(idx) else null
                }
                else -> null
            }
            if (cur == null || cur == JSONObject.NULL) return null
        }
        return when (cur) {
            is JSONObject -> cur.toString(2)
            is JSONArray -> cur.toString(2)
            else -> cur.toString()
        }
    }

    private fun extractOpenAiOrGeneric(json: JSONObject, customPath: String): Pair<String, String> {
        if (customPath.isNotBlank()) {
            val extracted = extractByPath(json, customPath)
            if (extracted != null) return extracted to ""
        }

        val choices = json.optJSONArray("choices")
        if (choices != null && choices.length() > 0) {
            val first = choices.optJSONObject(0)
            if (first != null) {
                val delta = first.optJSONObject("delta")
                if (delta != null) {
                    val content = delta.optString("content", "")
                    val reasoning = delta.optString("reasoning_content", "")
                        .ifEmpty { delta.optString("reasoning", "") }
                    return content to reasoning
                }
                val message = first.optJSONObject("message")
                if (message != null) {
                    val content = message.optString("content", "")
                    val reasoning = message.optString("reasoning_content", "")
                        .ifEmpty { message.optString("reasoning", "") }
                    if (content.isNotEmpty() || reasoning.isNotEmpty()) {
                        return content to reasoning
                    }
                }
                val text = first.optString("text", "")
                if (text.isNotEmpty()) return text to ""
            }
        }

        // Anthropic Messages API 格式兼容
        val contentArr = json.optJSONArray("content")
        if (contentArr != null && contentArr.length() > 0) {
            val firstBlock = contentArr.optJSONObject(0)
            val txt = firstBlock?.optString("text", "").orEmpty()
            if (txt.isNotEmpty()) return txt to ""
        }
        val anthropicDelta = json.optJSONObject("delta")
        if (anthropicDelta != null) {
            val txt = anthropicDelta.optString("text", "")
            if (txt.isNotEmpty()) return txt to ""
        }

        // Gemini API 格式兼容
        val candidates = json.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val parts = candidates.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
            if (parts != null && parts.length() > 0) {
                val txt = parts.optJSONObject(0)?.optString("text", "").orEmpty()
                if (txt.isNotEmpty()) return txt to ""
            }
        }

        // SSE 末尾单独的 usage 分片（choices 为空数组）或流式控制分片，不将其当成正文输出
        if (json.has("choices") ||
            json.optString("object") == "chat.completion.chunk" ||
            json.optString("type") in setOf(
                "message_start", "message_delta", "message_stop",
                "content_block_start", "content_block_stop", "ping"
            )
        ) {
            return "" to ""
        }

        // 若有常见通用文本字段则提取，否则直接美化输出整段 JSON
        for (key in listOf("reply", "answer", "output", "response", "text", "result")) {
            if (json.has(key) && !json.isNull(key)) {
                val v = json.get(key)
                if (v is String) return v to ""
            }
        }
        return json.toString(2) to ""
    }

    private fun extractUsageFromJson(json: JSONObject): TokenUsage? {
        val usageObj = json.optJSONObject("usage")
            ?: json.optJSONObject("message")?.optJSONObject("usage")
        if (usageObj != null) {
            val prompt = usageObj.optInt("prompt_tokens", -1).takeIf { it >= 0 }
                ?: usageObj.optInt("input_tokens", -1).takeIf { it >= 0 }
                ?: 0
            val completion = usageObj.optInt("completion_tokens", -1).takeIf { it >= 0 }
                ?: usageObj.optInt("output_tokens", -1).takeIf { it >= 0 }
                ?: 0
            val total = usageObj.optInt("total_tokens", -1).takeIf { it >= 0 }
                ?: (prompt + completion)

            val details = usageObj.optJSONObject("prompt_tokens_details")
                ?: usageObj.optJSONObject("input_tokens_details")
            val hit = usageObj.optInt("prompt_cache_hit_tokens", -1).takeIf { it >= 0 }
                ?: details?.optInt("cached_tokens", -1)?.takeIf { it >= 0 }
                ?: usageObj.optInt("cache_read_input_tokens", -1).takeIf { it >= 0 }
                ?: usageObj.optInt("cached_tokens", -1).takeIf { it >= 0 }
                ?: 0
            val miss = usageObj.optInt("prompt_cache_miss_tokens", -1).takeIf { it >= 0 }
                ?: if (prompt >= hit) prompt - hit else 0

            if (prompt > 0 || completion > 0 || total > 0 || hit > 0) {
                return TokenUsage(
                    promptTokens = prompt,
                    completionTokens = completion,
                    totalTokens = total,
                    cacheHitTokens = hit,
                    cacheMissTokens = miss,
                    isEstimated = false,
                )
            }
        }

        val geminiMeta = json.optJSONObject("usageMetadata")
        if (geminiMeta != null) {
            val prompt = geminiMeta.optInt("promptTokenCount", 0).coerceAtLeast(0)
            val completion = geminiMeta.optInt("candidatesTokenCount", 0).coerceAtLeast(0)
            val total = geminiMeta.optInt("totalTokenCount", prompt + completion).coerceAtLeast(prompt + completion)
            val hit = geminiMeta.optInt("cachedContentTokenCount", 0).coerceAtLeast(0)
            val miss = if (prompt >= hit) prompt - hit else 0
            if (prompt > 0 || completion > 0 || total > 0 || hit > 0) {
                return TokenUsage(
                    promptTokens = prompt,
                    completionTokens = completion,
                    totalTokens = total,
                    cacheHitTokens = hit,
                    cacheMissTokens = miss,
                    isEstimated = false,
                )
            }
        }
        return null
    }

    private fun mergeUsage(prev: TokenUsage?, next: TokenUsage?): TokenUsage? {
        if (next == null) return prev
        if (prev == null) return next
        val prompt = maxOf(prev.promptTokens, next.promptTokens)
        val completion = maxOf(prev.completionTokens, next.completionTokens)
        val total = maxOf(prev.totalTokens, next.totalTokens, prompt + completion)
        val hit = maxOf(prev.cacheHitTokens, next.cacheHitTokens)
        val miss = if (next.cacheMissTokens > 0) next.cacheMissTokens
            else if (prev.cacheMissTokens > 0) prev.cacheMissTokens
            else if (prompt >= hit) prompt - hit else 0
        return TokenUsage(
            promptTokens = prompt,
            completionTokens = completion,
            totalTokens = total,
            cacheHitTokens = hit,
            cacheMissTokens = miss,
            isEstimated = false,
        )
    }

    private fun estimateFallbackUsage(
        profile: ApiProfile,
        history: List<ChatMessage>,
        sessionSystemPrompt: String,
        responseContent: String,
        reasoningContent: String,
    ): TokenUsage {
        val effectiveSystem = sessionSystemPrompt.ifBlank { profile.systemPrompt }.trim()
        val promptEst = TokenEstimator.estimateConversationContext(effectiveSystem, history)
        val completionEst = TokenEstimator.estimateTokens(responseContent) +
            TokenEstimator.estimateTokens(reasoningContent)
        return TokenUsage(
            promptTokens = promptEst,
            completionTokens = completionEst,
            totalTokens = promptEst + completionEst,
            cacheHitTokens = 0,
            cacheMissTokens = promptEst,
            isEstimated = true,
        )
    }

    suspend fun sendMessage(
        profile: ApiProfile,
        history: List<ChatMessage>,
        sessionSystemPrompt: String,
        onStreamUpdate: (contentSoFar: String, reasoningSoFar: String, liveUsage: TokenUsage?) -> Unit,
        onCallCreated: (Call) -> Unit = {},
    ): ApiCallResult = sendMessageInternal(
        profile = profile,
        history = history,
        sessionSystemPrompt = sessionSystemPrompt,
        excludedFields = emptySet(),
        retriesLeft = 2,
        onStreamUpdate = onStreamUpdate,
        onCallCreated = onCallCreated,
    )

    private suspend fun sendMessageInternal(
        profile: ApiProfile,
        history: List<ChatMessage>,
        sessionSystemPrompt: String,
        excludedFields: Set<String>,
        retriesLeft: Int,
        onStreamUpdate: (contentSoFar: String, reasoningSoFar: String, liveUsage: TokenUsage?) -> Unit,
        onCallCreated: (Call) -> Unit,
    ): ApiCallResult = withContext(Dispatchers.IO) {
        val url = resolveEndpointUrl(profile)
        if (url.isBlank()) {
            return@withContext ApiCallResult(
                content = "请先在右上角「接口配置」中填写有效的 API Base URL。",
                reasoningContent = "",
                rawRequestPreview = "",
                rawResponsePreview = "",
                isError = true,
            )
        }

        val startMs = System.currentTimeMillis()
        val bodyString: String
        val headersMap: Map<String, String>
        try {
            bodyString = buildRequestBodyJson(profile, history, sessionSystemPrompt, excludedFields)
            headersMap = buildHeadersMap(profile)
        } catch (e: Exception) {
            return@withContext ApiCallResult(
                content = "构建请求体失败：${e.message}",
                reasoningContent = "",
                rawRequestPreview = "",
                rawResponsePreview = "",
                isError = true,
            )
        }

        val reqPreview = buildString {
            appendLine("POST $url")
            headersMap.forEach { (k, v) -> appendLine("$k: ${maskHeaderValue(k, v)}") }
            appendLine()
            val prettyBody = runCatching { JSONObject(bodyString).toString(2) }.getOrElse { bodyString }
            append(prettyBody.take(4000))
        }

        val reqBuilder = Request.Builder()
            .url(url)
            .post(bodyString.toRequestBody("application/json; charset=utf-8".toMediaType()))
        headersMap.forEach { (k, v) -> reqBuilder.header(k, v) }

        val call = client.newCall(reqBuilder.build())
        onCallCreated(call)

        try {
            call.execute().use { resp ->
                val elapsed = System.currentTimeMillis() - startMs
                val contentType = resp.header("Content-Type").orEmpty()

                if (!resp.isSuccessful) {
                    val errBody = resp.body?.string().orEmpty()
                    if (retriesLeft > 0 && (resp.code == 400 || resp.code == 422)) {
                        val newlyExcluded = mutableSetOf<String>()
                        if (errBody.contains("stream_options", ignoreCase = true)) newlyExcluded += "stream_options"
                        if (errBody.contains("service_tier", ignoreCase = true)) newlyExcluded += "service_tier"
                        if (errBody.contains("provider", ignoreCase = true)) newlyExcluded += "provider"
                        if (errBody.contains("reasoning", ignoreCase = true)) newlyExcluded += "reasoning"
                        if (errBody.contains("temperature", ignoreCase = true)) newlyExcluded += "temperature"
                        if (errBody.contains("top_p", ignoreCase = true)) newlyExcluded += "top_p"
                        if (errBody.contains(":nitro", ignoreCase = true)) newlyExcluded += "nitro"

                        if ((newlyExcluded - excludedFields).isNotEmpty()) {
                            return@withContext sendMessageInternal(
                                profile = profile,
                                history = history,
                                sessionSystemPrompt = sessionSystemPrompt,
                                excludedFields = excludedFields + newlyExcluded,
                                retriesLeft = retriesLeft - 1,
                                onStreamUpdate = onStreamUpdate,
                                onCallCreated = onCallCreated,
                            )
                        }
                    }
                    val formattedErr = runCatching {
                        val tok = JSONTokener(errBody).nextValue()
                        if (tok is JSONObject) tok.toString(2) else errBody
                    }.getOrElse { errBody }
                    return@withContext ApiCallResult(
                        content = "接口返回错误 (HTTP ${resp.code}):\n$formattedErr",
                        reasoningContent = "",
                        rawRequestPreview = reqPreview,
                        rawResponsePreview = "HTTP ${resp.code} (${elapsed}ms)\nContent-Type: $contentType\n\n$formattedErr",
                        isError = true,
                    )
                }

                val isEventStream = profile.stream && contentType.contains("text/event-stream", ignoreCase = true)
                val body = resp.body ?: return@withContext ApiCallResult(
                    content = "(接口返回为空)",
                    reasoningContent = "",
                    rawRequestPreview = reqPreview,
                    rawResponsePreview = "HTTP ${resp.code} (${elapsed}ms) - Empty Body",
                    isError = false,
                )

                if (isEventStream) {
                    val contentAcc = StringBuilder()
                    val reasoningAcc = StringBuilder()
                    val rawTrace = StringBuilder()
                    val source = body.source()
                    var capturedUsage: TokenUsage? = null
                    val estPromptBase = TokenEstimator.estimateConversationContext(
                        sessionSystemPrompt.ifBlank { profile.systemPrompt }.trim(),
                        history,
                    )

                    while (!source.exhausted()) {
                        currentCoroutineContext().ensureActive()
                        val line = source.readUtf8Line() ?: break
                        if (rawTrace.length < 6000) {
                            rawTrace.appendLine(line)
                        }
                        val trimmed = line.trim()
                        if (trimmed.isEmpty() || trimmed.startsWith(":")) continue
                        if (!trimmed.startsWith("data:")) continue

                        val dataStr = trimmed.removePrefix("data:").trim()
                        if (dataStr == "[DONE]") break

                        val jsonObj = runCatching { JSONObject(dataStr) }.getOrNull()
                        if (jsonObj != null) {
                            capturedUsage = mergeUsage(capturedUsage, extractUsageFromJson(jsonObj))
                            val (cChunk, rChunk) = extractOpenAiOrGeneric(jsonObj, profile.customResponsePath)
                            if (cChunk.isNotEmpty() || rChunk.isNotEmpty() || capturedUsage != null) {
                                contentAcc.append(cChunk)
                                reasoningAcc.append(rChunk)
                                val liveEstCompletion = TokenEstimator.estimateTokens(contentAcc.toString()) +
                                    TokenEstimator.estimateTokens(reasoningAcc.toString())
                                val liveUsage = capturedUsage ?: TokenUsage(
                                    promptTokens = estPromptBase,
                                    completionTokens = liveEstCompletion,
                                    totalTokens = estPromptBase + liveEstCompletion,
                                    cacheHitTokens = 0,
                                    cacheMissTokens = estPromptBase,
                                    isEstimated = true,
                                )
                                onStreamUpdate(contentAcc.toString(), reasoningAcc.toString(), liveUsage)
                            }
                        }
                    }

                    val finalContent = contentAcc.toString()
                    val finalReasoning = reasoningAcc.toString()
                    val finalUsage = capturedUsage ?: estimateFallbackUsage(
                        profile, history, sessionSystemPrompt, finalContent, finalReasoning,
                    )
                    val totalElapsed = System.currentTimeMillis() - startMs
                    return@withContext ApiCallResult(
                        content = finalContent.ifEmpty { "(流式响应结束，未收到文本内容，请点击详情查看原始报文)" },
                        reasoningContent = finalReasoning,
                        rawRequestPreview = reqPreview,
                        rawResponsePreview = "HTTP ${resp.code} SSE (${totalElapsed}ms)\nContent-Type: $contentType\n\n${rawTrace.toString().take(6000)}",
                        isError = false,
                        usage = finalUsage,
                    )
                } else {
                    val rawStr = body.string()
                    // 有些网关虽然未返回 text/event-stream 头，但实际返回了 data: {...} SSE 内容
                    if (rawStr.trimStart().startsWith("data:")) {
                        val contentAcc = StringBuilder()
                        val reasoningAcc = StringBuilder()
                        var capturedUsage: TokenUsage? = null
                        rawStr.lineSequence().forEach { line ->
                            val t = line.trim()
                            if (t.startsWith("data:")) {
                                val d = t.removePrefix("data:").trim()
                                if (d != "[DONE]") {
                                    runCatching { JSONObject(d) }.getOrNull()?.let { obj ->
                                        capturedUsage = mergeUsage(capturedUsage, extractUsageFromJson(obj))
                                        val (c, r) = extractOpenAiOrGeneric(obj, profile.customResponsePath)
                                        contentAcc.append(c)
                                        reasoningAcc.append(r)
                                    }
                                }
                            }
                        }
                        if (contentAcc.isNotEmpty() || reasoningAcc.isNotEmpty()) {
                            val finalContent = contentAcc.toString()
                            val finalReasoning = reasoningAcc.toString()
                            val finalUsage = capturedUsage ?: estimateFallbackUsage(
                                profile, history, sessionSystemPrompt, finalContent, finalReasoning,
                            )
                            return@withContext ApiCallResult(
                                content = finalContent,
                                reasoningContent = finalReasoning,
                                rawRequestPreview = reqPreview,
                                rawResponsePreview = "HTTP ${resp.code} (${elapsed}ms)\n\n${rawStr.take(6000)}",
                                usage = finalUsage,
                            )
                        }
                    }

                    val parsed = runCatching { JSONTokener(rawStr).nextValue() }.getOrNull()
                    val capturedUsage = if (parsed is JSONObject) extractUsageFromJson(parsed) else null
                    val (finalContent, finalReasoning) = when (parsed) {
                        is JSONObject -> extractOpenAiOrGeneric(parsed, profile.customResponsePath)
                        is JSONArray -> {
                            val byPath = extractByPath(parsed, profile.customResponsePath)
                            (byPath ?: parsed.toString(2)) to ""
                        }
                        else -> rawStr to ""
                    }
                    val prettyRaw = when (parsed) {
                        is JSONObject -> parsed.toString(2)
                        is JSONArray -> parsed.toString(2)
                        else -> rawStr
                    }
                    val finalUsage = capturedUsage ?: estimateFallbackUsage(
                        profile, history, sessionSystemPrompt, finalContent, finalReasoning,
                    )

                    return@withContext ApiCallResult(
                        content = finalContent.ifEmpty { "(响应为空)" },
                        reasoningContent = finalReasoning,
                        rawRequestPreview = reqPreview,
                        rawResponsePreview = "HTTP ${resp.code} (${elapsed}ms)\nContent-Type: $contentType\n\n${prettyRaw.take(6000)}",
                        usage = finalUsage,
                    )
                }
            }
        } catch (e: IOException) {
            val isCanceled = call.isCanceled()
            return@withContext ApiCallResult(
                content = if (isCanceled) "[已手动停止生成]" else "网络请求异常：${e.message}",
                reasoningContent = "",
                rawRequestPreview = reqPreview,
                rawResponsePreview = "Exception: ${e.javaClass.simpleName}: ${e.message}",
                isError = !isCanceled,
            )
        } catch (e: Exception) {
            return@withContext ApiCallResult(
                content = "处理响应失败：${e.message}",
                reasoningContent = "",
                rawRequestPreview = reqPreview,
                rawResponsePreview = "Exception: ${e.javaClass.simpleName}: ${e.message}",
                isError = true,
            )
        }
    }

    suspend fun fetchModels(profile: ApiProfile): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = resolveModelsUrl(profile)
            require(url.isNotBlank()) { "请先填写 Base URL" }
            val headersMap = buildHeadersMap(profile)
            val reqBuilder = Request.Builder().url(url).get()
            headersMap.forEach { (k, v) ->
                if (!k.equals("Accept", ignoreCase = true)) reqBuilder.header(k, v)
            }
            client.newCall(reqBuilder.build()).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    error("HTTP ${resp.code}: ${raw.take(200)}")
                }
                val obj = JSONObject(raw)
                val data = obj.optJSONArray("data") ?: obj.optJSONArray("models")
                val models = mutableListOf<String>()
                if (data != null) {
                    for (i in 0 until data.length()) {
                        val item = data.opt(i)
                        when (item) {
                            is JSONObject -> {
                                val id = item.optString("id", "").ifEmpty { item.optString("name", "") }
                                if (id.isNotBlank()) models.add(id.removePrefix("models/"))
                            }
                            is String -> if (item.isNotBlank()) models.add(item)
                        }
                    }
                }
                require(models.isNotEmpty()) { "接口未返回模型列表" }
                val sorted = models.sorted()
                val preferredPrefix = when {
                    !ModelCatalog.isOpenRouter(profile) -> null
                    profile.id.contains("claude", true) || profile.model.contains("claude", true) || profile.name.contains("claude", true) -> "anthropic/"
                    profile.id.contains("gpt", true) || profile.model.startsWith("openai/", true) || profile.name.contains("chatgpt", true) -> "openai/"
                    else -> null
                }
                if (preferredPrefix != null) {
                    val top = sorted.filter { it.startsWith(preferredPrefix, ignoreCase = true) }
                    val rest = sorted.filterNot { it.startsWith(preferredPrefix, ignoreCase = true) }
                    top + rest
                } else {
                    sorted
                }
            }
        }
    }
}
