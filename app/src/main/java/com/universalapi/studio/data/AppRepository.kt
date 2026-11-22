package com.universalapi.studio.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class AppRepository(context: Context) {
    private val prefs = context.getSharedPreferences("uapi_oss_data", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PROFILES = "api_profiles_json"
        private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"
        private const val KEY_SESSIONS = "chat_sessions_json"
        private const val KEY_ACTIVE_SESSION_ID = "active_session_id"
        private const val KEY_MIGRATED_OPENROUTER_V11 = "migrated_openrouter_v11"

        private fun openRouterDefaultHeaders(): List<HeaderPair> = listOf(
            HeaderPair(key = "HTTP-Referer", value = "https://github.com/kaka714/Universal-API-Studio", enabled = true),
            HeaderPair(key = "X-Title", value = "Universal API Studio", enabled = true),
        )

        fun openRouterPresetProfiles(): List<ApiProfile> = listOf(
            ApiProfile(
                id = "default-openrouter-gpt",
                name = "OpenRouter · ChatGPT",
                baseUrl = "https://openrouter.ai/api/v1",
                model = "openai/gpt-6.2-sol",
                stream = true,
                contextWindow = 1_050_000,
                reasoningEffort = "medium",
                speedMode = "",
                customHeaders = openRouterDefaultHeaders(),
            ),
            ApiProfile(
                id = "default-openrouter-claude",
                name = "OpenRouter · Claude",
                baseUrl = "https://openrouter.ai/api/v1",
                model = "anthropic/claude-sonnet-5.5",
                stream = true,
                contextWindow = 1_000_000,
                reasoningEffort = "medium",
                speedMode = "",
                customHeaders = openRouterDefaultHeaders(),
            ),
        )

        fun defaultProfiles(): List<ApiProfile> = openRouterPresetProfiles() + listOf(
            ApiProfile(
                id = "default-deepseek",
                name = "DeepSeek API 模板",
                baseUrl = "https://api.deepseek.com/v1",
                model = "deepseek-chat",
                stream = true,
                systemPrompt = "",
            ),
            ApiProfile(
                id = "default-openai",
                name = "OpenAI 兼容默认接口",
                baseUrl = "https://api.openai.com/v1",
                model = "gpt-4o-mini",
                stream = true,
                systemPrompt = "你是一个专业、简洁、高效的助手。",
            ),
            ApiProfile(
                id = "default-custom-http",
                name = "通用 POST JSON 接口模板",
                baseUrl = "https://httpbin.org/post",
                useExactUrl = true,
                model = "custom",
                stream = false,
                customHeaders = listOf(
                    HeaderPair(key = "X-Client-From", value = "UniversalApiStudio", enabled = true),
                ),
                customBodyTemplate = "{\n  \"model\": \"{{model}}\",\n  \"prompt\": {{prompt_json}},\n  \"temperature\": {{temperature}}\n}",
                customResponsePath = "",
            ),
        )
    }

    fun loadProfiles(): List<ApiProfile> {
        val raw = prefs.getString(KEY_PROFILES, null)
        if (raw == null) {
            val defaults = defaultProfiles()
            saveProfiles(defaults)
            prefs.edit().putBoolean(KEY_MIGRATED_OPENROUTER_V11, true).apply()
            return defaults
        }
        val parsed = runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { add(ApiProfile.fromJson(it)) }
                }
            }.ifEmpty { defaultProfiles() }
        }.getOrElse { defaultProfiles() }

        if (!prefs.getBoolean(KEY_MIGRATED_OPENROUTER_V11, false)) {
            val existingIds = parsed.map { it.id }.toSet()
            val missing = openRouterPresetProfiles().filter { it.id !in existingIds }
            val merged = if (missing.isNotEmpty()) missing + parsed else parsed
            if (missing.isNotEmpty()) {
                saveProfiles(merged)
            }
            prefs.edit().putBoolean(KEY_MIGRATED_OPENROUTER_V11, true).apply()
            return merged
        }
        return parsed
    }

    fun applyKeyToMatchingOpenRouterProfiles(
        profiles: List<ApiProfile>,
        editedProfile: ApiProfile,
        newKey: String,
    ): List<ApiProfile> {
        val trimmedKey = newKey.trim()
        val isOpenRouterKey = ModelCatalog.isOpenRouter(editedProfile) ||
            trimmedKey.startsWith("sk-or-", ignoreCase = true)
        return profiles.map { p ->
            when {
                p.id == editedProfile.id -> p.copy(apiKey = trimmedKey)
                isOpenRouterKey && trimmedKey.isNotEmpty() && ModelCatalog.isOpenRouter(p) && p.apiKey.isBlank() ->
                    p.copy(apiKey = trimmedKey)
                else -> p
            }
        }
    }

    fun saveProfiles(profiles: List<ApiProfile>) {
        val arr = JSONArray().apply { profiles.forEach { put(it.toJson()) } }
        prefs.edit().putString(KEY_PROFILES, arr.toString()).apply()
    }

    fun getActiveProfileId(profiles: List<ApiProfile>): String {
        val saved = prefs.getString(KEY_ACTIVE_PROFILE_ID, null)
        return if (saved != null && profiles.any { it.id == saved }) saved else profiles.first().id
    }

    fun saveActiveProfileId(id: String) {
        prefs.edit().putString(KEY_ACTIVE_PROFILE_ID, id).apply()
    }

    fun loadSessions(defaultProfileId: String): List<ChatSession> {
        val raw = prefs.getString(KEY_SESSIONS, null)
        if (raw.isNullOrBlank()) {
            val initial = listOf(ChatSession(title = "新对话", profileId = defaultProfileId))
            saveSessions(initial)
            return initial
        }
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { add(ChatSession.fromJson(it)) }
                }
            }.ifEmpty { listOf(ChatSession(title = "新对话", profileId = defaultProfileId)) }
        }.getOrElse { listOf(ChatSession(title = "新对话", profileId = defaultProfileId)) }
    }

    fun saveSessions(sessions: List<ChatSession>) {
        val arr = JSONArray().apply { sessions.forEach { put(it.toJson()) } }
        prefs.edit().putString(KEY_SESSIONS, arr.toString()).apply()
    }

    fun getActiveSessionId(sessions: List<ChatSession>): String {
        val saved = prefs.getString(KEY_ACTIVE_SESSION_ID, null)
        return if (saved != null && sessions.any { it.id == saved }) saved else sessions.first().id
    }

    fun saveActiveSessionId(id: String) {
        prefs.edit().putString(KEY_ACTIVE_SESSION_ID, id).apply()
    }

    fun exportProfilesJson(profiles: List<ApiProfile>): String {
        val arr = JSONArray().apply { profiles.forEach { put(it.toJson()) } }
        return arr.toString(2)
    }

    fun importProfilesJson(rawJson: String): Result<List<ApiProfile>> = runCatching {
        val trimmed = rawJson.trim()
        val parsed = if (trimmed.startsWith("[")) {
            val arr = JSONArray(trimmed)
            buildList {
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { add(ApiProfile.fromJson(it)) }
                }
            }
        } else {
            listOf(ApiProfile.fromJson(JSONObject(trimmed)))
        }
        require(parsed.isNotEmpty()) { "未解析到任何有效配置" }
        parsed
    }
}
