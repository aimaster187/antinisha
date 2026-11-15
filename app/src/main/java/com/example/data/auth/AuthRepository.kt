package com.example.data.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.firebase.FirestoreSyncRepository
import com.example.data.local.LocalAccountEntity
import com.example.data.local.NishaDao
import com.example.data.preferences.NishaPreferences
import com.example.data.security.KeystoreSecretVault
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed interface NishaSession {
    data object Unauthenticated : NishaSession
    data class FirebaseGoogle(
        val uid: String,
        val displayName: String,
        val email: String,
        val emailVerified: Boolean,
        val username: String
    ) : NishaSession

    data class LocalVault(
        val accountId: String,
        val username: String,
        val fullName: String,
        val email: String,
        val phone: String,
        val emailVerified: Boolean
    ) : NishaSession
}

class AuthRepository(
    private val context: Context,
    private val dao: NishaDao,
    private val vault: KeystoreSecretVault,
    private val preferences: NishaPreferences,
    private val firestoreSync: FirestoreSyncRepository
) {
    private val auth: FirebaseAuth = Firebase.auth
    private val credentialManager: CredentialManager = CredentialManager.create(context)

    fun firebaseAuthStateFlow(): Flow<com.google.firebase.auth.FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun attemptSilentGoogleSignIn(): Result<Unit> = runCatching {
        if (auth.currentUser != null) return@runCatching
        val clientId = context.getString(R.string.default_web_client_id)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            val authCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authRes = auth.signInWithCredential(authCredential).await()
            val user = authRes.user
            if (user != null) {
                val baseUsername = (user.email?.substringBefore("@") ?: "nisha_user")
                    .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                    .take(30)
                firestoreSync.upsertUserProfile(
                    username = baseUsername,
                    fullName = user.displayName ?: baseUsername,
                    email = user.email.orEmpty(),
                    phone = user.phoneNumber.orEmpty(),
                    authProvider = "google",
                    emailVerified = user.isEmailVerified
                )
            }
        }
    }

    suspend fun signInWithGoogleInteractive(activity: Activity): Result<String> {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            return Result.failure(IllegalStateException("Google Sign-In config missing: default_web_client_id"))
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

        return try {
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authRes = auth.signInWithCredential(authCredential).await()
                val user = authRes.user ?: error("Firebase user unavailable after sign-in.")
                val baseUsername = (user.email?.substringBefore("@") ?: "nisha_user")
                    .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                    .take(30)
                firestoreSync.upsertUserProfile(
                    username = baseUsername,
                    fullName = user.displayName ?: baseUsername,
                    email = user.email.orEmpty(),
                    phone = user.phoneNumber.orEmpty(),
                    authProvider = "google",
                    emailVerified = user.isEmailVerified
                )
                preferences.setActiveLocalAccountId("")
                Result.success(user.uid)
            } else {
                Result.failure(IllegalStateException("Unexpected credential type returned."))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            Result.failure(e)
        }
    }

    suspend fun signUpLocalAccount(
        username: String,
        fullName: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String
    ): Result<LocalAccountEntity> = runCatching {
        val cleanUser = username.trim()
        val cleanName = fullName.trim()
        val cleanEmail = email.trim().lowercase()
        val cleanPhone = phone.trim()

        require(cleanUser.length >= 3) { "Username must be at least 3 characters." }
        require(cleanName.isNotBlank()) { "Full name is required." }
        require(cleanEmail.contains("@") && cleanEmail.contains(".")) { "Enter a valid email address." }
        require(password.length >= 8) { "Password must be at least 8 characters." }
        require(password == confirmPassword) { "Passwords do not match." }

        val existingUser = dao.findLocalAccount(cleanUser) ?: dao.findLocalAccount(cleanEmail)
        require(existingUser == null) { "An account with this username or email already exists." }

        val (salt, hash) = vault.hashPassword(password)
        val entity = LocalAccountEntity(
            username = cleanUser,
            fullName = cleanName,
            email = cleanEmail,
            phone = cleanPhone,
            passwordSalt = salt,
            passwordHash = hash,
            emailVerified = false
        )
        dao.upsertLocalAccount(entity)
        preferences.setActiveLocalAccountId(entity.id)
        entity
    }

    suspend fun signInLocalAccount(
        usernameOrEmail: String,
        password: String
    ): Result<LocalAccountEntity> = runCatching {
        val identifier = usernameOrEmail.trim().lowercase()
        require(identifier.isNotBlank()) { "Enter your username or email." }
        require(password.isNotBlank()) { "Enter your password." }

        val account = dao.findLocalAccount(identifier)
            ?: dao.findLocalAccount(usernameOrEmail.trim())
            ?: error("Account not found. Sign up or use Google Sign-In for cloud access.")

        val valid = vault.verifyPassword(password, account.passwordSalt, account.passwordHash)
        require(valid) { "Incorrect password." }

        preferences.setActiveLocalAccountId(account.id)
        account
    }

    suspend fun resetLocalPassword(
        identifier: String,
        newPassword: String,
        confirmPassword: String
    ): Result<Unit> = runCatching {
        require(newPassword.length >= 8) { "New password must be at least 8 characters." }
        require(newPassword == confirmPassword) { "Passwords do not match." }
        val account = dao.findLocalAccount(identifier.trim().lowercase())
            ?: dao.findLocalAccount(identifier.trim())
            ?: error("No local account found matching '$identifier'.")
        val (salt, hash) = vault.hashPassword(newPassword)
        dao.upsertLocalAccount(account.copy(passwordSalt = salt, passwordHash = hash))
    }

    suspend fun markLocalEmailVerified(accountId: String): Result<Unit> = runCatching {
        val account = dao.getLocalAccountById(accountId) ?: error("Local account not found.")
        dao.upsertLocalAccount(account.copy(emailVerified = true))
    }

    suspend fun updateLocalProfile(
        accountId: String,
        fullName: String,
        phone: String
    ): Result<Unit> = runCatching {
        val account = dao.getLocalAccountById(accountId) ?: error("Account not found.")
        dao.upsertLocalAccount(account.copy(fullName = fullName.trim(), phone = phone.trim()))
    }

    suspend fun signOutAll() {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                auth.signOut()
                preferences.setActiveLocalAccountId("")
            }
        }
    }

    suspend fun deleteActiveAccount(localAccountId: String): Result<Unit> = runCatching {
        if (auth.currentUser != null) {
            firestoreSync.deleteOwnAccountData().getOrThrow()
            runCatching { auth.currentUser?.delete()?.await() }
        }
        if (localAccountId.isNotBlank()) {
            dao.deleteLocalAccount(localAccountId)
        }
        signOutAll()
    }
}
