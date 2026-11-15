package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.auth.NishaSession
import com.example.di.NishaAppContainer
import com.example.domain.AiCoreState
import com.example.domain.AssistantRuntimeCoordinator
import com.example.service.NishaAssistantService
import com.example.service.NishaVoiceController
import com.example.ui.components.Floating10DOrb
import com.example.ui.components.FourEdgeLightingFrame
import com.example.ui.screens.AdminCenterScreen
import com.example.ui.screens.AgentsScreen
import com.example.ui.screens.AuthOnboardingScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.CodingStudioScreen
import com.example.ui.screens.CommunicationScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MemoryBrainScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SubscriptionsScreen
import com.example.ui.screens.ToolsHubScreen
import com.example.ui.screens.VoiceOsScreen
import com.example.ui.theme.AlabasterCanvas
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.FaGlyphs
import com.example.ui.theme.FaIcon
import com.example.ui.theme.FaStyle
import com.example.ui.theme.FrostBorder
import com.example.ui.theme.HairlineDivider
import com.example.ui.theme.IceBlueDeep
import com.example.ui.theme.IndigoMuted
import com.example.ui.theme.LavenderGlow
import com.example.ui.theme.NishaSoftAuraGradient
import com.example.ui.theme.NishaTheme
import com.example.ui.theme.ObsidianSlate
import com.example.ui.theme.PearlWhite
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.AgentsStudioViewModel
import com.example.viewmodel.NishaMainViewModel
import kotlinx.coroutines.launch

object NishaRoutes {
    const val HOME = "home"
    const val VOICE = "voice"
    const val CHAT = "chat"
    const val AGENTS = "agents"
    const val STUDIO = "studio"
    const val BRAIN = "brain"
    const val COMM = "comm"
    const val TOOLS = "tools"
    const val SUBSCRIPTIONS = "subscriptions"
    const val SETTINGS = "settings"
    const val ADMIN = "admin"
}

class MainActivity : ComponentActivity() {

    private var pendingLaunchRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeIntentExtras(intent)

        val container = NishaAppContainer(applicationContext)

