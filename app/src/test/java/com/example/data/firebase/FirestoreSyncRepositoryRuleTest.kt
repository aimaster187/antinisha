package com.example.data.firebase

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class FirestoreSyncRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun upsertUserProfile_authenticatedOwner_createsAndReadsOwnProfile() = runBlocking {
        val uid = signInTestUser("alice_${UUID.randomUUID().toString().take(6)}@test.com")
        val repo = FirestoreSyncRepository(firestore, auth)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.upsertUserProfile(
                username = "alice_nisha",
                fullName = "Alice Nisha",
                email = "alice@test.com",
                phone = "+919876543210",
                authProvider = "google",
                emailVerified = true
            )
        }
        assertTrue("Expected upsertUserProfile to succeed: ${result.exceptionOrNull()}", result.isSuccess)
        val profile = result.getOrThrow()
        assertEquals(uid, profile.userId)
        assertEquals("free", profile.tier)
    }

    @Test
    fun saveAndObserveMemory_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        signInTestUser("bob_${UUID.randomUUID().toString().take(6)}@test.com")
        val repo = FirestoreSyncRepository(firestore, auth)
        val memoryId = "mem_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveMemory(
                memoryId = memoryId,
                category = "preference",
                title = "Voice Style",
                content = "Use concise Hinglish responses"
            )
        }
        assertTrue("Expected saveMemory to succeed: ${saveResult.exceptionOrNull()}", saveResult.isSuccess)

        val memories = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeMemories().first { list -> list.any { it.id == memoryId } }
        }
        assertTrue(memories.any { it.id == memoryId && it.title == "Voice Style" })
    }

    @Test
    fun getUserProfile_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_iso_${UUID.randomUUID().toString().take(6)}@test.com")
        val aliceRepo = FirestoreSyncRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.upsertUserProfile("alice_iso", "Alice Iso", "alice_iso@test.com").getOrThrow()
        }

        signInTestUser("bob_iso_${UUID.randomUUID().toString().take(6)}@test.com")
        val bobRepo = FirestoreSyncRepository(firestore, auth)
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getUserProfile(aliceUid)
        }
        assertTrue("Cross-user getUserProfile should fail", result.isFailure)
        val fsEx = result.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun observeBroadcastAnnouncements_unauthenticatedCaller_throwsAndFailsSafely() = runBlocking {
        auth.signOut()
        val repo = FirestoreSyncRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeBroadcastAnnouncements().first()
            }
            fail("Expected unauthenticated observeBroadcastAnnouncements to fail")
        } catch (e: Exception) {
            assertTrue(e is IllegalStateException || e.unwrapFirestoreException()?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED)
        }
    }

    @Test
    fun adminRepository_nonAdminUser_cannotListAllUsers() = runBlocking {
        signInTestUser("regular_${UUID.randomUUID().toString().take(6)}@test.com")
        val adminRepo = AdminRepository(firestore, auth)

        val isAdmin = withTimeout(DEFAULT_TIMEOUT_MS) { adminRepo.verifyAdminStatus().getOrThrow() }
        assertEquals(false, isAdmin)

        val usersResult = withTimeout(DEFAULT_TIMEOUT_MS) { adminRepo.listAllUsers() }
        assertTrue("Non-admin listAllUsers should fail", usersResult.isFailure)
        val fsEx = usersResult.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
