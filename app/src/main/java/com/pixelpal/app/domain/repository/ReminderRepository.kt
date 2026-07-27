package com.pixelpal.app.domain.repository

import com.pixelpal.app.domain.model.Reminder
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun getPendingReminders(): Flow<List<Reminder>>
    fun getCompletedReminders(): Flow<List<Reminder>>
    fun getAllReminders(): Flow<List<Reminder>>
    suspend fun getById(id: Long): Reminder?
    suspend fun insert(reminder: Reminder): Long
    suspend fun update(reminder: Reminder)
    suspend fun delete(reminder: Reminder)
    suspend fun complete(id: Long)
    suspend fun snooze(id: Long, newTriggerTime: Long)
}
