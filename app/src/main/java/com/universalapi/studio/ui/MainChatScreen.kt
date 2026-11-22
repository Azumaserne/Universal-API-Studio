package com.universalapi.studio.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universalapi.studio.data.ApiProfile
import com.universalapi.studio.data.AppRepository
import com.universalapi.studio.data.ChatMessage
import com.universalapi.studio.data.ChatSession
import com.universalapi.studio.data.ModelCatalog
import com.universalapi.studio.data.TokenEstimator
import com.universalapi.studio.i18n.AppLanguage
import com.universalapi.studio.i18n.LocalAppLanguage
import com.universalapi.studio.i18n.LocalAppStrings
import com.universalapi.studio.network.UniversalApiClient
import com.universalapi.studio.ui.theme.Brand
import com.universalapi.studio.ui.theme.GradientButton
import com.universalapi.studio.ui.theme.glassCard
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.Call
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainChatScreen(
    repository: AppRepository,
    initialProfiles: List<ApiProfile>,
    initialActiveProfileId: String,
    onChangeLanguage: (AppLanguage) -> Unit,
) {
    val strings = LocalAppStrings.current
    val currentLang = LocalAppLanguage.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val listState = rememberLazyListState()

    var profiles by remember { mutableStateOf(initialProfiles) }
    var activeProfileId by remember { mutableStateOf(initialActiveProfileId) }
    var sessions by remember { mutableStateOf(repository.loadSessions(initialActiveProfileId)) }
    var activeSessionId by remember { mutableStateOf(repository.getActiveSessionId(sessions)) }

    val currentSession = sessions.firstOrNull { it.id == activeSessionId } ?: sessions.first()
    val currentProfile = profiles.firstOrNull { it.id == activeProfileId } ?: profiles.first()

    var inputText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var activeCall by remember { mutableStateOf<Call?>(null) }
    var activeJob by remember { mutableStateOf<Job?>(null) }

    var showProfileDropdown by remember { mutableStateOf(false) }
    var showConfigEditor by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showSessionPromptDialog by remember { mutableStateOf(false) }
    var showUsageDialog by remember { mutableStateOf(false) }
    var inspectingMessage by remember { mutableStateOf<ChatMessage?>(null) }

    var showModelDropdown by remember { mutableStateOf(false) }
    var showEffortDropdown by remember { mutableStateOf(false) }
    var showSpeedDropdown by remember { mutableStateOf(false) }
    var fetchedQuickModels by remember(currentProfile.id) { mutableStateOf<List<String>>(emptyList()) }
    var isFetchingQuickModels by remember { mutableStateOf(false) }

    fun updateCurrentProfile(transform: (ApiProfile) -> ApiProfile) {
        val updated = profiles.map {
            if (it.id == currentProfile.id) transform(it) else it
        }
        profiles = updated
        repository.saveProfiles(updated)
    }

    val usageSummary = remember(
        currentSession.messages,
        currentSession.sessionSystemPrompt,
        currentProfile,
    ) {
        val effectiveSystem = currentSession.sessionSystemPrompt.ifBlank { currentProfile.systemPrompt }
        val lastIdx = currentSession.messages.indexOfLast {
            it.role == "assistant" && !it.isError && (it.totalTokens > 0 || it.promptTokens > 0)
        }
        val lastAssistant = if (lastIdx >= 0) currentSession.messages[lastIdx] else null
        val tailMessages = if (lastIdx >= 0 && lastIdx + 1 < currentSession.messages.size) {
            currentSession.messages.subList(lastIdx + 1, currentSession.messages.size)
        } else {
            emptyList()
        }
        val tailEst = if (tailMessages.isNotEmpty()) {
            TokenEstimator.estimateConversationContext("", tailMessages)
        } else 0

        val currentContextTokens = if (lastAssistant != null) {
            (lastAssistant.totalTokens.takeIf { it > 0 }
                ?: (lastAssistant.promptTokens + lastAssistant.completionTokens)) + tailEst
        } else {
            TokenEstimator.estimateConversationContext(effectiveSystem, currentSession.messages)
        }

        val ctxWindow = currentProfile.contextWindow.coerceAtLeast(1)
        val ctxRatio = (currentContextTokens.toFloat() / ctxWindow.toFloat()).coerceIn(0f, 1f)
        val lastPrompt = lastAssistant?.promptTokens ?: 0
        val lastCompletion = lastAssistant?.completionTokens ?: 0
        val lastTotal = lastAssistant?.totalTokens ?: 0
        val lastHit = lastAssistant?.cacheHitTokens ?: 0
        val lastMiss = lastAssistant?.cacheMissTokens ?: 0
        val lastHitPct = if (lastPrompt > 0) (lastHit * 100f / lastPrompt).coerceIn(0f, 100f) else 0f

        val validRounds = currentSession.messages.filter {
            it.role == "assistant" && !it.isError && it.promptTokens > 0
        }
        val sumPrompt = validRounds.sumOf { it.promptTokens }
        val sumCompletion = validRounds.sumOf { it.completionTokens }
        val sumHit = validRounds.sumOf { it.cacheHitTokens }
        val sumHitPct = if (sumPrompt > 0) (sumHit * 100f / sumPrompt).coerceIn(0f, 100f) else 0f

        SessionUsageSummary(
            currentContextTokens = currentContextTokens,
            contextWindow = ctxWindow,
            contextRatio = ctxRatio,
            hasAssistantStats = lastAssistant != null,
            lastPromptTokens = lastPrompt,
            lastCompletionTokens = lastCompletion,
            lastTotalTokens = lastTotal,
            lastCacheHitTokens = lastHit,
            lastCacheMissTokens = lastMiss,
            lastCacheHitRatePct = lastHitPct,
            lastIsEstimated = lastAssistant?.usageIsEstimated ?: true,
            sessionTotalPrompt = sumPrompt,
            sessionTotalCompletion = sumCompletion,
            sessionTotalCacheHit = sumHit,
            sessionCacheHitRatePct = sumHitPct,
        )
    }

    fun copyText(text: String, tip: String = strings.copiedMsg) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("uapi", text))
        Toast.makeText(context, tip, Toast.LENGTH_SHORT).show()
    }

    fun updateSessions(newList: List<ChatSession>, persist: Boolean = true) {
        sessions = newList
        if (persist) repository.saveSessions(newList)
    }

    fun updateSessionById(
        sessionId: String,
        persist: Boolean = true,
        transform: (ChatSession) -> ChatSession,
    ) {
        updateSessions(
            sessions.map { if (it.id == sessionId) transform(it) else it },
            persist,
        )
    }

    fun updateCurrentSession(transform: (ChatSession) -> ChatSession) {
        updateSessionById(currentSession.id, true, transform)
    }

    fun createNewSession() {
        val newSess = ChatSession(
            title = "${strings.newChatPrefix} ${sessions.size + 1}",
            profileId = currentProfile.id,
        )
        updateSessions(listOf(newSess) + sessions)
        activeSessionId = newSess.id
        repository.saveActiveSessionId(newSess.id)
    }

    fun sendPrompt(overridePrompt: String? = null, retryFromHistory: List<ChatMessage>? = null) {
        val text = (overridePrompt ?: inputText).trim()
        if (text.isEmpty() && retryFromHistory == null) return
        if (isGenerating) return

        val baseMessages = retryFromHistory ?: (currentSession.messages + ChatMessage(
            role = "user",
            content = text,
        ))
        val newTitle = if (currentSession.messages.isEmpty() && text.isNotBlank()) {
            text.take(18)
        } else {
            currentSession.title
        }

        val assistantPlaceholderId = UUID.randomUUID().toString()
        val placeholderMsg = ChatMessage(
            id = assistantPlaceholderId,
            role = "assistant",
            content = String.format(Locale.US, strings.requestingFmt, currentProfile.name),
        )

        if (overridePrompt == null && retryFromHistory == null) {
            inputText = ""
        }

        updateCurrentSession {
            it.copy(
                title = newTitle,
                messages = baseMessages + placeholderMsg,
                updatedAt = System.currentTimeMillis(),
            )
        }

        isGenerating = true
        val targetSessionId = currentSession.id
        val profileSnapshot = currentProfile
        val systemPromptSnapshot = currentSession.sessionSystemPrompt
        val job = scope.launch {
            try {
                val result = UniversalApiClient.sendMessage(
                    profile = profileSnapshot,
                    history = baseMessages,
                    sessionSystemPrompt = systemPromptSnapshot,
                    onStreamUpdate = { contentSoFar, reasoningSoFar, liveUsage ->
                        scope.launch {
                            // 流式过程中只更新内存，不写本地存储，避免长回复卡顿
                            updateSessionById(targetSessionId, persist = false) { sess ->
                                sess.copy(
                                    messages = sess.messages.map { m ->
                                        if (m.id == assistantPlaceholderId) {
                                            m.copy(
                                                content = contentSoFar.ifEmpty { strings.receivingMsg },
                                                reasoningContent = reasoningSoFar,
                                                promptTokens = liveUsage?.promptTokens ?: m.promptTokens,
                                                completionTokens = liveUsage?.completionTokens ?: m.completionTokens,
                                                totalTokens = liveUsage?.totalTokens ?: m.totalTokens,
                                                cacheHitTokens = liveUsage?.cacheHitTokens ?: m.cacheHitTokens,
                                                cacheMissTokens = liveUsage?.cacheMissTokens ?: m.cacheMissTokens,
                                                usageIsEstimated = liveUsage?.isEstimated ?: m.usageIsEstimated,
                                            )
                                        } else m
                                    },
                                )
                            }
                        }
                    },
                    onCallCreated = { call -> activeCall = call },
                )

                updateSessionById(targetSessionId, persist = true) { sess ->
                    sess.copy(
                        messages = sess.messages.map { m ->
                            if (m.id == assistantPlaceholderId) {
                                val u = result.usage
                                m.copy(
                                    content = result.content,
                                    reasoningContent = result.reasoningContent,
                                    isError = result.isError,
                                    rawRequestPreview = result.rawRequestPreview,
                                    rawResponsePreview = result.rawResponsePreview,
                                    promptTokens = u?.promptTokens ?: m.promptTokens,
                                    completionTokens = u?.completionTokens ?: m.completionTokens,
                                    totalTokens = u?.totalTokens ?: m.totalTokens,
                                    cacheHitTokens = u?.cacheHitTokens ?: m.cacheHitTokens,
                                    cacheMissTokens = u?.cacheMissTokens ?: m.cacheMissTokens,
                                    usageIsEstimated = u?.isEstimated ?: m.usageIsEstimated,
                                )
                            } else m
                        },
                        updatedAt = System.currentTimeMillis(),
                    )
                }
            } finally {
                // 无论正常结束还是手动停止，都把当前内容落盘一次
                repository.saveSessions(sessions)
                isGenerating = false
                activeCall = null
                activeJob = null
            }
        }
        activeJob = job
    }

    LaunchedEffect(currentSession.messages.size) {
        if (currentSession.messages.isNotEmpty()) {
            listState.animateScrollToItem(currentSession.messages.size - 1)
        }
    }

    if (showConfigEditor) {
        ApiConfigScreen(
            profiles = profiles,
            activeProfileId = activeProfileId,
            onChangeLanguage = onChangeLanguage,
            onSaveAll = { updated, newActiveId ->
                val edited = updated.firstOrNull { it.id == newActiveId }
                val synced = if (edited != null && edited.apiKey.isNotBlank()) {
                    repository.applyKeyToMatchingOpenRouterProfiles(updated, edited, edited.apiKey)
                } else {
                    updated
                }
                profiles = synced
                activeProfileId = newActiveId
                repository.saveProfiles(synced)
                repository.saveActiveProfileId(newActiveId)
                Toast.makeText(context, strings.configSavedMsg, Toast.LENGTH_SHORT).show()
            },
            onExportJson = { repository.exportProfilesJson(it) },
            onImportJson = { repository.importProfilesJson(it) },
            onClose = { showConfigEditor = false },
        )
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Brand.Surface) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                ) {
                    Text(
                        text = strings.sessionsTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                    )
                    GradientButton(
                        onClick = {
                            createNewSession()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.newChatBtn)
                    }
                    Spacer(Modifier.height(10.dp))
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(sessions, key = { it.id }) { sess ->
                            NavigationDrawerItem(
                                shape = RoundedCornerShape(14.dp),
                                colors = androidx.compose.material3.NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = Brand.Accent.copy(alpha = 0.16f),
                                    selectedTextColor = Brand.AccentSoft,
                                    unselectedContainerColor = Color.Transparent,
                                ),
                                label = {
                                    Text(
                                        sess.title.ifBlank { strings.defaultChatTitle },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                selected = sess.id == currentSession.id,
                                onClick = {
                                    activeSessionId = sess.id
                                    repository.saveActiveSessionId(sess.id)
                                    scope.launch { drawerState.close() }
                                },
                                badge = {
                                    if (sessions.size > 1) {
                                        IconButton(
                                            onClick = {
                                                val remaining = sessions.filter { it.id != sess.id }
                                                updateSessions(remaining)
                                                if (activeSessionId == sess.id) {
                                                    activeSessionId = remaining.first().id
                                                    repository.saveActiveSessionId(activeSessionId)
                                                }
                                            },
                                            modifier = Modifier.size(24.dp),
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = strings.deleteSessionCd,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    }
                    HorizontalDivider()
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            scope.launch { drawerState.close() }
                            showLanguageDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "🌐 ${strings.languageSettingBtn}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            scope.launch { drawerState.close() }
                            showAboutDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            strings.aboutOssBtn,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Universal API Studio OSS · @Azumaserne",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
            }
        },
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.background(Brand.PageBackground),
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Brand.TextPrimary,
                        navigationIconContentColor = Brand.TextSecondary,
                        actionIconContentColor = Brand.TextSecondary,
                    ),
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = strings.menuCd)
                        }
                    },
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                Column(
                                    modifier = Modifier.clickable { showProfileDropdown = true },
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = currentProfile.name.ifBlank { strings.unnamedProfile },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false),
                                        )
                                        Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                    Text(
                                        text = "${UniversalApiClient.resolveEffectiveModelId(currentProfile)} · ${if (currentProfile.stream) "SSE" else "JSON"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                DropdownMenu(
                                    expanded = showProfileDropdown,
                                    onDismissRequest = { showProfileDropdown = false },
                                ) {
                                    profiles.forEach { p ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(p.name, fontWeight = if (p.id == currentProfile.id) FontWeight.Bold else FontWeight.Normal)
                                                    Text(p.model, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            },
                                            onClick = {
                                                activeProfileId = p.id
                                                repository.saveActiveProfileId(p.id)
                                                showProfileDropdown = false
                                            },
                                        )
                                    }
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text(strings.manageProfilesItem) },
                                        onClick = {
                                            showProfileDropdown = false
                                            showConfigEditor = true
                                        },
                                    )
                                }
                            }
                            ContextCacheStatusPill(
                                summary = usageSummary,
                                onClick = { showUsageDialog = true },
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showConfigEditor = true }) {
                            Icon(Icons.Default.Settings, contentDescription = strings.settingsCd)
                        }
                        IconButton(onClick = { showAboutDialog = true }) {
                            Icon(Icons.Default.Info, contentDescription = strings.aboutCd, tint = Brand.AccentSoft)
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding(),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (currentSession.messages.isEmpty()) {
                        item {
                            EmptyStateCard(
                                profile = currentProfile,
                                onSaveApiKey = { rawKey ->
                                    val trimmed = rawKey.trim()
                                    if (trimmed.isEmpty()) {
                                        Toast.makeText(context, strings.enterValidKeyMsg, Toast.LENGTH_SHORT).show()
                                    } else {
                                        val synced = repository.applyKeyToMatchingOpenRouterProfiles(
                                            profiles = profiles,
                                            editedProfile = currentProfile,
                                            newKey = trimmed,
                                        )
                                        profiles = synced
                                        repository.saveProfiles(synced)
                                        Toast.makeText(context, strings.keySavedReadyMsg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onOpenConfig = { showConfigEditor = true },
                                onSendTest = { sendPrompt(strings.testConnectivityPrompt) },
                            )
                        }
                    } else {
                        items(currentSession.messages, key = { it.id }) { msg ->
                            MessageBubble(
                                message = msg,
                                onCopy = { copyText(msg.content) },
                                onInspect = { inspectingMessage = msg },
                                onRetry = {
                                    val idx = currentSession.messages.indexOfFirst { it.id == msg.id }
                                    if (idx > 0) {
                                        val truncated = currentSession.messages.subList(0, idx)
                                        sendPrompt(retryFromHistory = truncated)
                                    }
                                },
                            )
                        }
                    }
                }

                // 底部输入栏
                Surface(
                    color = Brand.Surface.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Brand.Border, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val familyPresets = remember(currentProfile.id, currentProfile.baseUrl, currentProfile.model) {
                                ModelCatalog.presetsForProfile(currentProfile)
                            }

                            Box {
                                QuickControlChip(
                                    label = "${ModelCatalog.displayModelShort(currentProfile.model)} ⌄",
                                    highlighted = false,
                                    accentColor = Brand.Accent,
                                    onClick = { showModelDropdown = true },
                                )
                                DropdownMenu(
                                    expanded = showModelDropdown,
                                    onDismissRequest = { showModelDropdown = false },
                                ) {
                                    if (familyPresets.isNotEmpty()) {
                                        familyPresets.forEach { preset ->
                                            val isSelected = currentProfile.model.substringBefore(":")
                                                .equals(preset.id, ignoreCase = true)
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(
                                                            text = if (preset.tag.isNotBlank()) "${preset.displayName} · ${preset.tag}" else preset.displayName,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        )
                                                        Text(
                                                            text = preset.id,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        )
                                                    }
                                                },
                                                trailingIcon = if (isSelected) {
                                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Brand.Success) }
                                                } else null,
                                                onClick = {
                                                    updateCurrentProfile {
                                                        it.copy(model = preset.id, contextWindow = preset.contextWindow)
                                                    }
                                                    showModelDropdown = false
                                                },
                                            )
                                        }
                                        HorizontalDivider()
                                    }
                                    if (fetchedQuickModels.isNotEmpty()) {
                                        fetchedQuickModels.take(25).forEach { mId ->
                                            val isSelected = currentProfile.model.equals(mId, ignoreCase = true)
                                            DropdownMenuItem(
                                                text = { Text(mId, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                                trailingIcon = if (isSelected) {
                                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Brand.Success) }
                                                } else null,
                                                onClick = {
                                                    updateCurrentProfile { it.copy(model = mId) }
                                                    showModelDropdown = false
                                                },
                                            )
                                        }
                                        HorizontalDivider()
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Text(if (isFetchingQuickModels) strings.fetchingModelsItem else strings.fetchOnlineModelsItem)
                                        },
                                        enabled = !isFetchingQuickModels,
                                        onClick = {
                                            scope.launch {
                                                isFetchingQuickModels = true
                                                UniversalApiClient.fetchModels(currentProfile)
                                                    .onSuccess { list ->
                                                        fetchedQuickModels = list
                                                        Toast.makeText(
                                                            context,
                                                            String.format(Locale.US, strings.fetchedModelsToastFmt, list.size),
                                                            Toast.LENGTH_SHORT,
                                                        ).show()
                                                    }
                                                    .onFailure { err ->
                                                        Toast.makeText(
                                                            context,
                                                            String.format(Locale.US, strings.fetchFailedToastFmt, err.message.orEmpty()),
                                                            Toast.LENGTH_SHORT,
                                                        ).show()
                                                    }
                                                isFetchingQuickModels = false
                                            }
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(strings.customModelConfigItem) },
                                        onClick = {
                                            showModelDropdown = false
                                            showConfigEditor = true
                                        },
                                    )
                                }
                            }

                            Box {
                                QuickControlChip(
                                    label = "${strings.effortPrefix}: ${strings.effortShort(currentProfile.reasoningEffort)} ⌄",
                                    highlighted = currentProfile.reasoningEffort.isNotBlank() && currentProfile.reasoningEffort != "none",
                                    accentColor = Brand.AccentSoft,
                                    onClick = { showEffortDropdown = true },
                                )
                                DropdownMenu(
                                    expanded = showEffortDropdown,
                                    onDismissRequest = { showEffortDropdown = false },
                                ) {
                                    ModelCatalog.reasoningEfforts.forEach { opt ->
                                        val isSelected = currentProfile.reasoningEffort == opt.value
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        strings.effortLabel(opt.value),
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    )
                                                    if (opt.hint.isNotBlank() && (currentLang == AppLanguage.ZH_CN || currentLang == AppLanguage.ZH_HK)) {
                                                        Text(opt.hint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            },
                                            trailingIcon = if (isSelected) {
                                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Brand.Success) }
                                            } else null,
                                            onClick = {
                                                updateCurrentProfile { it.copy(reasoningEffort = opt.value) }
                                                showEffortDropdown = false
                                            },
                                        )
                                    }
                                }
                            }

                            Box {
                                QuickControlChip(
                                    label = "${strings.speedPrefix}: ${strings.speedShort(currentProfile.speedMode)} ⌄",
                                    highlighted = currentProfile.speedMode.isNotBlank(),
                                    accentColor = Brand.Cyan,
                                    onClick = { showSpeedDropdown = true },
                                )
                                DropdownMenu(
                                    expanded = showSpeedDropdown,
                                    onDismissRequest = { showSpeedDropdown = false },
                                ) {
                                    ModelCatalog.speedModes.forEach { opt ->
                                        val isSelected = currentProfile.speedMode == opt.value
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        strings.speedLabel(opt.value),
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    )
                                                    if (opt.hint.isNotBlank() && (currentLang == AppLanguage.ZH_CN || currentLang == AppLanguage.ZH_HK)) {
                                                        Text(opt.hint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            },
                                            trailingIcon = if (isSelected) {
                                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Brand.Success) }
                                            } else null,
                                            onClick = {
                                                updateCurrentProfile { it.copy(speedMode = opt.value) }
                                                showSpeedDropdown = false
                                            },
                                        )
                                    }
                                }
                            }
                        }

                        if (currentSession.sessionSystemPrompt.isNotBlank()) {
                            Text(
                                text = strings.sessionPromptHint,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            IconButton(
                                onClick = { showSessionPromptDialog = true },
                                modifier = Modifier.size(44.dp),
                            ) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = strings.sessionPromptCd,
                                    tint = if (currentSession.sessionSystemPrompt.isNotBlank()) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = { Text(strings.inputPlaceholder) },
                                maxLines = 5,
                                shape = RoundedCornerShape(22.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Brand.Input,
                                    unfocusedContainerColor = Brand.Input,
                                    focusedBorderColor = Brand.Accent,
                                    unfocusedBorderColor = Brand.Border,
                                ),
                                modifier = Modifier.weight(1f),
                            )
                            if (isGenerating) {
                                IconButton(
                                    onClick = {
                                        activeCall?.cancel()
                                        activeJob?.cancel()
                                        isGenerating = false
                                    },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(MaterialTheme.colorScheme.error, CircleShape),
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = strings.stopCd, tint = Color.White)
                                }
                            } else {
                                IconButton(
                                    onClick = { sendPrompt() },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(Brand.Gradient, CircleShape),
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = strings.sendCd, tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // HTTP 报文调试弹窗
    inspectingMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { inspectingMessage = null },
            title = { Text(strings.packetDialogTitle) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(strings.requestPacketHeader, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    SelectionContainer {
                        Text(
                            text = msg.rawRequestPreview.ifBlank { "-" },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                        )
                    }
                    HorizontalDivider()
                    Text(strings.responsePacketHeader, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    SelectionContainer {
                        Text(
                            text = msg.rawResponsePreview.ifBlank { "-" },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    copyText("【Request】\n${msg.rawRequestPreview}\n\n【Response】\n${msg.rawResponsePreview}")
                }) {
                    Text(strings.copyAllPacketsBtn)
                }
            },
            dismissButton = {
                TextButton(onClick = { inspectingMessage = null }) {
                    Text(strings.closeBtn)
                }
            },
        )
    }

    // 会话级 System Prompt 弹窗
    if (showSessionPromptDialog) {
        var tempPrompt by remember(currentSession.id) { mutableStateOf(currentSession.sessionSystemPrompt) }
        AlertDialog(
            onDismissRequest = { showSessionPromptDialog = false },
            title = { Text(strings.sessionPromptDialogTitle) },
            text = {
                OutlinedTextField(
                    value = tempPrompt,
                    onValueChange = { tempPrompt = it },
                    label = { Text(strings.sessionPromptDialogLabel) },
                    minLines = 3,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    updateCurrentSession { it.copy(sessionSystemPrompt = tempPrompt) }
                    showSessionPromptDialog = false
                }) {
                    Text(strings.okBtn)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSessionPromptDialog = false }) {
                    Text(strings.cancelBtn)
                }
            },
        )
    }

    // 关于开源版弹窗
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(strings.aboutDialogTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(strings.aboutVersionLine, style = MaterialTheme.typography.bodySmall)
                    Text(strings.aboutPackageLine, style = MaterialTheme.typography.bodySmall)
                    Text(strings.aboutLicenseLine, style = MaterialTheme.typography.bodySmall, color = Brand.AccentSoft)
                    Text("${strings.authorPrefix}@Azumaserne", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        strings.aboutDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = Brand.Success,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(strings.okBtn)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    copyText("https://github.com/Azumaserne/Universal-API-Studio", strings.copiedRepoMsg)
                }) {
                    Text(strings.copyRepoBtn)
                }
            },
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(strings.chooseLanguageTitle) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    AppLanguage.entries.forEach { lang ->
                        val selected = lang == currentLang
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) Brand.Accent.copy(alpha = 0.16f) else Color.Transparent,
                                )
                                .clickable {
                                    onChangeLanguage(lang)
                                    showLanguageDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = lang.nativeName,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Brand.AccentSoft else Brand.TextPrimary,
                            )
                            if (selected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Brand.Success,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(strings.closeBtn)
                }
            },
        )
    }

    if (showUsageDialog) {
        UsageDetailsDialog(
            profile = currentProfile,
            summary = usageSummary,
            onSelectContextWindow = { newWindow ->
                val updated = profiles.map {
                    if (it.id == currentProfile.id) it.copy(contextWindow = newWindow) else it
                }
                profiles = updated
                repository.saveProfiles(updated)
            },
            onDismiss = { showUsageDialog = false },
        )
    }
}

