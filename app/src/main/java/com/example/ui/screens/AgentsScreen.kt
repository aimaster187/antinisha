package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AgentConfigEntity
import com.example.data.local.AgentTaskEntity
import com.example.domain.AgentSubtaskProgress
import com.example.ui.theme.AmberSoft
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CrimsonSoft
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.FaGlyphs
import com.example.ui.theme.FaIcon
import com.example.ui.theme.FrostBorder
import com.example.ui.theme.IceBlueDeep
import com.example.ui.theme.IceBlueSoft
import com.example.ui.theme.IndigoMuted
import com.example.ui.theme.LavenderGlow
import com.example.ui.theme.NishaGlassCard
import com.example.ui.theme.NishaSectionHeader
import com.example.ui.theme.NishaStatusPill
import com.example.ui.theme.ObsidianSlate
import com.example.ui.theme.PearlWhite

private val AGENT_ICON_OPTIONS = listOf(
    "atom", "code", "globe", "eye", "brain", "bolt", "terminal", "shield", "crown", "sparkles", "diagram", "robot"
)

@Composable
fun AgentsScreen(
    agents: List<AgentConfigEntity>,
    agentTasks: List<AgentTaskEntity>,
    liveSubtasks: List<AgentSubtaskProgress>,
    orchestrating: Boolean,
    orchestrationResult: String?,
    pendingSensitivePrompt: Pair<String, List<AgentConfigEntity>>?,
    statusNotice: String?,
    onSaveAgent: (AgentConfigEntity) -> Unit,
    onCreateCustomAgent: (String, String, String, String, String?) -> Unit,
    onDeleteAgent: (AgentConfigEntity) -> Unit,
    onRunOrchestration: (String, Set<String>) -> Unit,
    onConfirmSensitiveOrchestration: () -> Unit,
    onCancelSensitiveOrchestration: () -> Unit,
    onClearNotice: () -> Unit
) {
    var selectedAgentIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var objective by remember { mutableStateOf("") }
    var editingAgent by remember { mutableStateOf<AgentConfigEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(agents) {
        if (selectedAgentIds.isEmpty() && agents.isNotEmpty()) {
            selectedAgentIds = agents.filter { it.status == "active" }.take(2).map { it.id }.toSet()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 720.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 124.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("AI Agent Hub & Orchestrator", style = MaterialTheme.typography.headlineMedium, color = ObsidianSlate)
                    Text("Up to 5 specialized agents collaborating concurrently", style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                }
                NishaStatusPill(
                    label = "${agents.size}/5 Slots",
                    iconGlyph = FaGlyphs.ROBOT,
                    tint = ElectricViolet,
                    background = LavenderGlow
                )
            }
        }

        if (!statusNotice.isNullOrBlank()) {
            item {
                Surface(
                    color = IceBlueSoft,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FaIcon(FaGlyphs.CIRCLE_INFO, contentDescription = null, color = IceBlueDeep, size = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(statusNotice, style = MaterialTheme.typography.bodyMedium, color = ObsidianSlate, modifier = Modifier.weight(1f))
                        TextButton(onClick = onClearNotice) { Text("Dismiss") }
                    }
                }
            }
        }

        // Multi-Agent Orchestrator Card
        item {
            NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    NishaSectionHeader(
                        title = "Multi-Agent Orchestrator",
                        subtitle = "Select 1 to 5 active agents and decompose a complex objective",
                        iconGlyph = FaGlyphs.DIAGRAM_PROJECT
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(agents, key = { it.id }) { agent ->
                            val isSelected = agent.id in selectedAgentIds
                            FilterChip(
                                selected = isSelected,
                                enabled = agent.status == "active",
                                onClick = {
                                    selectedAgentIds = if (isSelected) {
                                        selectedAgentIds - agent.id
                                    } else {
                                        (selectedAgentIds + agent.id).take(5).toSet()
                                    }
                                },
                                leadingIcon = {
                                    FaIcon(FaGlyphs.byName(agent.iconName), contentDescription = null, size = 12.sp)
                                },
                                label = { Text(agent.name) },
                                modifier = Modifier.testTag("orch_chip_${agent.id}")
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = objective,
                        onValueChange = { objective = it },
                        label = { Text("Shared Goal / Complex Prompt") },
                        placeholder = { Text("e.g., Plan a Kotlin Jetpack Compose release checklist and research edge cases") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth().testTag("orchestrator_prompt_input")
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { onRunOrchestration(objective, selectedAgentIds) },
                        enabled = !orchestrating && objective.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("orchestrator_run_button")
                    ) {
                        if (orchestrating) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = PearlWhite, strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("Orchestrating Subtasks...")
                        } else {
                            FaIcon(FaGlyphs.BOLT, contentDescription = null, color = PearlWhite, size = 14.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("Orchestrate (${selectedAgentIds.size.coerceAtLeast(1)} Agents)")
                        }
                    }
                }
            }
        }

        // Live Subtask Progress
        if (liveSubtasks.isNotEmpty()) {
            item {
                NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Live Agent Execution Progress", style = MaterialTheme.typography.titleMedium, color = ObsidianSlate)
                        liveSubtasks.forEach { sub ->
                            val (pillBg, pillFg) = when (sub.status) {
                                "completed", "retried" -> EmeraldSoft to EmeraldSuccess
                                "failed", "blocked_sensitive" -> CrimsonSoft to CrimsonAlert
                                else -> LavenderGlow to ElectricViolet
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, FrostBorder),
                                color = PearlWhite,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        FaIcon(FaGlyphs.byName(sub.iconName), contentDescription = null, color = ElectricViolet, size = 14.sp)
                                        Spacer(Modifier.width(8.dp))
                                        Text(sub.agentName, style = MaterialTheme.typography.titleMedium, color = ObsidianSlate, modifier = Modifier.weight(1f))
                                        NishaStatusPill(sub.status.uppercase(), FaGlyphs.CIRCLE_CHECK, tint = pillFg, background = pillBg)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(sub.subtaskPrompt, style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                                    if (sub.errorMessage.isNotBlank()) {
                                        Text(sub.errorMessage, style = MaterialTheme.typography.labelSmall, color = CrimsonAlert)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Unified Orchestration Result
        if (!orchestrationResult.isNullOrBlank()) {
            item {
                NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        NishaSectionHeader(
                            title = "Synthesized Result",
                            subtitle = "Attributed multi-agent synthesis",
                            iconGlyph = FaGlyphs.SPARKLES
                        )
                        Spacer(Modifier.height(10.dp))
                        MarkdownMessageBody(orchestrationResult)
                    }
                }
            }
        }

        // Configured Agents (Max 5)
        item {
            NishaSectionHeader(
                title = "Configured Agents (Max 5)",
                subtitle = "Per-agent tool permissions default OFF until you enable them",
                iconGlyph = FaGlyphs.ROBOT,
                trailing = {
                    OutlinedButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.testTag("agent_create_button")
                    ) {
                        FaIcon(FaGlyphs.PLUS, contentDescription = null, size = 12.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(if (agents.size >= 5) "Replace" else "Create")
                    }
                }
            )
        }

        items(agents, key = { it.id }) { agent ->
            val enabledPerms = listOfNotNull(
                if (agent.permWebResearch) "Web" else null,
                if (agent.permFileRead) "Read" else null,
                if (agent.permFileWrite) "Write" else null,
                if (agent.permDeviceActions) "Device" else null,
                if (agent.permMessaging) "Msg" else null
            )
            NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FaIcon(FaGlyphs.byName(agent.iconName), contentDescription = agent.name, color = ElectricViolet, size = 18.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(agent.name, style = MaterialTheme.typography.titleMedium, color = ObsidianSlate)
                            Text("Slot ${agent.slotIndex + 1} • ${agent.role}", style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                        }
                        Switch(
                            checked = agent.status == "active",
                            onCheckedChange = { checked ->
                                onSaveAgent(agent.copy(status = if (checked) "active" else "paused"))
                            },
                            modifier = Modifier.testTag("agent_switch_${agent.id}")
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(agent.instructions, style = MaterialTheme.typography.bodyMedium, color = IndigoMuted, maxLines = 2)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NishaStatusPill(
                            label = if (enabledPerms.isEmpty()) "Zero Permissions (Safe)" else "Perms: ${enabledPerms.joinToString(", ")}",
                            iconGlyph = FaGlyphs.SHIELD_HALVED,
                            tint = if (enabledPerms.isEmpty()) EmeraldSuccess else AmberWarn,
                            background = if (enabledPerms.isEmpty()) EmeraldSoft else AmberSoft
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { editingAgent = agent }, modifier = Modifier.size(48.dp).testTag("agent_edit_${agent.id}")) {
                            FaIcon(FaGlyphs.SLIDERS, contentDescription = "Edit agent ${agent.name}", color = IceBlueDeep, size = 16.sp)
                        }
                        IconButton(onClick = { onDeleteAgent(agent) }, modifier = Modifier.size(48.dp).testTag("agent_delete_${agent.id}")) {
                            FaIcon(FaGlyphs.TRASH, contentDescription = "Delete agent ${agent.name}", color = CrimsonAlert, size = 16.sp)
                        }
                    }
                }
            }
        }

        if (agentTasks.isNotEmpty()) {
            item {
                NishaSectionHeader(
                    title = "Orchestration History",
                    subtitle = "Recent multi-agent task executions",
                    iconGlyph = FaGlyphs.CLOCK
                )
            }
            items(agentTasks.take(8), key = { it.id }) { task ->
                NishaGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(task.prompt, style = MaterialTheme.typography.titleMedium, color = ObsidianSlate, modifier = Modifier.weight(1f), maxLines = 2)
                            Spacer(Modifier.width(8.dp))
                            NishaStatusPill(task.status, FaGlyphs.CHECK)
                        }
                        Text("Agents: ${task.assignedAgents}", style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                    }
                }
            }
        }
    }

    // Edit Agent Dialog (Name, Role, Icon, Instructions, Capabilities, 5 Permissions)
    editingAgent?.let { original ->
        var name by remember(original.id) { mutableStateOf(original.name) }
        var role by remember(original.id) { mutableStateOf(original.role) }
        var iconName by remember(original.id) { mutableStateOf(original.iconName) }
        var instructions by remember(original.id) { mutableStateOf(original.instructions) }
        var capabilities by remember(original.id) { mutableStateOf(original.capabilities) }
        var pWeb by remember(original.id) { mutableStateOf(original.permWebResearch) }
        var pRead by remember(original.id) { mutableStateOf(original.permFileRead) }
        var pWrite by remember(original.id) { mutableStateOf(original.permFileWrite) }
        var pDevice by remember(original.id) { mutableStateOf(original.permDeviceActions) }
        var pMsg by remember(original.id) { mutableStateOf(original.permMessaging) }

        AlertDialog(
            onDismissRequest = { editingAgent = null },
            title = { Text("Configure ${original.name}") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                    OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Role") }, singleLine = true)
                    OutlinedTextField(value = capabilities, onValueChange = { capabilities = it }, label = { Text("Capabilities") }, singleLine = true)
                    OutlinedTextField(value = instructions, onValueChange = { instructions = it }, label = { Text("System Instructions") }, minLines = 2)
                    Text("Icon", style = MaterialTheme.typography.labelLarge, color = ObsidianSlate)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(AGENT_ICON_OPTIONS) { iconKey ->
                            FilterChip(
                                selected = iconName == iconKey,
                                onClick = { iconName = iconKey },
                                label = { FaIcon(FaGlyphs.byName(iconKey), contentDescription = iconKey, size = 14.sp) }
                            )
                        }
                    }
                    Text("Isolated Agent Permissions (Default OFF)", style = MaterialTheme.typography.labelLarge, color = ElectricViolet, fontWeight = FontWeight.Bold)
                    PermissionToggleRow("Web Research", pWeb) { pWeb = it }
                    PermissionToggleRow("File Read", pRead) { pRead = it }
                    PermissionToggleRow("File Write", pWrite) { pWrite = it }
                    PermissionToggleRow("Device Actions", pDevice) { pDevice = it }
                    PermissionToggleRow("Messaging", pMsg) { pMsg = it }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSaveAgent(
                        original.copy(
                            name = name.trim().ifBlank { original.name },
                            role = role.trim(),
                            iconName = iconName,
                            instructions = instructions.trim(),
                            capabilities = capabilities.trim(),
                            permWebResearch = pWeb,
                            permFileRead = pRead,
                            permFileWrite = pWrite,
                            permDeviceActions = pDevice,
                            permMessaging = pMsg
                        )
                    )
                    editingAgent = null
                }) { Text("Save Agent") }
            },
            dismissButton = {
                TextButton(onClick = { editingAgent = null }) { Text("Cancel") }
            }
        )
    }

    // Create / Replace Custom Agent Dialog (Max 5 safeguard)
    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var role by remember { mutableStateOf("") }
        var iconName by remember { mutableStateOf("sparkles") }
        var instructions by remember { mutableStateOf("") }
        var replaceId by remember(agents) {
            mutableStateOf(if (agents.size >= 5) agents.lastOrNull()?.id else null)
        }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(if (agents.size >= 5) "Replace Agent Slot (5/5 Full)" else "Create Custom Agent") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (agents.size >= 5) {
                        Text(
                            "Maximum 5 agents reached. Choose an existing slot to replace:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmberWarn
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(agents) { existing ->
                                FilterChip(
                                    selected = replaceId == existing.id,
                                    onClick = { replaceId = existing.id },
                                    label = { Text(existing.name) }
                                )
                            }
                        }
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Agent Name") }, singleLine = true)
                    OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Specialty Role") }, singleLine = true)
                    OutlinedTextField(value = instructions, onValueChange = { instructions = it }, label = { Text("System Instructions") }, minLines = 2)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(AGENT_ICON_OPTIONS) { iconKey ->
                            FilterChip(
                                selected = iconName == iconKey,
                                onClick = { iconName = iconKey },
                                label = { FaIcon(FaGlyphs.byName(iconKey), contentDescription = iconKey, size = 14.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateCustomAgent(name, role, iconName, instructions, replaceId)
                        showCreateDialog = false
                    },
                    enabled = name.isNotBlank() && role.isNotBlank()
                ) { Text(if (agents.size >= 5) "Replace Slot" else "Create Agent") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Sensitive Action Confirmation for Multi-Agent Orchestrator
    pendingSensitivePrompt?.let { (promptText, _) ->
        AlertDialog(
            onDismissRequest = onCancelSensitiveOrchestration,
            title = { Text("Sensitive Multi-Agent Request") },
            text = {
                Text("This objective mentions sensitive device, file, or messaging operations:\n\n\"$promptText\"\n\nNISHA agents will only generate plans/drafts and never silently trigger external actions. Proceed?")
            },
            confirmButton = {
                Button(onClick = onConfirmSensitiveOrchestration) { Text("Proceed Safely") }
            },
            dismissButton = {
                TextButton(onClick = onCancelSensitiveOrchestration) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PermissionToggleRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = ObsidianSlate, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}
