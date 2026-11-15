package com.example.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.local.CodingProjectEntity
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
import com.example.ui.theme.NishaSectionHeader
import com.example.ui.theme.NishaStatusPill
import com.example.ui.theme.ObsidianSlate
import com.example.ui.theme.PearlWhite

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CodingStudioScreen(
    projects: List<CodingProjectEntity>,
    aiOutput: String,
    loading: Boolean,
    onSaveProject: (CodingProjectEntity) -> Unit,
    onCreateProject: (String, String, String) -> Unit,
    onRunAiAssist: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var selectedId by remember { mutableStateOf("") }
    val activeProject = remember(projects, selectedId) {
        projects.firstOrNull { it.id == selectedId } ?: projects.firstOrNull()
    }
    var editorCode by remember(activeProject?.id) { mutableStateOf(activeProject?.codeContent.orEmpty()) }
    var buildLog by remember(activeProject?.id) { mutableStateOf(activeProject?.buildLog.orEmpty()) }
    var aiPrompt by remember { mutableStateOf("") }
    var showPreview by remember { mutableStateOf(true) }
    var showNewProjectDialog by remember { mutableStateOf(false) }

    LaunchedEffect(activeProject?.id) {
        if (activeProject != null && selectedId != activeProject.id) {
            selectedId = activeProject.id
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null && activeProject != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(editorCode.toByteArray(Charsets.UTF_8))
                }
                buildLog = "$buildLog\n[Export] Saved ${activeProject.fileName} via Storage Access Framework."
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null && activeProject != null) {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                editorCode = text
                buildLog = "$buildLog\n[Import] Loaded ${text.length} chars into ${activeProject.fileName}."
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 760.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 124.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Coding Studio", style = MaterialTheme.typography.headlineMedium, color = ObsidianSlate)
                    Text("Editor, AI code assist, live HTML preview & APK CI templates", style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                }
                OutlinedButton(onClick = { showNewProjectDialog = true }, modifier = Modifier.testTag("studio_new_project")) {
                    FaIcon(FaGlyphs.PLUS, contentDescription = null, size = 12.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("New")
                }
            }
        }

        // Project Explorer Bar
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(projects, key = { it.id }) { proj ->
                    FilterChip(
                        selected = activeProject?.id == proj.id,
                        onClick = { selectedId = proj.id },
                        leadingIcon = { FaIcon(FaGlyphs.FILE_CODE, contentDescription = null, size = 12.sp) },
                        label = { Text("${proj.fileName} (${proj.language})") }
                    )
                }
            }
        }

        // Code Editor Card
        if (activeProject != null) {
            item {
                NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NishaStatusPill(activeProject.fileName, FaGlyphs.CODE, tint = ElectricViolet, background = LavenderGlow)
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { importLauncher.launch(arrayOf("text/*", "*/*")) }) {
                                FaIcon(FaGlyphs.FOLDER_OPEN, contentDescription = null, size = 12.sp)
                                Spacer(Modifier.width(4.dp))
                                Text("Import")
                            }
                            TextButton(onClick = { exportLauncher.launch(activeProject.fileName) }) {
                                FaIcon(FaGlyphs.DOWNLOAD, contentDescription = null, size = 12.sp)
                                Spacer(Modifier.width(4.dp))
                                Text("Export")
                            }
                            Button(
                                onClick = {
                                    val logLine = "[Saved] ${activeProject.fileName} (${editorCode.lines().size} lines)"
                                    val updatedLog = "$buildLog\n$logLine".trim()
                                    buildLog = updatedLog
                                    onSaveProject(activeProject.copy(codeContent = editorCode, buildLog = updatedLog))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                modifier = Modifier.testTag("studio_save_button")
                            ) { Text("Save") }
                        }

                        OutlinedTextField(
                            value = editorCode,
                            onValueChange = { editorCode = it },
                            textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp, color = ObsidianSlate),
                            minLines = 9,
                            maxLines = 18,
                            modifier = Modifier.fillMaxWidth().testTag("studio_code_editor")
                        )
                    }
                }
            }

            // Live WebView Preview (for HTML/CSS/JS)
            if (activeProject.language.contains("HTML", ignoreCase = true)) {
                item {
                    NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                NishaSectionHeader(
                                    title = "Live HTML/CSS/JS Preview",
                                    subtitle = "Sandboxed WebView (no file/content access)",
                                    iconGlyph = FaGlyphs.EYE,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { showPreview = !showPreview }) {
                                    Text(if (showPreview) "Hide" else "Show")
                                }
                            }
                            if (showPreview) {
                                Spacer(Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().height(230.dp)
                                ) {
                                    AndroidView(
                                        factory = { ctx ->
                                            WebView(ctx).apply {
                                                webViewClient = WebViewClient()
                                                settings.javaScriptEnabled = true
                                                settings.allowFileAccess = false
                                                settings.allowContentAccess = false
                                            }
                                        },
                                        update = { webView ->
                                            webView.loadDataWithBaseURL(
                                                "https://nisha.local/",
                                                editorCode,
                                                "text/html",
                                                "UTF-8",
                                                null
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // AI Coding Assistant
        item {
            NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NishaSectionHeader(
                        title = "Code Master AI Assist",
                        subtitle = "Generate, explain, or debug using your Gemini key",
                        iconGlyph = FaGlyphs.LAPTOP_CODE
                    )
                    OutlinedTextField(
                        value = aiPrompt,
                        onValueChange = { aiPrompt = it },
                        label = { Text("Instruction or Error Trace") },
                        placeholder = { Text("e.g., Add responsive grid cards or fix syntax bug") },
                        modifier = Modifier.fillMaxWidth().testTag("studio_ai_prompt")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { onRunAiAssist("generate", editorCode, aiPrompt) },
                            enabled = !loading,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) { Text("Generate") }
                        OutlinedButton(
                            onClick = { onRunAiAssist("explain", editorCode, aiPrompt) },
                            enabled = !loading,
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) { Text("Explain") }
                        OutlinedButton(
                            onClick = { onRunAiAssist("debug", editorCode, aiPrompt) },
                            enabled = !loading,
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) { Text("Debug") }
                    }
                    if (loading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ElectricViolet)
                            Spacer(Modifier.width(8.dp))
                            Text("Code Master analyzing...", style = MaterialTheme.typography.bodyMedium, color = ElectricViolet)
                        }
                    }
                    if (aiOutput.isNotBlank()) {
                        MarkdownMessageBody(aiOutput)
                    }
                }
            }
        }

        // Build Log & Termux / GitHub Actions APK Workflow Exporter
        item {
            NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NishaSectionHeader(
                        title = "Build Log & Android APK CI Workflow",
                        subtitle = "Transparent Termux & GitHub Actions signed APK guide (never hidden)",
                        iconGlyph = FaGlyphs.TERMINAL
                    )
                    Surface(color = ObsidianSlate, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = buildLog.ifBlank { "[Studio] Ready." },
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = PearlWhite,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    val termuxSnippet = """
                        pkg update && pkg install -y openjdk-17 git gradle
                        git clone <your-nisha-repo-url> && cd nisha-ai
                        gradle assembleRelease
                    """.trimIndent()
                    Surface(color = MistSurface, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Termux Manual Commands (User-Executed)", style = MaterialTheme.typography.labelLarge, color = ElectricViolet)
                            Spacer(Modifier.height(4.dp))
                            Text(termuxSnippet, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = ObsidianSlate)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = {
                                    clipboard.setText(AnnotatedString(termuxSnippet))
                                    buildLog = "$buildLog\n[Studio] Copied Termux commands to clipboard."
                                }) {
                                    FaIcon(FaGlyphs.COPY, contentDescription = null, size = 12.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Copy Termux Script")
                                }
                                TextButton(onClick = {
                                    clipboard.setText(AnnotatedString(GITHUB_ACTIONS_TEMPLATE))
                                    buildLog = "$buildLog\n[Studio] Copied GitHub Actions release workflow YAML."
                                }) {
                                    FaIcon(FaGlyphs.BRAND_GITHUB, contentDescription = null, style = com.example.ui.theme.FaStyle.BRANDS, size = 12.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Copy GitHub Actions YAML")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewProjectDialog) {
        var projName by remember { mutableStateOf("") }
        var fileName by remember { mutableStateOf("app.html") }
        var language by remember { mutableStateOf("HTML/CSS/JS") }
        val langs = listOf("HTML/CSS/JS", "Kotlin", "Python", "JSON")

        AlertDialog(
            onDismissRequest = { showNewProjectDialog = false },
            title = { Text("Create Studio File") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = projName, onValueChange = { projName = it }, label = { Text("Project Name") }, singleLine = true)
                    OutlinedTextField(value = fileName, onValueChange = { fileName = it }, label = { Text("File Name") }, singleLine = true)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(langs) { l ->
                            FilterChip(selected = language == l, onClick = { language = l }, label = { Text(l) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onCreateProject(projName, fileName, language)
                    showNewProjectDialog = false
                }, enabled = projName.isNotBlank()) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewProjectDialog = false }) { Text("Cancel") }
            }
        )
    }
}

private val GITHUB_ACTIONS_TEMPLATE = """
name: Android Signed Release APK
on:
  workflow_dispatch:
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
      - name: Decode Keystore
        env:
          KEYSTORE_BASE64: ${'$'}{{ secrets.KEYSTORE_BASE64 }}
        run: echo "${'$'}KEYSTORE_BASE64" | base64 -d > my-upload-key.jks
      - name: Build Signed APK
        env:
          STORE_PASSWORD: ${'$'}{{ secrets.STORE_PASSWORD }}
          KEY_PASSWORD: ${'$'}{{ secrets.KEY_PASSWORD }}
        run: ./gradlew assembleRelease --no-daemon
      - name: Clean Keystore
        if: always()
        run: rm -f my-upload-key.jks
      - uses: actions/upload-artifact@v4
        with:
          name: nisha-ai-release-apk
          path: app/build/outputs/apk/release/*.apk
""".trimIndent()
