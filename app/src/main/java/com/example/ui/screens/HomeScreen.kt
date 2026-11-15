package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.firebase.FsAnnouncement
import com.example.data.local.AgentTaskEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.preferences.NishaSettingsState
import com.example.data.remote.WeatherSnapshot
import com.example.domain.AiCoreState
import com.example.domain.BatteryTelemetry
import com.example.ui.components.MayaCore3DCanvas
import com.example.ui.theme.AmberSoft
import com.example.ui.theme.AmberWarn
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
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@SuppressLint("MissingPermission")
@Composable
fun HomeScreen(
    displayName: String,
    coreState: AiCoreState,
    amplitude: Float,
    statusSubtitle: String,
    hasApiKey: Boolean,
    battery: BatteryTelemetry,
    weather: WeatherSnapshot?,
    settings: NishaSettingsState,
    recentChats: List<ChatSessionEntity>,
    recentTasks: List<AgentTaskEntity>,
    announcements: List<FsAnnouncement>,
    onOpenDrawer: () -> Unit,
    onOpenProfile: () -> Unit,
    onTapMayaCore: () -> Unit,
    onNavigateVoice: () -> Unit,
    onNavigateChat: (String?) -> Unit,
    onNavigateJournal: () -> Unit,
    onNavigateMusic: () -> Unit,
    onNavigateAgents: () -> Unit,
    onNavigateTools: () -> Unit,
    onRefreshBattery: () -> Unit,
    onLoadWeather: (Double, Double, String) -> Unit,
    onUpdateWidgets: (Boolean?, Boolean?, Boolean?, Boolean?) -> Unit,
    onQuickPrompt: (String) -> Unit
) {
    val context = LocalContext.current
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showNotifications by remember { mutableStateOf(false) }
    var showCustomizeWidgets by remember { mutableStateOf(false) }
    var showLocationRationale by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val client = LocationServices.getFusedLocationProviderClient(context)
            client.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    onLoadWeather(loc.latitude, loc.longitude, "Local GPS")
                } else {
                    onLoadWeather(28.6139, 77.2090, "New Delhi")
                }
            }.addOnFailureListener {
                onLoadWeather(28.6139, 77.2090, "New Delhi")
            }
        }
    }

    LaunchedEffect(Unit) {
        onRefreshBattery()
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasCoarse) {
            val client = LocationServices.getFusedLocationProviderClient(context)
            client.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) onLoadWeather(loc.latitude, loc.longitude, "Local Area")
                else onLoadWeather(28.6139, 77.2090, "New Delhi")
            }
        }
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(15_000L)
        }
    }

    val calendar = remember(nowMillis) { Calendar.getInstance().apply { timeInMillis = nowMillis } }
    val greeting = remember(nowMillis) {
        when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..21 -> "Good Evening"
            else -> "Good Night"
        }
    }
    val timeText = remember(nowMillis) { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(nowMillis)) }
    val dateText = remember(nowMillis) { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(nowMillis)) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.img_maya_aura_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(0.55f),
            contentScale = ContentScale.Crop
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Luxury Command Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onOpenDrawer,
                        shape = CircleShape,
                        color = PearlWhite.copy(alpha = 0.9f),
                        border = BorderStroke(1.dp, FrostBorder),
                        modifier = Modifier.size(48.dp).testTag("home_drawer_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            FaIcon(FaGlyphs.BARS, contentDescription = "Open navigation drawer", color = ObsidianSlate, size = 18.sp)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$greeting, ${displayName.substringBefore(" ")}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = ObsidianSlate,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$timeText • $dateText",
                            style = MaterialTheme.typography.bodyMedium,
                            color = IndigoMuted
                        )
                    }
                    IconButton(
                        onClick = { showNotifications = true },
                        modifier = Modifier.size(48.dp).testTag("home_notifications_button")
                    ) {
                        FaIcon(FaGlyphs.BELL, contentDescription = "Notifications", color = ElectricViolet, size = 18.sp)
                    }
                    Spacer(Modifier.width(4.dp))
                    Surface(
                        onClick = onOpenProfile,
                        shape = CircleShape,
                        color = LavenderGlow,
                        border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.35f)),
                        modifier = Modifier.size(48.dp).testTag("home_profile_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            FaIcon(FaGlyphs.USER, contentDescription = "Profile and Account", color = ElectricViolet, size = 17.sp)
                        }
                    }
                }
            }

            // Telemetry & Status Strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NishaStatusPill(
                        label = coreState.label,
                        iconGlyph = FaGlyphs.ATOM,
                        tint = ElectricViolet,
                        background = LavenderGlow
                    )
                    NishaStatusPill(
                        label = battery.statusLabel,
                        iconGlyph = FaGlyphs.BATTERY_THREE_QUARTERS,
                        tint = IceBlueDeep,
                        background = IceBlueSoft
                    )
                    NishaStatusPill(
                        label = if (hasApiKey) "Key Ready" else "Add Key",
                        iconGlyph = FaGlyphs.KEY,
                        tint = if (hasApiKey) EmeraldSuccess else AmberWarn,
                        background = if (hasApiKey) EmeraldSoft else AmberSoft
                    )
                }
            }

            // Central 3D Maya AI Core Hero Card
            item {
                NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "MAYA 3D AI CORE",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricViolet,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        MayaCore3DCanvas(
                            state = coreState,
                            amplitude = amplitude,
                            onTapCore = onTapMayaCore
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = statusSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = IndigoMuted
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onNavigateVoice,
                                modifier = Modifier.weight(1f).height(48.dp).testTag("home_voice_cta"),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                FaIcon(FaGlyphs.MICROPHONE, contentDescription = null, color = PearlWhite, size = 14.sp)
                                Spacer(Modifier.width(8.dp))
                                Text("Voice OS")
                            }
                            OutlinedButton(
                                onClick = { onNavigateChat(null) },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("home_chat_cta"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                FaIcon(FaGlyphs.COMMENTS, contentDescription = null, color = IceBlueDeep, size = 14.sp)
                                Spacer(Modifier.width(8.dp))
                                Text("AI Chat", color = IceBlueDeep)
                            }
                        }
                    }
                }
            }

            // Weather Widget (Customizable)
            if (settings.showWeatherWidget) {
                item {
                    NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(IceBlueSoft, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                FaIcon(FaGlyphs.CLOUD_SUN, contentDescription = "Weather", color = IceBlueDeep, size = 20.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                if (weather != null) {
                                    Text(
                                        text = "${weather.temperatureC}°C • ${weather.conditionLabel}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ObsidianSlate
                                    )
                                    Text(
                                        text = "${weather.locationLabel} • Wind ${weather.windKmh} km/h",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = IndigoMuted
                                    )
                                } else {
                                    Text(
                                        text = "Local Weather Telemetry",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ObsidianSlate
                                    )
                                    Text(
                                        text = "Grant coarse location permission for live weather",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = IndigoMuted
                                    )
                                }
                            }
                            TextButton(
                                onClick = {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.ACCESS_COARSE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) {
                                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                    } else {
                                        showLocationRationale = true
                                    }
                                },
                                modifier = Modifier.testTag("home_weather_refresh")
                            ) {
                                Text(if (weather == null) "Enable" else "Refresh")
                            }
                        }
                    }
                }
            }

            // Quick Actions (Customizable)
            if (settings.showQuickActionsWidget) {
                item {
                    NishaSectionHeader(
                        title = "Quick Actions",
                        subtitle = "Instant launch across NISHA subsystems",
                        iconGlyph = FaGlyphs.BOLT,
                        trailing = {
                            TextButton(onClick = { showCustomizeWidgets = true }) {
                                Text("Customize")
                            }
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                    val shortcuts = listOf(
                        Triple("Journal", FaGlyphs.BOOK_OPEN, onNavigateJournal),
                        Triple("Music", FaGlyphs.MUSIC, onNavigateMusic),
                        Triple("5 Agents", FaGlyphs.ROBOT, onNavigateAgents),
                        Triple("Tools Hub", FaGlyphs.TOOLBOX, onNavigateTools)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        shortcuts.forEach { (label, glyph, action) ->
                            NishaGlassCard(
                                modifier = Modifier.weight(1f),
                                cornerRadius = 18.dp,
                                onClick = action
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 14.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    FaIcon(glyph, contentDescription = label, color = ElectricViolet, size = 18.sp)
                                    Spacer(Modifier.height(6.dp))
                                    Text(label, style = MaterialTheme.typography.labelLarge, color = ObsidianSlate, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            // Personalized Suggestions (Customizable)
            if (settings.showSuggestionsWidget) {
                item {
                    NishaSectionHeader(
                        title = "Personalized Suggestions",
                        subtitle = "One-tap prompts tuned for your assistant",
                        iconGlyph = FaGlyphs.SPARKLES
                    )
                    Spacer(Modifier.height(10.dp))
                    val suggestions = listOf(
                        "Summarize my top 3 priorities for today in Hinglish",
                        "Draft a polite project update for WhatsApp",
                        "Review Kotlin Coroutines best practices for Android",
                        "Create a 25-minute deep focus study plan"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(suggestions) { prompt ->
                            NishaGlassCard(
                                modifier = Modifier.width(235.dp),
                                cornerRadius = 18.dp,
                                onClick = { onQuickPrompt(prompt) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FaIcon(FaGlyphs.WAND_MAGIC, contentDescription = null, color = IceBlueDeep, size = 15.sp)
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = prompt,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ObsidianSlate,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recent Conversations
            item {
                NishaSectionHeader(
                    title = "Recent Conversations",
                    subtitle = "Continue where you left off",
                    iconGlyph = FaGlyphs.COMMENT_DOTS
                )
                Spacer(Modifier.height(10.dp))
                if (recentChats.isEmpty()) {
                    NishaGlassCard(modifier = Modifier.fillMaxWidth(), onClick = { onNavigateChat(null) }) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            FaIcon(FaGlyphs.PLUS, contentDescription = null, color = ElectricViolet, size = 16.sp)
                            Spacer(Modifier.width(12.dp))
                            Text("Start your first Gemini AI conversation", style = MaterialTheme.typography.bodyLarge, color = IndigoMuted)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        recentChats.take(3).forEach { chat ->
                            NishaGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 16.dp,
                                onClick = { onNavigateChat(chat.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FaIcon(FaGlyphs.COMMENTS, contentDescription = null, color = ElectricViolet, size = 16.sp)
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(chat.title, style = MaterialTheme.typography.titleMedium, color = ObsidianSlate, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (chat.lastPreview.isNotBlank()) {
                                            Text(chat.lastPreview, style = MaterialTheme.typography.bodyMedium, color = IndigoMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    FaIcon(FaGlyphs.CHEVRON_RIGHT, contentDescription = null, color = IndigoMuted, size = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Recent Multi-Agent Tasks (Customizable)
            if (settings.showRecentTasksWidget) {
                item {
                    NishaSectionHeader(
                        title = "Recent Agent Tasks",
                        subtitle = "Coordinated multi-agent runs",
                        iconGlyph = FaGlyphs.DIAGRAM_PROJECT
                    )
                    Spacer(Modifier.height(10.dp))
                    if (recentTasks.isEmpty()) {
                        NishaGlassCard(modifier = Modifier.fillMaxWidth(), onClick = onNavigateAgents) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                FaIcon(FaGlyphs.ROBOT, contentDescription = null, color = IceBlueDeep, size = 16.sp)
                                Spacer(Modifier.width(12.dp))
                                Text("Run your 5-Agent Orchestrator on a task", style = MaterialTheme.typography.bodyLarge, color = IndigoMuted)
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            recentTasks.take(3).forEach { task ->
                                NishaGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp, onClick = onNavigateAgents) {
                                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        FaIcon(FaGlyphs.CIRCLE_NODES, contentDescription = null, color = IceBlueDeep, size = 16.sp)
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(task.prompt, style = MaterialTheme.typography.titleMedium, color = ObsidianSlate, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${task.assignedAgents} • ${task.status}", style = MaterialTheme.typography.bodyMedium, color = IndigoMuted, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLocationRationale) {
        AlertDialog(
            onDismissRequest = { showLocationRationale = false },
            title = { Text("Coarse Location for Weather") },
            text = { Text("NISHA AI uses approximate device location solely to fetch local temperature and conditions from Open-Meteo on your Home screen.") },
            confirmButton = {
                Button(onClick = {
                    showLocationRationale = false
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }) { Text("Allow Coarse Location") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showLocationRationale = false
                    onLoadWeather(28.6139, 77.2090, "New Delhi (Default)")
                }) { Text("Use Default City") }
            }
        )
    }

    if (showNotifications) {
        AlertDialog(
            onDismissRequest = { showNotifications = false },
            title = { Text("Notifications & Announcements") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (announcements.isEmpty()) {
                        Text("No new admin announcements right now.", style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                    } else {
                        announcements.take(5).forEach { ann ->
                            NishaGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(ann.title, style = MaterialTheme.typography.titleMedium, color = ObsidianSlate)
                                    Text(ann.body, style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotifications = false }) { Text("Close") }
            }
        )
    }

    if (showCustomizeWidgets) {
        AlertDialog(
            onDismissRequest = { showCustomizeWidgets = false },
            title = { Text("Customize Home Widgets") },
            text = {
                Column {
                    WidgetToggleRow("Weather Telemetry", settings.showWeatherWidget) {
                        onUpdateWidgets(it, null, null, null)
                    }
                    WidgetToggleRow("Quick Actions", settings.showQuickActionsWidget) {
                        onUpdateWidgets(null, it, null, null)
                    }
                    WidgetToggleRow("Personalized Suggestions", settings.showSuggestionsWidget) {
                        onUpdateWidgets(null, null, it, null)
                    }
                    WidgetToggleRow("Recent Agent Tasks", settings.showRecentTasksWidget) {
                        onUpdateWidgets(null, null, null, it)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCustomizeWidgets = false }) { Text("Done") }
            }
        )
    }
}

@Composable
private fun WidgetToggleRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onToggle)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = ObsidianSlate)
    }
}