@Composable
private fun EmptyStateCard(
    profile: ApiProfile,
    onSaveApiKey: (String) -> Unit,
    onOpenConfig: () -> Unit,
    onSendTest: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var apiKeyDraft by remember(profile.id) { mutableStateOf("") }
    val needsKey = profile.apiKey.isBlank() && !profile.baseUrl.contains("httpbin.org", ignoreCase = true)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth().glassCard(22),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = strings.emptyReadyTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = String.format(
                    Locale.US,
                    strings.emptyChannelInfoFmt,
                    profile.name,
                    UniversalApiClient.resolveEndpointUrl(profile),
                    UniversalApiClient.resolveEffectiveModelId(profile),
                    profile.customHeaders.count { it.enabled },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (needsKey) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brand.Input.copy(alpha = 0.75f), RoundedCornerShape(14.dp))
                        .border(1.dp, Brand.Warning.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = strings.pasteKeyTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = Brand.Warning,
                        fontWeight = FontWeight.Bold,
                    )
                    if (ModelCatalog.isOpenRouter(profile)) {
                        Text(
                            text = strings.openRouterKeyHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = Brand.TextSecondary,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = apiKeyDraft,
                            onValueChange = { apiKeyDraft = it },
                            placeholder = { Text("sk-...", fontSize = 13.sp) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = { onSaveApiKey(apiKeyDraft) },
                        ) {
                            Text(strings.saveBtn)
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = cm.primaryClip?.getItemAt(0)?.text?.toString().orEmpty().trim()
                            if (clip.isEmpty()) {
                                Toast.makeText(context, strings.clipboardEmptyKeyMsg, Toast.LENGTH_SHORT).show()
                            } else {
                                apiKeyDraft = clip
                                onSaveApiKey(clip)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.pasteEnableKeyBtn)
                    }
                }
            }
            Text(
                text = "by @Azumaserne",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onOpenConfig, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.configureApiBtn)
                }
                GradientButton(onClick = onSendTest, modifier = Modifier.weight(1f)) {
                    Text(strings.testConnectivityBtn)
                }
            }
        }
    }
}

