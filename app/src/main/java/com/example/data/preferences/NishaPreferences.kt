package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.nishaDataStore: DataStore<Preferences> by preferencesDataStore(name = "nisha_settings")

data class NishaSettingsState(
    val onboardingCompleted: Boolean = false,
    val selectedModel: String = "gemini-2.5-flash",
    val memoryEnabled: Boolean = true,
    val cloudSyncEnabled: Boolean = true,
    val voiceLanguage: String = "en-IN",
    val voiceSpeechRate: Float = 1.0f,
    val voicePitch: Float = 1.0f,
    val voiceAutoSpeak: Boolean = true,
    val wakePhraseEnabled: Boolean = false,
    val orbEnabled: Boolean = true,
    val orbSystemOverlayEnabled: Boolean = false,
    val orbCompactMode: Boolean = false,
    val orbSizeDp: Float = 62f,
    val orbOffsetX: Float = 0f,
    val orbOffsetY: Float = 0f,
    val edgeLightingEnabled: Boolean = true,
    val edgeLightingStyle: String = "Aurora Sweep",
    val edgeLightingPalette: String = "Ice & Violet",
    val edgeLightingBrightness: Float = 0.72f,
    val edgeLightingSpeed: Float = 1.0f,
    val backgroundServiceDesired: Boolean = false,
    val activeLocalAccountId: String = "",
    val subscriptionTier: String = "free",
    val showWeatherWidget: Boolean = true,
    val showQuickActionsWidget: Boolean = true,
    val showSuggestionsWidget: Boolean = true,
    val showRecentTasksWidget: Boolean = true
)

class NishaPreferences(private val context: Context) {
    private val ds = context.applicationContext.nishaDataStore

    val settingsFlow: Flow<NishaSettingsState> = ds.data.map { p ->
        NishaSettingsState(
            onboardingCompleted = p[KEY_ONBOARDING] ?: false,
            selectedModel = p[KEY_MODEL] ?: "gemini-2.5-flash",
            memoryEnabled = p[KEY_MEMORY_ENABLED] ?: true,
            cloudSyncEnabled = p[KEY_CLOUD_SYNC] ?: true,
            voiceLanguage = p[KEY_VOICE_LANG] ?: "en-IN",
            voiceSpeechRate = p[KEY_VOICE_RATE] ?: 1.0f,
            voicePitch = p[KEY_VOICE_PITCH] ?: 1.0f,
            voiceAutoSpeak = p[KEY_VOICE_AUTOSPEAK] ?: true,
            wakePhraseEnabled = p[KEY_WAKE_PHRASE] ?: false,
            orbEnabled = p[KEY_ORB_ENABLED] ?: true,
            orbSystemOverlayEnabled = p[KEY_ORB_OVERLAY] ?: false,
            orbCompactMode = p[KEY_ORB_COMPACT] ?: false,
            orbSizeDp = p[KEY_ORB_SIZE] ?: 62f,
            orbOffsetX = p[KEY_ORB_X] ?: 0f,
            orbOffsetY = p[KEY_ORB_Y] ?: 0f,
            edgeLightingEnabled = p[KEY_EDGE_ENABLED] ?: true,
            edgeLightingStyle = p[KEY_EDGE_STYLE] ?: "Aurora Sweep",
            edgeLightingPalette = p[KEY_EDGE_PALETTE] ?: "Ice & Violet",
            edgeLightingBrightness = p[KEY_EDGE_BRIGHTNESS] ?: 0.72f,
            edgeLightingSpeed = p[KEY_EDGE_SPEED] ?: 1.0f,
            backgroundServiceDesired = p[KEY_BG_SERVICE] ?: false,
            activeLocalAccountId = p[KEY_LOCAL_ACCOUNT_ID] ?: "",
            subscriptionTier = p[KEY_SUB_TIER] ?: "free",
            showWeatherWidget = p[KEY_W_WEATHER] ?: true,
            showQuickActionsWidget = p[KEY_W_QUICK] ?: true,
            showSuggestionsWidget = p[KEY_W_SUGGESTIONS] ?: true,
            showRecentTasksWidget = p[KEY_W_RECENT_TASKS] ?: true
        )
    }

    suspend fun setOnboardingCompleted(done: Boolean) = ds.edit { it[KEY_ONBOARDING] = done }
    suspend fun setSelectedModel(model: String) = ds.edit { it[KEY_MODEL] = model }
    suspend fun setMemoryEnabled(enabled: Boolean) = ds.edit { it[KEY_MEMORY_ENABLED] = enabled }
    suspend fun setCloudSyncEnabled(enabled: Boolean) = ds.edit { it[KEY_CLOUD_SYNC] = enabled }

