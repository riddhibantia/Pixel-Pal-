package com.pixelpal.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pixelpal.app.data.local.db.dao.BondDao
import com.pixelpal.app.data.local.db.dao.CompanionDao
import com.pixelpal.app.data.local.db.dao.ReminderDao
import com.pixelpal.app.data.local.db.dao.TaskDao
import com.pixelpal.app.data.remote.firebase.FirestoreSyncEngine
import com.pixelpal.app.domain.model.CompanionRole
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Background WorkManager worker for periodic and network-triggered 2-way Cloud Firestore synchronization.
 */
@HiltWorker
class FirestoreSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncEngine: FirestoreSyncEngine,
    private val companionDao: CompanionDao,
    private val taskDao: TaskDao,
    private val reminderDao: ReminderDao,
    private val bondDao: BondDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!syncEngine.isUserLoggedIn) {
            Timber.d("FirestoreSyncWorker skipped: user not logged in")
            return Result.success()
        }

        return try {
            val primaryEntity = companionDao.getPrimaryDirect()
            val companion = primaryEntity?.let {
                com.pixelpal.app.domain.model.Companion(
                    id = it.id,
                    name = it.name,
                    petType = it.petType,
                    role = CompanionRole.fromId(it.role),
                    description = it.description,
                    createdAt = it.createdAt,
                    lastUsedAt = it.lastUsedAt,
                    isFavorite = it.isFavorite,
                    isArchived = it.isArchived,
                    hatId = it.hatId,
                    outfitId = it.outfitId,
                    accessoryId = it.accessoryId,
                    species = it.species,
                    color = it.color,
                    pattern = it.pattern
                )
            }

            val tasks = primaryEntity?.let {
                taskDao.getTasksDirect(it.id).map { t ->
                    com.pixelpal.app.domain.model.Task(
                        id = t.id,
                        companionId = t.companionId,
                        title = t.title,
                        isDone = t.isDone,
                        dueAt = t.dueAt,
                        createdAt = t.createdAt,
                        completedAt = t.completedAt
                    )
                }
            } ?: emptyList()

            val reminders = reminderDao.getAllRemindersDirect().map { r ->
                com.pixelpal.app.domain.model.Reminder(
                    id = r.id,
                    title = r.title,
                    message = r.message,
                    triggerTime = r.triggerTime,
                    hour = r.hour,
                    minute = r.minute,
                    soundUri = r.soundUri,
                    recurrence = r.recurrence,
                    recurrenceInterval = r.recurrenceInterval,
                    category = r.category,
                    status = r.status,
                    snoozeCount = r.snoozeCount,
                    createdAt = r.createdAt,
                    completedAt = r.completedAt,
                    companionId = r.companionId
                )
            }

            val bond = primaryEntity?.let {
                bondDao.getBondDirect(it.id)?.let { b ->
                    com.pixelpal.app.domain.model.Bond(
                        companionId = b.companionId,
                        level = b.level,
                        totalInteractions = b.totalInteractions,
                        tapsToday = b.tapsToday,
                        feedsToday = b.feedsToday,
                        lastInteractionTime = b.lastInteractionTime,
                        streakDays = b.streakDays,
                        lastStreakDate = b.lastStreakDate
                    )
                }
            }

            val syncResult = syncEngine.syncAllLocalToCloud(
                companion = companion,
                tasks = tasks,
                reminders = reminders,
                bond = bond
            )

            if (syncResult.isSuccess) {
                Timber.d("FirestoreSyncWorker finished successfully")
                Result.success()
            } else {
                Timber.w("FirestoreSyncWorker encountered partial failure: %s", syncResult.exceptionOrNull()?.message)
                Result.retry()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error executing FirestoreSyncWorker")
            Result.retry()
        }
    }
}
