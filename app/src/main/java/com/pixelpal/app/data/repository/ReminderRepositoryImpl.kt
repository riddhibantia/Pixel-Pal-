package com.pixelpal.app.data.repository

import com.pixelpal.app.data.local.db.dao.ReminderDao
import com.pixelpal.app.data.local.db.entity.ReminderEntity
import com.pixelpal.app.data.remote.firebase.FirestoreSyncEngine
import com.pixelpal.app.domain.model.Reminder
import com.pixelpal.app.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepositoryImpl @Inject constructor(
    private val dao: ReminderDao,
    private val syncEngine: FirestoreSyncEngine
) : ReminderRepository {

    override fun getPendingReminders(): Flow<List<Reminder>> {
        return dao.getPendingReminders().map { list -> list.map { it.toDomain() } }
    }

    override fun getPendingForCompanion(companionId: Long): Flow<List<Reminder>> {
        return dao.getPendingForCompanion(companionId).map { list -> list.map { it.toDomain() } }
    }

    override fun getCompletedReminders(): Flow<List<Reminder>> {
        return dao.getCompletedReminders().map { list -> list.map { it.toDomain() } }
    }

    override fun getAllReminders(): Flow<List<Reminder>> {
        return dao.getAllReminders().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getById(id: Long): Reminder? {
        return dao.getReminderById(id)?.toDomain()
    }

    override suspend fun insert(reminder: Reminder): Long {
        val id = dao.insert(reminder.toEntity())
        val saved = reminder.copy(id = id)
        if (syncEngine.isUserLoggedIn) {
            syncEngine.syncReminderToCloud(saved)
        }
        return id
    }

    override suspend fun update(reminder: Reminder) {
        dao.update(reminder.toEntity())
        if (syncEngine.isUserLoggedIn) {
            syncEngine.syncReminderToCloud(reminder)
        }
    }

    override suspend fun delete(reminder: Reminder) {
        dao.delete(reminder.toEntity())
        if (syncEngine.isUserLoggedIn) {
            syncEngine.deleteReminderFromCloud(reminder.id)
        }
    }

    override suspend fun complete(id: Long) {
        val now = System.currentTimeMillis()
        dao.updateStatus(id, "COMPLETED", now)
        if (syncEngine.isUserLoggedIn) {
            dao.getReminderById(id)?.let {
                syncEngine.syncReminderToCloud(it.toDomain())
            }
        }
    }

    override suspend fun snooze(id: Long, newTriggerTime: Long) {
        dao.snooze(id, newTriggerTime)
        if (syncEngine.isUserLoggedIn) {
            dao.getReminderById(id)?.let {
                syncEngine.syncReminderToCloud(it.toDomain())
            }
        }
    }

    private fun ReminderEntity.toDomain() = Reminder(
        id = id,
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

    private fun Reminder.toEntity() = ReminderEntity(
        id = id,
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
}