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
        val entity = reminder.toEntity(updatedAt = System.currentTimeMillis())
        val id = dao.insert(entity)
        syncEngine.pushReminderAsync(entity.copy(id = id))
        return id
    }

    override suspend fun update(reminder: Reminder) {
        // Preserve the existing cloudId so the update lands on the same cloud doc.
        val entity = dao.getReminderById(reminder.id)
            ?.let { reminder.toEntity(cloudId = it.cloudId) }
            ?.copy(updatedAt = System.currentTimeMillis())
            ?: reminder.toEntity(updatedAt = System.currentTimeMillis())
        dao.update(entity)
        syncEngine.pushReminderAsync(entity)
    }

    override suspend fun delete(reminder: Reminder) {
        val entity = dao.getReminderById(reminder.id)
        if (entity != null) {
            dao.delete(entity)
            syncEngine.deleteReminderAsync(entity.cloudId)
        } else {
            dao.delete(reminder.toEntity())
        }
    }

    override suspend fun complete(id: Long) {
        val now = System.currentTimeMillis()
        dao.updateStatus(id, "COMPLETED", now, now)
        dao.getReminderById(id)?.let(syncEngine::pushReminderAsync)
    }

    override suspend fun snooze(id: Long, newTriggerTime: Long) {
        val now = System.currentTimeMillis()
        dao.snooze(id, newTriggerTime, now)
        dao.getReminderById(id)?.let(syncEngine::pushReminderAsync)
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

    private fun Reminder.toEntity(
        updatedAt: Long = 0L,
        cloudId: String = java.util.UUID.randomUUID().toString()
    ) = ReminderEntity(
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
        companionId = companionId,
        updatedAt = updatedAt,
        cloudId = cloudId
    )
}