    suspend fun updateVoiceSettings(
        language: String? = null,
        rate: Float? = null,
        pitch: Float? = null,
        autoSpeak: Boolean? = null,
        wakePhrase: Boolean? = null
    ) = ds.edit { p ->
        language?.let { p[KEY_VOICE_LANG] = it }
        rate?.let { p[KEY_VOICE_RATE] = it }
        pitch?.let { p[KEY_VOICE_PITCH] = it }
        autoSpeak?.let { p[KEY_VOICE_AUTOSPEAK] = it }
        wakePhrase?.let { p[KEY_WAKE_PHRASE] = it }
    }

    suspend fun updateOrbSettings(
        enabled: Boolean? = null,
        systemOverlay: Boolean? = null,
        compact: Boolean? = null,
        sizeDp: Float? = null,
        offsetX: Float? = null,
        offsetY: Float? = null
    ) = ds.edit { p ->
        enabled?.let { p[KEY_ORB_ENABLED] = it }
        systemOverlay?.let { p[KEY_ORB_OVERLAY] = it }
        compact?.let { p[KEY_ORB_COMPACT] = it }
        sizeDp?.let { p[KEY_ORB_SIZE] = it }
        offsetX?.let { p[KEY_ORB_X] = it }
        offsetY?.let { p[KEY_ORB_Y] = it }
    }

    suspend fun updateEdgeLightingSettings(
        enabled: Boolean? = null,
        style: String? = null,
        palette: String? = null,
        brightness: Float? = null,
        speed: Float? = null
    ) = ds.edit { p ->
        enabled?.let { p[KEY_EDGE_ENABLED] = it }
        style?.let { p[KEY_EDGE_STYLE] = it }
        palette?.let { p[KEY_EDGE_PALETTE] = it }
        brightness?.let { p[KEY_EDGE_BRIGHTNESS] = it }
        speed?.let { p[KEY_EDGE_SPEED] = it }
    }

    suspend fun setBackgroundServiceDesired(desired: Boolean) = ds.edit { it[KEY_BG_SERVICE] = desired }
    suspend fun setActiveLocalAccountId(id: String) = ds.edit { it[KEY_LOCAL_ACCOUNT_ID] = id }
    suspend fun setSubscriptionTier(tier: String) = ds.edit { it[KEY_SUB_TIER] = tier }

    suspend fun updateHomeWidgets(
        weather: Boolean? = null,
        quickActions: Boolean? = null,
        suggestions: Boolean? = null,
        recentTasks: Boolean? = null
    ) = ds.edit { p ->
        weather?.let { p[KEY_W_WEATHER] = it }
        quickActions?.let { p[KEY_W_QUICK] = it }
        suggestions?.let { p[KEY_W_SUGGESTIONS] = it }
        recentTasks?.let { p[KEY_W_RECENT_TASKS] = it }
    }

    private companion object {
        val KEY_ONBOARDING = booleanPreferencesKey("onboarding_completed")
        val KEY_MODEL = stringPreferencesKey("selected_model")
        val KEY_MEMORY_ENABLED = booleanPreferencesKey("memory_enabled")
        val KEY_CLOUD_SYNC = booleanPreferencesKey("cloud_sync_enabled")
        val KEY_VOICE_LANG = stringPreferencesKey("voice_language")
        val KEY_VOICE_RATE = floatPreferencesKey("voice_rate")
        val KEY_VOICE_PITCH = floatPreferencesKey("voice_pitch")
        val KEY_VOICE_AUTOSPEAK = booleanPreferencesKey("voice_autospeak")
        val KEY_WAKE_PHRASE = booleanPreferencesKey("wake_phrase_enabled")
        val KEY_ORB_ENABLED = booleanPreferencesKey("orb_enabled")
        val KEY_ORB_OVERLAY = booleanPreferencesKey("orb_overlay")
        val KEY_ORB_COMPACT = booleanPreferencesKey("orb_compact")
        val KEY_ORB_SIZE = floatPreferencesKey("orb_size_dp")
        val KEY_ORB_X = floatPreferencesKey("orb_offset_x")
        val KEY_ORB_Y = floatPreferencesKey("orb_offset_y")
        val KEY_EDGE_ENABLED = booleanPreferencesKey("edge_enabled")
        val KEY_EDGE_STYLE = stringPreferencesKey("edge_style")
        val KEY_EDGE_PALETTE = stringPreferencesKey("edge_palette")
        val KEY_EDGE_BRIGHTNESS = floatPreferencesKey("edge_brightness")
        val KEY_EDGE_SPEED = floatPreferencesKey("edge_speed")
        val KEY_BG_SERVICE = booleanPreferencesKey("bg_service_desired")
        val KEY_LOCAL_ACCOUNT_ID = stringPreferencesKey("active_local_account_id")
        val KEY_SUB_TIER = stringPreferencesKey("subscription_tier")
        val KEY_W_WEATHER = booleanPreferencesKey("widget_weather")
        val KEY_W_QUICK = booleanPreferencesKey("widget_quick")
        val KEY_W_SUGGESTIONS = booleanPreferencesKey("widget_suggestions")
        val KEY_W_RECENT_TASKS = booleanPreferencesKey("widget_recent_tasks")
    }
}
