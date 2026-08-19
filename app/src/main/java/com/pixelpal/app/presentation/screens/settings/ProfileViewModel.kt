package com.pixelpal.app.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelpal.app.data.local.datastore.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val userName: StateFlow<String> = preferencesManager.userName
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    val userEmail: StateFlow<String> = preferencesManager.userEmail
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    val avatarSeed: StateFlow<String> = preferencesManager.avatarSeed
        .stateIn(viewModelScope, SharingStarted.Lazily, "pixelpal")

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

    /** Cycles to the next deterministic avatar style for this user. */
    fun nextAvatarSeed(currentSeed: String): String {
        return "$currentSeed-variant"
    }
}
