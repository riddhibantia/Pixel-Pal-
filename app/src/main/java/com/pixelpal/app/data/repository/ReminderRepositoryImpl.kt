package com.pixelpal.app.data.repository

import com.pixelpal.app.data.local.db.dao.ReminderDao
import com.pixelpal.app.data.local.db.entity.ReminderEntity
import com.pixelpal.app.domain.model.Reminder
import com.pixelpal.app.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepositoryImpl @Inject constructor(
    private val dao: ReminderDao
) : ReminderRepository {

    override fun getPendingReminders(): Flow<List<Reminder>> {
        return dao.getPendingReminders().map { list -> list.map { it.toDomain() } }
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
        return dao.insert(reminder.toEntity())
    }

    override suspend fun update(reminder: Reminder) {
        dao.update(reminder.toEntity())
    }

    override suspend fun delete(reminder: Reminder) {
        dao.delete(reminder.toEntity())
    }

    override suspend fun complete(id: Long) {
        dao.updateStatus(id, "COMPLETED", System.currentTimeMillis())
    }

    override suspend fun snooze(id: Long, newTriggerTime: Long) {
        dao.snooze(id, newTriggerTime)
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
        completedAt = completedAt
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
        completedAt = completedAt
    )
}
