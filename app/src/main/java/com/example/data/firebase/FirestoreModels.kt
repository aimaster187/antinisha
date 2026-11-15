package com.example.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FsUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val authProvider: String = "google",
    val emailVerified: Boolean = false,
    val tier: String = "free",
    val status: String = "active",
    @ServerTimestamp val lastLoginAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FsConversation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val model: String = "gemini-2.5-flash",
    val messageCount: Long = 0L,
    val lastMessagePreview: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FsChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "user",
    val content: String = "",
    val agentName: String = "",
    val modelUsed: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FsMemoryFact(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = "preference",
    val title: String = "",
    val content: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FsAgentConfig(
    @DocumentId val id: String = "",
    val userId: String = "",
    val slotIndex: Long = 0L,
    val name: String = "",
    val role: String = "",
    val iconName: String = "robot",
    val instructions: String = "",
    val status: String = "active",
    val permWebResearch: Boolean = false,
    val permFileRead: Boolean = false,
    val permFileWrite: Boolean = false,
    val permDeviceActions: Boolean = false,
    val permMessaging: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FsAppConfig(
    @DocumentId val id: String = "",
    val maintenanceMode: Boolean = false,
    val latestVersionName: String = "1.0.0",
    val latestVersionCode: Long = 1L,
    val defaultModel: String = "gemini-2.5-flash",
    val announcementBanner: String = "",
    val releaseNotes: String = "",
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FsAnnouncement(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val targetMode: String = "all",
    val targetUsername: String = "",
    val targetUid: String = "",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FsLicenseRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val plan: String = "free",
    val status: String = "active",
    val source: String = "system",
    val expiresAtEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FsAuditLog(
    @DocumentId val id: String = "",
    val actorUid: String = "",
    val actorEmail: String = "",
    val action: String = "",
    val target: String = "",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
