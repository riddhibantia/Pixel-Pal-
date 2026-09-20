package com.pixelpal.app.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pixelpal.app.data.local.db.PixelPalDatabase
import com.pixelpal.app.data.local.db.entity.CompanionEntity
import com.pixelpal.app.data.local.db.entity.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskDaoTest {

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
    fun insertAndRead_roundTripsCloudId() = runBlocking {
        val id = db.taskDao().insert(
            TaskEntity(companionId = companionId, title = "Write tests", cloudId = "task-cloud-1")
        )
        val loaded = db.taskDao().getTaskById(id)
        assert(loaded != null)
        assert(loaded!!.title == "Write tests")
        assert(loaded.cloudId == "task-cloud-1")
        assert(db.taskDao().getByCloudId("task-cloud-1")?.id == id)
    }

    @Test
    fun markDoneAndUndone_togglesState() = runBlocking {
        val id = db.taskDao().insert(
            TaskEntity(companionId = companionId, title = "Toggle me")
        )
        db.taskDao().markDone(id, completedAt = 999L)
        assert(db.taskDao().getTaskById(id)?.isDone == true)

        db.taskDao().markUndone(id, undoneAt = 1000L)
        val undone = db.taskDao().getTaskById(id)
        assert(undone?.isDone == false)
        assert(undone?.completedAt == null)
    }

    @Test
    fun getTasks_ordersUndoneFirst() = runBlocking {
        val doneId = db.taskDao().insert(
            TaskEntity(companionId = companionId, title = "done", isDone = true)
        )
        db.taskDao().markDone(doneId, completedAt = 5L)
        db.taskDao().insert(
            TaskEntity(companionId = companionId, title = "todo")
        )
        val titles = db.taskDao().getTasks(companionId).first().map { it.title }
        assert(titles.first() == "todo") { "expected undone first, got $titles" }
    }
}