@Composable
private fun QuickControlChip(
    label: String,
    highlighted: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (highlighted) accentColor.copy(alpha = 0.14f) else Brand.SurfaceHigh.copy(alpha = 0.75f))
            .border(
                1.dp,
                if (highlighted) accentColor.copy(alpha = 0.6f) else Brand.Border,
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = if (highlighted) accentColor else Brand.TextSecondary,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    onCopy: () -> Unit,
    onInspect: () -> Unit,
    onRetry: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val isUser = message.role == "user"
    var showReasoning by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        val bubbleShape = RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomStart = if (isUser) 20.dp else 6.dp,
            bottomEnd = if (isUser) 6.dp else 20.dp,
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isUser -> Color(0xFF2A3170)
                    message.isError -> Color(0xFF3A1A22)
                    else -> Brand.SurfaceHigh
                },
            ),
            shape = bubbleShape,
            modifier = Modifier
                .widthIn(max = 330.dp)
                .border(
                    1.dp,
                    when {
                        isUser -> Brand.Accent.copy(alpha = 0.35f)
                        message.isError -> Brand.Danger.copy(alpha = 0.4f)
                        else -> Brand.Border
                    },
                    bubbleShape,
                ),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!isUser && message.reasoningContent.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x22000000), RoundedCornerShape(8.dp))
                            .clickable { showReasoning = !showReasoning }
                            .padding(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = String.format(
                                    Locale.US,
                                    strings.reasoningFoldFmt,
                                    if (showReasoning) strings.collapseWord else strings.expandWord,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = Brand.AccentSoft,
                            )
                            Icon(
                                if (showReasoning) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Brand.AccentSoft,
                            )
                        }
                        if (showReasoning) {
                            Spacer(Modifier.height(4.dp))
                            SelectionContainer {
                                Text(
                                    text = message.reasoningContent,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                SelectionContainer {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (!isUser) {
                    if (message.totalTokens > 0 || message.promptTokens > 0) {
                        val hitPct = if (message.promptTokens > 0) {
                            (message.cacheHitTokens * 100 / message.promptTokens).coerceIn(0, 100)
                        } else 0
                        val estPrefix = if (message.usageIsEstimated) "≈" else ""
                        Text(
                            text = String.format(
                                Locale.US,
                                strings.bubbleUsageFmt,
                                estPrefix,
                                message.totalTokens,
                                message.promptTokens,
                                message.completionTokens,
                                message.cacheHitTokens,
                                hitPct,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.5.sp,
                            color = if (message.cacheHitTokens > 0) Brand.Cyan else Brand.TextTertiary,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AssistChip(
                            onClick = onCopy,
                            label = { Text(strings.copyBtn, fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                        )
                        if (message.rawRequestPreview.isNotBlank() || message.rawResponsePreview.isNotBlank()) {
                            AssistChip(
                                onClick = onInspect,
                                label = { Text(strings.packetBtn, fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                            )
                        }
                        AssistChip(
                            onClick = onRetry,
                            label = { Text(strings.retryBtn, fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                        )
                    }
                }
            }
        }
    }
}

private data class SessionUsageSummary(
    val currentContextTokens: Int,
    val contextWindow: Int,
    val contextRatio: Float,
    val hasAssistantStats: Boolean,
    val lastPromptTokens: Int,
    val lastCompletionTokens: Int,
    val lastTotalTokens: Int,
    val lastCacheHitTokens: Int,
    val lastCacheMissTokens: Int,
    val lastCacheHitRatePct: Float,
    val lastIsEstimated: Boolean,
    val sessionTotalPrompt: Int,
    val sessionTotalCompletion: Int,
    val sessionTotalCacheHit: Int,
    val sessionCacheHitRatePct: Float,
)

@Composable
private fun ContextCacheStatusPill(
    summary: SessionUsageSummary,
    onClick: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val usageColor = when {
        summary.contextRatio >= 0.8f -> Brand.Danger
        summary.contextRatio >= 0.5f -> Brand.Warning
        else -> Brand.Success
    }
    val pctInt = (summary.contextRatio * 100f).toInt().coerceIn(0, 100)
    val ctxLabel = "${strings.contextWord} ${TokenEstimator.formatCompact(summary.currentContextTokens)}/${TokenEstimator.formatCompact(summary.contextWindow)} ($pctInt%)"

    val (cacheLabel, cacheColor) = when {
        !summary.hasAssistantStats -> strings.cacheWaiting to Brand.TextTertiary
        summary.lastCacheHitTokens > 0 ->
            "Cache ${summary.lastCacheHitRatePct.toInt()}% (${TokenEstimator.formatCompact(summary.lastCacheHitTokens)})" to Brand.Cyan
        summary.lastIsEstimated -> strings.cacheEstimated to Brand.TextSecondary
        else -> strings.cacheMissed to Brand.TextSecondary
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Brand.SurfaceHigh.copy(alpha = 0.85f))
            .border(1.dp, Brand.BorderStrong, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(usageColor, CircleShape),
                )
                Text(
                    text = ctxLabel,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Brand.TextPrimary,
                    maxLines = 1,
                    softWrap = false,
                )
            }
            Text(
                text = cacheLabel,
                fontSize = 9.5.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Medium,
                color = cacheColor,
                maxLines = 1,
                softWrap = false,
            )
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(2.5.dp)
                    .clip(CircleShape)
                    .background(Brand.Border),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(summary.contextRatio.coerceAtLeast(0.02f))
                        .height(2.5.dp)
                        .background(usageColor),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UsageDetailsDialog(
    profile: ApiProfile,
    summary: SessionUsageSummary,
    onSelectContextWindow: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val usageColor = when {
        summary.contextRatio >= 0.8f -> Brand.Danger
        summary.contextRatio >= 0.5f -> Brand.Warning
        else -> Brand.Success
    }
    val pctStr = String.format(Locale.US, "%.1f", summary.contextRatio * 100f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.usageDialogTitle) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = String.format(Locale.US, strings.usageWindowLabelFmt, profile.name),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                LinearProgressIndicator(
                    progress = { summary.contextRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = usageColor,
                    trackColor = Brand.Border,
                )
                Text(
                    text = "${summary.currentContextTokens} / ${summary.contextWindow} tokens ($pctStr%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = usageColor,
                    fontFamily = FontFamily.Monospace,
                )

                Text(
                    text = strings.usageQuickWindowHint,
                    style = MaterialTheme.typography.labelSmall,
                    color = Brand.TextSecondary,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val presets = listOf(
                        "32K" to 32000,
                        "64K" to 64000,
                        "128K" to 128000,
                        "200K" to 200000,
                        "1M" to 1000000,
                    )
                    presets.forEach { (label, tokens) ->
                        FilterChip(
                            selected = profile.contextWindow == tokens,
                            onClick = { onSelectContextWindow(tokens) },
                            label = { Text(label, fontSize = 11.sp) },
                        )
                    }
                }

                HorizontalDivider()

                Text(
                    text = strings.usageLastRoundTitle,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (!summary.hasAssistantStats) {
                    Text(
                        text = strings.usageNoResponseMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = Brand.TextSecondary,
                    )
                } else {
                    val hitRateStr = String.format(Locale.US, "%.1f", summary.lastCacheHitRatePct)
                    Text(
                        text = String.format(
                            Locale.US,
                            strings.usageRoundDetailFmt,
                            summary.lastPromptTokens,
                            summary.lastCompletionTokens,
                            summary.lastTotalTokens,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = String.format(
                            Locale.US,
                            strings.usageCacheDetailFmt,
                            summary.lastCacheHitTokens,
                            hitRateStr,
                            summary.lastCacheMissTokens,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (summary.lastCacheHitTokens > 0) Brand.Cyan else Brand.TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (summary.lastIsEstimated) {
                            strings.usageSourceEstimated
                        } else {
                            strings.usageSourceReal
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (summary.lastIsEstimated) Brand.Warning else Brand.Success,
                    )
                }

                HorizontalDivider()

                Text(
                    text = strings.usageSessionTitle,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                val sumRateStr = String.format(Locale.US, "%.1f", summary.sessionCacheHitRatePct)
                Text(
                    text = String.format(
                        Locale.US,
                        strings.usageSessionSummaryFmt,
                        summary.sessionTotalPrompt,
                        summary.sessionTotalCompletion,
                        summary.sessionTotalCacheHit,
                        sumRateStr,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Brand.TextSecondary,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.closeBtn)
            }
        },
    )
}
