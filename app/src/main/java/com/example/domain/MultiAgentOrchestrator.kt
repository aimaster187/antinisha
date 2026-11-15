package com.example.domain

import com.example.data.local.AgentConfigEntity
import com.example.data.local.MemoryFactEntity
import com.example.data.remote.GeminiRestClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import org.json.JSONArray
import org.json.JSONObject

data class AgentSubtaskProgress(
    val agentId: String,
    val agentName: String,
    val agentRole: String,
    val iconName: String,
    val subtaskPrompt: String,
    val status: String, // queued | running | completed | retried | failed | blocked_sensitive
    val output: String = "",
    val errorMessage: String = ""
)

data class OrchestrationOutcome(
    val subtasks: List<AgentSubtaskProgress>,
    val combinedResponse: String,
    val requiresSensitiveConfirmation: Boolean,
    val sensitiveActionReason: String = ""
)

class MultiAgentOrchestrator(
    private val geminiClient: GeminiRestClient
) {
    suspend fun executeOrchestration(
        objective: String,
        selectedAgents: List<AgentConfigEntity>,
        memories: List<MemoryFactEntity>,
        model: String,
        allowSensitiveProceed: Boolean = false,
        onProgress: (List<AgentSubtaskProgress>) -> Unit
    ): Result<OrchestrationOutcome> = runCatching {
        val activeAgents = selectedAgents.filter { it.status == "active" }.take(5)
        require(activeAgents.isNotEmpty()) { "Enable at least one active agent (max 5) to orchestrate." }

        val sensitiveReason = detectSensitiveIntent(objective)
        if (sensitiveReason != null && !allowSensitiveProceed) {
            val blocked = activeAgents.map { agent ->
                AgentSubtaskProgress(
                    agentId = agent.id,
                    agentName = agent.name,
                    agentRole = agent.role,
                    iconName = agent.iconName,
                    subtaskPrompt = objective,
                    status = "blocked_sensitive",
                    output = "Awaiting explicit user approval: $sensitiveReason"
                )
            }
            onProgress(blocked)
            return@runCatching OrchestrationOutcome(
                subtasks = blocked,
                combinedResponse = "Sensitive operation detected ($sensitiveReason). Confirm approval before executing.",
                requiresSensitiveConfirmation = true,
                sensitiveActionReason = sensitiveReason
            )
        }

        val memoryBlock = if (memories.isNotEmpty()) {
            memories.take(8).joinToString("\n") { "- [${it.category}] ${it.title}: ${it.content}" }
        } else {
            "No saved memory context."
        }

        // Step 1: Decompose objective into 1..5 coordinated subtasks
        val plannedSubtasks = planSubtasks(objective, activeAgents, memoryBlock, model)
        var progressList = plannedSubtasks.map { (agent, taskText) ->
            AgentSubtaskProgress(
                agentId = agent.id,
                agentName = agent.name,
                agentRole = agent.role,
                iconName = agent.iconName,
                subtaskPrompt = taskText,
                status = "running"
            )
        }
        onProgress(progressList)

        // Step 2: Execute up to 5 agents concurrently with isolated permission boundaries and safe retry
        val completedList = supervisorScope {
            progressList.mapIndexed { idx, item ->
                async {
                    val agent = activeAgents[idx]
                    val permSummary = buildString {
                        append("Permissions enabled -> ")
                        append("WebResearch=${agent.permWebResearch}, ")
                        append("FileRead=${agent.permFileRead}, ")
                        append("FileWrite=${agent.permFileWrite}, ")
                        append("DeviceActions=${agent.permDeviceActions}, ")
                        append("Messaging=${agent.permMessaging}. ")
                        append("Never claim to have performed any action whose permission is false.")
                    }
                    val systemPrompt = "${agent.instructions}\nRole: ${agent.role}\n$permSummary\nShared User Context:\n$memoryBlock"

                    var attemptResult = geminiClient.generateContent(
                        model = model,
                        systemInstruction = systemPrompt,
                        history = emptyList(),
                        prompt = "Main Objective: $objective\nYour Assigned Subtask: ${item.subtaskPrompt}\nProvide concise, actionable output for synthesis."
                    )
                    var finalStatus = "completed"
                    if (attemptResult.isFailure) {
                        // Safe retry once on transient failure
                        attemptResult = geminiClient.generateContent(
                            model = model,
                            systemInstruction = systemPrompt,
                            history = emptyList(),
                            prompt = "Main Objective: $objective\nYour Assigned Subtask: ${item.subtaskPrompt}\nProvide concise, actionable output for synthesis."
                        )
                        if (attemptResult.isSuccess) finalStatus = "retried"
                    }

                    val updated = if (attemptResult.isSuccess) {
                        item.copy(status = finalStatus, output = attemptResult.getOrNull().orEmpty())
                    } else {
                        item.copy(
                            status = "failed",
                            errorMessage = attemptResult.exceptionOrNull()?.localizedMessage ?: "Agent execution failed"
                        )
                    }
                    updated
                }
            }.awaitAll()
        }

        progressList = completedList
        onProgress(progressList)

        // Step 3: Synthesize all agent results into one attributed unified response
        val successfulOutputs = completedList.filter { it.status == "completed" || it.status == "retried" }
        require(successfulOutputs.isNotEmpty()) {
            completedList.firstOrNull { it.errorMessage.isNotBlank() }?.errorMessage
                ?: "All agents failed to complete their subtasks."
        }

        val synthesisInput = buildString {
            appendLine("Original User Objective: $objective")
            appendLine("\nAgent Subtask Outputs:")
            completedList.forEach { res ->
                appendLine("### Agent: ${res.agentName} (${res.agentRole}) [Status: ${res.status}]")
                appendLine("Subtask: ${res.subtaskPrompt}")
                if (res.output.isNotBlank()) appendLine(res.output)
                if (res.errorMessage.isNotBlank()) appendLine("Error: ${res.errorMessage}")
                appendLine()
            }
        }

        val combined = if (successfulOutputs.size == 1 && completedList.size == 1) {
            "**Handled by ${successfulOutputs.first().agentName}**\n\n${successfulOutputs.first().output}"
        } else {
            val synthResult = geminiClient.generateContent(
                model = model,
                systemInstruction = "You are the NISHA Multi-Agent Orchestrator Synthesizer. Merge the agent findings cleanly, clearly attribute which agent contributed each section, note any failed subtasks honestly, and return a unified luxury-grade final answer.",
                history = emptyList(),
                prompt = synthesisInput
            )
            synthResult.getOrElse {
                buildString {
                    appendLine("## Coordinated Multi-Agent Output")
                    completedList.forEach { res ->
                        appendLine("\n### ${res.agentName} (${res.status})")
                        appendLine(res.output.ifBlank { res.errorMessage })
                    }
                }
            }
        }

        OrchestrationOutcome(
            subtasks = completedList,
            combinedResponse = combined,
            requiresSensitiveConfirmation = false
        )
    }

    private suspend fun planSubtasks(
        objective: String,
        agents: List<AgentConfigEntity>,
        memoryBlock: String,
        model: String
    ): List<Pair<AgentConfigEntity, String>> {
        if (agents.size == 1) return listOf(agents.first() to objective)

        val agentCatalog = agents.joinToString("\n") {
            "- id=${it.id}, name=${it.name}, role=${it.role}, capabilities=${it.capabilities}"
        }
        val plannerPrompt = """
            Decompose this user objective into 1 focused subtask per agent below.
            Return ONLY valid JSON array of objects: [{"agentId":"...","subtask":"..."}]
            Agents:
            $agentCatalog
            Context:
            $memoryBlock
            Objective:
            $objective
        """.trimIndent()

        val rawPlan = geminiClient.generateContent(
            model = model,
            systemInstruction = "Output strict JSON array only, no markdown fences.",
            history = emptyList(),
            prompt = plannerPrompt,
            temperature = 0.2
        ).getOrNull().orEmpty()

        val parsedMap = mutableMapOf<String, String>()
        runCatching {
            val start = rawPlan.indexOf('[')
            val end = rawPlan.lastIndexOf(']')
            if (start >= 0 && end > start) {
                val arr = JSONArray(rawPlan.substring(start, end + 1))
                for (i in 0 until arr.length()) {
                    val obj: JSONObject = arr.getJSONObject(i)
                    val id = obj.optString("agentId")
                    val sub = obj.optString("subtask")
                    if (id.isNotBlank() && sub.isNotBlank()) parsedMap[id] = sub
                }
            }
        }

        return agents.map { agent ->
            val subtask = parsedMap[agent.id] ?: "Apply your specialty (${agent.role}) to address: $objective"
            agent to subtask
        }
    }

    private fun detectSensitiveIntent(prompt: String): String? {
        val lower = prompt.lowercase()
        return when {
            listOf("send message", "send whatsapp", "send sms", "text my", "call ").any { it in lower } ->
                "Messaging or calling external contacts"
            listOf("delete file", "erase all", "wipe ", "remove file").any { it in lower } ->
                "File deletion or destructive action"
            listOf("change setting", "turn off wifi", "disable bluetooth", "device action").any { it in lower } ->
                "Device system action"
            else -> null
        }
    }
}
