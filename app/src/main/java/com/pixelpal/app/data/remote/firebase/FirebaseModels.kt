package com.pixelpal.app.data.remote.firebase

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.pixelpal.app.data.local.db.entity.ReminderEntity
import com.pixelpal.app.data.local.db.entity.TaskEntity
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
 * The companion is a single row, so the document id is the fixed "primary".
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

fun Companion.toFirestore(updatedAt: Long = System.currentTimeMillis()): FirestoreCompanion = FirestoreCompanion(
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
    updatedAt = updatedAt
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
 * Cloud task representation. The Firestore document id is the local row's
 * stable [TaskEntity.cloudId] UUID, never the device-local Room row id.
 */
@IgnoreExtraProperties
data class FirestoreTask(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String? = null,
    val isDone: Boolean = false,
    val dueAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

fun TaskEntity.toFirestore(): FirestoreTask = FirestoreTask(
    id = cloudId,
    title = title,
    description = description,
    isDone = isDone,
    dueAt = dueAt,
    createdAt = createdAt,
    completedAt = completedAt,
    updatedAt = updatedAt.takeIf { it > 0L } ?: createdAt
)

/** Maps a cloud task onto a local row. Pass the existing local [localId] when updating, 0 when inserting. */
fun FirestoreTask.toEntity(localId: Long, companionId: Long): TaskEntity = TaskEntity(
    id = localId,
    companionId = companionId,
    title = title,
    description = description,
    isDone = isDone,
    dueAt = dueAt,
    createdAt = createdAt,
    completedAt = completedAt,
    cloudId = id,
    updatedAt = updatedAt
)

/**
 * Cloud reminder representation, keyed by the stable [ReminderEntity.cloudId] UUID.
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

fun ReminderEntity.toFirestore(): FirestoreReminder = FirestoreReminder(
    id = cloudId,
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
    updatedAt = updatedAt.takeIf { it > 0L } ?: createdAt
)

/** Maps a cloud reminder onto a local row. Pass the existing local [localId] when updating, 0 when inserting. */
fun FirestoreReminder.toEntity(localId: Long, companionId: Long?): ReminderEntity = ReminderEntity(
    id = localId,
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
    companionId = companionId,
    cloudId = id,
    updatedAt = updatedAt
)

/**
 * Cloud subtask representation, keyed by the stable [com.pixelpal.app.data.local.db.entity.SubtaskEntity.cloudId] UUID.
 * `parentCloudId` links it to its task's cloud document.
 */
@IgnoreExtraProperties
data class FirestoreSubtask(
    @DocumentId val id: String = "",
    val parentCloudId: String = "",
    val title: String = "",
    val isDone: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

fun com.pixelpal.app.data.local.db.entity.SubtaskEntity.toFirestore(parentCloudId: String): FirestoreSubtask = FirestoreSubtask(
    id = cloudId,
    parentCloudId = parentCloudId,
    title = title,
    isDone = isDone,
    sortOrder = sortOrder,
    createdAt = createdAt,
    completedAt = completedAt,
    updatedAt = updatedAt.takeIf { it > 0L } ?: createdAt
)

/** Maps a cloud subtask onto a local row. Pass the existing local [localId] when updating, 0 when inserting. */
fun FirestoreSubtask.toSubtaskEntity(localId: Long, taskId: Long): com.pixelpal.app.data.local.db.entity.SubtaskEntity =
    com.pixelpal.app.data.local.db.entity.SubtaskEntity(
        id = localId,
        taskId = taskId,
        title = title,
        isDone = isDone,
        sortOrder = sortOrder,
        createdAt = createdAt,
        completedAt = completedAt,
        cloudId = id,
        updatedAt = updatedAt
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

fun Bond.toFirestore(updatedAt: Long = System.currentTimeMillis()): FirestoreBond = FirestoreBond(
    level = level,
    totalInteractions = totalInteractions,
    streakDays = streakDays,
    lastStreakDate = lastStreakDate,
    lastInteractionTime = lastInteractionTime,
    tapsToday = tapsToday,
    feedsToday = feedsToday,
    updatedAt = updatedAt
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
