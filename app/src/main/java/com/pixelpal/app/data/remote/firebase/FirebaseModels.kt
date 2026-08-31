package com.pixelpal.app.data.remote.firebase

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * User account metadata stored at `users/{userId}`.
 */
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val userId: String = "",
    val email: String? = null,
    val displayName: String? = null,
    val isAnonymous: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis()
)

/**
 * Cloud companion representation stored at `users/{userId}/companion/primary`.
 */
@IgnoreExtraProperties
data class FirestoreCompanion(
    val name: String = "PixelPal",
    val petType: String = "cat",
    val role: String = "Companion",
    val description: String? = null,
    val species: String = "cat",
    val color: String = "orange",
    val pattern: String = "plain",
    val hatId: String? = null,
    val outfitId: String? = null,
    val accessoryId: String? = null,
    val isFavorite: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Cloud task representation stored at `users/{userId}/tasks/{taskId}`.
 */
@IgnoreExtraProperties
data class FirestoreTask(
    @DocumentId val id: String = "",
    val title: String = "",
    val isDone: Boolean = false,
    val dueAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

/**
 * Cloud reminder representation stored at `users/{userId}/reminders/{reminderId}`.
 */
@IgnoreExtraProperties
data class FirestoreReminder(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val scheduledAt: Long = 0L,
    val isEnabled: Boolean = true,
    val repeatInterval: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Cloud bond & streak metrics stored at `users/{userId}/metrics/bond`.
 */
@IgnoreExtraProperties
data class FirestoreBond(
    val level: Int = 1,
    val currentPoints: Int = 0,
    val streakDays: Int = 0,
    val lastInteractionDate: String = "",
    val tapsToday: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)
