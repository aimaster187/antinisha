package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.local.AutomationRuleEntity
import com.example.data.local.JournalEntryEntity
import com.example.data.local.MemoryFactEntity
import com.example.data.local.NoteItemEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.WatchlistItemEntity
import com.example.data.remote.GeminiAttachment
import com.example.domain.PendingSensitiveAction
import com.example.ui.theme.AmberSoft
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.FaGlyphs
import com.example.ui.theme.FaIcon
import com.example.ui.theme.FaStyle
import com.example.ui.theme.IceBlueDeep
import com.example.ui.theme.IceBlueSoft
import com.example.ui.theme.IndigoMuted
import com.example.ui.theme.LavenderGlow
import com.example.ui.theme.NishaGlassCard
import com.example.ui.theme.NishaSectionHeader
import com.example.ui.theme.NishaStatusPill
import com.example.ui.theme.ObsidianSlate
import com.example.ui.theme.PearlWhite
import java.io.File

@Composable
fun MemoryBrainScreen(
    memories: List<MemoryFactEntity>,
    memoryEnabled: Boolean,
    isCloudSignedIn: Boolean,
    onToggleMemory: (Boolean) -> Unit,
    onSaveMemory: (String, String, String, String) -> Unit,
    onDeleteMemory: (MemoryFactEntity) -> Unit,
    onExportJson: () -> String,
    onImportJson: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var search by remember { mutableStateOf("") }
    var scopeFilter by remember { mutableStateOf("ALL") }
    var category by remember { mutableStateOf("Preference") }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var saveScope by remember { mutableStateOf("LOCAL") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf<String?>(null) }

    val filtered = remember(memories, search, scopeFilter) {
        memories.filter { m ->
            (scopeFilter == "ALL" || m.scope == scopeFilter) &&
                (search.isBlank() || m.title.contains(search, true) || m.content.contains(search, true) || m.category.contains(search, true))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 720.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 124.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Knowledge Brain", style = MaterialTheme.typography.headlineMedium, color = ObsidianSlate)
                    Text("User-controlled facts separated between Local & Cloud", style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                }
                Switch(checked = memoryEnabled, onCheckedChange = onToggleMemory, modifier = Modifier.testTag("brain_enable_switch"))
            }
        }

        if (!feedback.isNullOrBlank()) {
            item {
                NishaStatusPill(feedback!!, FaGlyphs.CIRCLE_CHECK, tint = EmeraldSuccess, background = EmeraldSoft)
            }
        }

        item {
            NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NishaSectionHeader("Add Memory Fact", "Stored transparently and injected only when Memory is enabled", FaGlyphs.BRAIN)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Preference", "Project", "Person", "Rule").forEach { cat ->
                            FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                        }
                    }
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Memory Detail") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(selected = saveScope == "LOCAL", onClick = { saveScope = "LOCAL" }, label = { Text("Local Room") })
                        FilterChip(
                            selected = saveScope == "CLOUD",
                            enabled = isCloudSignedIn,
                            onClick = { saveScope = "CLOUD" },
                            label = { Text(if (isCloudSignedIn) "Cloud Sync" else "Cloud (Sign in)") }
                        )
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = {
                                onSaveMemory(category, title, content, saveScope)
                                title = ""
                                content = ""
                            },
                            enabled = title.isNotBlank() && content.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            modifier = Modifier.testTag("brain_save_button")
                        ) { Text("Save") }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search memory...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = {
                    clipboard.setText(AnnotatedString(onExportJson()))
                    feedback = "Exported ${memories.size} memories as JSON to clipboard"
                }) { Text("Export") }
                OutlinedButton(onClick = { showImportDialog = true }) { Text("Import") }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ALL", "LOCAL", "CLOUD").forEach { sc ->
                    FilterChip(selected = scopeFilter == sc, onClick = { scopeFilter = sc }, label = { Text(sc) })
                }
            }
        }

        items(filtered, key = { it.id }) { mem ->
            NishaGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            NishaStatusPill(mem.category, FaGlyphs.BRAIN)
                            NishaStatusPill(mem.scope, FaGlyphs.DATABASE, tint = IceBlueDeep, background = IceBlueSoft)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(mem.title, style = MaterialTheme.typography.titleMedium, color = ObsidianSlate)
                        Text(mem.content, style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                    }
                    IconButton(onClick = { onDeleteMemory(mem) }, modifier = Modifier.size(48.dp)) {
                        FaIcon(FaGlyphs.TRASH, contentDescription = "Delete memory", color = CrimsonAlert, size = 15.sp)
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Memory JSON") },
            text = {
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    label = { Text("Paste JSON Array") },
                    minLines = 4
                )
            },
            confirmButton = {
                Button(onClick = {
                    onImportJson(importText)
                    showImportDialog = false
                }) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun CommunicationScreen(
    aiResult: String,
    loading: Boolean,
    onRunAiDraft: (String, String) -> Unit,
    onProposeSensitiveAction: (PendingSensitiveAction) -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf("") }
    var contextNote by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 720.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 124.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("WhatsApp AI & Communication", style = MaterialTheme.typography.headlineMedium, color = ObsidianSlate)
            Text("AI drafting & official Android intent launchers with confirmation", style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
        }

        item {
            NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NishaSectionHeader("Compose & AI Assist", "No messages are ever sent without opening your chosen app", FaGlyphs.BRAND_WHATSAPP)
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone / WhatsApp Number (optional for share)") },
                        placeholder = { Text("+919876543210") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("comm_phone_input")
                    )
                    OutlinedTextField(
                        value = contextNote,
                        onValueChange = { contextNote = it },
                        label = { Text("Prompt or Paste Received Message") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                onRunAiDraft(
                                    "Draft a clear, warm message ready to send via WhatsApp or SMS.",
                                    "Draft a message about: $contextNote"
                                )
                            },
                            enabled = !loading && contextNote.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) { Text("AI Draft") }
                        OutlinedButton(
                            onClick = {
                                onRunAiDraft(
                                    "Rewrite the user's draft to sound polished, concise, and polite.",
                                    "Rewrite politely: ${draft.ifBlank { contextNote }}"
                                )
                            },
                            enabled = !loading && (draft.isNotBlank() || contextNote.isNotBlank()),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) { Text("Rewrite") }
                        OutlinedButton(
                            onClick = {
                                onRunAiDraft(
                                    "Summarize the pasted conversation clearly with bullet points and action items.",
                                    "Summarize conversation:\n$contextNote"
                                )
                            },
                            enabled = !loading && contextNote.isNotBlank(),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) { Text("Summarize") }
                    }
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ElectricViolet)
                    }
                    if (aiResult.isNotBlank()) {
                        Surface(color = LavenderGlow, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(aiResult, style = MaterialTheme.typography.bodyMedium, color = ObsidianSlate)
                                TextButton(onClick = { draft = aiResult }) { Text("Use AI Text in Draft") }
                            }
                        }
                    }

                    // Templates
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val templates = listOf(
                            "Running 10 mins late, starting soon!" to "Late ETA",
                            "Please share the updated files when free." to "File Request",
                            "Confirmed for our meeting today at 4 PM." to "Confirm Meeting"
                        )
                        items(templates) { (body, label) ->
                            FilterChip(selected = false, onClick = { draft = body }, label = { Text(label) })
                        }
                    }

                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        label = { Text("Final Message Text") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("comm_draft_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                onProposeSensitiveAction(
                                    PendingSensitiveAction(
                                        title = "Open WhatsApp Composer",
                                        description = "Launch WhatsApp with your drafted message. You review and tap Send inside WhatsApp.",
                                        actionType = "WHATSAPP",
                                        target = phone,
                                        payload = draft
                                    )
                                )
                            },
                            enabled = draft.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("comm_whatsapp_btn")
                        ) {
                            FaIcon(FaGlyphs.BRAND_WHATSAPP, contentDescription = null, style = FaStyle.BRANDS, color = PearlWhite, size = 15.sp)
                            Spacer(Modifier.width(6.dp))
                            Text("WhatsApp")
                        }
                        OutlinedButton(
                            onClick = {
                                onProposeSensitiveAction(
                                    PendingSensitiveAction(
                                        title = "Open SMS Composer",
                                        description = "Open Android Messages composer with your drafted SMS.",
                                        actionType = "SMS",
                                        target = phone,
                                        payload = draft
                                    )
                                )
                            },
                            enabled = draft.isNotBlank(),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("comm_sms_btn")
                        ) {
                            FaIcon(FaGlyphs.COMMENT_SMS, contentDescription = null, size = 14.sp)
                            Spacer(Modifier.width(6.dp))
                            Text("SMS")
                        }
                        OutlinedButton(
                            onClick = {
                                onProposeSensitiveAction(
                                    PendingSensitiveAction(
                                        title = "Open Phone Dialer",
                                        description = "Prepare Android Dialer for '$phone'.",
                                        actionType = "DIAL",
                                        target = phone,
                                        payload = ""
                                    )
                                )
                            },
                            enabled = phone.isNotBlank(),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("comm_dial_btn")
                        ) {
                            FaIcon(FaGlyphs.PHONE, contentDescription = null, size = 14.sp)
                            Spacer(Modifier.width(6.dp))
                            Text("Call")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToolsHubScreen(
    journalEntries: List<JournalEntryEntity>,
    notes: List<NoteItemEntity>,
    reminders: List<ReminderEntity>,
    watchlist: List<WatchlistItemEntity>,
    automations: List<AutomationRuleEntity>,
    aiResult: String,
    loading: Boolean,
    onRunAiTool: (String, String, GeminiAttachment?) -> Unit,
    onAddJournal: (String, String, String, Int) -> Unit,
    onDeleteJournal: (String) -> Unit,
    onAddNote: (String, String, String) -> Unit,
    onDeleteNote: (String) -> Unit,
    onAddReminder: (String, String) -> Unit,
    onToggleReminder: (ReminderEntity) -> Unit,
    onDeleteReminder: (String) -> Unit,
    onSetTimerAction: (PendingSensitiveAction) -> Unit,
    onAddWatchlist: (String, String, String, String) -> Unit,
    onDeleteWatchlist: (String) -> Unit,
    onAddAutomation: (String, String, String, String) -> Unit,
    onToggleAutomation: (AutomationRuleEntity) -> Unit,
    onDeleteAutomation: (String) -> Unit,
    onExportBackupJson: () -> String,
    onImportBackupJson: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var activeModule by remember { mutableStateOf("Vision & OCR") }
    val modules = listOf(
        "Vision & OCR",
        "Docs & PDF",
        "Journal",
        "Notes",
        "Planner & Timer",
        "Study & Marketing",
        "Watchlist",
        "Automations",
        "Media & Backup"
    )

    var cameraOutputUri by remember { mutableStateOf<Uri?>(null) }
    var selectedAttachment by remember { mutableStateOf<GeminiAttachment?>(null) }
    var visionPrompt by remember { mutableStateOf("Describe this image and extract any visible text cleanly.") }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved && cameraOutputUri != null) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(cameraOutputUri!!)?.use { it.readBytes() }
                if (bytes != null) {
                    selectedAttachment = GeminiAttachment("Camera Capture", "image/jpeg", Base64.encodeToString(bytes, Base64.NO_WRAP))
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val tmp = File(context.cacheDir, "nisha_cam_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tmp)
            cameraOutputUri = uri
            cameraLauncher.launch(uri)
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null && bytes.size <= 4_500_000) {
                    val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                    selectedAttachment = GeminiAttachment("Selected Photo", mime, Base64.encodeToString(bytes, Base64.NO_WRAP))
                }
            }
        }
    }

    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null && bytes.size <= 4_500_000) {
                    val mime = context.contentResolver.getType(uri) ?: "application/pdf"
                    selectedAttachment = GeminiAttachment(
                        uri.lastPathSegment?.substringAfterLast('/') ?: "Document",
                        mime,
                        Base64.encodeToString(bytes, Base64.NO_WRAP)
                    )
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 720.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 124.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("AI Productivity & Tools Hub", style = MaterialTheme.typography.headlineMedium, color = ObsidianSlate)
            Text("Vision, OCR, Docs, Mood Journal, Notes, Planner, Watchlist & Rules", style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(modules) { mod ->
                    FilterChip(
                        selected = activeModule == mod,
                        onClick = { activeModule = mod },
                        label = { Text(mod) },
                        modifier = Modifier.testTag("tools_tab_$mod")
                    )
                }
            }
        }

        when (activeModule) {
            "Vision & OCR" -> {
                item {
                    NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            NishaSectionHeader("Vision AI & OCR Scanner", "Use Android Photo Picker or Camera to extract text & insights", FaGlyphs.CAMERA)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    FaIcon(FaGlyphs.IMAGE, contentDescription = null, color = PearlWhite, size = 14.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Photo Picker")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                        if (granted) {
                                            val tmp = File(context.cacheDir, "nisha_cam_${System.currentTimeMillis()}.jpg")
                                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tmp)
                                            cameraOutputUri = uri
                                            cameraLauncher.launch(uri)
                                        } else {
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    FaIcon(FaGlyphs.CAMERA, contentDescription = null, size = 14.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Camera")
                                }
                            }
                            selectedAttachment?.let {
                                NishaStatusPill("Ready: ${it.label}", FaGlyphs.CIRCLE_CHECK, tint = EmeraldSuccess, background = EmeraldSoft)
                            }
                            OutlinedTextField(value = visionPrompt, onValueChange = { visionPrompt = it }, label = { Text("Vision / OCR Instruction") }, modifier = Modifier.fillMaxWidth())
                            Button(
                                onClick = {
                                    onRunAiTool("You are NISHA Vision & OCR AI. Extract text accurately and explain visual details clearly.", visionPrompt, selectedAttachment)
                                },
                                enabled = !loading && selectedAttachment != null,
                                colors = ButtonDefaults.buttonColors(containerColor = IceBlueDeep),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) { Text("Analyze Image / OCR") }
                        }
                    }
                }
            }

            "Docs & PDF" -> {
                item {
                    var docQuestion by remember { mutableStateOf("Summarize key points, action items, and dates from this document.") }
                    NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            NishaSectionHeader("Document Reader & PDF Summarizer", "Zero-broad-storage SAF Document Picker", FaGlyphs.FILE_LINES)
                            OutlinedButton(
                                onClick = { docPicker.launch(arrayOf("application/pdf", "text/plain", "text/*")) },
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                FaIcon(FaGlyphs.FOLDER_OPEN, contentDescription = null, size = 14.sp)
                                Spacer(Modifier.width(8.dp))
                                Text("Select PDF or Text Document")
                            }
                            selectedAttachment?.let {
                                NishaStatusPill("Loaded: ${it.label}", FaGlyphs.FILE_LINES, tint = IceBlueDeep, background = IceBlueSoft)
                            }
                            OutlinedTextField(value = docQuestion, onValueChange = { docQuestion = it }, label = { Text("Question / Summary Goal") }, modifier = Modifier.fillMaxWidth())
                            Button(
                                onClick = {
                                    onRunAiTool("You are NISHA Document Summarizer. Provide structured key findings cleanly.", docQuestion, selectedAttachment)
                                },
                                enabled = !loading && selectedAttachment != null,
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) { Text("Summarize Document") }
                        }
                    }
                }
            }

            "Journal" -> {
                item {
                    var jTitle by remember { mutableStateOf("") }
                    var jBody by remember { mutableStateOf("") }
                    var mood by remember { mutableStateOf("Focused") }
                    var score by remember { mutableFloatStateOf(4f) }
                    NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            NishaSectionHeader("Journal & Mood Tracker", "Track thoughts & energy with optional AI reflection", FaGlyphs.BOOK_OPEN)
                            OutlinedTextField(value = jTitle, onValueChange = { jTitle = it }, label = { Text("Entry Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = jBody, onValueChange = { jBody = it }, label = { Text("How is your day going?") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(listOf("Focused", "Calm", "Inspired", "Tired", "Stressed")) { m ->
                                    FilterChip(selected = mood == m, onClick = { mood = m }, label = { Text(m) })
                                }
                            }
                            Text("Mood Energy: ${score.toInt()}/5", style = MaterialTheme.typography.labelLarge, color = IndigoMuted)
                            Slider(value = score, onValueChange = { score = it }, valueRange = 1f..5f, steps = 3)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        onAddJournal(jTitle, jBody, mood, score.toInt())
                                        jTitle = ""
                                        jBody = ""
                                    },
                                    enabled = jTitle.isNotBlank() && jBody.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) { Text("Save Entry") }
                                OutlinedButton(
                                    onClick = {
                                        onRunAiTool("You are an empathetic Personal AI reflection coach.", "Reflect kindly on this mood ($mood, ${score.toInt()}/5): $jTitle - $jBody", null)
                                    },
                                    enabled = jBody.isNotBlank() && !loading,
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) { Text("AI Reflect") }
                            }
                        }
                    }
                }
                items(journalEntries, key = { it.id }) { entry ->
                    NishaGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                NishaStatusPill("${entry.moodLabel} (${entry.moodScore}/5)", FaGlyphs.HEART_PULSE)
                                Spacer(Modifier.height(4.dp))
                                Text(entry.title, style = MaterialTheme.typography.titleMedium, color = ObsidianSlate)
                                Text(entry.content, style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                            }
                            IconButton(onClick = { onDeleteJournal(entry.id) }, modifier = Modifier.size(48.dp)) {
                                FaIcon(FaGlyphs.TRASH, contentDescription = "Delete journal entry", color = CrimsonAlert, size = 14.sp)
                            }
                        }
                    }
                }
            }

            "Notes" -> {
                item {
                    var nTitle by remember { mutableStateOf("") }
                    var nBody by remember { mutableStateOf("") }
                    var nTag by remember { mutableStateOf("General") }
                    NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            NishaSectionHeader("Smart Notes", "Fast Room-backed notes", FaGlyphs.NOTE_STICKY)
                            OutlinedTextField(value = nTitle, onValueChange = { nTitle = it }, label = { Text("Note Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = nTag, onValueChange = { nTag = it }, label = { Text("Tag") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = nBody, onValueChange = { nBody = it }, label = { Text("Body") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                            Button(
                                onClick = {
                                    onAddNote(nTitle, nBody, nTag)
                                    nTitle = ""
                                    nBody = ""
                                },
                                enabled = nTitle.isNotBlank() && nBody.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) { Text("Save Note") }
                        }
                    }
                }
                items(notes, key = { it.id }) { note ->
                    NishaGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                NishaStatusPill(note.tag, FaGlyphs.NOTE_STICKY)
                                Spacer(Modifier.height(4.dp))
                                Text(note.title, style = MaterialTheme.typography.titleMedium, color = ObsidianSlate)
                                Text(note.body, style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                            }
                            IconButton(onClick = { onDeleteNote(note.id) }, modifier = Modifier.size(48.dp)) {
                                FaIcon(FaGlyphs.TRASH, contentDescription = "Delete note", color = CrimsonAlert, size = 14.sp)
                            }
                        }
                    }
                }
            }

            "Planner & Timer" -> {
                item {
                    var rTitle by remember { mutableStateOf("") }
                    var rSched by remember { mutableStateOf("Today 6:00 PM") }
                    var timerMin by remember { mutableStateOf("10") }
                    NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            NishaSectionHeader("Planner, Reminders & Clock Timer", "Manage tasks or set an Android Clock timer", FaGlyphs.CALENDAR_CHECK)
                            OutlinedTextField(value = rTitle, onValueChange = { rTitle = it }, label = { Text("Task / Reminder") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = rSched, onValueChange = { rSched = it }, label = { Text("Time / Schedule") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Button(
                                onClick = {
                                    onAddReminder(rTitle, rSched)
                                    rTitle = ""
                                },
                                enabled = rTitle.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) { Text("Add Reminder") }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(value = timerMin, onValueChange = { timerMin = it }, label = { Text("Minutes") }, singleLine = true, modifier = Modifier.weight(1f))
                                OutlinedButton(
                                    onClick = {
                                        onSetTimerAction(
                                            PendingSensitiveAction(
                                                title = "Set Android Clock Timer",
                                                description = "Launch Android Clock timer for $timerMin minutes.",
                                                actionType = "TIMER",
                                                target = timerMin,
                                                payload = "NISHA Focus Timer"
                                            )
                                        )
                                    },
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    FaIcon(FaGlyphs.HOURGLASS_HALF, contentDescription = null, size = 14.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Start Timer")
                                }
                            }
                        }
                    }
                }
                items(reminders, key = { it.id }) { rem ->
                    NishaGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = rem.isDone, onCheckedChange = { onToggleReminder(rem) })
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rem.title, style = MaterialTheme.typography.titleMedium, color = ObsidianSlate)
                                Text(rem.scheduleText, style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                            }
                            IconButton(onClick = { onDeleteReminder(rem.id) }, modifier = Modifier.size(48.dp)) {
                                FaIcon(FaGlyphs.TRASH, contentDescription = "Delete reminder", color = CrimsonAlert, size = 14.sp)
                            }
                        }
                    }
                }
            }

            "Study & Marketing" -> {
                item {
                    var topic by remember { mutableStateOf("") }
                    NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            NishaSectionHeader("Study & Marketing Studio", "Generate flashcards, quizzes, or campaign copy", FaGlyphs.BULLHORN)
                            OutlinedTextField(value = topic, onValueChange = { topic = it }, label = { Text("Topic, Exam Chapter, or Product Offer") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onRunAiTool("You are an expert Study Tutor. Create concise notes + 5 practice quiz questions.", topic, null) },
                                    enabled = topic.isNotBlank() && !loading,
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) { Text("Study Quiz") }
                                OutlinedButton(
                                    onClick = { onRunAiTool("You are a Luxury Brand Marketing Strategist. Generate hooks, ad copy, and CTA.", topic, null) },
                                    enabled = topic.isNotBlank() && !loading,
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) { Text("Marketing Copy") }
                            }
                        }
                    }
                }
            }

            "Watchlist" -> {
                item {
                    var sym by remember { mutableStateOf("") }
                    var name by remember { mutableStateOf("") }
                    var target by remember { mutableStateOf("") }
                    var note by remember { mutableStateOf("") }
                    NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            NishaSectionHeader("Market Watchlist & AI Research", "Manual watchlist tracking (live brokerage quotes require external market API key)", FaGlyphs.CHART_LINE)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = sym, onValueChange = { sym = it }, label = { Text("Symbol") }, singleLine = true, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text("Target") }, singleLine = true, modifier = Modifier.weight(1f))
                            }
                            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Asset Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Thesis / Note") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        onAddWatchlist(sym, name, target, note)
                                        sym = ""
                                        name = ""
                                        target = ""
                                        note = ""
                                    },
                                    enabled = sym.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) { Text("Track Asset") }
                                OutlinedButton(
                                    onClick = {
                                        val listSummary = watchlist.joinToString("\n") { "${it.symbol} (${it.assetName}) target=${it.targetPrice}: ${it.note}" }
                                        onRunAiTool("You are a Financial Research Assistant. Do not invent live prices; analyze risk/thesis from notes.", "Analyze watchlist:\n$listSummary", null)
                                    },
                                    enabled = watchlist.isNotEmpty() && !loading,
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) { Text("AI Review") }
                            }
                        }
                    }
                }
                items(watchlist, key = { it.id }) { item ->
                    NishaGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${item.symbol} • ${item.assetName}", style = MaterialTheme.typography.titleMedium, color = ObsidianSlate)
                                Text("Target: ${item.targetPrice} | ${item.note}", style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                            }
                            IconButton(onClick = { onDeleteWatchlist(item.id) }, modifier = Modifier.size(48.dp)) {
                                FaIcon(FaGlyphs.TRASH, contentDescription = "Delete asset", color = CrimsonAlert, size = 14.sp)
                            }
                        }
                    }
                }
            }

            "Automations" -> {
                item {
                    var ruleName by remember { mutableStateOf("") }
                    var trigger by remember { mutableStateOf("Voice phrase: Good Morning") }
                    var cond by remember { mutableStateOf("Battery > 20%") }
                    var action by remember { mutableStateOf("Read schedule + weather") }
                    NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            NishaSectionHeader("Visual Automation Builder & AI Rules", "Trigger -> Condition -> Action workflows", FaGlyphs.SLIDERS)
                            OutlinedTextField(value = ruleName, onValueChange = { ruleName = it }, label = { Text("Rule Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = trigger, onValueChange = { trigger = it }, label = { Text("Trigger Node") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = cond, onValueChange = { cond = it }, label = { Text("Condition Node") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = action, onValueChange = { action = it }, label = { Text("Action Node (Requires Confirmation)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Button(
                                onClick = {
                                    onAddAutomation(ruleName, trigger, cond, action)
                                    ruleName = ""
                                },
                                enabled = ruleName.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) { Text("Create Automation Rule") }
                        }
                    }
                }
                items(automations, key = { it.id }) { rule ->
                    NishaGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(rule.name, style = MaterialTheme.typography.titleMedium, color = ObsidianSlate, modifier = Modifier.weight(1f))
                                Switch(checked = rule.enabled, onCheckedChange = { onToggleAutomation(rule) })
                                IconButton(onClick = { onDeleteAutomation(rule.id) }, modifier = Modifier.size(48.dp)) {
                                    FaIcon(FaGlyphs.TRASH, contentDescription = "Delete rule", color = CrimsonAlert, size = 14.sp)
                                }
                            }
                            Text("WHEN ${rule.triggerNode} -> IF ${rule.conditionNode} -> THEN ${rule.actionNode}", style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                        }
                    }
                }
            }

            "Media & Backup" -> {
                item {
                    var restoreInput by remember { mutableStateOf("") }
                    var backupStatus by remember { mutableStateOf<String?>(null) }
                    NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            NishaSectionHeader("Media Launcher, Backup & External Modules", "Official Android intents and honest integration statuses", FaGlyphs.TOOLBOX)
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    runCatching { context.startActivity(intent) }
                                        .onFailure { backupStatus = "No default music player installed on this device." }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                FaIcon(FaGlyphs.MUSIC, contentDescription = null, size = 14.sp)
                                Spacer(Modifier.width(8.dp))
                                Text("Open Device Music Player")
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(onExportBackupJson()))
                                        backupStatus = "Knowledge Brain JSON backup copied to clipboard."
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) { Text("Backup JSON") }
                                OutlinedButton(
                                    onClick = {
                                        if (restoreInput.isNotBlank()) {
                                            onImportBackupJson(restoreInput)
                                            backupStatus = "Restore attempted."
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) { Text("Restore JSON") }
                            }
                            OutlinedTextField(
                                value = restoreInput,
                                onValueChange = { restoreInput = it },
                                label = { Text("Paste Backup JSON to Restore") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                            backupStatus?.let {
                                NishaStatusPill(it, FaGlyphs.CIRCLE_INFO)
                            }
                            Surface(color = AmberSoft, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("External Hardware / Service Dependencies", style = MaterialTheme.typography.titleMedium, color = ObsidianSlate)
                                    Text(
                                        "• Phone-to-PC Sync requires pairing with a desktop companion daemon on your local network.\n• Agent Marketplace requires an external verified agent catalog endpoint.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = IndigoMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (loading) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ElectricViolet, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Processing with Gemini AI...", style = MaterialTheme.typography.bodyMedium, color = ElectricViolet)
                }
            }
        }

        if (aiResult.isNotBlank() && activeModule in setOf("Vision & OCR", "Docs & PDF", "Journal", "Study & Marketing", "Watchlist")) {
            item {
                NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        NishaSectionHeader("AI Output", "Generated directly by your configured Gemini model", FaGlyphs.SPARKLES)
                        Spacer(Modifier.height(8.dp))
                        MarkdownMessageBody(aiResult)
                    }
                }
            }
        }
    }
}
