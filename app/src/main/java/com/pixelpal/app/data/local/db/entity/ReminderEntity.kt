package com.pixelpal.app.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = CompanionEntity::class,
            parentColumns = ["id"],
            childColumns = ["companionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("companionId")]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String? = null,
    val triggerTime: Long,
    // Defaults must stay in sync with DatabaseMigrations; ALTER TABLE cannot add
    // a NOT NULL column without a DEFAULT, so the schema declares them too.
    @ColumnInfo(defaultValue = "0") val hour: Int = 0,
    @ColumnInfo(defaultValue = "0") val minute: Int = 0,
    val soundUri: String? = null,
    val recurrence: String = "ONCE",       // ONCE, DAILY, WEEKLY, MONTHLY
    val recurrenceInterval: Long? = null,
    val category: String = "CUSTOM",       // MEETING, MEDICINE, BIRTHDAY, SHOPPING, ASSIGNMENT, CUSTOM
    val status: String = "PENDING",        // PENDING, TRIGGERED, COMPLETED, SNOOZED, DISMISSED
    val snoozeCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val companionId: Long? = null,
    // Stable cross-device identity for Cloud Firestore documents (Room row ids
    // are device-local autoincrement values and must never key cloud docs).
    @ColumnInfo(defaultValue = "") val cloudId: String = java.util.UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0L
)