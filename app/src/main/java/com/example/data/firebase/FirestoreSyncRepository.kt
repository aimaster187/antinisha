package com.example.data.firebase

import android.content.Context
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        FirebaseAuth.getInstance()
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    suspend fun upsertUserProfile(
        username: String,
        fullName: String,
        email: String,
        phone: String = "",
        authProvider: String = "google",
        emailVerified: Boolean = false
    ): Result<FsUserProfile> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val existingSnap = docRef.get().await()
        if (!existingSnap.exists()) {
            val payload = mapOf(
                "userId" to uid,
                "username" to username.take(40).ifBlank { "nisha_user" },
                "fullName" to fullName.take(80).ifBlank { "NISHA User" },
                "email" to email.take(160),
                "phone" to phone.take(32),
                "authProvider" to authProvider.take(40),
                "emailVerified" to emailVerified,
                "tier" to "free",
                "status" to "active",
                "lastLoginAt" to FieldValue.serverTimestamp(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } else {
            val updatePayload = mapOf(
                "username" to username.take(40).ifBlank { existingSnap.getString("username") ?: "nisha_user" },
                "fullName" to fullName.take(80).ifBlank { existingSnap.getString("fullName") ?: "NISHA User" },
                "phone" to phone.take(32),
                "authProvider" to authProvider.take(40),
                "emailVerified" to emailVerified,
                "lastLoginAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updatePayload).await()
        }
        docRef.get().await()
            .toObject(FsUserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?: FsUserProfile(userId = uid, username = username, fullName = fullName, email = email)
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}", auth)
    }

    suspend fun getUserProfile(userId: String): Result<FsUserProfile?> = runCatching {
        requireUserId()
        val docRef = db.collection("users").document(userId)
        docRef.get().await()
            .toObject(FsUserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.GET, "users/$userId", auth)
    }

    fun observeOwnProfile(): Flow<FsUserProfile?> = flow {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        emitAll(
            docRef.snapshots()
                .map { it.toObject(FsUserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsError ?: error, OperationType.GET, docRef.path, auth)
                    }
                    throw error
                }
        )
    }

    suspend fun saveMemory(
        memoryId: String = UUID.randomUUID().toString(),
        category: String,
        title: String,
        content: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val safeId = memoryId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        val docRef = db.collection("users").document(uid).collection("memories").document(safeId)
        val snap = docRef.get().await()
        if (!snap.exists()) {
            val payload = mapOf(
                "userId" to uid,
                "category" to category.take(60).ifBlank { "fact" },
                "title" to title.take(120).ifBlank { "Memory" },
                "content" to content.take(4000).ifBlank { "-" },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } else {
            val payload = mapOf(
                "category" to category.take(60).ifBlank { "fact" },
                "title" to title.take(120).ifBlank { "Memory" },
                "content" to content.take(4000).ifBlank { "-" },
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(payload).await()
        }
        safeId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/memories", auth)
    }

    fun observeMemories(): Flow<List<FsMemoryFact>> = flow {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("memories")
        emitAll(
            colRef.snapshots()
                .map { snap -> snap.toObjects(FsMemoryFact::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsError ?: error, OperationType.LIST, colRef.path, auth)
                    }
                    throw error
                }
        )
    }

    suspend fun deleteMemory(memoryId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val safeId = memoryId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        val docRef = db.collection("users").document(uid).collection("memories").document(safeId)
        docRef.delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "users/${auth.currentUser?.uid}/memories/$memoryId", auth)
    }

    suspend fun syncConversation(
        conversationId: String,
        title: String,
        model: String,
        messageCount: Long,
        lastMessagePreview: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val safeId = conversationId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        val docRef = db.collection("users").document(uid).collection("conversations").document(safeId)
        val snap = docRef.get().await()
        if (!snap.exists()) {
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "title" to title.take(160).ifBlank { "New Conversation" },
                    "model" to model.take(80),
                    "messageCount" to messageCount.coerceAtLeast(0L),
                    "lastMessagePreview" to lastMessagePreview.take(300),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            docRef.update(
                mapOf(
                    "title" to title.take(160).ifBlank { "Conversation" },
                    "model" to model.take(80),
                    "messageCount" to messageCount.coerceAtLeast(0L),
                    "lastMessagePreview" to lastMessagePreview.take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/conversations/$conversationId", auth)
    }

    suspend fun appendMessage(
        conversationId: String,
        messageId: String,
        role: String,
        content: String,
        agentName: String = "",
        modelUsed: String = ""
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val safeConvId = conversationId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        val safeMsgId = messageId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        val docRef = db.collection("users").document(uid)
            .collection("conversations").document(safeConvId)
            .collection("messages").document(safeMsgId)
        docRef.set(
            mapOf(
                "userId" to uid,
                "role" to role,
                "content" to content.take(60000).ifBlank { " " },
                "agentName" to agentName.take(80),
                "modelUsed" to modelUsed.take(80),
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "users/${auth.currentUser?.uid}/conversations/$conversationId/messages", auth)
    }

    suspend fun getAppConfig(): Result<FsAppConfig?> = runCatching {
        requireUserId()
        db.collection("app_config").document("global")
            .get().await()
            .toObject(FsAppConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.GET, "app_config/global", auth)
    }

    fun observeBroadcastAnnouncements(): Flow<List<FsAnnouncement>> = flow {
        requireUserId()
        val query = db.collection("announcements").whereEqualTo("targetMode", "all")
        emitAll(
            query.snapshots()
                .map { snap -> snap.toObjects(FsAnnouncement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    handleFirestoreError(fsError ?: error, OperationType.LIST, "announcements", auth)
                    throw error
                }
        )
    }

    suspend fun deleteOwnAccountData(): Result<Unit> = runCatching {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val memories = userRef.collection("memories").get().await()
        memories.documents.forEach { it.reference.delete().await() }
        val agents = userRef.collection("agents").get().await()
        agents.documents.forEach { it.reference.delete().await() }
        val convs = userRef.collection("conversations").get().await()
        convs.documents.forEach { conv ->
            val msgs = conv.reference.collection("messages").get().await()
            msgs.documents.forEach { it.reference.delete().await() }
            conv.reference.delete().await()
        }
        userRef.delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "users/${auth.currentUser?.uid}", auth)
    }
}
