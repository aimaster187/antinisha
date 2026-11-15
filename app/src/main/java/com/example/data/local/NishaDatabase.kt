package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LocalAccountEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        AgentConfigEntity::class,
        AgentTaskEntity::class,
        MemoryFactEntity::class,
        VoiceMacroEntity::class,
        VoiceHistoryEntity::class,
        JournalEntryEntity::class,
        NoteItemEntity::class,
        ReminderEntity::class,
        CodingProjectEntity::class,
        WatchlistItemEntity::class,
        AutomationRuleEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NishaDatabase : RoomDatabase() {
    abstract fun nishaDao(): NishaDao

    companion object {
        @Volatile
        private var INSTANCE: NishaDatabase? = null

        fun getInstance(context: Context): NishaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NishaDatabase::class.java,
                    "nisha_ai_vault.db"
                ).build().also { INSTANCE = it }
            }
        }

        val DEFAULT_FIVE_AGENTS = listOf(
            AgentConfigEntity(
                id = "agent_nisha_core",
                slotIndex = 0,
                name = "Nisha Core",
                role = "Executive Coordinator & Multilingual Voice Strategist",
                iconName = "atom",
                instructions = "You are Nisha Core, the primary executive intelligence. Synthesize inputs, keep responses lucid, structured, and helpful in English, Hindi, or Hinglish as requested.",
                capabilities = "Planning, synthesis, multilingual reasoning, context routing",
                status = "active",
                isDefault = true
            ),
            AgentConfigEntity(
                id = "agent_code_master",
                slotIndex = 1,
                name = "Code Master",
                role = "Principal Software Architect & Debugger",
                iconName = "code",
                instructions = "You are Code Master. Write clean, production-grade Kotlin, Python, TypeScript, HTML/CSS/JS, diagnose stack traces, and verify edge cases.",
                capabilities = "Code generation, refactoring, debugging, web & Android architecture",
                status = "active",
                isDefault = true
            ),
            AgentConfigEntity(
                id = "agent_research_pro",
                slotIndex = 2,
                name = "Research Pro",
                role = "Deep Analyst & Structured Fact Synthesizer",
                iconName = "globe",
                instructions = "You are Research Pro. Break down complex topics into verifiable pillars, highlight assumptions, compare trade-offs, and never fabricate citations.",
                capabilities = "Deep synthesis, comparative analysis, structured briefings",
                status = "active",
                isDefault = true
            ),
            AgentConfigEntity(
                id = "agent_vision_ai",
                slotIndex = 3,
                name = "Vision AI",
                role = "Multimodal Visual & Document Inspector",
                iconName = "eye",
                instructions = "You are Vision AI. Analyze images, UI screenshots, charts, and documents with precision, extracting structured key points and actionable insights.",
                capabilities = "Image analysis, OCR summarization, UI & document review",
                status = "active",
                isDefault = true
            ),
            AgentConfigEntity(
                id = "agent_personal_ai",
                slotIndex = 4,
                name = "Personal AI",
                role = "Productivity, Wellness & Communication Coach",
                iconName = "brain",
                instructions = "You are Personal AI. Help organize tasks, draft polite WhatsApp/SMS messages, reflect on journals, and support study and daily planning.",
                capabilities = "Drafting messages, scheduling, study coaching, journaling reflection",
                status = "active",
                isDefault = true
            )
        )
    }
}
