package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "local_accounts")
data class LocalAccountEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val username: String,
    val fullName: String,
    val email: String,
    val phone: String = "",
    val passwordSalt: String,
    val passwordHash: String,
    val emailVerified: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val model: String = "gemini-2.5-flash",
    val lastPreview: String = "",
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val role: String, // user | assistant | system
    val content: String,
    val attachmentLabel: String = "",
    val agentAttribution: String = "",
    val modelUsed: String = "",
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "agent_configs")
data class AgentConfigEntity(
    @PrimaryKey val id: String,
    val slotIndex: Int, // 0..4 (maximum 5 agents)
    val name: String,
    val role: String,
    val iconName: String,
    val instructions: String,
    val capabilities: String,
    val status: String = "active", // active | paused
    val permWebResearch: Boolean = false,
    val permFileRead: Boolean = false,
    val permFileWrite: Boolean = false,
    val permDeviceActions: Boolean = false,
    val permMessaging: Boolean = false,
    val isDefault: Boolean = false,
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "agent_tasks")
data class AgentTaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val prompt: String,
    val assignedAgents: String,
    val status: String, // queued | running | completed | failed | awaiting_confirmation
    val subtaskBreakdown: String = "",
    val agentResultsJson: String = "",
    val finalResponse: String = "",
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "memory_facts")
data class MemoryFactEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val category: String,
    val title: String,
    val content: String,
    val scope: String = "LOCAL", // LOCAL | CLOUD
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "voice_macros")
data class VoiceMacroEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val triggerPhrase: String,
    val actionSummary: String,
    val targetType: String, // ai_prompt | open_app | timer | whatsapp | sms
    val payload: String,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "voice_history")
data class VoiceHistoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val transcript: String,
    val response: String,
    val language: String,
    val actionExecuted: String = "",
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val moodLabel: String = "Calm",
    val moodScore: Int = 4,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "notes_items")
data class NoteItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val tag: String = "General",
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val scheduleText: String,
    val isDone: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "coding_projects")
data class CodingProjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val fileName: String,
    val language: String,
    val codeContent: String,
    val buildLog: String = "",
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "watchlist_items")
data class WatchlistItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val symbol: String,
    val assetName: String,
    val note: String,
    val targetPrice: String,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "automation_rules")
data class AutomationRuleEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val triggerNode: String,
    val conditionNode: String,
    val actionNode: String,
    val enabled: Boolean = true,
    val createdAtMs: Long = System.currentTimeMillis()
)
