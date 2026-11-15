package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AgentConfigEntity
import com.example.data.local.AgentTaskEntity
import com.example.data.local.AutomationRuleEntity
import com.example.data.local.CodingProjectEntity
import com.example.data.local.JournalEntryEntity
import com.example.data.local.MemoryFactEntity
import com.example.data.local.NoteItemEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.WatchlistItemEntity
import com.example.data.remote.GeminiAttachment
import com.example.di.NishaAppContainer
import com.example.domain.AgentSubtaskProgress
import com.example.domain.AiCoreState
import com.example.domain.AssistantRuntimeCoordinator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AgentsStudioViewModel(
    private val container: NishaAppContainer
) : ViewModel() {

    private val dao = container.dao
    private val gemini = container.geminiClient

    // Agents & Orchestration
    val agents: StateFlow<List<AgentConfigEntity>> = dao.observeAgents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val agentTasks: StateFlow<List<AgentTaskEntity>> = dao.observeAgentTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _liveSubtasks = MutableStateFlow<List<AgentSubtaskProgress>>(emptyList())
    val liveSubtasks: StateFlow<List<AgentSubtaskProgress>> = _liveSubtasks.asStateFlow()

    private val _orchestrating = MutableStateFlow(false)
    val orchestrating: StateFlow<Boolean> = _orchestrating.asStateFlow()

    private val _orchestrationResult = MutableStateFlow<String?>(null)
    val orchestrationResult: StateFlow<String?> = _orchestrationResult.asStateFlow()

    private val _pendingSensitivePrompt = MutableStateFlow<Pair<String, List<AgentConfigEntity>>?>(null)
    val pendingSensitivePrompt: StateFlow<Pair<String, List<AgentConfigEntity>>?> = _pendingSensitivePrompt.asStateFlow()

    // Knowledge Brain
    val memories: StateFlow<List<MemoryFactEntity>> = dao.observeMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Coding Studio
    val codingProjects: StateFlow<List<CodingProjectEntity>> = dao.observeCodingProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _codingAiOutput = MutableStateFlow("")
    val codingAiOutput: StateFlow<String> = _codingAiOutput.asStateFlow()

    private val _codingLoading = MutableStateFlow(false)
    val codingLoading: StateFlow<Boolean> = _codingLoading.asStateFlow()

    // Tools Hub Entities
    val journalEntries: StateFlow<List<JournalEntryEntity>> = dao.observeJournalEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<NoteItemEntity>> = dao.observeNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<ReminderEntity>> = dao.observeReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchlist: StateFlow<List<WatchlistItemEntity>> = dao.observeWatchlist()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val automations: StateFlow<List<AutomationRuleEntity>> = dao.observeAutomationRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _toolAiResult = MutableStateFlow("")
    val toolAiResult: StateFlow<String> = _toolAiResult.asStateFlow()

    private val _toolLoading = MutableStateFlow(false)
    val toolLoading: StateFlow<Boolean> = _toolLoading.asStateFlow()

    private val _statusNotice = MutableStateFlow<String?>(null)
    val statusNotice: StateFlow<String?> = _statusNotice.asStateFlow()

    init {
        viewModelScope.launch {
            if (dao.observeCodingProjects().first().isEmpty()) {
                dao.upsertCodingProject(
                    CodingProjectEntity(
                        id = "proj_welcome_web",
                        name = "Nisha Luxury Landing",
                        fileName = "index.html",
                        language = "HTML/CSS/JS",
                        codeContent = DEFAULT_STARTER_HTML,
                        buildLog = "[Studio] Ready. Edit HTML/CSS/JS and preview live below."
                    )
                )
            }
        }
    }

    // --- 5-Agent Hub Operations ---
    fun saveAgent(agent: AgentConfigEntity) {
        viewModelScope.launch {
            val existing = dao.getAgentsOnce()
            if (existing.none { it.id == agent.id } && existing.size >= 5) {
                _statusNotice.value = "Maximum 5 agents allowed. Pause or replace an existing agent."
                return@launch
            }
            dao.upsertAgent(agent.copy(updatedAtMs = System.currentTimeMillis()))
            _statusNotice.value = "${agent.name} saved."
        }
    }

    fun createCustomAgent(
        name: String,
        role: String,
        iconName: String,
        instructions: String,
        replaceAgentId: String? = null
    ) {
        viewModelScope.launch {
            val current = dao.getAgentsOnce()
            if (current.size >= 5 && replaceAgentId.isNullOrBlank()) {
                _statusNotice.value = "All 5 agent slots are full. Select an agent to replace."
                return@launch
            }
            val slotIndex = if (!replaceAgentId.isNullOrBlank()) {
                val target = current.firstOrNull { it.id == replaceAgentId }
                dao.deleteAgent(replaceAgentId)
                target?.slotIndex ?: current.size.coerceAtMost(4)
            } else {
                (0..4).firstOrNull { idx -> current.none { it.slotIndex == idx } } ?: 4
            }
            dao.upsertAgent(
                AgentConfigEntity(
                    id = "agent_${UUID.randomUUID().toString().replace("-", "").take(10)}",
                    slotIndex = slotIndex,
                    name = name.trim().take(60),
                    role = role.trim().take(120),
                    iconName = iconName,
                    instructions = instructions.trim(),
                    capabilities = role.trim(),
                    status = "active",
                    isDefault = false
                )
            )
            _statusNotice.value = "Custom agent '$name' added to slot ${slotIndex + 1}."
        }
    }

    fun deleteAgentSafely(agent: AgentConfigEntity) {
        viewModelScope.launch {
            val current = dao.getAgentsOnce()
            if (current.size <= 1) {
                _statusNotice.value = "At least 1 active agent must remain."
                return@launch
            }
            dao.deleteAgent(agent.id)
            _statusNotice.value = "Removed ${agent.name}."
        }
    }

    fun runOrchestration(
        objective: String,
        selectedAgentIds: Set<String>,
        allowSensitiveProceed: Boolean = false
    ) {
        val clean = objective.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val chosen = agents.value.filter { it.id in selectedAgentIds && it.status == "active" }
                .ifEmpty { agents.value.filter { it.status == "active" }.take(2) }
            if (chosen.isEmpty()) {
                _statusNotice.value = "Enable at least one active agent first."
                return@launch
            }

            _orchestrating.value = true
            _orchestrationResult.value = null
            _pendingSensitivePrompt.value = null
            AssistantRuntimeCoordinator.setCoreState(AiCoreState.THINKING, "Orchestrating ${chosen.size} agents...")

            val settings = container.preferences.settingsFlow.first()
            val memoriesList = if (settings.memoryEnabled) dao.getMemoriesOnce() else emptyList()
            val outcome = container.orchestrator.executeOrchestration(
                objective = clean,
                selectedAgents = chosen,
                memories = memoriesList,
                model = settings.selectedModel,
                allowSensitiveProceed = allowSensitiveProceed,
                onProgress = { _liveSubtasks.value = it }
            )

            outcome.onSuccess { res ->
                if (res.requiresSensitiveConfirmation) {
                    _pendingSensitivePrompt.value = clean to chosen
                    _orchestrationResult.value = res.combinedResponse
                } else {
                    _orchestrationResult.value = res.combinedResponse
                    dao.upsertAgentTask(
                        AgentTaskEntity(
                            prompt = clean,
                            assignedAgents = chosen.joinToString(", ") { it.name },
                            status = "completed",
                            subtaskBreakdown = res.subtasks.joinToString("\n") { "${it.agentName}: ${it.subtaskPrompt}" },
                            finalResponse = res.combinedResponse
                        )
                    )
                }
            }.onFailure { err ->
                _orchestrationResult.value = "Orchestration failed: ${err.localizedMessage}"
                dao.upsertAgentTask(
                    AgentTaskEntity(
                        prompt = clean,
                        assignedAgents = chosen.joinToString(", ") { it.name },
                        status = "failed",
                        finalResponse = err.localizedMessage ?: "Error"
                    )
                )
            }
            _orchestrating.value = false
            AssistantRuntimeCoordinator.setCoreState(AiCoreState.IDLE, "Ready")
        }
    }

    fun confirmSensitiveOrchestration() {
        val pending = _pendingSensitivePrompt.value ?: return
        _pendingSensitivePrompt.value = null
        runOrchestration(
            objective = pending.first,
            selectedAgentIds = pending.second.map { it.id }.toSet(),
            allowSensitiveProceed = true
        )
    }

    fun cancelSensitiveOrchestration() {
        _pendingSensitivePrompt.value = null
    }

    // --- Knowledge Brain & Memory ---
    fun saveMemory(category: String, title: String, content: String, scope: String = "LOCAL") {
        if (title.isBlank() || content.isBlank()) return
        viewModelScope.launch {
            val id = "mem_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            dao.upsertMemory(
                MemoryFactEntity(
                    id = id,
                    category = category.trim(),
                    title = title.trim(),
                    content = content.trim(),
                    scope = scope
                )
            )
            if (scope == "CLOUD" && FirebaseAuth.getInstance().currentUser != null) {
                container.firestoreSync.saveMemory(id, category.trim(), title.trim(), content.trim())
            }
            _statusNotice.value = "Memory saved ($scope)."
        }
    }

    fun deleteMemory(item: MemoryFactEntity) {
        viewModelScope.launch {
            dao.deleteMemory(item.id)
            if (item.scope == "CLOUD" && FirebaseAuth.getInstance().currentUser != null) {
                container.firestoreSync.deleteMemory(item.id)
            }
        }
    }

    fun exportMemoriesJson(): String {
        val arr = JSONArray()
        memories.value.forEach { m ->
            arr.put(
                JSONObject()
                    .put("category", m.category)
                    .put("title", m.title)
                    .put("content", m.content)
                    .put("scope", m.scope)
            )
        }
        return arr.toString(2)
    }

    fun importMemoriesJson(rawJson: String) {
        viewModelScope.launch {
            runCatching {
                val arr = JSONArray(rawJson)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    dao.upsertMemory(
                        MemoryFactEntity(
                            category = o.optString("category", "Imported"),
                            title = o.optString("title", "Note ${i + 1}"),
                            content = o.optString("content", ""),
                            scope = "LOCAL"
                        )
                    )
                }
                _statusNotice.value = "Imported ${arr.length()} memory records."
            }.onFailure {
                _statusNotice.value = "Invalid JSON format: ${it.localizedMessage}"
            }
        }
    }

    // --- Coding Studio ---
    fun saveCodingProject(project: CodingProjectEntity) {
        viewModelScope.launch {
            dao.upsertCodingProject(project.copy(updatedAtMs = System.currentTimeMillis()))
        }
    }

    fun createCodingProject(name: String, fileName: String, language: String) {
        viewModelScope.launch {
            dao.upsertCodingProject(
                CodingProjectEntity(
                    name = name.trim().ifBlank { "Untitled Project" },
                    fileName = fileName.trim().ifBlank { "main.kt" },
                    language = language,
                    codeContent = if (language.contains("HTML", true)) DEFAULT_STARTER_HTML else "// $name\nfun main() {\n    println(\"Hello from NISHA Studio\")\n}",
                    buildLog = "[Studio] Created $fileName ($language)"
                )
            )
        }
    }

    fun runCodingAiAssist(mode: String, code: String, instruction: String) {
        viewModelScope.launch {
            _codingLoading.value = true
            val model = container.preferences.settingsFlow.first().selectedModel
            val prompt = when (mode) {
                "generate" -> "Generate clean code for: $instruction\nExisting context:\n```\n$code\n```"
                "explain" -> "Explain what this code does line-by-line and highlight potential improvements:\n```\n$code\n```"
                "debug" -> "Analyze and fix bugs or errors in this code. User issue: $instruction\n```\n$code\n```"
                else -> "$instruction\n```\n$code\n```"
            }
            gemini.generateContent(
                model = model,
                systemInstruction = "You are Code Master inside NISHA Coding Studio. Provide crisp, production-grade code and diagnostics.",
                history = emptyList(),
                prompt = prompt
            ).onSuccess { _codingAiOutput.value = it }
                .onFailure { _codingAiOutput.value = "Error: ${it.localizedMessage}" }
            _codingLoading.value = false
        }
    }

    // --- Tools Hub Actions ---
    fun runAiToolPrompt(systemRole: String, prompt: String, attachment: GeminiAttachment? = null) {
        viewModelScope.launch {
            _toolLoading.value = true
            val model = container.preferences.settingsFlow.first().selectedModel
            gemini.generateContent(
                model = model,
                systemInstruction = systemRole,
                history = emptyList(),
                prompt = prompt,
                attachment = attachment
            ).onSuccess { _toolAiResult.value = it }
                .onFailure { _toolAiResult.value = "Error: ${it.localizedMessage}" }
            _toolLoading.value = false
        }
    }

    fun addJournalEntry(title: String, content: String, moodLabel: String, moodScore: Int) {
        if (title.isBlank() || content.isBlank()) return
        viewModelScope.launch {
            dao.upsertJournalEntry(
                JournalEntryEntity(
                    title = title.trim(),
                    content = content.trim(),
                    moodLabel = moodLabel,
                    moodScore = moodScore
                )
            )
        }
    }

    fun deleteJournalEntry(id: String) = viewModelScope.launch { dao.deleteJournalEntry(id) }

    fun addNote(title: String, body: String, tag: String) {
        if (title.isBlank() || body.isBlank()) return
        viewModelScope.launch {
            dao.upsertNote(NoteItemEntity(title = title.trim(), body = body.trim(), tag = tag.trim().ifBlank { "General" }))
        }
    }

    fun deleteNote(id: String) = viewModelScope.launch { dao.deleteNote(id) }

    fun addReminder(title: String, scheduleText: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            dao.upsertReminder(ReminderEntity(title = title.trim(), scheduleText = scheduleText.trim()))
        }
    }

    fun toggleReminder(item: ReminderEntity) = viewModelScope.launch {
        dao.upsertReminder(item.copy(isDone = !item.isDone))
    }

    fun deleteReminder(id: String) = viewModelScope.launch { dao.deleteReminder(id) }

    fun addWatchlistItem(symbol: String, assetName: String, targetPrice: String, note: String) {
        if (symbol.isBlank()) return
        viewModelScope.launch {
            dao.upsertWatchlistItem(
                WatchlistItemEntity(
                    symbol = symbol.trim().uppercase(),
                    assetName = assetName.trim(),
                    targetPrice = targetPrice.trim(),
                    note = note.trim()
                )
            )
        }
    }

    fun deleteWatchlistItem(id: String) = viewModelScope.launch { dao.deleteWatchlistItem(id) }

    fun addAutomationRule(name: String, triggerNode: String, conditionNode: String, actionNode: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            dao.upsertAutomationRule(
                AutomationRuleEntity(
                    name = name.trim(),
                    triggerNode = triggerNode.trim(),
                    conditionNode = conditionNode.trim(),
                    actionNode = actionNode.trim()
                )
            )
        }
    }

    fun toggleAutomationRule(rule: AutomationRuleEntity) = viewModelScope.launch {
        dao.upsertAutomationRule(rule.copy(enabled = !rule.enabled))
    }

    fun deleteAutomationRule(id: String) = viewModelScope.launch { dao.deleteAutomationRule(id) }

    fun clearNotice() {
        _statusNotice.value = null
    }

    companion object {
        val DEFAULT_STARTER_HTML = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
              <style>
                body { font-family: sans-serif; background: #F6F8FF; color: #0F172A; padding: 24px; }
                .card { background: white; border-radius: 18px; padding: 20px; border: 1px solid #D5E3FF; box-shadow: 0 8px 24px rgba(124,58,237,0.08); }
                h1 { color: #6D28D9; margin-top: 0; }
                button { background: linear-gradient(135deg,#0284C7,#7C3AED); color: white; border: none; padding: 10px 18px; border-radius: 999px; font-weight: 600; }
              </style>
            </head>
            <body>
              <div class="card">
                <h1>NISHA AI Web Studio</h1>
                <p>Interactive HTML/CSS/JS preview running inside Coding Studio.</p>
                <button onclick="document.getElementById('out').innerText='Core pulse verified!'">Test Script</button>
                <p id="out" style="color:#0284C7;font-weight:600;"></p>
              </div>
            </body>
            </html>
        """.trimIndent()
    }
}
