package com.example.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AiCoreState(val label: String) {
    IDLE("Ready"),
    LISTENING("Listening"),
    THINKING("Thinking"),
    SPEAKING("Speaking")
}

object AssistantRuntimeCoordinator {
    private val _coreState = MutableStateFlow(AiCoreState.IDLE)
    val coreState: StateFlow<AiCoreState> = _coreState.asStateFlow()

    private val _serviceRunning = MutableStateFlow(false)
    val serviceRunning: StateFlow<Boolean> = _serviceRunning.asStateFlow()

    private val _voiceAmplitude = MutableStateFlow(0f)
    val voiceAmplitude: StateFlow<Float> = _voiceAmplitude.asStateFlow()

    private val _statusSubtitle = MutableStateFlow("Tap the Maya Core or 10D Orb to engage")
    val statusSubtitle: StateFlow<String> = _statusSubtitle.asStateFlow()

    private val _voiceTriggerNonce = MutableStateFlow(0L)
    val voiceTriggerNonce: StateFlow<Long> = _voiceTriggerNonce.asStateFlow()

    fun setCoreState(state: AiCoreState, subtitle: String? = null) {
        _coreState.value = state
        if (subtitle != null) _statusSubtitle.value = subtitle
    }

    fun setServiceRunning(running: Boolean) {
        _serviceRunning.value = running
    }

    fun updateAmplitude(rmsdB: Float) {
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        _voiceAmplitude.value = normalized
    }

    fun triggerVoiceActivation() {
        _voiceTriggerNonce.value = System.currentTimeMillis()
    }
}
