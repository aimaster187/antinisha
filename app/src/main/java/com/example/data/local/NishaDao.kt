package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NishaDao {
    // Local Accounts (PBKDF2 hashed)
    @Query("SELECT * FROM local_accounts WHERE username = :identifier OR email = :identifier LIMIT 1")
    suspend fun findLocalAccount(identifier: String): LocalAccountEntity?

    @Query("SELECT * FROM local_accounts WHERE id = :id LIMIT 1")
    suspend fun getLocalAccountById(id: String): LocalAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocalAccount(account: LocalAccountEntity)

    @Query("DELETE FROM local_accounts WHERE id = :id")
    suspend fun deleteLocalAccount(id: String)

    // Chat Sessions & Messages
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAtMs DESC")
    fun observeChatSessions(): Flow<List<ChatSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChatSession(session: ChatSessionEntity)

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteChatSession(sessionId: String)

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY createdAtMs ASC")
    fun observeMessages(sessionId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY createdAtMs ASC")
    suspend fun getMessagesOnce(sessionId: String): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    // Agents (Max 5)
    @Query("SELECT * FROM agent_configs ORDER BY slotIndex ASC")
    fun observeAgents(): Flow<List<AgentConfigEntity>>

    @Query("SELECT * FROM agent_configs ORDER BY slotIndex ASC")
    suspend fun getAgentsOnce(): List<AgentConfigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAgent(agent: AgentConfigEntity)

    @Query("DELETE FROM agent_configs WHERE id = :agentId")
    suspend fun deleteAgent(agentId: String)

    // Agent Tasks
    @Query("SELECT * FROM agent_tasks ORDER BY createdAtMs DESC LIMIT 40")
    fun observeAgentTasks(): Flow<List<AgentTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAgentTask(task: AgentTaskEntity)

    // Memories
    @Query("SELECT * FROM memory_facts ORDER BY updatedAtMs DESC")
    fun observeMemories(): Flow<List<MemoryFactEntity>>

    @Query("SELECT * FROM memory_facts ORDER BY updatedAtMs DESC")
    suspend fun getMemoriesOnce(): List<MemoryFactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMemory(memory: MemoryFactEntity)

    @Query("DELETE FROM memory_facts WHERE id = :id")
    suspend fun deleteMemory(id: String)

    // Voice Macros & History
    @Query("SELECT * FROM voice_macros ORDER BY createdAtMs DESC")
    fun observeVoiceMacros(): Flow<List<VoiceMacroEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVoiceMacro(macro: VoiceMacroEntity)

    @Query("DELETE FROM voice_macros WHERE id = :id")
    suspend fun deleteVoiceMacro(id: String)

    @Query("SELECT * FROM voice_history ORDER BY createdAtMs DESC LIMIT 50")
    fun observeVoiceHistory(): Flow<List<VoiceHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceHistory(item: VoiceHistoryEntity)

    // Journal, Notes, Reminders
    @Query("SELECT * FROM journal_entries ORDER BY createdAtMs DESC")
    fun observeJournalEntries(): Flow<List<JournalEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJournalEntry(entry: JournalEntryEntity)

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun deleteJournalEntry(id: String)

    @Query("SELECT * FROM notes_items ORDER BY updatedAtMs DESC")
    fun observeNotes(): Flow<List<NoteItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: NoteItemEntity)

    @Query("DELETE FROM notes_items WHERE id = :id")
    suspend fun deleteNote(id: String)

    @Query("SELECT * FROM reminders ORDER BY isDone ASC, createdAtMs DESC")
    fun observeReminders(): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminder(id: String)

    // Coding Projects
    @Query("SELECT * FROM coding_projects ORDER BY updatedAtMs DESC")
    fun observeCodingProjects(): Flow<List<CodingProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCodingProject(project: CodingProjectEntity)

    @Query("DELETE FROM coding_projects WHERE id = :id")
    suspend fun deleteCodingProject(id: String)

    // Watchlist & Automation
    @Query("SELECT * FROM watchlist_items ORDER BY createdAtMs DESC")
    fun observeWatchlist(): Flow<List<WatchlistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWatchlistItem(item: WatchlistItemEntity)

    @Query("DELETE FROM watchlist_items WHERE id = :id")
    suspend fun deleteWatchlistItem(id: String)

    @Query("SELECT * FROM automation_rules ORDER BY createdAtMs DESC")
    fun observeAutomationRules(): Flow<List<AutomationRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAutomationRule(rule: AutomationRuleEntity)

    @Query("DELETE FROM automation_rules WHERE id = :id")
    suspend fun deleteAutomationRule(id: String)
}
