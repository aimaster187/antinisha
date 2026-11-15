package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.preferences.NishaSettingsState
import com.example.data.remote.GeminiAttachment
import com.example.data.remote.GeminiRestClient
import com.example.ui.theme.AmberSoft
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CrimsonSoft
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.FaGlyphs
import com.example.ui.theme.FaIcon
import com.example.ui.theme.IceBlueDeep
import com.example.ui.theme.IceBlueSoft
import com.example.ui.theme.IndigoMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LavenderGlow
import com.example.ui.theme.MistSurface
import com.example.ui.theme.NishaGlassCard
import com.example.ui.theme.NishaStatusPill
import com.example.ui.theme.ObsidianSlate
import com.example.ui.theme.PearlWhite

@Composable
fun ChatScreen(
    sessions: List<ChatSessionEntity>,
    activeSessionId: String,
    messages: List<ChatMessageEntity>,
    generating: Boolean,
    chatError: String?,
    hasApiKey: Boolean,
    settings: NishaSettingsState,
    pendingAttachment: GeminiAttachment?,
    onSelectSession: (String) -> Unit,
    onNewChat: () -> Unit,
    onRenameChat: (String, String) -> Unit,
    onDeleteChat: (String) -> Unit,
    onSelectModel: (String) -> Unit,
    onSetAttachment: (GeminiAttachment?) -> Unit,
    onSendMessage: (String) -> Unit,
    onRegenerate: () -> Unit,
    onStopGeneration: () -> Unit,
    onOpenApiKeySettings: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var inputText by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<ChatSessionEntity?>(null) }
    var renameText by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null && bytes.size <= 4_500_000) {
                    val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                    onSetAttachment(
                        GeminiAttachment(
                            label = "Image Attachment",
                            mimeType = mime,
                            base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        )
                    )
                }
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null && bytes.size <= 4_500_000) {
                    val mime = context.contentResolver.getType(uri) ?: "text/plain"
                    onSetAttachment(
                        GeminiAttachment(
                            label = uri.lastPathSegment?.substringAfterLast('/') ?: "Document",
                            mimeType = mime,
                            base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        )
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(bottom = 86.dp)
    ) {
        // Conversation & Model Header
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("AI Chat", style = MaterialTheme.typography.headlineMedium, color = ObsidianSlate, modifier = Modifier.weight(1f))
                NishaStatusPill(
                    label = if (settings.memoryEnabled) "Memory On" else "Memory Off",
                    iconGlyph = FaGlyphs.BRAIN,
                    tint = ElectricViolet,
                    background = LavenderGlow
                )
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onNewChat, modifier = Modifier.testTag("chat_new_button")) {
                    FaIcon(FaGlyphs.PLUS, contentDescription = null, size = 12.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("New")
                }
            }
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(GeminiRestClient.SUPPORTED_MODELS) { model ->
                    FilterChip(
                        selected = settings.selectedModel == model,
                        onClick = { onSelectModel(model) },
                        label = { Text(model, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            if (sessions.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sessions.take(10)) { s ->
                        FilterChip(
                            selected = s.id == activeSessionId,
                            onClick = { onSelectSession(s.id) },
                            label = {
                                Text(s.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 120.dp))
                            },
                            trailingIcon = {
                                if (s.id == activeSessionId) {
                                    Row {
                                        IconButton(
                                            onClick = {
                                                renameTarget = s
                                                renameText = s.title
                                            },
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            FaIcon(FaGlyphs.PEN_TO_SQUARE, contentDescription = "Rename chat", size = 11.sp)
                                        }
                                        IconButton(
                                            onClick = { onDeleteChat(s.id) },
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            FaIcon(FaGlyphs.TRASH, contentDescription = "Delete chat", size = 11.sp)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        if (!hasApiKey) {
            Surface(
                color = AmberSoft,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    FaIcon(FaGlyphs.KEY, contentDescription = null, color = AmberWarn, size = 15.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Add your Gemini API key in Settings -> API Keys to chat.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ObsidianSlate,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onOpenApiKeySettings) { Text("Add Key") }
                }
            }
        }

        if (!chatError.isNullOrBlank()) {
            Surface(
                color = CrimsonSoft,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    FaIcon(FaGlyphs.TRIANGLE_EXCLAMATION, contentDescription = null, color = CrimsonAlert, size = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(chatError, style = MaterialTheme.typography.bodyMedium, color = CrimsonAlert)
                }
            }
        }

        // Messages List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            FaIcon(FaGlyphs.ATOM, contentDescription = null, color = ElectricViolet, size = 28.sp)
                            Spacer(Modifier.height(10.dp))
                            Text("Start a conversation with NISHA AI", style = MaterialTheme.typography.titleLarge, color = ObsidianSlate)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Attach photos via Android Photo Picker, pick files via Storage Access Framework, or ask anything with Markdown & code support.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = IndigoMuted
                            )
                        }
                    }
                }
            }
            items(messages, key = { it.id }) { msg ->
                val isUser = msg.role == "user"
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                ) {
                    NishaGlassCard(
                        modifier = Modifier.widthIn(max = 520.dp),
                        cornerRadius = 18.dp,
                        containerColor = if (isUser) LavenderGlow else PearlWhite
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FaIcon(
                                    glyph = if (isUser) FaGlyphs.USER else FaGlyphs.ROBOT,
                                    contentDescription = null,
                                    color = if (isUser) ElectricViolet else IceBlueDeep,
                                    size = 12.sp
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (isUser) "You" else msg.agentAttribution.ifBlank { "NISHA AI" },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isUser) ElectricViolet else IceBlueDeep
                                )
                                if (msg.attachmentLabel.isNotBlank()) {
                                    Spacer(Modifier.width(8.dp))
                                    NishaStatusPill(msg.attachmentLabel, FaGlyphs.FILE_LINES, tint = IceBlueDeep, background = IceBlueSoft)
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            MarkdownMessageBody(msg.content)
                            if (!isUser) {
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { clipboard.setText(AnnotatedString(msg.content)) }) {
                                        FaIcon(FaGlyphs.COPY, contentDescription = null, size = 12.sp)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Copy")
                                    }
                                    TextButton(onClick = {
                                        val share = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, msg.content)
                                        }
                                        context.startActivity(Intent.createChooser(share, "Share AI Response"))
                                    }) {
                                        FaIcon(FaGlyphs.SHARE_NODES, contentDescription = null, size = 12.sp)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Share")
                                    }
                                    if (msg.id == messages.lastOrNull()?.id && !generating) {
                                        TextButton(onClick = onRegenerate) {
                                            FaIcon(FaGlyphs.ROTATE_RIGHT, contentDescription = null, size = 12.sp)
                                            Spacer(Modifier.width(4.dp))
                                            Text("Regenerate")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Composer Bar
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
            if (pendingAttachment != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    NishaStatusPill(pendingAttachment.label, FaGlyphs.IMAGE, tint = IceBlueDeep, background = IceBlueSoft)
                    IconButton(onClick = { onSetAttachment(null) }, modifier = Modifier.size(28.dp)) {
                        FaIcon(FaGlyphs.XMARK, contentDescription = "Remove attachment", size = 12.sp)
                    }
                }
            }
            NishaGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        FaIcon(FaGlyphs.IMAGE, contentDescription = "Attach Photo", color = IceBlueDeep, size = 16.sp)
                    }
                    IconButton(
                        onClick = { filePicker.launch(arrayOf("*/*")) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        FaIcon(FaGlyphs.FOLDER_OPEN, contentDescription = "Attach File", color = ElectricViolet, size = 16.sp)
                    }
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask NISHA AI...") },
                        modifier = Modifier.weight(1f).testTag("chat_message_input"),
                        maxLines = 4
                    )
                    Spacer(Modifier.width(6.dp))
                    if (generating) {
                        Button(
                            onClick = onStopGeneration,
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert),
                            modifier = Modifier.height(48.dp).testTag("chat_stop_button")
                        ) {
                            FaIcon(FaGlyphs.STOP, contentDescription = "Stop", color = PearlWhite, size = 14.sp)
                        }
                    } else {
                        Button(
                            onClick = {
                                val text = inputText
                                inputText = ""
                                onSendMessage(text)
                            },
                            enabled = inputText.isNotBlank() || pendingAttachment != null,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            modifier = Modifier.height(48.dp).testTag("chat_send_button")
                        ) {
                            FaIcon(FaGlyphs.PAPER_PLANE, contentDescription = "Send", color = PearlWhite, size = 14.sp)
                        }
                    }
                }
            }
            if (generating) {
                Row(
                    modifier = Modifier.padding(top = 6.dp, start = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = ElectricViolet)
                    Spacer(Modifier.width(8.dp))
                    Text("Generating via ${settings.selectedModel}...", style = MaterialTheme.typography.labelSmall, color = ElectricViolet)
                }
            }
        }
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename Conversation") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Chat Title") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    onRenameChat(target.id, renameText)
                    renameTarget = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun MarkdownMessageBody(content: String) {
    val segments = remember(content) { content.split("```") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        segments.forEachIndexed { idx, part ->
            val trimmed = part.trim()
            if (trimmed.isNotEmpty()) {
                if (idx % 2 == 1) {
                    Surface(
                        color = ObsidianSlate,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = trimmed,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = PearlWhite,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    val lines = trimmed.lines()
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        lines.forEach { line ->
                            val cleanLine = line.replace("**", "")
                            val isHeading = line.startsWith("#")
                            Text(
                                text = cleanLine.trimStart('#', ' '),
                                style = if (isHeading) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isHeading) FontWeight.Bold else FontWeight.Normal,
                                color = ObsidianSlate
                            )
                        }
                    }
                }
            }
        }
    }
}
