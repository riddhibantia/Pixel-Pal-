package com.pixelpal.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pixelpal.app.data.local.db.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    // TRIGGERED (fired but not yet completed/dismissed) stays visible in the
    // pending lists so a reminder that fired on a locked screen is never lost.
    @Query("SELECT * FROM reminders WHERE status IN ('PENDING', 'TRIGGERED') ORDER BY triggerTime ASC")
    fun getPendingReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE status IN ('PENDING', 'TRIGGERED') AND companionId = :companionId ORDER BY triggerTime ASC")
    fun getPendingForCompanion(companionId: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun getCompletedReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders ORDER BY triggerTime ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders ORDER BY triggerTime ASC")
    suspend fun getAllRemindersDirect(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE cloudId = :cloudId")
    suspend fun getByCloudId(cloudId: String): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Delete
    suspend fun delete(reminder: ReminderEntity)

    @Query("UPDATE reminders SET status = :status, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, completedAt: Long? = null, updatedAt: Long = 0L)

    @Query("UPDATE reminders SET snoozeCount = snoozeCount + 1, triggerTime = :newTriggerTime, status = 'PENDING', updatedAt = :updatedAt WHERE id = :id")
    suspend fun snooze(id: Long, newTriggerTime: Long, updatedAt: Long = 0L)

    @Query("DELETE FROM reminders")
    suspend fun deleteAll()
}