package com.example.di

import android.content.Context
import com.example.R
import com.example.data.auth.AuthRepository
import com.example.data.firebase.AdminRepository
import com.example.data.firebase.FirestoreSyncRepository
import com.example.data.local.NishaDatabase
import com.example.data.preferences.NishaPreferences
import com.example.data.remote.GeminiRestClient
import com.example.data.remote.WeatherClient
import com.example.data.security.KeystoreSecretVault
import com.example.domain.DeviceActionRouter
import com.example.domain.MultiAgentOrchestrator
import com.example.service.FloatingOrbOverlayManager
import com.google.firebase.firestore.FirebaseFirestore

class NishaAppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val databaseId: String = appContext.getString(R.string.firestore_database_id)
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(databaseId)

    val secretVault = KeystoreSecretVault(appContext)
    val preferences = NishaPreferences(appContext)
    val database = NishaDatabase.getInstance(appContext)
    val dao = database.nishaDao()

    val geminiClient = GeminiRestClient(secretVault)
    val weatherClient = WeatherClient()
    val orchestrator = MultiAgentOrchestrator(geminiClient)
    val deviceActionRouter = DeviceActionRouter(appContext)
    val overlayManager = FloatingOrbOverlayManager(appContext)

    val firestoreSync = FirestoreSyncRepository(firestore)
    val adminRepository = AdminRepository(firestore)
    val authRepository = AuthRepository(
        context = appContext,
        dao = dao,
        vault = secretVault,
        preferences = preferences,
        firestoreSync = firestoreSync
    )
}
