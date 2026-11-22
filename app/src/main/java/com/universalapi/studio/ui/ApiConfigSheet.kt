package com.universalapi.studio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.background
import com.universalapi.studio.ui.theme.Brand
import com.universalapi.studio.ui.theme.GradientButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.universalapi.studio.data.ApiProfile
import com.universalapi.studio.data.HeaderPair
import com.universalapi.studio.data.ModelCatalog
import com.universalapi.studio.network.UniversalApiClient
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiConfigScreen(
    profiles: List<ApiProfile>,
    activeProfileId: String,
    onSaveAll: (updatedProfiles: List<ApiProfile>, newActiveId: String) -> Unit,
    onExportJson: (List<ApiProfile>) -> String,
    onImportJson: (String) -> Result<List<ApiProfile>>,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var localProfiles by remember(profiles) { mutableStateOf(profiles) }
    var editingId by remember(activeProfileId) { mutableStateOf(activeProfileId) }
    val current = localProfiles.firstOrNull { it.id == editingId } ?: localProfiles.first()

    var showApiKey by remember { mutableStateOf(false) }
    var isFetchingModels by remember { mutableStateOf(false) }
    var fetchedModels by remember { mutableStateOf<List<String>>(emptyList()) }
    var showModelMenu by remember { mutableStateOf(false) }
    var feedbackMsg by remember { mutableStateOf<String?>(null) }
    var showImportExportDialog by remember { mutableStateOf(false) }
    var jsonTransferText by remember { mutableStateOf("") }

    fun updateCurrent(transform: (ApiProfile) -> ApiProfile) {
        localProfiles = localProfiles.map {
            if (it.id == current.id) transform(it) else it
        }
    }

    Scaffold(
        containerColor = Brand.Bg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Brand.Bg),
                title = { Text("通用 API 渠道与高级参数") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        jsonTransferText = onExportJson(localProfiles)
                        showImportExportDialog = true
                    }) {
                        Icon(Icons.Default.Download, contentDescription = "导入导出")
                    }
                    TextButton(onClick = {
                        onSaveAll(localProfiles, current.id)
                        onClose()
                    }) {
                        Text("保存", fontWeight = FontWeight.Bold)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 配置方案切换栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("接口配置方案", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        val newProf = ApiProfile(
                            id = UUID.randomUUID().toString(),
                            name = "新接口 ${localProfiles.size + 1}",
                        )
                        localProfiles = localProfiles + newProf
                        editingId = newProf.id
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("新增")
                    }
                    TextButton(onClick = {
                        val copy = current.copy(
                            id = UUID.randomUUID().toString(),
                            name = "${current.name} (副本)",
                        )
                        localProfiles = localProfiles + copy
                        editingId = copy.id
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("复制")
                    }
                    if (localProfiles.size > 1) {
                        IconButton(onClick = {
                            val filtered = localProfiles.filter { it.id != current.id }
                            localProfiles = filtered
                            editingId = filtered.first().id
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除当前配置", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(localProfiles, key = { it.id }) { prof ->
                    FilterChip(
                        selected = prof.id == current.id,
                        onClick = { editingId = prof.id },
                        label = { Text(prof.name.ifBlank { "未命名" }) },
                    )
                }
            }

            feedbackMsg?.let { msg ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            // 基础接口设置
            Text("基础连接配置", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = current.name,
                onValueChange = { v -> updateCurrent { it.copy(name = v) } },
                label = { Text("配置名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = current.baseUrl,
                onValueChange = { v -> updateCurrent { it.copy(baseUrl = v) } },
                label = { Text("API Base URL / 完整请求地址") },
                placeholder = { Text("如 https://api.openai.com/v1") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "实际请求 URL: ${UniversalApiClient.resolveEndpointUrl(current)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("严格使用原始 URL（不自动拼接后缀）", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "接入非 OpenAI 通用 POST 接口时请开启",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = current.useExactUrl,
                    onCheckedChange = { v -> updateCurrent { it.copy(useExactUrl = v) } },
                )
            }

            OutlinedTextField(
                value = current.apiKey,
                onValueChange = { v -> updateCurrent { it.copy(apiKey = v) } },
                label = { Text("API Key（自动填入 Authorization: Bearer，可留空）") },
                singleLine = true,
                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showApiKey = !showApiKey }) {
                        Icon(
                            if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "显示/隐藏",
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = current.model,
                        onValueChange = { v -> updateCurrent { it.copy(model = v) } },
                        label = { Text("模型名称 (Model)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DropdownMenu(
                        expanded = showModelMenu,
                        onDismissRequest = { showModelMenu = false },
                    ) {
                        fetchedModels.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m) },
                                onClick = {
                                    updateCurrent { it.copy(model = m) }
                                    showModelMenu = false
                                },
                            )
                        }
                    }
                }
                OutlinedButton(
                    enabled = !isFetchingModels,
                    onClick = {
                        scope.launch {
                            isFetchingModels = true
                            feedbackMsg = null
                            UniversalApiClient.fetchModels(current)
                                .onSuccess { list ->
                                    fetchedModels = list
                                    showModelMenu = true
                                    feedbackMsg = "成功获取 ${list.size} 个可用模型，请在下拉框中选择"
                                }
                                .onFailure { err ->
                                    feedbackMsg = "拉取模型失败：${err.message}"
                                }
                            isFetchingModels = false
                        }
                    },
                ) {
                    if (isFetchingModels) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    Text("拉取")
                }
            }

            val familyPresets = remember(current.id, current.baseUrl, current.model) {
                ModelCatalog.presetsForProfile(current)
            }
            if (familyPresets.isNotEmpty()) {
                Text(
                    "同家族快捷切换",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(familyPresets, key = { it.id }) { preset ->
                        FilterChip(
                            selected = current.model.substringBefore(":").equals(preset.id, ignoreCase = true),
                            onClick = {
                                updateCurrent {
                                    it.copy(model = preset.id, contextWindow = preset.contextWindow)
                                }
                            },
                            label = {
                                val suffix = if (preset.tag.isNotBlank()) " · ${preset.tag}" else ""
                                Text("${preset.displayName}$suffix")
                            },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("流式传输 (SSE Stream)", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "边生成边打字输出；测试普通 JSON 接口可关闭",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = current.stream,
                    onCheckedChange = { v -> updateCurrent { it.copy(stream = v) } },
                )
            }

            HorizontalDivider()

            // 自定义请求头
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("自定义请求头 (Headers)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                TextButton(onClick = {
                    updateCurrent {
                        it.copy(customHeaders = it.customHeaders + HeaderPair())
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("添加 Header")
                }
            }

            if (current.customHeaders.isEmpty()) {
                Text(
                    "暂无自定义请求头，可添加 x-api-key、HTTP-Referer、User-Agent 等任意头。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                current.customHeaders.forEach { header ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Switch(
                            checked = header.enabled,
                            onCheckedChange = { enabled ->
                                updateCurrent { prof ->
                                    prof.copy(customHeaders = prof.customHeaders.map {
                                        if (it.id == header.id) it.copy(enabled = enabled) else it
                                    })
                                }
                            },
                        )
                        OutlinedTextField(
                            value = header.key,
                            onValueChange = { k ->
                                updateCurrent { prof ->
                                    prof.copy(customHeaders = prof.customHeaders.map {
                                        if (it.id == header.id) it.copy(key = k) else it
                                    })
                                }
                            },
                            label = { Text("Header Key") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = header.value,
                            onValueChange = { v ->
                                updateCurrent { prof ->
                                    prof.copy(customHeaders = prof.customHeaders.map {
                                        if (it.id == header.id) it.copy(value = v) else it
                                    })
                                }
                            },
                            label = { Text("Value") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = {
                            updateCurrent { prof ->
                                prof.copy(customHeaders = prof.customHeaders.filter { it.id != header.id })
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除 Header")
                        }
                    }
                }
            }

            HorizontalDivider()

            // 高级参数与 Body 模板
            Text("高级模型参数 & Body 模板", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = current.systemPrompt,
                onValueChange = { v -> updateCurrent { it.copy(systemPrompt = v) } },
                label = { Text("System Prompt (默认系统提示词)") },
                minLines = 2,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
            )

            Column {
                Text(
                    "Temperature: ${String.format(java.util.Locale.US, "%.2f", current.temperature)}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Slider(
                    value = current.temperature,
                    onValueChange = { v -> updateCurrent { it.copy(temperature = v) } },
                    valueRange = 0f..2f,
                )
            }

            Column {
                Text(
                    "Top P: ${String.format(java.util.Locale.US, "%.2f", current.topP)}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Slider(
                    value = current.topP,
                    onValueChange = { v -> updateCurrent { it.copy(topP = v) } },
                    valueRange = 0f..1f,
                )
            }

            OutlinedTextField(
                value = current.maxTokens?.toString().orEmpty(),
                onValueChange = { v ->
                    val parsed = v.filter { it.isDigit() }.toIntOrNull()
                    updateCurrent { it.copy(maxTokens = parsed) }
                },
                label = { Text("Max Tokens（留空表示不限制）") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = current.contextWindow.toString(),
                onValueChange = { v ->
                    val parsed = v.filter { it.isDigit() }.toIntOrNull()
                    if (parsed != null && parsed > 0) {
                        updateCurrent { it.copy(contextWindow = parsed) }
                    }
                },
                label = { Text("最大上下文窗口 (Context Window Tokens)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val presets = listOf(
                    "32K" to 32000,
                    "64K" to 64000,
                    "128K" to 128000,
                    "200K" to 200000,
                    "1M" to 1000000,
                )
                items(presets) { (label, tokens) ->
                    FilterChip(
                        selected = current.contextWindow == tokens,
                        onClick = { updateCurrent { it.copy(contextWindow = tokens) } },
                        label = { Text(label) },
                    )
                }
            }

            Text("思考等级 (Reasoning Effort)", style = MaterialTheme.typography.bodySmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ModelCatalog.reasoningEfforts, key = { it.value }) { opt ->
                    FilterChip(
                        selected = current.reasoningEffort == opt.value,
                        onClick = { updateCurrent { it.copy(reasoningEffort = opt.value) } },
                        label = { Text(opt.label) },
                    )
                }
            }

            Text("速度 / 服务等级 (Speed / Service Tier)", style = MaterialTheme.typography.bodySmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ModelCatalog.speedModes, key = { it.value }) { opt ->
                    FilterChip(
                        selected = current.speedMode == opt.value,
                        onClick = { updateCurrent { it.copy(speedMode = opt.value) } },
                        label = { Text(opt.label) },
                    )
                }
            }

            OutlinedTextField(
                value = current.extraBodyJson,
                onValueChange = { v -> updateCurrent { it.copy(extraBodyJson = v) } },
                label = { Text("附加顶层 JSON 参数 (Extra Body)") },
                placeholder = { Text("例如 {\"reasoning_effort\": \"high\"}") },
                minLines = 2,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("自定义 Body 模板（留空使用标准 OpenAI 格式）", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = {
                    updateCurrent {
                        it.copy(
                            customBodyTemplate = "{\n  \"model\": \"{{model}}\",\n  \"prompt\": {{prompt_json}},\n  \"system\": {{system_prompt_json}},\n  \"temperature\": {{temperature}}\n}",
                        )
                    }
                }) {
                    Text("填入示例模板")
                }
            }

            OutlinedTextField(
                value = current.customBodyTemplate,
                onValueChange = { v -> updateCurrent { it.copy(customBodyTemplate = v) } },
                label = { Text("Custom Body Template") },
                placeholder = { Text("支持 {{model}} {{prompt_json}} {{system_prompt_json}} {{messages_json}} {{temperature}} {{stream}}") },
                minLines = 3,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = current.customResponsePath,
                onValueChange = { v -> updateCurrent { it.copy(customResponsePath = v) } },
                label = { Text("自定义响应 JSON 提取路径（可选）") },
                placeholder = { Text("如 data.reply 或 choices[0].message.content；留空自动识别") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            GradientButton(
                onClick = {
                    onSaveAll(localProfiles, current.id)
                    onClose()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("保存并启用此接口配置")
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Designed & Authored by @Azumaserne",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showImportExportDialog) {
        AlertDialog(
            onDismissRequest = { showImportExportDialog = false },
            title = { Text("导入 / 导出全部 API 配置 (JSON)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "你可以复制下方 JSON 备份配置，或粘贴他人分享的 JSON 后点导入：",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = jsonTransferText,
                        onValueChange = { jsonTransferText = it },
                        minLines = 6,
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onImportJson(jsonTransferText)
                        .onSuccess { imported ->
                            localProfiles = imported
                            editingId = imported.first().id
                            showImportExportDialog = false
                            feedbackMsg = "成功导入 ${imported.size} 个 API 配置，请记得点击右上角保存"
                        }
                        .onFailure { err ->
                            feedbackMsg = "导入失败：${err.message}"
                            showImportExportDialog = false
                        }
                }) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("导入覆盖")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportExportDialog = false }) {
                    Text("关闭")
                }
            },
        )
    }
}
