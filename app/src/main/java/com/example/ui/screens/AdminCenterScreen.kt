package com.example.ui.screens

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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.auth.NishaSession
import com.example.data.firebase.AdminRepository
import com.example.data.firebase.FsAppConfig
import com.example.data.firebase.FsAuditLog
import com.example.data.firebase.FsLicenseRecord
import com.example.data.firebase.FsUserProfile
import com.example.data.local.AgentConfigEntity
import com.example.data.local.AgentTaskEntity
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
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LavenderGlow
import com.example.ui.theme.NishaGlassCard
import com.example.ui.theme.NishaSectionHeader
import com.example.ui.theme.NishaStatusPill
import com.example.ui.theme.ObsidianSlate
import com.example.ui.theme.PearlWhite

@Composable
fun AdminCenterScreen(
    session: NishaSession,
    isAdminVerified: Boolean,
    checkingAdmin: Boolean,
    users: List<FsUserProfile>,
    licenses: List<FsLicenseRecord>,
    auditLogs: List<FsAuditLog>,
    appConfig: FsAppConfig,
    explorerDocs: List<Pair<String, Map<String, Any?>>>,
    localAgents: List<AgentConfigEntity>,
    localAgentTasks: List<AgentTaskEntity>,
    feedback: String?,
    onVerifyAndLoad: () -> Unit,
    onBrowseCollection: (String) -> Unit,
    onUpdateUserAccess: (String, String, String) -> Unit,
    onSaveAppConfig: (Boolean, String, Long, String, String, String) -> Unit,
    onSendAnnouncement: (String, String, String, String, String) -> Unit,
    onUpsertLicense: (String, String, String, String, String, Long) -> Unit,
    onClearFeedback: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    LaunchedEffect(session) {
        onVerifyAndLoad()
    }

    var activeModule by remember { mutableStateOf("Dashboard") }
    val adminTabs = listOf(
        "Dashboard",
        "Firestore",
        "Auth",
        "Users",
        "Agents",
        "Licenses",
        "Config",
        "Notify",
        "Security"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 124.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("VIP 10D Admin Control Center", style = MaterialTheme.typography.headlineMedium, color = ObsidianSlate)
                    Text("Server-enforced Firestore Security Rules & Custom Claim guard", style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                }
                NishaStatusPill(
                    label = if (isAdminVerified) "Admin Verified" else "Restricted",
                    iconGlyph = FaGlyphs.USER_SHIELD,
                    tint = if (isAdminVerified) EmeraldSuccess else CrimsonAlert,
                    background = if (isAdminVerified) EmeraldSoft else CrimsonSoft
                )
            }
        }

        if (!feedback.isNullOrBlank()) {
            item {
                Surface(color = IceBlueSoft, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(feedback, style = MaterialTheme.typography.bodyMedium, color = ObsidianSlate, modifier = Modifier.weight(1f))
                        TextButton(onClick = onClearFeedback) { Text("Close") }
                    }
                }
            }
        }

        if (checkingAdmin) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ElectricViolet)
                    Spacer(Modifier.width(10.dp))
                    Text("Verifying Firestore /admins/{uid} & auth claims...", style = MaterialTheme.typography.bodyMedium, color = ElectricViolet)
                }
            }
            return@LazyColumn
        }

        if (!isAdminVerified) {
            item {
                NishaGlassCard(modifier = Modifier.fillMaxWidth(), borderColor = CrimsonAlert.copy(alpha = 0.4f)) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        NishaSectionHeader("Admin Access Denied (Fail-Closed)", "Client-only bypasses are blocked by Firestore Security Rules", FaGlyphs.LOCK)
                        val currentEmail = when (session) {
                            is NishaSession.FirebaseGoogle -> "${session.email} (verified=${session.emailVerified})"
                            is NishaSession.LocalVault -> "${session.email} (Local vault - not Firebase verified)"
                            NishaSession.Unauthenticated -> "Not signed in"
                        }
                        Text("Active Identity: $currentEmail", style = MaterialTheme.typography.bodyMedium, color = ObsidianSlate)
                        Text(
                            "To unlock VIP 10D Admin:\n1. Sign in via Google Sign-In with verified email '${AdminRepository.BOOTSTRAP_ADMIN_EMAIL}', OR\n2. Add document `/admins/<uid>` with `active: true` in Firebase Console.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = IndigoMuted
                        )
                        Button(
                            onClick = onVerifyAndLoad,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) { Text("Re-Verify Admin Credentials") }
                    }
                }
            }
            return@LazyColumn
        }

        // 9-Module Selector Bar
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(adminTabs) { tab ->
                    FilterChip(
                        selected = activeModule == tab,
                        onClick = { activeModule = tab },
                        label = { Text(tab) },
                        modifier = Modifier.testTag("admin_tab_$tab")
                    )
                }
            }
        }

        when (activeModule) {
            "Dashboard" -> {
                item {
                    val activeUsers = users.count { it.status == "active" }
                    val proCount = users.count { it.tier == "pro" }
                    val ultraCount = users.count { it.tier == "ultra" }
                    NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            NishaSectionHeader("A. Live Admin Telemetry", "Synchronized from Firestore project", FaGlyphs.CHART_LINE)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                AdminStatTile("Users", "${users.size}", Modifier.weight(1f))
                                AdminStatTile("Active", "$activeUsers", Modifier.weight(1f))
                                AdminStatTile("Pro/Ultra", "${proCount + ultraCount}", Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                AdminStatTile("Licenses", "${licenses.size}", Modifier.weight(1f))
                                AdminStatTile("Audit Logs", "${auditLogs.size}", Modifier.weight(1f))
                                AdminStatTile("App Ver", "v${appConfig.latestVersionName}", Modifier.weight(1f))
                            }
                            OutlinedButton(onClick = onVerifyAndLoad, modifier = Modifier.fillMaxWidth()) {
                                Text("Refresh Live Telemetry")
                            }
                        }
                    }
                }
            }

            "Firestore" -> {
                item {
                    var selectedCol by remember { mutableStateOf("users") }
                    val collections = listOf("users", "app_config", "announcements", "licenses", "audit_logs")
                    NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            NishaSectionHeader("B. Firestore Explorer", "Inspect top-level verified documents", FaGlyphs.DATABASE)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(collections) { col ->
                                    FilterChip(
                                        selected = selectedCol == col,
                                        onClick = {
                                            selectedCol = col
                                            onBrowseCollection(col)
                                        },
                                        label = { Text(col) }
                                    )
                                }
                            }
                            Button(
                                onClick = { onBrowseCollection(selectedCol) },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) { Text("Load '$selectedCol' Documents") }
                        }
                    }
                }
                items(explorerDocs, key = { it.first }) { (docId, data) ->
                    NishaGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Doc ID: $docId", style = MaterialTheme.typography.titleMedium, color = ElectricViolet)
                            Text(
                                data.entries.joinToString("\n") { "${it.key}: ${it.value}" },
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = ObsidianSlate
                            )
                        }
                    }
                }
            }

            "Auth" -> {
                item {
                    NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            NishaSectionHeader("C. Authentication Manager", "Provider readiness & OAuth fingerprint audit", FaGlyphs.KEY)
                            NishaStatusPill("Google Sign-In (Credential Manager): ACTIVE", FaGlyphs.CIRCLE_CHECK, tint = EmeraldSuccess, background = EmeraldSoft)
                            NishaStatusPill("Local Password Vault (PBKDF2): ACTIVE", FaGlyphs.CIRCLE_CHECK, tint = EmeraldSuccess, background = EmeraldSoft)
                            Surface(color = AmberSoft, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Firebase Console Provider Note", style = MaterialTheme.typography.titleMedium, color = ObsidianSlate)
                                    Text(
                                        "Phone OTP & Cloud Email/Password links require enabling those providers and SHA-1/Play Integrity verification inside Firebase Console -> Authentication -> Sign-in method.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = IndigoMuted
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { onOpenUrl("https://console.firebase.google.com/") },
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) { Text("Open Firebase Console") }
                        }
                    }
                }
            }

            "Users" -> {
                item {
                    NishaSectionHeader("D. User Manager (${users.size})", "Inspect accounts, toggle active/suspended status, or assign tier", FaGlyphs.USERS)
                }
                items(users, key = { it.id }) { user ->
                    NishaGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${user.fullName} (@${user.username})", style = MaterialTheme.typography.titleMedium, color = ObsidianSlate)
                                    Text("${user.email} • ${user.authProvider}", style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                                }
                                NishaStatusPill(
                                    label = "${user.tier.uppercase()} / ${user.status}",
                                    iconGlyph = FaGlyphs.USER,
                                    tint = if (user.status == "active") EmeraldSuccess else CrimsonAlert,
                                    background = if (user.status == "active") EmeraldSoft else CrimsonSoft
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val nextStatus = if (user.status == "active") "suspended" else "active"
                                        onUpdateUserAccess(user.id, user.tier, nextStatus)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text(if (user.status == "active") "Suspend" else "Activate") }
                                OutlinedButton(
                                    onClick = {
                                        val nextTier = when (user.tier) {
                                            "free" -> "pro"
                                            "pro" -> "ultra"
                                            else -> "free"
                                        }
                                        onUpdateUserAccess(user.id, nextTier, user.status)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Cycle Tier") }
                            }
                        }
                    }
                }
            }

            "Agents" -> {
                item {
                    NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            NishaSectionHeader("E. Agent Hub Oversight", "5-slot agent cap & recent orchestration counts", FaGlyphs.ROBOT)
                            Text("Local Active Agents: ${localAgents.count { it.status == "active" }} / ${localAgents.size} (Max 5)", style = MaterialTheme.typography.bodyLarge, color = ObsidianSlate)
                            Text("Recorded Orchestrations: ${localAgentTasks.size}", style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                            localAgents.forEach { ag ->
                                Surface(shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, FrostBorder), color = PearlWhite, modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        FaIcon(FaGlyphs.byName(ag.iconName), contentDescription = null, color = ElectricViolet, size = 14.sp)
                                        Spacer(Modifier.width(8.dp))
                                        Text("${ag.name} (${ag.status})", style = MaterialTheme.typography.bodyMedium, color = ObsidianSlate, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "Licenses" -> {
                item {
                    var licId by remember { mutableStateOf("") }
                    var uid by remember { mutableStateOf("") }
                    var plan by remember { mutableStateOf("pro") }
                    var status by remember { mutableStateOf("active") }
                    NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            NishaSectionHeader("F. VIP & License Manager", "Issue or revoke Pro/Ultra licenses in Firestore", FaGlyphs.CROWN)
                            OutlinedTextField(value = licId, onValueChange = { licId = it }, label = { Text("License ID (e.g. vip_001)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = uid, onValueChange = { uid = it }, label = { Text("Target User UID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("free", "pro", "ultra").forEach { p ->
                                    FilterChip(selected = plan == p, onClick = { plan = p }, label = { Text(p.uppercase()) })
                                }
                                FilterChip(selected = status == "active", onClick = { status = if (status == "active") "revoked" else "active" }, label = { Text(status) })
                            }
                            Button(
                                onClick = {
                                    val expires = System.currentTimeMillis() + 30L * 24L * 3600L * 1000L
                                    onUpsertLicense(licId, uid, plan, status, "admin_grant", expires)
                                    licId = ""
                                },
                                enabled = licId.isNotBlank() && uid.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) { Text("Save License Record") }
                        }
                    }
                }
                items(licenses, key = { it.id }) { lic ->
                    NishaGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${lic.id} -> ${lic.userId}", style = MaterialTheme.typography.titleMedium, color = ObsidianSlate)
                                Text("Plan: ${lic.plan} • Status: ${lic.status} • Source: ${lic.source}", style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                            }
                        }
                    }
                }
            }

            "Config" -> {
                item {
                    var maintenance by remember(appConfig) { mutableStateOf(appConfig.maintenanceMode) }
                    var verName by remember(appConfig) { mutableStateOf(appConfig.latestVersionName.ifBlank { BuildConfig.VERSION_NAME }) }
                    var verCode by remember(appConfig) { mutableStateOf(appConfig.latestVersionCode.toString()) }
                    var defModel by remember(appConfig) { mutableStateOf(appConfig.defaultModel) }
                    var banner by remember(appConfig) { mutableStateOf(appConfig.announcementBanner) }
                    var notes by remember(appConfig) { mutableStateOf(appConfig.releaseNotes) }

                    NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            NishaSectionHeader("G. App Configuration", "Global release version, model default & maintenance switch", FaGlyphs.GEAR)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Maintenance Mode", style = MaterialTheme.typography.bodyLarge, color = ObsidianSlate, modifier = Modifier.weight(1f))
                                Switch(checked = maintenance, onCheckedChange = { maintenance = it })
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = verName, onValueChange = { verName = it }, label = { Text("Version Name") }, singleLine = true, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = verCode, onValueChange = { verCode = it }, label = { Text("Version Code") }, singleLine = true, modifier = Modifier.weight(1f))
                            }
                            OutlinedTextField(value = defModel, onValueChange = { defModel = it }, label = { Text("Default Gemini Model") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = banner, onValueChange = { banner = it }, label = { Text("Announcement Banner") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Release Notes") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                            Button(
                                onClick = {
                                    onSaveAppConfig(maintenance, verName, verCode.toLongOrNull() ?: 1L, defModel, banner, notes)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) { Text("Publish App Config") }
                        }
                    }
                }
            }

            "Notify" -> {
                item {
                    var title by remember { mutableStateOf("") }
                    var body by remember { mutableStateOf("") }
                    var targetMode by remember { mutableStateOf("all") }
                    var targetUsername by remember { mutableStateOf("") }
                    var confirmSend by remember { mutableStateOf(false) }

                    NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            NishaSectionHeader("H. Notification & Announcement Manager", "Preview & confirm before broadcasting to users", FaGlyphs.BELL)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = targetMode == "all", onClick = { targetMode = "all" }, label = { Text("All Users") })
                                FilterChip(selected = targetMode == "user", onClick = { targetMode = "user" }, label = { Text("Specific Username") })
                            }
                            if (targetMode == "user") {
                                OutlinedTextField(value = targetUsername, onValueChange = { targetUsername = it }, label = { Text("Target Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            }
                            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Announcement Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Message Body") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                            Button(
                                onClick = { confirmSend = true },
                                enabled = title.isNotBlank() && body.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_send_announcement_btn")
                            ) { Text("Preview & Send Announcement") }
                        }
                    }

                    if (confirmSend) {
                        val matchedUid = users.firstOrNull { it.username.equals(targetUsername, true) }?.id.orEmpty()
                        AlertDialog(
                            onDismissRequest = { confirmSend = false },
                            title = { Text("Confirm Announcement Broadcast") },
                            text = {
                                Text("Target: ${if (targetMode == "all") "All Signed-In Users" else "@$targetUsername"}\n\nTitle: $title\n\n$body")
                            },
                            confirmButton = {
                                Button(onClick = {
                                    onSendAnnouncement(title, body, targetMode, targetUsername, matchedUid)
                                    title = ""
                                    body = ""
                                    confirmSend = false
                                }) { Text("Confirm & Send") }
                            },
                            dismissButton = {
                                TextButton(onClick = { confirmSend = false }) { Text("Cancel") }
                            }
                        )
                    }
                }
            }

            "Security" -> {
                item {
                    NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            NishaSectionHeader("I. Security & Immutable Audit Logs", "All administrative writes are logged & verified by Firestore rules", FaGlyphs.SHIELD_HALVED)
                            Text("Zero plaintext API keys, passwords, or private tokens exist in Firestore.", style = MaterialTheme.typography.bodyMedium, color = EmeraldSuccess)
                        }
                    }
                }
                items(auditLogs, key = { it.id }) { log ->
                    NishaGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("${log.action} -> ${log.target}", style = MaterialTheme.typography.titleMedium, color = ObsidianSlate)
                            Text("${log.actorEmail} | ${log.details}", style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminStatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = LavenderGlow,
        border = BorderStroke(1.dp, FrostBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ElectricViolet)
            Text(label, style = MaterialTheme.typography.labelSmall, color = IndigoMuted)
        }
    }
}
