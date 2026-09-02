package com.pixelpal.app.data.remote.firebase

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.pixelpal.app.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AuthState {
    data object Idle : AuthState
    data object Loading : AuthState
    data class Authenticated(val user: FirebaseUser) : AuthState
    data object Unauthenticated : AuthState
    data class Error(val message: String) : AuthState
}

/**
 * Manages Firebase Authentication operations (Email/Password, Anonymous/Guest, Sign Out).
 */
@Singleton
class FirebaseAuthManager @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val auth: FirebaseAuth
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val authStateFlow: Flow<AuthState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                trySend(AuthState.Authenticated(user))
            } else {
                trySend(AuthState.Unauthenticated)
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Signs in anonymously for zero-friction guest onboarding.
     */
    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val result = auth.signInAnonymously().await()
            val user = result.user ?: throw IllegalStateException("Firebase user was null")
            Result.success(user)
        } catch (e: Exception) {
            Timber.e(e, "Anonymous sign-in failed")
            Result.failure(e)
        }
    }

    /**
     * Signs in with email and password.
     */
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = result.user ?: throw IllegalStateException("Firebase user was null")
            Result.success(user)
        } catch (e: Exception) {
            Timber.e(e, "Email sign-in failed")
            Result.failure(e)
        }
    }

    /**
     * Creates a new user account with email and password.
     */
    suspend fun signUpWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = result.user ?: throw IllegalStateException("Firebase user was null")
            Result.success(user)
        } catch (e: Exception) {
            Timber.e(e, "Email sign-up failed")
            Result.failure(e)
        }
    }

    /**
     * Sends password reset email.
     */
    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Password reset failed")
            Result.failure(e)
        }
    }

    /**
     * Google Sign-In via Android Credential Manager, then Firebase sign-in
     * with the resulting Google ID token.
     */
    suspend fun signInWithGoogle(activityContext: Context): Result<FirebaseUser> {
        return try {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .setFilterByAuthorizedAccounts(false)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
                val user = auth.signInWithCredential(firebaseCredential).await().user
                    ?: throw IllegalStateException("Firebase user was null")
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Unsupported credential type"))
            }
        } catch (e: GoogleIdTokenParsingException) {
            Timber.e(e, "Google Sign-In: invalid credential response")
            Result.failure(e)
        } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
            Timber.d("Google Sign-In cancelled by user")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Google Sign-In failed")
            Result.failure(e)
        }
    }

    /**
     * Signs out the current user session.
     */
    fun signOut() {
        auth.signOut()
        // Clear the credential state so the next Google Sign-In shows all accounts.
        val credentialManager = CredentialManager.create(context)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
        }
    }
}
