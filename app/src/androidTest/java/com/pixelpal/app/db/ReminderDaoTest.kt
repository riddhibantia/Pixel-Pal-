package com.pixelpal.app.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pixelpal.app.data.local.db.PixelPalDatabase
import com.pixelpal.app.data.local.db.entity.CompanionEntity
import com.pixelpal.app.data.local.db.entity.ReminderEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderDaoTest {

    private lateinit var db: PixelPalDatabase
    private var companionId: Long = 0

    @Before
    fun setup() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(ctx, PixelPalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        companionId = runBlocking {
            db.companionDao().insert(
                CompanionEntity(name = "TestPet", petType = "cat", role = "friend")
            )
        }
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun pendingIncludesTriggered_notCompleted() = runBlocking {
        db.reminderDao().insert(
            ReminderEntity(title = "pending", triggerTime = 1, companionId = companionId, status = "PENDING")
        )
        db.reminderDao().insert(
            ReminderEntity(title = "fired", triggerTime = 2, companionId = companionId, status = "TRIGGERED")
        )
        db.reminderDao().insert(
            ReminderEntity(title = "done", triggerTime = 3, companionId = companionId, status = "COMPLETED")
        )
        val pending = db.reminderDao().getPendingForCompanion(companionId).first().map { it.title }.toSet()
        assert(pending == setOf("pending", "fired")) { "unexpected pending set: $pending" }
    }

    @Test
    fun snooze_rearmsAsPendingWithNewTime() = runBlocking {
        val id = db.reminderDao().insert(
            ReminderEntity(title = "nap", triggerTime = 100, companionId = companionId)
        )
        db.reminderDao().snooze(id, newTriggerTime = 200)
        val reloaded = db.reminderDao().getReminderById(id)
        assert(reloaded?.status == "PENDING")
        assert(reloaded?.triggerTime == 200L)
        assert(reloaded?.snoozeCount == 1)
    }

    @Test
    fun updateStatus_marksCompleted() = runBlocking {
        val id = db.reminderDao().insert(
            ReminderEntity(title = "finish", triggerTime = 50, companionId = companionId, cloudId = "rem-cloud-9")
        )
        db.reminderDao().updateStatus(id, status = "COMPLETED", completedAt = 777L)
        val loaded = db.reminderDao().getReminderById(id)
        assert(loaded?.status == "COMPLETED")
        assert(loaded?.completedAt == 777L)
        assert(db.reminderDao().getByCloudId("rem-cloud-9")?.id == id)
    }
}
