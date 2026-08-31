package com.pixelpal.app.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelpal.app.data.local.datastore.PreferencesManager
import com.pixelpal.app.data.remote.firebase.FirebaseAuthManager
import com.pixelpal.app.data.remote.firebase.FirestoreSyncEngine
import com.pixelpal.app.worker.WorkerScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val authManager: FirebaseAuthManager,
    private val syncEngine: FirestoreSyncEngine,
    private val workerScheduler: WorkerScheduler
) : ViewModel() {

    val userName: StateFlow<String> = preferencesManager.userName
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    val userEmail: StateFlow<String> = preferencesManager.userEmail
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    val avatarSeed: StateFlow<String> = preferencesManager.avatarSeed
        .stateIn(viewModelScope, SharingStarted.Lazily, "pixelpal")

    val isUserLoggedIn: Boolean get() = syncEngine.isUserLoggedIn
    val isAnonymousUser: Boolean get() = syncEngine.isAnonymousUser
    val currentUserEmail: String? get() = syncEngine.currentUserEmail

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    /** Reads the currently stored values directly (DataStore) — not the StateFlow initial value. */
    suspend fun currentUserName(): String = preferencesManager.getUserName()

    suspend fun currentUserEmail(): String = preferencesManager.getUserEmail()

    suspend fun currentAvatarSeed(): String = preferencesManager.getAvatarSeed()

    fun saveProfile(name: String, email: String, avatarSeed: String) {
        viewModelScope.launch {
            preferencesManager.setUserName(name.trim())
            preferencesManager.setUserEmail(email.trim())
            preferencesManager.setAvatarSeed(avatarSeed)
        }
    }

    fun triggerCloudSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            workerScheduler.triggerImmediateSync()
            kotlinx.coroutines.delay(1500)
            _isSyncing.value = false
        }
    }

    fun signOut() {
        authManager.signOut()
    }

    /** Cycles to the next deterministic avatar style for this user. */
    fun nextAvatarSeed(currentSeed: String): String {
        return "$currentSeed-variant"
    }
}

