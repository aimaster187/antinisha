package com.example.data.firebase

import android.content.Context
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AdminRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        FirebaseAuth.getInstance()
    )

    suspend fun verifyAdminStatus(): Result<Boolean> = runCatching {
        val user = auth.currentUser ?: return@runCatching false
        if (user.isEmailVerified && user.email == BOOTSTRAP_ADMIN_EMAIL) {
            return@runCatching true
        }
        val adminDoc = db.collection("admins").document(user.uid).get().await()
        adminDoc.exists()
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.GET, "admins/${auth.currentUser?.uid}", auth)
    }

    suspend fun listAllUsers(): Result<List<FsUserProfile>> = runCatching {
        val snap = db.collection("users").limit(100).get().await()
        snap.toObjects(FsUserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.LIST, "users", auth)
    }

    suspend fun updateUserTierAndStatus(
        targetUserId: String,
        tier: String,
        status: String
    ): Result<Unit> = runCatching {
        val actor = auth.currentUser ?: throw IllegalStateException("Not authenticated")
        val docRef = db.collection("users").document(targetUserId)
        docRef.update(
            mapOf(
                "tier" to tier,
                "status" to status,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        logAuditAction(
            actorUid = actor.uid,
            actorEmail = actor.email.orEmpty(),
            action = "UPDATE_USER_ACCESS",
            target = "users/$targetUserId",
            details = "Set tier=$tier, status=$status"
        )
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "users/$targetUserId", auth)
    }

    suspend fun saveAppConfig(
        maintenanceMode: Boolean,
        latestVersionName: String,
        latestVersionCode: Long,
        defaultModel: String,
        announcementBanner: String,
        releaseNotes: String
    ): Result<Unit> = runCatching {
        val actor = auth.currentUser ?: throw IllegalStateException("Not authenticated")
        val docRef = db.collection("app_config").document("global")
        docRef.set(
            mapOf(
                "maintenanceMode" to maintenanceMode,
                "latestVersionName" to latestVersionName.take(32),
                "latestVersionCode" to latestVersionCode.coerceAtLeast(1L),
                "defaultModel" to defaultModel.take(80),
                "announcementBanner" to announcementBanner.take(500),
                "releaseNotes" to releaseNotes.take(2000),
                "updatedBy" to actor.uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        logAuditAction(
            actorUid = actor.uid,
            actorEmail = actor.email.orEmpty(),
            action = "UPDATE_APP_CONFIG",
            target = "app_config/global",
            details = "maintenance=$maintenanceMode, version=$latestVersionName ($latestVersionCode), model=$defaultModel"
        )
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "app_config/global", auth)
    }

    suspend fun sendAnnouncement(
        title: String,
        body: String,
        targetMode: String,
        targetUsername: String = "",
        targetUid: String = ""
    ): Result<String> = runCatching {
        val actor = auth.currentUser ?: throw IllegalStateException("Not authenticated")
        val id = "ann_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        db.collection("announcements").document(id).set(
            mapOf(
                "title" to title.take(120),
                "body" to body.take(1500),
                "targetMode" to targetMode,
                "targetUsername" to targetUsername.take(60),
                "targetUid" to targetUid.take(128),
                "createdBy" to actor.uid,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
        logAuditAction(
            actorUid = actor.uid,
            actorEmail = actor.email.orEmpty(),
            action = "SEND_ANNOUNCEMENT",
            target = "announcements/$id",
            details = "mode=$targetMode target=${targetUsername.ifBlank { "all" }} title=$title"
        )
        id
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "announcements", auth)
    }

    suspend fun listLicenses(): Result<List<FsLicenseRecord>> = runCatching {
        val snap = db.collection("licenses").limit(100).get().await()
        snap.toObjects(FsLicenseRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.LIST, "licenses", auth)
    }

    suspend fun upsertLicense(
        licenseId: String,
        userId: String,
        plan: String,
        status: String,
        source: String,
        expiresAtEpochMs: Long
    ): Result<Unit> = runCatching {
        val actor = auth.currentUser ?: throw IllegalStateException("Not authenticated")
        val safeId = licenseId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        val docRef = db.collection("licenses").document(safeId)
        val snap = docRef.get().await()
        if (!snap.exists()) {
            docRef.set(
                mapOf(
                    "userId" to userId,
                    "plan" to plan,
                    "status" to status,
                    "source" to source.take(60),
                    "expiresAtEpochMs" to expiresAtEpochMs,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            docRef.update(
                mapOf(
                    "plan" to plan,
                    "status" to status,
                    "source" to source.take(60),
                    "expiresAtEpochMs" to expiresAtEpochMs,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        logAuditAction(
            actorUid = actor.uid,
            actorEmail = actor.email.orEmpty(),
            action = "UPSERT_LICENSE",
            target = "licenses/$safeId",
            details = "user=$userId plan=$plan status=$status"
        )
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "licenses/$licenseId", auth)
    }

    suspend fun listAuditLogs(): Result<List<FsAuditLog>> = runCatching {
        val snap = db.collection("audit_logs").limit(100).get().await()
        snap.toObjects(FsAuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.LIST, "audit_logs", auth)
    }

    suspend fun browseCollection(collectionPath: String): Result<List<Pair<String, Map<String, Any?>>>> = runCatching {
        val allowed = setOf("users", "app_config", "announcements", "licenses", "audit_logs")
        require(collectionPath in allowed) { "Collection '$collectionPath' is not permitted in Admin Explorer." }
        val snap = db.collection(collectionPath).limit(50).get().await()
        snap.documents.map { doc -> doc.id to (doc.data ?: emptyMap()) }
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.LIST, collectionPath, auth)
    }

    private suspend fun logAuditAction(
        actorUid: String,
        actorEmail: String,
        action: String,
        target: String,
        details: String
    ) {
        val id = "log_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        db.collection("audit_logs").document(id).set(
            mapOf(
                "actorUid" to actorUid,
                "actorEmail" to actorEmail.take(160),
                "action" to action.take(120),
                "target" to target.take(160),
                "details" to details.take(1000),
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    companion object {
        const val BOOTSTRAP_ADMIN_EMAIL = "ai.master.bot.official@gmail.com"
    }
}
