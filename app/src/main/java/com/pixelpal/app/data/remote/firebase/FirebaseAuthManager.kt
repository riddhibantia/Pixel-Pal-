package com.pixelpal.app.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
     * Signs out the current user session.
     */
    fun signOut() {
        auth.signOut()
    }
}
