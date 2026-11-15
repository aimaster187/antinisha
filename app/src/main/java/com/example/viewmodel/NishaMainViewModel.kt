package com.example.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.NishaSession
import com.example.data.firebase.FsAnnouncement
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.NishaDatabase
import com.example.data.local.VoiceMacroEntity
import com.example.data.local.VoiceHistoryEntity
import com.example.data.preferences.NishaSettingsState
import com.example.data.remote.GeminiAttachment
import com.example.data.remote.GeminiRestClient
import com.example.data.remote.GeminiTurn
import com.example.data.remote.WeatherSnapshot
import com.example.di.NishaAppContainer
import com.example.domain.AiCoreState
import com.example.domain.AssistantRuntimeCoordinator
import com.example.domain.BatteryTelemetry
import com.example.domain.PendingSensitiveAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class NishaMainViewModel(
    private val container: NishaAppContainer
) : ViewModel() {

    private val dao = container.dao
    private val prefs = container.preferences
    private val vault = container.secretVault
    private val gemini = container.geminiClient

    val settings: StateFlow<NishaSettingsState> = prefs.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NishaSettingsState()
    )

    val session: StateFlow<NishaSession> = combine(
        container.authRepository.firebaseAuthStateFlow(),
        prefs.settingsFlow
    ) { firebaseUser, st ->
        if (firebaseUser != null) {
            NishaSession.FirebaseGoogle(
                uid = firebaseUser.uid,
                displayName = firebaseUser.displayName ?: "NISHA Member",
                email = firebaseUser.email.orEmpty(),
                emailVerified = firebaseUser.isEmailVerified,
                username = (firebaseUser.email?.substringBefore("@") ?: "nisha_user")
            )
        } else if (st.activeLocalAccountId.isNotBlank()) {
            val local = dao.getLocalAccountById(st.activeLocalAccountId)
            if (local != null) {
                NishaSession.LocalVault(
                    accountId = local.id,
                    username = local.username,
                    fullName = local.fullName,
                    email = local.email,
                    phone = local.phone,
                    emailVerified = local.emailVerified
                )
            } else {
                NishaSession.Unauthenticated
            }
        } else {
            NishaSession.Unauthenticated
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, NishaSession.Unauthenticated)

    private val _authMessage = MutableStateFlow<String?>(null)
    val authMessage: StateFlow<String?> = _authMessage.asStateFlow()

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _hasApiKey = MutableStateFlow(vault.hasGeminiApiKey())
    val hasApiKey: StateFlow<Boolean> = _hasApiKey.asStateFlow()

    private val _maskedApiKey = MutableStateFlow(vault.getMaskedGeminiKeyPreview())
    val maskedApiKey: StateFlow<String> = _maskedApiKey.asStateFlow()

    private val _apiKeyStatus = MutableStateFlow<String?>(null)
    val apiKeyStatus: StateFlow<String?> = _apiKeyStatus.asStateFlow()

    private val _weather = MutableStateFlow<WeatherSnapshot?>(null)
    val weather: StateFlow<WeatherSnapshot?> = _weather.asStateFlow()

    private val _battery = MutableStateFlow(container.deviceActionRouter.readBatteryTelemetry())
    val battery: StateFlow<BatteryTelemetry> = _battery.asStateFlow()

    private val _announcements = MutableStateFlow<List<FsAnnouncement>>(emptyList())
    val announcements: StateFlow<List<FsAnnouncement>> = _announcements.asStateFlow()

    // Chat state
    val chatSessions: StateFlow<List<ChatSessionEntity>> = dao.observeChatSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeSessionId = MutableStateFlow("")
    val activeSessionId: StateFlow<String> = _activeSessionId.asStateFlow()

    val activeMessages: StateFlow<List<ChatMessageEntity>> = _activeSessionId
        .flatMapLatest { sid ->
            if (sid.isBlank()) flowOf(emptyList()) else dao.observeMessages(sid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _chatGenerating = MutableStateFlow(false)
    val chatGenerating: StateFlow<Boolean> = _chatGenerating.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    private val _pendingAttachment = MutableStateFlow<GeminiAttachment?>(null)
    val pendingAttachment: StateFlow<GeminiAttachment?> = _pendingAttachment.asStateFlow()

    // Voice OS state
    val voiceMacros: StateFlow<List<VoiceMacroEntity>> = dao.observeVoiceMacros()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val voiceHistory: StateFlow<List<VoiceHistoryEntity>> = dao.observeVoiceHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _lastVoiceResponse = MutableStateFlow("")
    val lastVoiceResponse: StateFlow<String> = _lastVoiceResponse.asStateFlow()

    private val _pendingAction = MutableStateFlow<PendingSensitiveAction?>(null)
    val pendingAction: StateFlow<PendingSensitiveAction?> = _pendingAction.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private var chatJob: Job? = null

    init {
        viewModelScope.launch {
            seedDefaultsIfNeeded()
        }
        viewModelScope.launch {
            container.authRepository.attemptSilentGoogleSignIn()
        }
        viewModelScope.launch {
            session.collect { current ->
                if (current is NishaSession.FirebaseGoogle) {
                    launch {
                        runCatching {
                            container.firestoreSync.observeBroadcastAnnouncements().collect { list ->
                                _announcements.value = list
                            }
                        }
                    }
                } else {
                    _announcements.value = emptyList()
                }
            }
        }
    }

    private suspend fun seedDefaultsIfNeeded() {
        val existingAgents = dao.getAgentsOnce()
        if (existingAgents.isEmpty()) {
            NishaDatabase.DEFAULT_FIVE_AGENTS.forEach { dao.upsertAgent(it) }
        }
        val existingSessions = dao.observeChatSessions().map { it }.let {
            // Seed initial welcome conversation if empty
            val memories = dao.getMemoriesOnce()
            if (memories.isEmpty()) {
                dao.upsertMemory(
                    com.example.data.local.MemoryFactEntity(
                        id = "mem_seed_lang",
                        category = "Preference",
                        title = "Communication Style",
                        content = "Supports English, Hindi, and Hinglish seamlessly.",
                        scope = "LOCAL"
                    )
                )
            }
        }
    }

    fun refreshBattery() {
        _battery.value = container.deviceActionRouter.readBatteryTelemetry()
    }

    fun loadWeather(lat: Double, lon: Double, label: String) {
        viewModelScope.launch {
            container.weatherClient.fetchCurrentWeather(lat, lon, label)
                .onSuccess { _weather.value = it }
        }
    }

    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            _authLoading.value = true
            _authMessage.value = null
            container.authRepository.signInWithGoogleInteractive(activity)
                .onSuccess { _authMessage.value = null }
                .onFailure { _authMessage.value = it.localizedMessage ?: "Google Sign-In failed" }
            _authLoading.value = false
        }
    }

    fun signUpLocal(
        username: String,
        fullName: String,
        email: String,
        phone: String,
        password: String,
        confirm: String
    ) {
        viewModelScope.launch {
            _authLoading.value = true
            container.authRepository.signUpLocalAccount(username, fullName, email, phone, password, confirm)
                .onSuccess { _authMessage.value = null }
                .onFailure { _authMessage.value = it.localizedMessage ?: "Sign up failed" }
            _authLoading.value = false
        }
    }

    fun signInLocal(identifier: String, password: String) {
        viewModelScope.launch {
            _authLoading.value = true
            container.authRepository.signInLocalAccount(identifier, password)
                .onSuccess { _authMessage.value = null }
                .onFailure { _authMessage.value = it.localizedMessage ?: "Sign in failed" }
            _authLoading.value = false
        }
    }

    fun resetLocalPassword(identifier: String, newPass: String, confirmPass: String) {
        viewModelScope.launch {
            container.authRepository.resetLocalPassword(identifier, newPass, confirmPass)
                .onSuccess { _authMessage.value = "Local password updated. You can sign in now." }
                .onFailure { _authMessage.value = it.localizedMessage ?: "Reset failed" }
        }
    }

    fun verifyLocalEmail(accountId: String) {
        viewModelScope.launch {
            container.authRepository.markLocalEmailVerified(accountId)
                .onSuccess { _snackbarMessage.value = "Local email verification confirmed." }
        }
    }

    fun updateLocalProfile(accountId: String, fullName: String, phone: String) {
        viewModelScope.launch {
            container.authRepository.updateLocalProfile(accountId, fullName, phone)
                .onSuccess { _snackbarMessage.value = "Profile saved." }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            container.authRepository.signOutAll()
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            val localId = settings.value.activeLocalAccountId
            container.authRepository.deleteActiveAccount(localId)
                .onSuccess { _snackbarMessage.value = "Account and cloud records deleted." }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Deletion failed." }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch { prefs.setOnboardingCompleted(true) }
    }

    fun saveAndVerifyGeminiKey(rawKey: String) {
        viewModelScope.launch {
            _apiKeyStatus.value = "Verifying key with Gemini API..."
            val model = settings.value.selectedModel
            gemini.verifyApiKey(rawKey, model)
                .onSuccess {
                    vault.saveGeminiApiKey(rawKey)
                    _hasApiKey.value = vault.hasGeminiApiKey()
                    _maskedApiKey.value = vault.getMaskedGeminiKeyPreview()
                    _apiKeyStatus.value = "Key verified and encrypted in Android Keystore."
                }
                .onFailure { err ->
                    _apiKeyStatus.value = "Verification failed: ${err.localizedMessage}"
                }
        }
    }

    fun saveGeminiKeyWithoutVerify(rawKey: String) {
        vault.saveGeminiApiKey(rawKey)
        _hasApiKey.value = vault.hasGeminiApiKey()
        _maskedApiKey.value = vault.getMaskedGeminiKeyPreview()
        _apiKeyStatus.value = "Key encrypted in Android Keystore."
    }

    fun clearGeminiKey() {
        vault.clearGeminiApiKey()
        _hasApiKey.value = false
        _maskedApiKey.value = vault.getMaskedGeminiKeyPreview()
        _apiKeyStatus.value = "API key cleared from Keystore."
    }

    fun selectModel(model: String) {
        viewModelScope.launch { prefs.setSelectedModel(model) }
    }

    // Chat actions
    fun startNewChat() {
        viewModelScope.launch {
            val id = "chat_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            dao.upsertChatSession(
                ChatSessionEntity(
                    id = id,
                    title = "New Conversation",
                    model = settings.value.selectedModel
                )
            )
            _activeSessionId.value = id
            _chatError.value = null
        }
    }

    fun selectChatSession(sessionId: String) {
        _activeSessionId.value = sessionId
        _chatError.value = null
    }

    fun renameChatSession(sessionId: String, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            val current = chatSessions.value.firstOrNull { it.id == sessionId } ?: return@launch
            dao.upsertChatSession(current.copy(title = newTitle.trim(), updatedAtMs = System.currentTimeMillis()))
        }
    }

    fun deleteChatSession(sessionId: String) {
        viewModelScope.launch {
            dao.deleteMessagesForSession(sessionId)
            dao.deleteChatSession(sessionId)
            if (_activeSessionId.value == sessionId) {
                _activeSessionId.value = ""
            }
        }
    }

    fun setPendingAttachment(attachment: GeminiAttachment?) {
        _pendingAttachment.value = attachment
    }

    fun sendChatMessage(promptText: String) {
        val clean = promptText.trim()
        if (clean.isEmpty() && _pendingAttachment.value == null) return

        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            _chatError.value = null
            _chatGenerating.value = true
            AssistantRuntimeCoordinator.setCoreState(AiCoreState.THINKING, "Nisha is composing response...")

            val currentSessionId = _activeSessionId.value.ifBlank {
                val id = "chat_${UUID.randomUUID().toString().replace("-", "").take(12)}"
                dao.upsertChatSession(
                    ChatSessionEntity(
                        id = id,
                        title = clean.take(42).ifBlank { "Multimodal Chat" },
                        model = settings.value.selectedModel
                    )
                )
                _activeSessionId.value = id
                id
            }

            val attachment = _pendingAttachment.value
            _pendingAttachment.value = null
            val existingMsgs = dao.getMessagesOnce(currentSessionId)

            val userMsg = ChatMessageEntity(
                sessionId = currentSessionId,
                role = "user",
                content = clean.ifBlank { "Analyze the attached ${attachment?.label ?: "file"}." },
                attachmentLabel = attachment?.label.orEmpty()
            )
            dao.insertMessage(userMsg)

            val memoriesText = if (settings.value.memoryEnabled) {
                dao.getMemoriesOnce().take(10).joinToString("\n") { "- [${it.category}] ${it.title}: ${it.content}" }
            } else ""

            val systemPrompt = buildString {
                append("You are NISHA AI, a refined, intelligent personal assistant. Respond clearly with clean Markdown and code blocks when useful.")
                if (memoriesText.isNotBlank()) {
                    append("\n\nUser-approved Memory context:\n$memoriesText")
                }
            }

            val history = existingMsgs.map { GeminiTurn(role = it.role, text = it.content) }
            val model = settings.value.selectedModel
            val result = gemini.generateContent(
                model = model,
                systemInstruction = systemPrompt,
                history = history,
                prompt = userMsg.content,
                attachment = attachment
            )

            result.onSuccess { reply ->
                val aiMsg = ChatMessageEntity(
                    sessionId = currentSessionId,
                    role = "assistant",
                    content = reply,
                    agentAttribution = "Nisha Core",
                    modelUsed = model
                )
                dao.insertMessage(aiMsg)
                val updatedTitle = if (existingMsgs.isEmpty()) clean.take(42).ifBlank { "Multimodal Chat" }
                else chatSessions.value.firstOrNull { it.id == currentSessionId }?.title ?: "Conversation"

                dao.upsertChatSession(
                    ChatSessionEntity(
                        id = currentSessionId,
                        title = updatedTitle,
                        model = model,
                        lastPreview = reply.take(90),
                        updatedAtMs = System.currentTimeMillis()
                    )
                )
                if (session.value is NishaSession.FirebaseGoogle && settings.value.cloudSyncEnabled) {
                    container.firestoreSync.syncConversation(
                        conversationId = currentSessionId,
                        title = updatedTitle,
                        model = model,
                        messageCount = (existingMsgs.size + 2).toLong(),
                        lastMessagePreview = reply.take(120)
                    )
                    container.firestoreSync.appendMessage(currentSessionId, userMsg.id, "user", userMsg.content)
                    container.firestoreSync.appendMessage(currentSessionId, aiMsg.id, "assistant", reply, "Nisha Core", model)
                }
            }.onFailure { err ->
                _chatError.value = err.localizedMessage ?: "Failed to generate response."
            }

            _chatGenerating.value = false
            AssistantRuntimeCoordinator.setCoreState(AiCoreState.IDLE, "Ready")
        }
    }

    fun regenerateLastResponse() {
        val msgs = activeMessages.value
        val lastUser = msgs.lastOrNull { it.role == "user" } ?: return
        val lastAi = msgs.lastOrNull { it.role == "assistant" }
        viewModelScope.launch {
            if (lastAi != null && lastAi.createdAtMs >= lastUser.createdAtMs) {
                dao.deleteMessage(lastAi.id)
            }
            sendChatMessage(lastUser.content)
        }
    }

    fun stopGeneration() {
        chatJob?.cancel()
        _chatGenerating.value = false
        AssistantRuntimeCoordinator.setCoreState(AiCoreState.IDLE, "Generation stopped")
    }

    // Voice OS processing
    fun handleVoiceInput(transcript: String, speakBack: (String) -> Unit) {
        viewModelScope.launch {
            val clean = transcript.trim()
            if (clean.isEmpty()) return@launch
            val lang = settings.value.voiceLanguage

            // 1. Check user-defined macros
            val matchedMacro = voiceMacros.value.firstOrNull {
                clean.contains(it.triggerPhrase, ignoreCase = true)
            }
            if (matchedMacro != null) {
                when (matchedMacro.targetType) {
                    "open_app" -> {
                        val res = container.deviceActionRouter.openInstalledApp(matchedMacro.payload)
                        val reply = res.getOrElse { it.localizedMessage ?: "Could not open app" }
                        recordVoiceTurn(clean, reply, lang, "Macro: ${matchedMacro.triggerPhrase}")
                        _lastVoiceResponse.value = reply
                        if (settings.value.voiceAutoSpeak) speakBack(reply)
                        return@launch
                    }
                    "whatsapp" -> {
                        proposeSensitiveAction(
                            PendingSensitiveAction(
                                title = "Confirm Macro WhatsApp",
                                description = "Run macro '${matchedMacro.triggerPhrase}' to open WhatsApp composer.",
                                actionType = "WHATSAPP",
                                target = "",
                                payload = matchedMacro.payload
                            )
                        )
                        return@launch
                    }
                }
            }

            // 2. Check safe read-only device telemetry or open app command
            val lower = clean.lowercase()
            if ("battery" in lower) {
                val tel = container.deviceActionRouter.readBatteryTelemetry()
                val reply = "Battery is at ${tel.levelPercent}%${if (tel.isCharging) " and currently charging" else ""}."
                recordVoiceTurn(clean, reply, lang, "Read battery")
                _lastVoiceResponse.value = reply
                if (settings.value.voiceAutoSpeak) speakBack(reply)
                return@launch
            }
            if (lower.startsWith("open ")) {
                val appName = clean.substring(5).trim()
                val res = container.deviceActionRouter.openInstalledApp(appName)
                if (res.isSuccess) {
                    val reply = res.getOrThrow()
                    recordVoiceTurn(clean, reply, lang, "Opened $appName")
                    _lastVoiceResponse.value = reply
                    if (settings.value.voiceAutoSpeak) speakBack(reply)
                    return@launch
                }
            }

            // 3. Check sensitive device intents requiring user confirmation
            val proposal = container.deviceActionRouter.proposeVoiceOrDeviceAction(clean)
            if (proposal != null) {
                _pendingAction.value = proposal
                val reply = "Please confirm '${proposal.title}' on screen before I proceed."
                recordVoiceTurn(clean, reply, lang, "Awaiting confirmation: ${proposal.actionType}")
                _lastVoiceResponse.value = reply
                if (settings.value.voiceAutoSpeak) speakBack(reply)
                return@launch
            }

            // 4. Route to Gemini Voice OS
            AssistantRuntimeCoordinator.setCoreState(AiCoreState.THINKING, "Thinking...")
            val langInstruction = when (lang) {
                "hi-IN" -> "Respond naturally in Hindi (Devanagari script) suitable for spoken voice output."
                "hinglish" -> "Respond naturally in conversational Hinglish (Hindi + English mix in Latin script) suitable for spoken voice output."
                else -> "Respond clearly in natural conversational English suitable for spoken voice output."
            }
            val recentVoiceHistory = voiceHistory.value.take(4).reversed().flatMap {
                listOf(GeminiTurn("user", it.transcript), GeminiTurn("model", it.response))
            }
            val res = gemini.generateContent(
                model = settings.value.selectedModel,
                systemInstruction = "You are Nisha Voice OS. Keep replies crisp, accurate, and conversational. $langInstruction",
                history = recentVoiceHistory,
                prompt = clean
            )
            val reply = res.getOrElse { it.localizedMessage ?: "Configure your Gemini API key in Settings -> API Keys." }
            recordVoiceTurn(clean, reply, lang)
            _lastVoiceResponse.value = reply
            if (settings.value.voiceAutoSpeak) speakBack(reply)
            else AssistantRuntimeCoordinator.setCoreState(AiCoreState.IDLE, "Ready")
        }
    }

    private suspend fun recordVoiceTurn(
        transcript: String,
        response: String,
        language: String,
        action: String = ""
    ) {
        dao.insertVoiceHistory(
            VoiceHistoryEntity(
                transcript = transcript,
                response = response,
                language = language,
                actionExecuted = action
            )
        )
    }

    fun addVoiceMacro(phrase: String, targetType: String, payload: String) {
        if (phrase.isBlank() || payload.isBlank()) return
        viewModelScope.launch {
            dao.upsertVoiceMacro(
                VoiceMacroEntity(
                    triggerPhrase = phrase.trim(),
                    actionSummary = "$targetType -> ${payload.take(40)}",
                    targetType = targetType,
                    payload = payload.trim()
                )
            )
        }
    }

    fun deleteVoiceMacro(id: String) {
        viewModelScope.launch { dao.deleteVoiceMacro(id) }
    }

    fun proposeSensitiveAction(action: PendingSensitiveAction) {
        _pendingAction.value = action
    }

    fun confirmPendingAction() {
        val action = _pendingAction.value ?: return
        _pendingAction.value = null
        container.deviceActionRouter.executeConfirmedAction(action)
            .onSuccess { _snackbarMessage.value = it }
            .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Action could not be launched." }
    }

    fun dismissPendingAction() {
        _pendingAction.value = null
    }

    fun updateOrbSettings(
        enabled: Boolean? = null,
        systemOverlay: Boolean? = null,
        compact: Boolean? = null,
        sizeDp: Float? = null,
        offsetX: Float? = null,
        offsetY: Float? = null
    ) {
        viewModelScope.launch {
            prefs.updateOrbSettings(enabled, systemOverlay, compact, sizeDp, offsetX, offsetY)
        }
    }

    fun updateEdgeLightingSettings(
        enabled: Boolean? = null,
        style: String? = null,
        palette: String? = null,
        brightness: Float? = null,
        speed: Float? = null
    ) {
        viewModelScope.launch {
            prefs.updateEdgeLightingSettings(enabled, style, palette, brightness, speed)
        }
    }

    fun updateVoiceSettings(
        language: String? = null,
        rate: Float? = null,
        pitch: Float? = null,
        autoSpeak: Boolean? = null,
        wakePhrase: Boolean? = null
    ) {
        viewModelScope.launch {
            prefs.updateVoiceSettings(language, rate, pitch, autoSpeak, wakePhrase)
        }
    }

    fun setMemoryEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setMemoryEnabled(enabled) }
    }

    fun updateHomeWidgets(
        weather: Boolean? = null,
        quickActions: Boolean? = null,
        suggestions: Boolean? = null,
        recentTasks: Boolean? = null
    ) {
        viewModelScope.launch { prefs.updateHomeWidgets(weather, quickActions, suggestions, recentTasks) }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}
