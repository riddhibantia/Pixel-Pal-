package com.pixelpal.app.presentation.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelpal.app.data.remote.firebase.AuthState
import com.pixelpal.app.data.remote.firebase.FirebaseAuthManager
import com.pixelpal.app.data.remote.firebase.FirestoreSyncCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isSignUpMode: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: FirebaseAuthManager,
    private val syncCoordinator: FirestoreSyncCoordinator
) : ViewModel() {

    val authState: StateFlow<AuthState> = authManager.authStateFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, AuthState.Idle)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** True while a post-login cloud restore (pull → push) is in flight. */
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    /**
     * True once the post-login sync ATTEMPT finished (success or failure).
     * Navigation waits for this — never for auth alone — so Home renders
     * after Room holds the user's companion/metrics, fixing the race where
     * Firestore was still restoring when the UI first read Room.
     */
    private val _isDataReady = MutableStateFlow(false)
    val isDataReady: StateFlow<Boolean> = _isDataReady.asStateFlow()

    /**
     * Firestore/application-init failure AFTER successful authentication.
     * Kept separate from [AuthUiState.errorMessage] (auth failures) so the
     * UI can tell "login worked, data load failed" apart from "login failed".
     */
    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun toggleAuthMode() {
        _uiState.update {
            it.copy(
                isSignUpMode = !it.isSignUpMode,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun submitEmailAuth() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter both email and password") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = if (state.isSignUpMode) {
                authManager.signUpWithEmail(state.email, state.password)
            } else {
                authManager.signInWithEmail(state.email, state.password)
            }

            result.fold(
                onSuccess = { user ->
                    // Auth done — now restore users/{uid} into Room BEFORE the
                    // UI navigates. Awaiting here (not fire-and-forget) closes
                    // the race where Home read an empty Room mid-restore.
                    timber.log.Timber.d("[AUTH] Login successful uid=%s, restoring data", user.uid)
                    runPostAuthSync()
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Authentication failed"
                        )
                    }
                }
            )
        }
    }

    fun signInWithGoogle(activityContext: android.content.Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            _syncError.value = null
            _isDataReady.value = false
            val result = authManager.signInWithGoogle(activityContext)
            result.fold(
                onSuccess = { user ->
                    timber.log.Timber.d("[AUTH] Google login successful uid=%s, restoring data", user.uid)
                    runPostAuthSync()
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    // User cancellation is not an error to display loudly.
                    val cancelled = error is java.util.concurrent.CancellationException ||
                        error is androidx.credentials.exceptions.GetCredentialCancellationException
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = if (cancelled) null
                            else error.localizedMessage ?: "Google sign-in failed"
                        )
                    }
                }
            )
        }
    }

    fun signInAsGuest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authManager.signInAnonymously()
            result.fold(
                onSuccess = { user ->
                    timber.log.Timber.d("[AUTH] Guest login successful uid=%s", user.uid)
                    runPostAuthSync()
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Guest login failed"
                        )
                    }
                }
            )
        }
    }

    /**
     * Pulls users/{uid}/companion/primary + metrics/bond (+tasks/reminders)
     * into Room and rebinds realtime listeners. Auth is already complete when
     * this runs, so the uid is guaranteed available — no null-uid requests.
     * Sync failure does NOT log the user out; it surfaces via [syncError]
     * while [isDataReady] still unblocks navigation.
     */
    private suspend fun runPostAuthSync() {
        _isSyncing.value = true
        _syncError.value = null
        _uiState.update { it.copy(isLoading = true) }
        try {
            val result = syncCoordinator.onUserAuthenticated()
            result.fold(
                onSuccess = {
                    timber.log.Timber.d("[FIRESTORE] Post-login restore complete")
                    _syncError.value = null
                },
                onFailure = { error ->
                    val msg = error.message ?: "Sync failed"
                    timber.log.Timber.w("[FIRESTORE] Post-login restore failed: %s", msg)
                    _syncError.value = "Signed in, but data couldn't load: $msg"
                }
            )
        } catch (e: Exception) {
            timber.log.Timber.w("[FIRESTORE] Post-login restore failed: %s", e.message)
            _syncError.value = "Signed in, but data couldn't load: ${e.message}"
        } finally {
            _isSyncing.value = false
            _isDataReady.value = true
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /** Re-runs the post-login restore after a [syncError] (retry button). */
    fun retrySync() {
        viewModelScope.launch { runPostAuthSync() }
    }

    fun sendPasswordReset() {
        val email = _uiState.value.email
        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter your email above to reset password") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authManager.sendPasswordReset(email)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Password reset email sent!"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to send reset email"
                        )
                    }
                }
            )
        }
    }

    fun signOut() {
        syncCoordinator.signOut()
    }
}
