package com.pixelpal.app.data.remote.firebase

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.pixelpal.app.domain.model.Bond
import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.model.CompanionRole
import com.pixelpal.app.domain.model.Reminder
import com.pixelpal.app.domain.model.Task

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

fun Companion.toFirestore(): FirestoreCompanion = FirestoreCompanion(
    name = name,
    petType = petType,
    role = role.id,
    description = description,
    species = species,
    color = color,
    pattern = pattern,
    hatId = hatId,
    outfitId = outfitId,
    accessoryId = accessoryId,
    isFavorite = isFavorite,
    updatedAt = System.currentTimeMillis()
)

fun FirestoreCompanion.toDomain(id: Long = 1L): Companion = Companion(
    id = id,
    name = name,
    petType = petType,
    role = CompanionRole.fromId(role),
    description = description,
    isFavorite = isFavorite,
    hatId = hatId,
    outfitId = outfitId,
    accessoryId = accessoryId,
    species = species,
    color = color,
    pattern = pattern
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
    val completedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

fun Task.toFirestore(): FirestoreTask = FirestoreTask(
    id = id.toString(),
    title = title,
    isDone = isDone,
    dueAt = dueAt,
    createdAt = createdAt,
    completedAt = completedAt,
    updatedAt = System.currentTimeMillis()
)

fun FirestoreTask.toDomain(companionId: Long = 1L): Task = Task(
    id = id.toLongOrNull() ?: 0L,
    companionId = companionId,
    title = title,
    isDone = isDone,
    dueAt = dueAt,
    createdAt = createdAt,
    completedAt = completedAt
)

/**
 * Cloud reminder representation stored at `users/{userId}/reminders/{reminderId}`.
 */
@IgnoreExtraProperties
data class FirestoreReminder(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String? = null,
    val triggerTime: Long = 0L,
    val hour: Int = 0,
    val minute: Int = 0,
    val soundUri: String? = null,
    val recurrence: String = "ONCE",
    val recurrenceInterval: Long? = null,
    val category: String = "CUSTOM",
    val status: String = "PENDING",
    val snoozeCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

fun Reminder.toFirestore(): FirestoreReminder = FirestoreReminder(
    id = id.toString(),
    title = title,
    message = message,
    triggerTime = triggerTime,
    hour = hour,
    minute = minute,
    soundUri = soundUri,
    recurrence = recurrence,
    recurrenceInterval = recurrenceInterval,
    category = category,
    status = status,
    snoozeCount = snoozeCount,
    createdAt = createdAt,
    completedAt = completedAt,
    updatedAt = System.currentTimeMillis()
)

fun FirestoreReminder.toDomain(companionId: Long = 1L): Reminder = Reminder(
    id = id.toLongOrNull() ?: 0L,
    title = title,
    message = message,
    triggerTime = triggerTime,
    hour = hour,
    minute = minute,
    soundUri = soundUri,
    recurrence = recurrence,
    recurrenceInterval = recurrenceInterval,
    category = category,
    status = status,
    snoozeCount = snoozeCount,
    createdAt = createdAt,
    completedAt = completedAt,
    companionId = companionId
)

/**
 * Cloud bond & streak metrics stored at `users/{userId}/metrics/bond`.
 */
@IgnoreExtraProperties
data class FirestoreBond(
    val level: Int = 1,
    val totalInteractions: Int = 0,
    val streakDays: Int = 0,
    val lastStreakDate: String = "",
    val lastInteractionTime: Long = 0L,
    val tapsToday: Int = 0,
    val feedsToday: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

fun Bond.toFirestore(): FirestoreBond = FirestoreBond(
    level = level,
    totalInteractions = totalInteractions,
    streakDays = streakDays,
    lastStreakDate = lastStreakDate,
    lastInteractionTime = lastInteractionTime,
    tapsToday = tapsToday,
    feedsToday = feedsToday,
    updatedAt = System.currentTimeMillis()
)

fun FirestoreBond.toDomain(companionId: Long = 1L): Bond = Bond(
    companionId = companionId,
    level = level,
    totalInteractions = totalInteractions,
    tapsToday = tapsToday,
    feedsToday = feedsToday,
    lastInteractionTime = lastInteractionTime,
    streakDays = streakDays,
    lastStreakDate = lastStreakDate
)
