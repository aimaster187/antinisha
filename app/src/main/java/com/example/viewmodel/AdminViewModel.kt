package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FsAppConfig
import com.example.data.firebase.FsAuditLog
import com.example.data.firebase.FsLicenseRecord
import com.example.data.firebase.FsUserProfile
import com.example.di.NishaAppContainer
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminViewModel(
    private val container: NishaAppContainer
) : ViewModel() {

    private val adminRepo = container.adminRepository

    private val _isAdminVerified = MutableStateFlow(false)
    val isAdminVerified: StateFlow<Boolean> = _isAdminVerified.asStateFlow()

    private val _checkingAdmin = MutableStateFlow(false)
    val checkingAdmin: StateFlow<Boolean> = _checkingAdmin.asStateFlow()

    private val _users = MutableStateFlow<List<FsUserProfile>>(emptyList())
    val users: StateFlow<List<FsUserProfile>> = _users.asStateFlow()

    private val _licenses = MutableStateFlow<List<FsLicenseRecord>>(emptyList())
    val licenses: StateFlow<List<FsLicenseRecord>> = _licenses.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<FsAuditLog>>(emptyList())
    val auditLogs: StateFlow<List<FsAuditLog>> = _auditLogs.asStateFlow()

    private val _appConfig = MutableStateFlow(FsAppConfig())
    val appConfig: StateFlow<FsAppConfig> = _appConfig.asStateFlow()

    private val _explorerDocs = MutableStateFlow<List<Pair<String, Map<String, Any?>>>>(emptyList())
    val explorerDocs: StateFlow<List<Pair<String, Map<String, Any?>>>> = _explorerDocs.asStateFlow()

    private val _adminFeedback = MutableStateFlow<String?>(null)
    val adminFeedback: StateFlow<String?> = _adminFeedback.asStateFlow()

    fun verifyAndLoadAdminCenter() {
        viewModelScope.launch {
            if (FirebaseAuth.getInstance().currentUser == null) {
                _isAdminVerified.value = false
                return@launch
            }
            _checkingAdmin.value = true
            adminRepo.verifyAdminStatus()
                .onSuccess { verified ->
                    _isAdminVerified.value = verified
                    if (verified) refreshAllAdminData()
                }
                .onFailure {
                    _isAdminVerified.value = false
                    _adminFeedback.value = "Admin verification failed: ${it.localizedMessage}"
                }
            _checkingAdmin.value = false
        }
    }

    fun refreshAllAdminData() {
        viewModelScope.launch {
            adminRepo.listAllUsers().onSuccess { _users.value = it }
            adminRepo.listLicenses().onSuccess { _licenses.value = it }
            adminRepo.listAuditLogs().onSuccess { _auditLogs.value = it }
            container.firestoreSync.getAppConfig().onSuccess { cfg ->
                if (cfg != null) _appConfig.value = cfg
            }
        }
    }

    fun browseCollection(path: String) {
        viewModelScope.launch {
            adminRepo.browseCollection(path)
                .onSuccess { _explorerDocs.value = it }
                .onFailure { _adminFeedback.value = it.localizedMessage ?: "Explorer read failed" }
        }
    }

    fun updateUserAccess(userId: String, tier: String, status: String) {
        viewModelScope.launch {
            adminRepo.updateUserTierAndStatus(userId, tier, status)
                .onSuccess {
                    _adminFeedback.value = "Updated $userId -> $tier / $status"
                    refreshAllAdminData()
                }
                .onFailure { _adminFeedback.value = it.localizedMessage }
        }
    }

    fun saveAppConfig(
        maintenanceMode: Boolean,
        latestVersionName: String,
        latestVersionCode: Long,
        defaultModel: String,
        banner: String,
        releaseNotes: String
    ) {
        viewModelScope.launch {
            adminRepo.saveAppConfig(
                maintenanceMode = maintenanceMode,
                latestVersionName = latestVersionName,
                latestVersionCode = latestVersionCode,
                defaultModel = defaultModel,
                announcementBanner = banner,
                releaseNotes = releaseNotes
            ).onSuccess {
                _adminFeedback.value = "Global configuration saved and audited."
                refreshAllAdminData()
            }.onFailure { _adminFeedback.value = it.localizedMessage }
        }
    }

    fun sendAnnouncement(
        title: String,
        body: String,
        targetMode: String,
        targetUsername: String,
        targetUid: String
    ) {
        viewModelScope.launch {
            adminRepo.sendAnnouncement(title, body, targetMode, targetUsername, targetUid)
                .onSuccess {
                    _adminFeedback.value = "Announcement delivered ($it)."
                    refreshAllAdminData()
                }
                .onFailure { _adminFeedback.value = it.localizedMessage }
        }
    }

    fun upsertLicense(
        licenseId: String,
        userId: String,
        plan: String,
        status: String,
        source: String,
        expiresAtEpochMs: Long
    ) {
        viewModelScope.launch {
            adminRepo.upsertLicense(licenseId, userId, plan, status, source, expiresAtEpochMs)
                .onSuccess {
                    _adminFeedback.value = "License saved."
                    refreshAllAdminData()
                }
                .onFailure { _adminFeedback.value = it.localizedMessage }
        }
    }

    fun clearFeedback() {
        _adminFeedback.value = null
    }
}
