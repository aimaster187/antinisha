package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.data.auth.NishaSession
import com.example.data.firebase.FsAppConfig
import com.example.data.preferences.NishaSettingsState
import com.example.data.remote.GeminiRestClient
import com.example.service.NishaAccessibilityService
import com.example.service.NishaAssistantService
import com.example.ui.theme.AmberSoft
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CrimsonSoft
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.FaGlyphs
import com.example.ui.theme.FaIcon
import com.example.ui.theme.FrostBorder
import com.example.ui.theme.IceBlueDeep
import com.example.ui.theme.IceBlueSoft
import com.example.ui.theme.IndigoMuted
import com.example.ui.theme.LavenderGlow
import com.example.ui.theme.NishaGlassCard
import com.example.ui.theme.NishaSectionHeader
import com.example.ui.theme.NishaStatusPill
import com.example.ui.theme.ObsidianSlate
import com.example.ui.theme.PearlWhite

@Composable
fun SettingsScreen(
    session: NishaSession,
    settings: NishaSettingsState,
    hasApiKey: Boolean,
    maskedApiKey: String,
    apiKeyStatus: String?,
    isServiceRunning: Boolean,
    appConfig: FsAppConfig,
    onSelectModel: (String) -> Unit,
    onSaveVerifyKey: (String) -> Unit,
    onSaveKeyWithoutVerify: (String) -> Unit,
    onClearKey: () -> Unit,
    onUpdateOrb: (Boolean?, Boolean?, Boolean?, Float?) -> Unit,
    onUpdateEdgeLighting: (Boolean?, String?, String?, Float?, Float?) -> Unit,
    onUpdateVoiceLanguage: (String) -> Unit,
    onToggleMemory: (Boolean) -> Unit,
    onVerifyLocalEmail: (String) -> Unit,
    onUpdateLocalProfile: (String, String, String) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenSubscriptions: () -> Unit,
    onOpenAgents: () -> Unit,
    onOpenBrain: () -> Unit,
    onOpenVoiceOs: () -> Unit,
    onOpenAdminCenter: () -> Unit,
    onCheckExternalReleaseUrl: (String) -> Unit
) {
    val context = LocalContext.current
    var apiKeyInput by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }
    var showAccessibilityGuide by remember { mutableStateOf(false) }
    var updateCheckMessage by remember { mutableStateOf<String?>(null) }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) NishaAssistantService.start(context)
    }
    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val locLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val camLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    val canDrawOverlays = remember(settings.orbSystemOverlayEnabled, isServiceRunning) {
        Settings.canDrawOverlays(context)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 720.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 124.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Settings & System Control", style = MaterialTheme.typography.headlineMedium, color = ObsidianSlate)
            Text("20 integrated controls for AI, Keys, Orb, Edge Lighting, Permissions & Security", style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
        }

        // 1. Account & Session Management
        item {
            NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NishaSectionHeader("1. Account & Session", "Manage profile, verification, sign out, or permanent deletion", FaGlyphs.USER)
                    when (session) {
                        is NishaSession.FirebaseGoogle -> {
                            Text("${session.displayName} (@${session.username})", style = MaterialTheme.typography.titleMedium, color = ObsidianSlate)
                            Text("Google Sign-In • ${session.email}", style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                            NishaStatusPill(
                                label = if (session.emailVerified) "Email Verified" else "Email Unverified",
                                iconGlyph = FaGlyphs.CIRCLE_CHECK,
                                tint = if (session.emailVerified) EmeraldSuccess else AmberWarn,
                                background = if (session.emailVerified) EmeraldSoft else AmberSoft
                            )
                        }
                        is NishaSession.LocalVault -> {
                            Text("${session.fullName} (@${session.username})", style = MaterialTheme.typography.titleMedium, color = ObsidianSlate)
                            Text("Local Vault Account • ${session.email}", style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!session.emailVerified) {
                                    OutlinedButton(onClick = { onVerifyLocalEmail(session.accountId) }) { Text("Confirm Local Email") }
                                } else {
                                    NishaStatusPill("Local Email Confirmed", FaGlyphs.CIRCLE_CHECK, tint = EmeraldSuccess, background = EmeraldSoft)
                                }
                                OutlinedButton(onClick = { showEditProfile = true }) { Text("Edit Profile") }
                            }
                        }
                        NishaSession.Unauthenticated -> {
                            Text("Not signed in", style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onSignOut, modifier = Modifier.weight(1f).height(48.dp).testTag("settings_signout_btn")) {
                            FaIcon(FaGlyphs.RIGHT_FROM_BRACKET, contentDescription = null, size = 14.sp)
                            Spacer(Modifier.width(6.dp))
                            Text("Sign Out")
                        }
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonAlert),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("settings_delete_account_btn")
                        ) {
                            FaIcon(FaGlyphs.TRASH, contentDescription = null, color = CrimsonAlert, size = 14.sp)
                            Spacer(Modifier.width(6.dp))
                            Text("Delete Account")
                        }
                    }
                }
            }
        }

        // 2-5. Appearance, Language, Voice OS & AI Model
        item {
            NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NishaSectionHeader("2-5. Theme, Language, Voice & Gemini Model", "Luxury White Surface • Multi-lingual Voice OS", FaGlyphs.ATOM)
                    NishaStatusPill("Theme: Luxury White Interface Active", FaGlyphs.SUN, tint = IceBlueDeep, background = IceBlueSoft)
                    Text("Assistant Voice Language", style = MaterialTheme.typography.labelLarge, color = ObsidianSlate)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("en-IN" to "English", "hi-IN" to "Hindi", "hinglish" to "Hinglish").forEach { (code, label) ->
                            FilterChip(selected = settings.voiceLanguage == code, onClick = { onUpdateVoiceLanguage(code) }, label = { Text(label) })
                        }
                    }
                    Text("Active Gemini Model", style = MaterialTheme.typography.labelLarge, color = ObsidianSlate)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(GeminiRestClient.SUPPORTED_MODELS) { m ->
                            FilterChip(selected = settings.selectedModel == m, onClick = { onSelectModel(m) }, label = { Text(m) })
                        }
                    }
                    TextButton(onClick = onOpenVoiceOs) {
                        FaIcon(FaGlyphs.MICROPHONE, contentDescription = null, size = 13.sp)
                        Spacer(Modifier.width(6.dp))
                        Text("Open Full Voice OS Settings & Macros")
                    }
                }
            }
        }

        // 6. Android Keystore API Key Manager
        item {
            NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NishaSectionHeader("6. Gemini API Key (Android Keystore)", "Encrypted AES-256-GCM on-device; never synced or logged", FaGlyphs.KEY)
                    NishaStatusPill(
                        label = if (hasApiKey) "Saved: $maskedApiKey" else "No API Key Configured",
                        iconGlyph = FaGlyphs.LOCK,
                        tint = if (hasApiKey) EmeraldSuccess else AmberWarn,
                        background = if (hasApiKey) EmeraldSoft else AmberSoft
                    )
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("Enter Gemini API Key (AIza...)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_api_key_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onSaveVerifyKey(apiKeyInput)
                                apiKeyInput = ""
                            },
                            enabled = apiKeyInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("settings_verify_key_btn")
                        ) { Text("Verify & Encrypt") }
                        OutlinedButton(
                            onClick = {
                                onSaveKeyWithoutVerify(apiKeyInput)
                                apiKeyInput = ""
                            },
                            enabled = apiKeyInput.isNotBlank(),
                            modifier = Modifier.height(48.dp)
                        ) { Text("Save") }
                        if (hasApiKey) {
                            OutlinedButton(onClick = onClearKey, modifier = Modifier.height(48.dp)) { Text("Clear") }
                        }
                    }
                    if (!apiKeyStatus.isNullOrBlank()) {
                        Text(apiKeyStatus, style = MaterialTheme.typography.bodyMedium, color = IceBlueDeep)
                    }
                }
            }
        }

        // 7. Floating 10D AI Orb Settings
        item {
            NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NishaSectionHeader("7. Floating 10D AI Orb", "Independent from Edge Lighting • Tap for Voice, Long-Press to Configure", FaGlyphs.CIRCLE_NODES)
                    SettingsSwitchRow("Show In-App Floating 10D Orb", settings.orbEnabled) {
                        onUpdateOrb(it, null, null, null)
                    }
                    SettingsSwitchRow("Compact Orb Mode", settings.orbCompactMode) {
                        onUpdateOrb(null, null, it, null)
                    }
                    Text("Orb Size (${settings.orbSizeDp.toInt()} dp)", style = MaterialTheme.typography.labelLarge, color = IndigoMuted)
                    Slider(
                        value = settings.orbSizeDp,
                        onValueChange = { onUpdateOrb(null, null, null, it) },
                        valueRange = 48f..88f
                    )
                    SettingsSwitchRow("System Overlay Outside App (Opt-In)", settings.orbSystemOverlayEnabled && canDrawOverlays) { enable ->
                        if (enable && !canDrawOverlays) {
                            onOpenOverlaySettings()
                        } else {
                            onUpdateOrb(null, enable, null, null)
                        }
                    }
                    if (!canDrawOverlays) {
                        OutlinedButton(onClick = onOpenOverlaySettings, modifier = Modifier.fillMaxWidth()) {
                            Text("Grant 'Display Over Other Apps' Permission")
                        }
                    }
                }
            }
        }

        // 8. Independent Four-Edge Lighting
        item {
            NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NishaSectionHeader("8. Four-Edge Lighting", "Independent animated glow around all 4 edges", FaGlyphs.SPARKLES)
                    SettingsSwitchRow("Enable Four-Edge Lighting", settings.edgeLightingEnabled) {
                        onUpdateEdgeLighting(it, null, null, null, null)
                    }
                    Text("Animation Style", style = MaterialTheme.typography.labelLarge, color = ObsidianSlate)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("Aurora Sweep", "Quantum Pulse", "Comet Orbit")) { st ->
                            FilterChip(
                                selected = settings.edgeLightingStyle == st,
                                onClick = { onUpdateEdgeLighting(null, st, null, null, null) },
                                label = { Text(st) }
                            )
                        }
                    }
                    Text("Color Palette", style = MaterialTheme.typography.labelLarge, color = ObsidianSlate)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("Ice & Violet", "Pearl Prism", "Royal Aurora", "Arctic Cyan")) { pal ->
                            FilterChip(
                                selected = settings.edgeLightingPalette == pal,
                                onClick = { onUpdateEdgeLighting(null, null, pal, null, null) },
                                label = { Text(pal) }
                            )
                        }
                    }
                    Text("Brightness (${(settings.edgeLightingBrightness * 100).toInt()}%)", style = MaterialTheme.typography.labelLarge, color = IndigoMuted)
                    Slider(
                        value = settings.edgeLightingBrightness,
                        onValueChange = { onUpdateEdgeLighting(null, null, null, it, null) },
                        valueRange = 0.2f..1.0f
                    )
                    Text("Speed (${String.format("%.1fx", settings.edgeLightingSpeed)})", style = MaterialTheme.typography.labelLarge, color = IndigoMuted)
                    Slider(
                        value = settings.edgeLightingSpeed,
                        onValueChange = { onUpdateEdgeLighting(null, null, null, null, it) },
                        valueRange = 0.5f..2.2f
                    )
                }
            }
        }

        // 9-14. Background Assistant Service, Agents, Privacy, Security, Storage, Backup
        item {
            NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NishaSectionHeader("9-14. Background Service, Privacy & Security", "Foreground service controls & encrypted vault health", FaGlyphs.SHIELD_HALVED)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Background Assistant Service", style = MaterialTheme.typography.titleMedium, color = ObsidianSlate)
                            Text(if (isServiceRunning) "Running with persistent notification" else "Stopped", style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                        }
                        Button(
                            onClick = {
                                if (isServiceRunning) {
                                    NishaAssistantService.stop(context)
                                } else {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        NishaAssistantService.start(context)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isServiceRunning) CrimsonAlert else ElectricViolet),
                            modifier = Modifier.testTag("settings_bg_service_btn")
                        ) { Text(if (isServiceRunning) "Stop" else "Start") }
                    }
                    SettingsSwitchRow("Knowledge Brain Memory Enabled", settings.memoryEnabled) { onToggleMemory(it) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onOpenAgents, modifier = Modifier.weight(1f)) { Text("Agents (Max 5)") }
                        OutlinedButton(onClick = onOpenBrain, modifier = Modifier.weight(1f)) { Text("Memory & Backup") }
                    }
                }
            }
        }

        // 15-16. Opt-in Accessibility & Permissions Dashboard
        item {
            NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NishaSectionHeader("15-16. Accessibility & Runtime Permissions", "Zero passive monitoring; request permissions only when needed", FaGlyphs.LOCK)
                    val accEnabled = NishaAccessibilityService.isEnabled()
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("On-Demand Screen Context Helper", style = MaterialTheme.typography.titleMedium, color = ObsidianSlate)
                            Text(if (accEnabled) "Enabled by user" else "Disabled (Opt-In)", style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                        }
                        OutlinedButton(onClick = { showAccessibilityGuide = true }) { Text("Configure") }
                    }
                    PermissionCheckRow(
                        label = "Microphone (Voice OS)",
                        granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    ) { audioLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                    PermissionCheckRow(
                        label = "Coarse Location (Weather)",
                        granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    ) { locLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }
                    PermissionCheckRow(
                        label = "Camera (Vision Scanner)",
                        granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    ) { camLauncher.launch(Manifest.permission.CAMERA) }
                }
            }
        }

        // 17-20. Subscriptions, App Updates, Admin Portal & About
        item {
            NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NishaSectionHeader("17-20. Subscriptions, App Updates & Admin", "Installed v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", FaGlyphs.CROWN)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onOpenSubscriptions,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) { Text("Plans & Billing") }
                        OutlinedButton(
                            onClick = onOpenAdminCenter,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("settings_open_admin_btn")
                        ) { Text("VIP 10D Admin") }
                    }
                    Surface(color = LavenderGlow, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Latest Published Release: v${appConfig.latestVersionName} (Build ${appConfig.latestVersionCode})", style = MaterialTheme.typography.titleMedium, color = ObsidianSlate)
                            if (appConfig.releaseNotes.isNotBlank()) {
                                Text(appConfig.releaseNotes, style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = {
                                    updateCheckMessage = if (appConfig.latestVersionCode > BuildConfig.VERSION_CODE) {
                                        "Update available: v${appConfig.latestVersionName}. Open official release page to download APK."
                                    } else {
                                        "You are running the latest version (v${BuildConfig.VERSION_NAME})."
                                    }
                                }) { Text("Check Updates") }
                                if (appConfig.latestVersionCode > BuildConfig.VERSION_CODE) {
                                    TextButton(onClick = { onCheckExternalReleaseUrl("https://github.com") }) {
                                        Text("Open Release Link")
                                    }
                                }
                            }
                            updateCheckMessage?.let {
                                Text(it, style = MaterialTheme.typography.bodyMedium, color = IceBlueDeep)
                            }
                        }
                    }
                    Text("NISHA AI — THE DARK AI • Luxury White Edition • Android Keystore + Firebase + Room", style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account Permanently?") },
            text = { Text("This deletes your active local vault / Firebase account records and signs you out. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert)
                ) { Text("Delete Permanently") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showAccessibilityGuide) {
        AlertDialog(
            onDismissRequest = { showAccessibilityGuide = false },
            title = { Text("Opt-In Accessibility Disclosure") },
            text = {
                Text("NISHA AI Screen Context Helper never logs passwords, keystrokes, or background activity. It reads visible text only when you explicitly tap 'Analyze Screen'. Proceed to Android Accessibility Settings?")
            },
            confirmButton = {
                Button(onClick = {
                    showAccessibilityGuide = false
                    onOpenAccessibilitySettings()
                }) { Text("Open Accessibility Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showAccessibilityGuide = false }) { Text("Cancel") }
            }
        )
    }

    if (showEditProfile && session is NishaSession.LocalVault) {
        var name by remember { mutableStateOf(session.fullName) }
        var phone by remember { mutableStateOf(session.phone) }
        AlertDialog(
            onDismissRequest = { showEditProfile = false },
            title = { Text("Edit Local Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, singleLine = true)
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("WhatsApp / Phone") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateLocalProfile(session.accountId, name, phone)
                    showEditProfile = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfile = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SubscriptionsScreen(
    currentTier: String,
    isCloudSignedIn: Boolean,
    onBack: () -> Unit
) {
    var billingNote by remember { mutableStateOf<String?>(null) }
    val tiers = listOf(
        Triple("Free", "Active Default • BYOK Gemini", listOf("Luxury White 3D Core", "BYOK Gemini Chat & Voice OS", "5 Configurable Agents", "Local Room + Basic Tools")),
        Triple("Pro", "Play Billing SKU: nisha_pro_monthly", listOf("Cloud Sync & Unlimited History", "Priority Multi-Agent Templates", "Advanced Studio Exporters", "Requires Google Play Console SKU")),
        Triple("Ultra", "Play Billing SKU: nisha_ultra_monthly", listOf("Everything in Pro", "VIP Multi-Device Cloud Knowledge", "Priority Admin / Team Licenses", "Requires Play Console Merchant Setup"))
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 720.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 124.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) {
                    FaIcon(FaGlyphs.ARROW_LEFT, contentDescription = "Back", size = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("Back")
                }
                Text("Subscriptions & Plans", style = MaterialTheme.typography.headlineMedium, color = ObsidianSlate)
            }
            Spacer(Modifier.height(6.dp))
            NishaStatusPill("Current Active Plan: ${currentTier.uppercase()}", FaGlyphs.CROWN, tint = ElectricViolet, background = LavenderGlow)
        }

        items(tiers) { (name, sub, perks) ->
            val active = currentTier.equals(name, ignoreCase = true)
            NishaGlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (active) ElectricViolet else FrostBorder
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FaIcon(FaGlyphs.GEM, contentDescription = null, color = ElectricViolet, size = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("NISHA $name", style = MaterialTheme.typography.titleLarge, color = ObsidianSlate)
                            Text(sub, style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                        }
                        if (active) {
                            NishaStatusPill("Active", FaGlyphs.CHECK, tint = EmeraldSuccess, background = EmeraldSoft)
                        }
                    }
                    perks.forEach { perk ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FaIcon(FaGlyphs.CIRCLE_CHECK, contentDescription = null, color = IceBlueDeep, size = 12.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(perk, style = MaterialTheme.typography.bodyMedium, color = ObsidianSlate)
                        }
                    }
                    if (!active) {
                        OutlinedButton(
                            onClick = {
                                billingNote = if (!isCloudSignedIn) {
                                    "Sign in with Google first, and publish SKU '$sub' in Google Play Console to activate real in-app purchases."
                                } else {
                                    "Google Play Billing requires linking this signed APK package (${BuildConfig.APPLICATION_ID}) to Play Console subscriptions ($sub). Admin can also assign verified licenses in VIP 10D Admin."
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) { Text("View Play Billing Setup for $name") }
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    billingNote = "Restore Purchases queries Google Play BillingClient.queryPurchasesAsync() once Play Console SKUs are configured."
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("Restore Purchases Info") }
        }

        billingNote?.let { note ->
            item {
                Surface(color = AmberSoft, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(note, style = MaterialTheme.typography.bodyMedium, color = ObsidianSlate, modifier = Modifier.padding(14.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = ObsidianSlate, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun PermissionCheckRow(label: String, granted: Boolean, onGrant: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, FrostBorder),
        color = PearlWhite,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium, color = ObsidianSlate)
                Text(if (granted) "Granted" else "Not granted yet", style = MaterialTheme.typography.bodyMedium, color = if (granted) EmeraldSuccess else IndigoMuted)
            }
            if (!granted) {
                TextButton(onClick = onGrant) { Text("Allow") }
            } else {
                FaIcon(FaGlyphs.CIRCLE_CHECK, contentDescription = "Granted", color = EmeraldSuccess, size = 16.sp)
            }
        }
    }
}
