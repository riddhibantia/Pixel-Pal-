package com.pixelpal.app.data.remote.firebase

import com.pixelpal.app.data.local.db.entity.ReminderEntity
import com.pixelpal.app.data.local.db.entity.TaskEntity
import com.pixelpal.app.domain.model.Bond
import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.model.CompanionRole
import org.junit.Assert.assertEquals
import org.junit.Test

class FirebaseModelsTest {

    @Test
    fun companion_toFirestore_andBack_preservesFields() {
        val original = Companion(
            id = 42L,
            name = "Mittens",
            petType = "cat",
            role = CompanionRole.AI_AGENT,
            description = "Smart coding companion",
            species = "cat",
            color = "orange",
            pattern = "striped",
            hatId = "wizard_hat",
            outfitId = "robe",
            accessoryId = "glasses",
            isFavorite = true
        )

        val firestoreModel = original.toFirestore()
        assertEquals("Mittens", firestoreModel.name)
        assertEquals("AI_AGENT", firestoreModel.role)
        assertEquals("wizard_hat", firestoreModel.hatId)

        val restored = firestoreModel.toDomain(id = 42L)
        assertEquals(original.id, restored.id)
        assertEquals(original.name, restored.name)
        assertEquals(original.role, restored.role)
        assertEquals(original.species, restored.species)
        assertEquals(original.color, restored.color)
        assertEquals(original.pattern, restored.pattern)
        assertEquals(original.hatId, restored.hatId)
        assertEquals(original.outfitId, restored.outfitId)
        assertEquals(original.accessoryId, restored.accessoryId)
        assertEquals(original.isFavorite, restored.isFavorite)
    }

    @Test
    fun taskEntity_toFirestore_keysByCloudId_notLocalRowId() {
        val original = TaskEntity(
            id = 101L,
            companionId = 1L,
            title = "Implement Cloud Sync",
            isDone = true,
            dueAt = 1700000000L,
            createdAt = 1690000000L,
            completedAt = 1700000050L,
            cloudId = "stable-uuid-1",
            updatedAt = 1700000060L
        )

        val firestoreModel = original.toFirestore()
        assertEquals("stable-uuid-1", firestoreModel.id)
        assertEquals("Implement Cloud Sync", firestoreModel.title)
        assertEquals(true, firestoreModel.isDone)
        assertEquals(1700000060L, firestoreModel.updatedAt)

        val restored = firestoreModel.toEntity(localId = 101L, companionId = 1L)
        assertEquals("stable-uuid-1", restored.cloudId)
        assertEquals(101L, restored.id)
        assertEquals(1L, restored.companionId)
        assertEquals(original.title, restored.title)
        assertEquals(original.isDone, restored.isDone)
        assertEquals(original.dueAt, restored.dueAt)
        assertEquals(original.completedAt, restored.completedAt)
        assertEquals(original.updatedAt, restored.updatedAt)
    }

    @Test
    fun reminderEntity_toFirestore_keysByCloudId_notLocalRowId() {
        val original = ReminderEntity(
            id = 202L,
            title = "Water Plants",
            message = "Don't forget the ferns",
            triggerTime = 1750000000L,
            hour = 14,
            minute = 30,
            soundUri = "uri://bell",
            recurrence = "DAILY",
            recurrenceInterval = 86400000L,
            category = "HABIT",
            status = "PENDING",
            snoozeCount = 1,
            companionId = 1L,
            cloudId = "stable-uuid-2",
            updatedAt = 1750000050L
        )

        val firestoreModel = original.toFirestore()
        assertEquals("stable-uuid-2", firestoreModel.id)
        assertEquals("Water Plants", firestoreModel.title)
        assertEquals("DAILY", firestoreModel.recurrence)
        assertEquals(1750000050L, firestoreModel.updatedAt)

        val restored = firestoreModel.toEntity(localId = 202L, companionId = 1L)
        assertEquals("stable-uuid-2", restored.cloudId)
        assertEquals(202L, restored.id)
        assertEquals(original.title, restored.title)
        assertEquals(original.message, restored.message)
        assertEquals(original.triggerTime, restored.triggerTime)
        assertEquals(original.recurrence, restored.recurrence)
        assertEquals(original.status, restored.status)
        assertEquals(original.snoozeCount, restored.snoozeCount)
        assertEquals(original.updatedAt, restored.updatedAt)
    }

    @Test
    fun bond_toFirestore_andBack_preservesFields() {
        val original = Bond(
            companionId = 1L,
            level = 5,
            totalInteractions = 88,
            tapsToday = 3,
            feedsToday = 2,
            lastInteractionTime = 1720000000L,
            streakDays = 14,
            lastStreakDate = "2026-09-01"
        )

        val firestoreModel = original.toFirestore()
        assertEquals(5, firestoreModel.level)
        assertEquals(14, firestoreModel.streakDays)
        assertEquals("2026-09-01", firestoreModel.lastStreakDate)

        val restored = firestoreModel.toDomain(companionId = 1L)
        assertEquals(original.companionId, restored.companionId)
        assertEquals(original.level, restored.level)
        assertEquals(original.totalInteractions, restored.totalInteractions)
        assertEquals(original.tapsToday, restored.tapsToday)
        assertEquals(original.feedsToday, restored.feedsToday)
        assertEquals(original.streakDays, restored.streakDays)
        assertEquals(original.lastStreakDate, restored.lastStreakDate)
    }
}
