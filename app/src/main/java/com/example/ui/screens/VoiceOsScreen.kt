package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.VoiceHistoryEntity
import com.example.data.local.VoiceMacroEntity
import com.example.data.preferences.NishaSettingsState
import com.example.domain.AiCoreState
import com.example.domain.DeviceActionRouter
import com.example.domain.PendingSensitiveAction
import com.example.service.NishaAccessibilityService
import com.example.service.NishaAssistantService
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.FaGlyphs
import com.example.ui.theme.FaIcon
import com.example.ui.theme.IceBlueDeep
import com.example.ui.theme.IndigoMuted
import com.example.ui.theme.LavenderGlow
import com.example.ui.theme.NishaGlassCard
import com.example.ui.theme.NishaSectionHeader
import com.example.ui.theme.NishaStatusPill
import com.example.ui.theme.ObsidianSlate
import com.example.ui.theme.PearlWhite

@Composable
fun VoiceOsScreen(
    coreState: AiCoreState,
    serviceRunning: Boolean,
    settings: NishaSettingsState,
    lastResponse: String,
    voiceHistory: List<VoiceHistoryEntity>,
    voiceMacros: List<VoiceMacroEntity>,
    deviceActionRouter: DeviceActionRouter,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onStopSpeaking: () -> Unit,
    onSubmitTextVoiceCommand: (String) -> Unit,
    onUpdateVoiceSettings: (String?, Float?, Float?, Boolean?, Boolean?) -> Unit,
    onAddMacro: (String, String, String) -> Unit,
    onDeleteMacro: (String) -> Unit,
    onProposeAction: (PendingSensitiveAction) -> Unit
) {
    val context = LocalContext.current
    var commandInput by remember { mutableStateOf("") }
    var macroPhrase by remember { mutableStateOf("") }
    var macroPayload by remember { mutableStateOf("") }
    var macroType by remember { mutableStateOf("open_app") }
    var showAppPicker by remember { mutableStateOf(false) }
    var showMicRationale by remember { mutableStateOf(false) }
    var screenAnalysisNote by remember { mutableStateOf<String?>(null) }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onStartListening()
    }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        NishaAssistantService.start(context)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 680.dp),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            NishaSectionHeader(
                title = "Nisha Voice OS",
                subtitle = "Hindi • English • Hinglish multilingual voice intelligence",
                iconGlyph = FaGlyphs.MICROPHONE,
                trailing = {
                    NishaStatusPill(
                        label = coreState.label,
                        iconGlyph = FaGlyphs.ATOM,
                        tint = ElectricViolet,
                        background = LavenderGlow
                    )
                }
            )
        }

        // Voice Activation & Language Card
        item {
            NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Voice Language", style = MaterialTheme.typography.titleMedium, color = ObsidianSlate)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("en-IN" to "English", "hi-IN" to "Hindi", "hinglish" to "Hinglish").forEach { (code, label) ->
                            FilterChip(
                                selected = settings.voiceLanguage == code,
                                onClick = { onUpdateVoiceSettings(code, null, null, null, null) },
                                label = { Text(label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                if (coreState == AiCoreState.LISTENING) {
                                    onStopListening()
                                } else {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) onStartListening() else showMicRationale = true
                                }
                            },
                            modifier = Modifier.weight(1f).height(50.dp).testTag("voice_mic_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            FaIcon(
                                glyph = if (coreState == AiCoreState.LISTENING) FaGlyphs.STOP else FaGlyphs.MICROPHONE,
                                contentDescription = null,
                                color = PearlWhite,
                                size = 15.sp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (coreState == AiCoreState.LISTENING) "Stop Mic" else "Speak to Nisha")
                        }
                        if (coreState == AiCoreState.SPEAKING) {
                            OutlinedButton(
                                onClick = onStopSpeaking,
                                modifier = Modifier.height(50.dp)
                            ) {
                                FaIcon(FaGlyphs.VOLUME_HIGH, contentDescription = null, size = 14.sp)
                                Spacer(Modifier.width(6.dp))
                                Text("Mute")
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = commandInput,
                        onValueChange = { commandInput = it },
                        label = { Text("Type or test voice command (e.g. 'set timer 5', 'battery', 'open chrome')") },
                        modifier = Modifier.fillMaxWidth().testTag("voice_text_command_input"),
                        trailingIcon = {
                            IconButton(onClick = {
                                if (commandInput.isNotBlank()) {
                                    onSubmitTextVoiceCommand(commandInput)
                                    commandInput = ""
                                }
                            }) {
                                FaIcon(FaGlyphs.PAPER_PLANE, contentDescription = "Run command", color = ElectricViolet, size = 16.sp)
                            }
                        }
                    )

                    if (lastResponse.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        NishaGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Nisha Response", style = MaterialTheme.typography.labelLarge, color = ElectricViolet)
                                Spacer(Modifier.height(4.dp))
                                Text(lastResponse, style = MaterialTheme.typography.bodyLarge, color = ObsidianSlate)
                            }
                        }
                    }
                }
            }
        }

        // Speech Controls & Wake-Phrase Notice
        item {
            NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Speech & Wake-Phrase Controls", style = MaterialTheme.typography.titleMedium, color = ObsidianSlate)
                    Spacer(Modifier.height(8.dp))
                    Text("Speech Speed: ${"%.1f".format(settings.voiceSpeechRate)}x", style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                    Slider(
                        value = settings.voiceSpeechRate,
                        onValueChange = { onUpdateVoiceSettings(null, it, null, null, null) },
                        valueRange = 0.6f..1.8f
                    )
                    Text("Voice Pitch: ${"%.1f".format(settings.voicePitch)}x", style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                    Slider(
                        value = settings.voicePitch,
                        onValueChange = { onUpdateVoiceSettings(null, null, it, null, null) },
                        valueRange = 0.7f..1.4f
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Spoken TTS Replies", style = MaterialTheme.typography.bodyLarge, color = ObsidianSlate)
                            Text("Read responses aloud after voice queries", style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                        }
                        Switch(
                            checked = settings.voiceAutoSpeak,
                            onCheckedChange = { onUpdateVoiceSettings(null, null, null, it, null) }
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Wake Phrase ('Hey Nisha' / 'Hello Nisha')", style = MaterialTheme.typography.bodyLarge, color = ObsidianSlate)
                            Text("Active only while Voice OS or Foreground Service is running; Android restricts background mic after Force Stop.", style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                        }
                        Switch(
                            checked = settings.wakePhraseEnabled,
                            onCheckedChange = { onUpdateVoiceSettings(null, null, null, null, it) }
                        )
                    }
                }
            }
        }

        // Background Assistant Foreground Service
        item {
            NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FaIcon(FaGlyphs.SATELLITE_DISH, contentDescription = null, color = IceBlueDeep, size = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Background Assistant Service", style = MaterialTheme.typography.titleMedium, color = ObsidianSlate, modifier = Modifier.weight(1f))
                        NishaStatusPill(
                            label = if (serviceRunning) "Running" else "Stopped",
                            iconGlyph = if (serviceRunning) FaGlyphs.CIRCLE_CHECK else FaGlyphs.PAUSE,
                            tint = if (serviceRunning) EmeraldSuccess else IndigoMuted,
                            background = if (serviceRunning) EmeraldSoft else LavenderGlow
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Runs an Android Foreground Service with persistent notification controls for Voice and Stop. Battery-conscious; cannot persist if force-stopped by the user or OS.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IndigoMuted
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    NishaAssistantService.start(context)
                                }
                            },
                            enabled = !serviceRunning,
                            modifier = Modifier.weight(1f).testTag("start_fgs_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = IceBlueDeep)
                        ) { Text("Start Service") }
                        OutlinedButton(
                            onClick = { NishaAssistantService.stop(context) },
                            enabled = serviceRunning,
                            modifier = Modifier.weight(1f).testTag("stop_fgs_button")
                        ) { Text("Stop Service") }
                    }
                }
            }
        }

        // Device Actions & Accessibility Helper
        item {
            NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Confirmed Device Actions", style = MaterialTheme.typography.titleMedium, color = ObsidianSlate)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { showAppPicker = true }, modifier = Modifier.weight(1f)) {
                            Text("Open App")
                        }
                        OutlinedButton(
                            onClick = {
                                onProposeAction(
                                    PendingSensitiveAction("Confirm 5-Min Timer", "Launch Clock app to set a 5-minute timer.", "TIMER", "5", "NISHA Timer")
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Set Timer") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                onProposeAction(
                                    PendingSensitiveAction("Open System Settings", "Open Android Settings safely.", "SETTINGS", android.provider.Settings.ACTION_SETTINGS, "")
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Settings") }
                        OutlinedButton(
                            onClick = {
                                if (NishaAccessibilityService.isEnabled()) {
                                    val captured = NishaAccessibilityService.captureScreenOnDemand()
                                    screenAnalysisNote = if (captured.isNullOrBlank()) {
                                        "No visible text found on the active window."
                                    } else {
                                        "Captured ${captured.length} chars on-demand. Sending to Voice OS..."
                                    }
                                    if (!captured.isNullOrBlank()) {
                                        onSubmitTextVoiceCommand("Summarize this screen content:\n$captured")
                                    }
                                } else {
                                    deviceActionRouter.openAccessibilitySettings()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (NishaAccessibilityService.isEnabled()) "Analyze Screen" else "Enable Accessibility")
                        }
                    }
                    if (!screenAnalysisNote.isNullOrBlank()) {
                        Text(screenAnalysisNote!!, style = MaterialTheme.typography.bodyMedium, color = ElectricViolet)
                    }
                }
            }
        }

        // User-Defined Macros
        item {
            NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Voice Commands & Macros", style = MaterialTheme.typography.titleMedium, color = ObsidianSlate)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("open_app" to "Open App", "whatsapp" to "WhatsApp Draft").forEach { (type, lbl) ->
                            FilterChip(selected = macroType == type, onClick = { macroType = type }, label = { Text(lbl) })
                        }
                    }
                    OutlinedTextField(
                        value = macroPhrase,
                        onValueChange = { macroPhrase = it },
                        label = { Text("Trigger phrase (e.g. 'launch notes')") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = macroPayload,
                        onValueChange = { macroPayload = it },
                        label = { Text(if (macroType == "open_app") "App name (e.g. Chrome)" else "Draft message") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            onAddMacro(macroPhrase, macroType, macroPayload)
                            macroPhrase = ""
                            macroPayload = ""
                        },
                        enabled = macroPhrase.isNotBlank() && macroPayload.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                    ) { Text("Save Macro") }

                    voiceMacros.forEach { macro ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("\"${macro.triggerPhrase}\"", style = MaterialTheme.typography.titleMedium, color = ObsidianSlate)
                                Text(macro.actionSummary, style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                            }
                            IconButton(onClick = { onDeleteMacro(macro.id) }) {
                                FaIcon(FaGlyphs.TRASH, contentDescription = "Delete macro", color = IndigoMuted, size = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // Voice History
        if (voiceHistory.isNotEmpty()) {
            item {
                NishaSectionHeader(title = "Voice History", iconGlyph = FaGlyphs.CLOCK)
            }
            items(voiceHistory.take(8)) { item ->
                NishaGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("You (${item.language}): ${item.transcript}", style = MaterialTheme.typography.titleMedium, color = ObsidianSlate)
                        Spacer(Modifier.height(4.dp))
                        Text(item.response, style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                    }
                }
            }
        }
    }

    if (showMicRationale) {
        AlertDialog(
            onDismissRequest = { showMicRationale = false },
            title = { Text("Microphone Permission Needed") },
            text = { Text("Nisha Voice OS needs microphone access to transcribe your spoken English, Hindi, or Hinglish commands. Audio is never secretly recorded.") },
            confirmButton = {
                Button(onClick = {
                    showMicRationale = false
                    micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }) { Text("Allow Microphone") }
            },
            dismissButton = {
                TextButton(onClick = { showMicRationale = false }) { Text("Cancel") }
            }
        )
    }

    if (showAppPicker) {
        val apps = remember { deviceActionRouter.listInstalledLauncherApps().take(25) }
        AlertDialog(
            onDismissRequest = { showAppPicker = false },
            title = { Text("Open Installed App") },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(apps) { (label, _) ->
                        TextButton(
                            onClick = {
                                showAppPicker = false
                                deviceActionRouter.openInstalledApp(label)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppPicker = false }) { Text("Close") }
            }
        )
    }
}