        setContent {
            val factory = remember(container) {
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return when {
                            modelClass.isAssignableFrom(NishaMainViewModel::class.java) ->
                                NishaMainViewModel(container) as T
                            modelClass.isAssignableFrom(AgentsStudioViewModel::class.java) ->
                                AgentsStudioViewModel(container) as T
                            modelClass.isAssignableFrom(AdminViewModel::class.java) ->
                                AdminViewModel(container) as T
                            else -> error("Unknown ViewModel class: ${modelClass.name}")
                        }
                    }
                }
            }

            val mainVm: NishaMainViewModel = viewModel(factory = factory)
            val studioVm: AgentsStudioViewModel = viewModel(factory = factory)
            val adminVm: AdminViewModel = viewModel(factory = factory)

            NishaTheme {
                NishaRootApp(
                    container = container,
                    mainVm = mainVm,
                    studioVm = studioVm,
                    adminVm = adminVm,
                    pendingLaunchRoute = pendingLaunchRoute,
                    onConsumedLaunchRoute = { pendingLaunchRoute = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeIntentExtras(intent)
    }

    private fun consumeIntentExtras(intent: Intent?) {
        if (intent == null) return
        when {
            intent.getBooleanExtra(NishaAssistantService.EXTRA_OPEN_VOICE, false) -> {
                pendingLaunchRoute = NishaRoutes.VOICE
            }
            intent.getBooleanExtra("extra_open_orb_settings", false) -> {
                pendingLaunchRoute = NishaRoutes.SETTINGS
            }
        }
    }
}

@Composable
private fun NishaRootApp(
    container: NishaAppContainer,
    mainVm: NishaMainViewModel,
    studioVm: AgentsStudioViewModel,
    adminVm: AdminViewModel,
    pendingLaunchRoute: String?,
    onConsumedLaunchRoute: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val session by mainVm.session.collectAsStateWithLifecycle()
    val settings by mainVm.settings.collectAsStateWithLifecycle()
    val authLoading by mainVm.authLoading.collectAsStateWithLifecycle()
    val authMessage by mainVm.authMessage.collectAsStateWithLifecycle()
    val hasApiKey by mainVm.hasApiKey.collectAsStateWithLifecycle()
    val maskedApiKey by mainVm.maskedApiKey.collectAsStateWithLifecycle()
    val apiKeyStatus by mainVm.apiKeyStatus.collectAsStateWithLifecycle()
    val weather by mainVm.weather.collectAsStateWithLifecycle()
    val battery by mainVm.battery.collectAsStateWithLifecycle()
    val announcements by mainVm.announcements.collectAsStateWithLifecycle()
    val chatSessions by mainVm.chatSessions.collectAsStateWithLifecycle()
    val activeSessionId by mainVm.activeSessionId.collectAsStateWithLifecycle()
    val activeMessages by mainVm.activeMessages.collectAsStateWithLifecycle()
    val chatGenerating by mainVm.chatGenerating.collectAsStateWithLifecycle()
    val chatError by mainVm.chatError.collectAsStateWithLifecycle()
    val pendingAttachment by mainVm.pendingAttachment.collectAsStateWithLifecycle()
    val voiceMacros by mainVm.voiceMacros.collectAsStateWithLifecycle()
    val voiceHistory by mainVm.voiceHistory.collectAsStateWithLifecycle()
    val lastVoiceResponse by mainVm.lastVoiceResponse.collectAsStateWithLifecycle()
    val pendingAction by mainVm.pendingAction.collectAsStateWithLifecycle()
    val snackbarMessage by mainVm.snackbarMessage.collectAsStateWithLifecycle()

    val coreState by AssistantRuntimeCoordinator.coreState.collectAsStateWithLifecycle()
    val voiceAmplitude by AssistantRuntimeCoordinator.voiceAmplitude.collectAsStateWithLifecycle()
    val statusSubtitle by AssistantRuntimeCoordinator.statusSubtitle.collectAsStateWithLifecycle()
    val serviceRunning by AssistantRuntimeCoordinator.serviceRunning.collectAsStateWithLifecycle()
    val voiceTriggerNonce by AssistantRuntimeCoordinator.voiceTriggerNonce.collectAsStateWithLifecycle()

    val agents by studioVm.agents.collectAsStateWithLifecycle()
    val agentTasks by studioVm.agentTasks.collectAsStateWithLifecycle()
    val liveSubtasks by studioVm.liveSubtasks.collectAsStateWithLifecycle()
    val orchestrating by studioVm.orchestrating.collectAsStateWithLifecycle()
    val orchestrationResult by studioVm.orchestrationResult.collectAsStateWithLifecycle()
    val pendingSensitivePrompt by studioVm.pendingSensitivePrompt.collectAsStateWithLifecycle()
    val memories by studioVm.memories.collectAsStateWithLifecycle()
    val codingProjects by studioVm.codingProjects.collectAsStateWithLifecycle()
    val codingAiOutput by studioVm.codingAiOutput.collectAsStateWithLifecycle()
    val codingLoading by studioVm.codingLoading.collectAsStateWithLifecycle()
    val journalEntries by studioVm.journalEntries.collectAsStateWithLifecycle()
    val notes by studioVm.notes.collectAsStateWithLifecycle()
    val reminders by studioVm.reminders.collectAsStateWithLifecycle()
    val watchlist by studioVm.watchlist.collectAsStateWithLifecycle()
    val automations by studioVm.automations.collectAsStateWithLifecycle()
    val toolAiResult by studioVm.toolAiResult.collectAsStateWithLifecycle()
    val toolLoading by studioVm.toolLoading.collectAsStateWithLifecycle()
    val statusNotice by studioVm.statusNotice.collectAsStateWithLifecycle()

    val isAdminVerified by adminVm.isAdminVerified.collectAsStateWithLifecycle()
    val checkingAdmin by adminVm.checkingAdmin.collectAsStateWithLifecycle()
    val adminUsers by adminVm.users.collectAsStateWithLifecycle()
    val adminLicenses by adminVm.licenses.collectAsStateWithLifecycle()
    val adminAuditLogs by adminVm.auditLogs.collectAsStateWithLifecycle()
    val appConfig by adminVm.appConfig.collectAsStateWithLifecycle()
    val explorerDocs by adminVm.explorerDocs.collectAsStateWithLifecycle()
    val adminFeedback by adminVm.adminFeedback.collectAsStateWithLifecycle()

    val voiceController = remember(context) {
        lateinit var ctrl: NishaVoiceController
        ctrl = NishaVoiceController(
            context = context,
            onTranscriptReady = { transcript ->
                mainVm.handleVoiceInput(transcript) { spoken ->
                    ctrl.speak(spoken)
                }
            },
            onError = { msg ->
                AssistantRuntimeCoordinator.setCoreState(AiCoreState.IDLE, msg)
            }
        )
        ctrl
    }

    DisposableEffect(voiceController) {
        onDispose { voiceController.release() }
    }

    LaunchedEffect(settings.voiceLanguage, settings.voiceSpeechRate, settings.voicePitch) {
        voiceController.applyVoiceConfig(
            languageMode = settings.voiceLanguage,
            rate = settings.voiceSpeechRate,
            voicePitch = settings.voicePitch
        )
    }

    LaunchedEffect(settings.orbSystemOverlayEnabled, settings.orbSizeDp) {
        if (settings.orbSystemOverlayEnabled && container.overlayManager.canDrawOverlays()) {
            container.overlayManager.showOverlay(settings.orbSizeDp.toInt())
        } else {
            container.overlayManager.hideOverlay()
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            mainVm.clearSnackbar()
        }
    }

    // Strict Auth Gate: unauthenticated users stay cleanly on AuthOnboardingScreen
    if (session is NishaSession.Unauthenticated) {
        AuthOnboardingScreen(
            onboardingCompleted = settings.onboardingCompleted,
            authLoading = authLoading,
            authMessage = authMessage,
            hasApiKey = hasApiKey,
            apiKeyStatus = apiKeyStatus,
            onCompleteOnboarding = mainVm::completeOnboarding,
            onGoogleSignIn = mainVm::signInWithGoogle,
            onSignUpLocal = mainVm::signUpLocal,
            onSignInLocal = mainVm::signInLocal,
            onResetLocalPassword = mainVm::resetLocalPassword,
            onSaveAndVerifyKey = mainVm::saveAndVerifyGeminiKey
        )
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: NishaRoutes.HOME
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    LaunchedEffect(pendingLaunchRoute) {
        pendingLaunchRoute?.let { route ->
            navController.navigate(route) { launchSingleTop = true }
            onConsumedLaunchRoute()
        }
    }

    LaunchedEffect(voiceTriggerNonce) {
        if (voiceTriggerNonce > 0L) {
            navController.navigate(NishaRoutes.VOICE) { launchSingleTop = true }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                voiceController.startListening(settings.voiceLanguage)
            }
        }
    }

    val displayName = when (val s = session) {
        is NishaSession.FirebaseGoogle -> s.displayName
        is NishaSession.LocalVault -> s.fullName
        NishaSession.Unauthenticated -> "Guest"
    }

    val drawerItems = remember {
        listOf(
            Triple(NishaRoutes.HOME, "Maya 3D Home", FaGlyphs.HOUSE),
            Triple(NishaRoutes.VOICE, "Nisha Voice OS", FaGlyphs.MICROPHONE),
            Triple(NishaRoutes.CHAT, "AI Chat", FaGlyphs.COMMENTS),
            Triple(NishaRoutes.AGENTS, "5-Agent Orchestrator", FaGlyphs.ROBOT),
            Triple(NishaRoutes.STUDIO, "Coding Studio", FaGlyphs.CODE),
            Triple(NishaRoutes.BRAIN, "Knowledge Brain", FaGlyphs.BRAIN),
            Triple(NishaRoutes.COMM, "WhatsApp AI & Comms", FaGlyphs.COMMENT_DOTS),
            Triple(NishaRoutes.TOOLS, "Productivity & Tools", FaGlyphs.TOOLBOX),
            Triple(NishaRoutes.SUBSCRIPTIONS, "Plans & Billing", FaGlyphs.CROWN),
            Triple(NishaRoutes.SETTINGS, "Settings (20 Sections)", FaGlyphs.GEAR),
            Triple(NishaRoutes.ADMIN, "VIP 10D Admin Center", FaGlyphs.USER_SHIELD)
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = PearlWhite,
                modifier = Modifier.width(292.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    Text("NISHA AI", style = MaterialTheme.typography.headlineMedium, color = ElectricViolet, fontWeight = FontWeight.Bold)
                    Text("THE DARK AI • Luxury White", style = MaterialTheme.typography.labelSmall, color = IndigoMuted)
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = HairlineDivider)
                    Spacer(Modifier.height(10.dp))
                    drawerItems.forEach { (route, label, glyph) ->
                        val selected = currentRoute == route
                        NavigationDrawerItem(
                            selected = selected,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(route) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                FaIcon(
                                    glyph = glyph,
                                    contentDescription = label,
                                    color = if (selected) ElectricViolet else IndigoMuted,
                                    size = 15.sp
                                )
                            },
                            label = { Text(label) },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = LavenderGlow,
                                selectedTextColor = ElectricViolet
                            ),
                            modifier = Modifier.testTag("drawer_item_$route")
                        )
                    }
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NishaSoftAuraGradient)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                NavHost(
                    navController = navController,
                    startDestination = NishaRoutes.HOME,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(NishaRoutes.HOME) {
                        HomeScreen(
                            displayName = displayName,
                            coreState = coreState,
                            amplitude = voiceAmplitude,
                            statusSubtitle = statusSubtitle,
                            hasApiKey = hasApiKey,
                            battery = battery,
                            weather = weather,
                            settings = settings,
                            recentChats = chatSessions,
                            recentTasks = agentTasks,
                            announcements = announcements,
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onOpenProfile = { navController.navigate(NishaRoutes.SETTINGS) },
                            onTapMayaCore = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    if (coreState == AiCoreState.LISTENING) {
                                        voiceController.stopListening()
                                    } else {
                                        voiceController.startListening(settings.voiceLanguage)
                                    }
                                } else {
                                    navController.navigate(NishaRoutes.VOICE)
                                }
                            },
                            onNavigateVoice = { navController.navigate(NishaRoutes.VOICE) },
                            onNavigateChat = { chatId ->
                                if (chatId != null) mainVm.selectChatSession(chatId)
                                navController.navigate(NishaRoutes.CHAT)
                            },
                            onNavigateJournal = { navController.navigate(NishaRoutes.TOOLS) },
                            onNavigateMusic = {
                                val musicIntent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                runCatching { context.startActivity(musicIntent) }
                                    .onFailure { navController.navigate(NishaRoutes.TOOLS) }
                            },
                            onNavigateAgents = { navController.navigate(NishaRoutes.AGENTS) },
                            onNavigateTools = { navController.navigate(NishaRoutes.TOOLS) },
                            onRefreshBattery = mainVm::refreshBattery,
                            onLoadWeather = mainVm::loadWeather,
                            onUpdateWidgets = mainVm::updateHomeWidgets,
                            onQuickPrompt = { prompt ->
                                mainVm.startNewChat()
                                navController.navigate(NishaRoutes.CHAT)
                                mainVm.sendChatMessage(prompt)
                            }
                        )
                    }

                    composable(NishaRoutes.VOICE) {
                        VoiceOsScreen(
                            coreState = coreState,
                            serviceRunning = serviceRunning,
                            settings = settings,
                            lastResponse = lastVoiceResponse,
                            voiceHistory = voiceHistory,
                            voiceMacros = voiceMacros,
                            deviceActionRouter = container.deviceActionRouter,
                            onStartListening = { voiceController.startListening(settings.voiceLanguage) },
                            onStopListening = { voiceController.stopListening() },
                            onStopSpeaking = { voiceController.stopSpeaking() },
                            onSubmitTextVoiceCommand = { cmd ->
                                mainVm.handleVoiceInput(cmd) { spoken -> voiceController.speak(spoken) }
                            },
                            onUpdateVoiceSettings = mainVm::updateVoiceSettings,
                            onAddMacro = mainVm::addVoiceMacro,
                            onDeleteMacro = mainVm::deleteVoiceMacro,
                            onProposeAction = mainVm::proposeSensitiveAction
                        )
                    }

                    composable(NishaRoutes.CHAT) {
                        ChatScreen(
                            sessions = chatSessions,
                            activeSessionId = activeSessionId,
                            messages = activeMessages,
                            generating = chatGenerating,
                            chatError = chatError,
                            hasApiKey = hasApiKey,
                            settings = settings,
                            pendingAttachment = pendingAttachment,
                            onSelectSession = mainVm::selectChatSession,
                            onNewChat = mainVm::startNewChat,
                            onRenameChat = mainVm::renameChatSession,
                            onDeleteChat = mainVm::deleteChatSession,
                            onSelectModel = mainVm::selectModel,
                            onSetAttachment = mainVm::setPendingAttachment,
                            onSendMessage = mainVm::sendChatMessage,
                            onRegenerate = mainVm::regenerateLastResponse,
                            onStopGeneration = mainVm::stopGeneration,
                            onOpenApiKeySettings = { navController.navigate(NishaRoutes.SETTINGS) }
                        )
                    }

                    composable(NishaRoutes.AGENTS) {
                        AgentsScreen(
                            agents = agents,
                            agentTasks = agentTasks,
                            liveSubtasks = liveSubtasks,
                            orchestrating = orchestrating,
                            orchestrationResult = orchestrationResult,
                            pendingSensitivePrompt = pendingSensitivePrompt,
                            statusNotice = statusNotice,
                            onSaveAgent = studioVm::saveAgent,
                            onCreateCustomAgent = studioVm::createCustomAgent,
                            onDeleteAgent = studioVm::deleteAgentSafely,
                            onRunOrchestration = { prompt, ids -> studioVm.runOrchestration(prompt, ids) },
                            onConfirmSensitiveOrchestration = studioVm::confirmSensitiveOrchestration,
                            onCancelSensitiveOrchestration = studioVm::cancelSensitiveOrchestration,
                            onClearNotice = studioVm::clearNotice
                        )
                    }

                    composable(NishaRoutes.STUDIO) {
                        CodingStudioScreen(
                            projects = codingProjects,
                            aiOutput = codingAiOutput,
                            loading = codingLoading,
                            onSaveProject = studioVm::saveCodingProject,
                            onCreateProject = studioVm::createCodingProject,
                            onRunAiAssist = studioVm::runCodingAiAssist
                        )
                    }

                    composable(NishaRoutes.BRAIN) {
                        MemoryBrainScreen(
                            memories = memories,
                            memoryEnabled = settings.memoryEnabled,
                            isCloudSignedIn = session is NishaSession.FirebaseGoogle,
                            onToggleMemory = mainVm::setMemoryEnabled,
                            onSaveMemory = studioVm::saveMemory,
                            onDeleteMemory = studioVm::deleteMemory,
                            onExportJson = studioVm::exportMemoriesJson,
                            onImportJson = studioVm::importMemoriesJson
                        )
                    }

                    composable(NishaRoutes.COMM) {
                        CommunicationScreen(
                            aiResult = toolAiResult,
                            loading = toolLoading,
                            onRunAiDraft = { role, prompt -> studioVm.runAiToolPrompt(role, prompt) },
                            onProposeSensitiveAction = mainVm::proposeSensitiveAction
                        )
                    }

                    composable(NishaRoutes.TOOLS) {
                        ToolsHubScreen(
                            journalEntries = journalEntries,
                            notes = notes,
                            reminders = reminders,
                            watchlist = watchlist,
                            automations = automations,
                            aiResult = toolAiResult,
                            loading = toolLoading,
                            onRunAiTool = studioVm::runAiToolPrompt,
                            onAddJournal = studioVm::addJournalEntry,
                            onDeleteJournal = studioVm::deleteJournalEntry,
                            onAddNote = studioVm::addNote,
                            onDeleteNote = studioVm::deleteNote,
                            onAddReminder = studioVm::addReminder,
                            onToggleReminder = studioVm::toggleReminder,
                            onDeleteReminder = studioVm::deleteReminder,
                            onSetTimerAction = mainVm::proposeSensitiveAction,
                            onAddWatchlist = studioVm::addWatchlistItem,
                            onDeleteWatchlist = studioVm::deleteWatchlistItem,
                            onAddAutomation = studioVm::addAutomationRule,
                            onToggleAutomation = studioVm::toggleAutomationRule,
                            onDeleteAutomation = studioVm::deleteAutomationRule,
                            onExportBackupJson = studioVm::exportMemoriesJson,
                            onImportBackupJson = studioVm::importMemoriesJson
                        )
                    }

                    composable(NishaRoutes.SUBSCRIPTIONS) {
                        SubscriptionsScreen(
                            currentTier = settings.subscriptionTier,
                            isCloudSignedIn = session is NishaSession.FirebaseGoogle,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(NishaRoutes.SETTINGS) {
                        SettingsScreen(
                            session = session,
                            settings = settings,
                            hasApiKey = hasApiKey,
                            maskedApiKey = maskedApiKey,
                            apiKeyStatus = apiKeyStatus,
                            isServiceRunning = serviceRunning,
                            appConfig = appConfig,
                            onSelectModel = mainVm::selectModel,
                            onSaveVerifyKey = mainVm::saveAndVerifyGeminiKey,
                            onSaveKeyWithoutVerify = mainVm::saveGeminiKeyWithoutVerify,
                            onClearKey = mainVm::clearGeminiKey,
                            onUpdateOrb = { en, ov, cmp, sz ->
                                mainVm.updateOrbSettings(enabled = en, systemOverlay = ov, compact = cmp, sizeDp = sz)
                            },
                            onUpdateEdgeLighting = mainVm::updateEdgeLightingSettings,
                            onUpdateVoiceLanguage = { lang -> mainVm.updateVoiceSettings(language = lang) },
                            onToggleMemory = mainVm::setMemoryEnabled,
                            onVerifyLocalEmail = mainVm::verifyLocalEmail,
                            onUpdateLocalProfile = mainVm::updateLocalProfile,
                            onSignOut = mainVm::signOut,
                            onDeleteAccount = mainVm::deleteAccount,
                            onOpenOverlaySettings = { container.deviceActionRouter.openOverlayPermissionSettings() },
                            onOpenAccessibilitySettings = { container.deviceActionRouter.openAccessibilitySettings() },
                            onOpenSubscriptions = { navController.navigate(NishaRoutes.SUBSCRIPTIONS) },
                            onOpenAgents = { navController.navigate(NishaRoutes.AGENTS) },
                            onOpenBrain = { navController.navigate(NishaRoutes.BRAIN) },
                            onOpenVoiceOs = { navController.navigate(NishaRoutes.VOICE) },
                            onOpenAdminCenter = { navController.navigate(NishaRoutes.ADMIN) },
                            onCheckExternalReleaseUrl = { url -> container.deviceActionRouter.openExternalUrl(url) }
                        )
                    }

                    composable(NishaRoutes.ADMIN) {
                        AdminCenterScreen(
                            session = session,
                            isAdminVerified = isAdminVerified,
                            checkingAdmin = checkingAdmin,
                            users = adminUsers,
                            licenses = adminLicenses,
                            auditLogs = adminAuditLogs,
                            appConfig = appConfig,
                            explorerDocs = explorerDocs,
                            localAgents = agents,
                            localAgentTasks = agentTasks,
                            feedback = adminFeedback,
                            onVerifyAndLoad = adminVm::verifyAndLoadAdminCenter,
                            onBrowseCollection = adminVm::browseCollection,
                            onUpdateUserAccess = adminVm::updateUserAccess,
                            onSaveAppConfig = adminVm::saveAppConfig,
                            onSendAnnouncement = adminVm::sendAnnouncement,
                            onUpsertLicense = adminVm::upsertLicense,
                            onClearFeedback = adminVm::clearFeedback,
                            onOpenUrl = { url -> container.deviceActionRouter.openExternalUrl(url) }
                        )
                    }
                }
            }

            // Floating Luxury White Bottom Navigation Bar
            NishaFloatingBottomBar(
                currentRoute = currentRoute,
                onSelectRoute = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )

            // Draggable Floating 10D AI Orb near bottom-center (independent of Edge Lighting)
            if (settings.orbEnabled) {
                Floating10DOrb(
                    coreState = coreState,
                    amplitude = voiceAmplitude,
                    sizeDp = settings.orbSizeDp,
                    compactMode = settings.orbCompactMode,
                    offsetX = settings.orbOffsetX,
                    offsetY = settings.orbOffsetY,
                    onDragDelta = { dx, dy ->
                        mainVm.updateOrbSettings(
                            offsetX = (settings.orbOffsetX + dx).coerceIn(-420f, 420f),
                            offsetY = (settings.orbOffsetY + dy).coerceIn(-900f, 120f)
                        )
                    },
                    onTapOrb = {
                        navController.navigate(NishaRoutes.VOICE) { launchSingleTop = true }
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            voiceController.startListening(settings.voiceLanguage)
                        }
                    },
                    onLongPressOrb = {
                        navController.navigate(NishaRoutes.SETTINGS) { launchSingleTop = true }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 84.dp)
                )
            }

            // Independent Four-Edge Lighting Overlay (non-blocking)
            FourEdgeLightingFrame(
                enabled = settings.edgeLightingEnabled,
                style = settings.edgeLightingStyle,
                palette = settings.edgeLightingPalette,
                brightness = settings.edgeLightingBrightness,
                speed = settings.edgeLightingSpeed,
                modifier = Modifier.fillMaxSize()
            )

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 8.dp)
            )
        }
    }

    // Global Confirmation Dialog for Sensitive Device / Messaging / Timer Intents
    pendingAction?.let { action ->
        AlertDialog(
            onDismissRequest = mainVm::dismissPendingAction,
            title = { Text(action.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(action.description, style = MaterialTheme.typography.bodyMedium, color = ObsidianSlate)
                    if (action.target.isNotBlank()) {
                        Text("Target: ${action.target}", style = MaterialTheme.typography.labelLarge, color = ElectricViolet)
                    }
                    if (action.payload.isNotBlank()) {
                        Text("Payload: ${action.payload}", style = MaterialTheme.typography.bodyMedium, color = IndigoMuted)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = mainVm::confirmPendingAction,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    modifier = Modifier.testTag("confirm_sensitive_action_btn")
                ) { Text("Confirm & Launch") }
            },
            dismissButton = {
                TextButton(onClick = mainVm::dismissPendingAction) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun NishaFloatingBottomBar(
    currentRoute: String,
    onSelectRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        Triple(NishaRoutes.HOME, "Home", FaGlyphs.HOUSE),
        Triple(NishaRoutes.VOICE, "Voice", FaGlyphs.MICROPHONE),
        Triple(NishaRoutes.CHAT, "Chat", FaGlyphs.COMMENTS),
        Triple(NishaRoutes.AGENTS, "Agents", FaGlyphs.ROBOT),
        Triple(NishaRoutes.TOOLS, "Tools", FaGlyphs.TOOLBOX)
    )
    Surface(
        modifier = modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(28.dp), ambientColor = ElectricViolet.copy(alpha = 0.16f)),
        shape = RoundedCornerShape(28.dp),
        color = PearlWhite.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, FrostBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { (route, label, glyph) ->
                val selected = currentRoute == route
                Surface(
                    onClick = { onSelectRoute(route) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (selected) LavenderGlow else PearlWhite.copy(alpha = 0f),
                    modifier = Modifier
                        .height(48.dp)
                        .weight(1f)
                        .testTag("bottom_nav_$route")
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        FaIcon(
                            glyph = glyph,
                            contentDescription = label,
                            color = if (selected) ElectricViolet else IndigoMuted,
                            size = 15.sp,
                            style = FaStyle.SOLID
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) ElectricViolet else IndigoMuted,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
