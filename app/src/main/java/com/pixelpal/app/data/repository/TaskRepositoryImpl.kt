package com.pixelpal.app.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.pixelpal.app.data.local.db.PixelPalDatabase
import com.pixelpal.app.data.local.db.dao.ActivityEventDao
import com.pixelpal.app.data.local.db.dao.TaskDao
import com.pixelpal.app.data.local.db.entity.ActivityEventEntity
import com.pixelpal.app.data.local.db.entity.TaskEntity
import com.pixelpal.app.domain.model.ActivityType
import com.pixelpal.app.domain.model.Task
import com.pixelpal.app.domain.repository.TaskRepository
import com.pixelpal.app.widget.TasksWidgetProvider
import com.pixelpal.app.widget.HomeWidgetProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: PixelPalDatabase,
    private val taskDao: TaskDao,
    private val activityEventDao: ActivityEventDao
) : TaskRepository {

    override fun getTasks(companionId: Long): Flow<List<Task>> {
        return taskDao.getTasks(companionId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun addTask(task: Task): Long {
        val id = taskDao.insert(task.toEntity())
        TasksWidgetProvider.updateAllWidgets(context)
        HomeWidgetProvider.updateAllWidgets(context)
        return id
    }

    override suspend fun completeTask(task: Task): Boolean {
        if (task.isDone) return false
        database.withTransaction {
            taskDao.markDone(task.id, System.currentTimeMillis())
            activityEventDao.insert(
                ActivityEventEntity(
                    companionId = task.companionId,
                    type = ActivityType.TASK_COMPLETED.id,
                    title = "Completed \"${task.title}\"",
                    createdAt = System.currentTimeMillis()
                )
            )
        }
        TasksWidgetProvider.updateAllWidgets(context)
        HomeWidgetProvider.updateAllWidgets(context)
        return true
    }

    override suspend fun toggleTask(task: Task) {
        if (task.isDone) {
            taskDao.markUndone(task.id)
        } else {
            completeTask(task)
            return
        }
        TasksWidgetProvider.updateAllWidgets(context)
        HomeWidgetProvider.updateAllWidgets(context)
    }

    private fun TaskEntity.toDomain() = Task(
        id = id,
        companionId = companionId,
        title = title,
        isDone = isDone,
        dueAt = dueAt,
        createdAt = createdAt,
        completedAt = completedAt
    )

    private fun Task.toEntity() = TaskEntity(
        id = id,
        companionId = companionId,
        title = title,
        isDone = isDone,
        dueAt = dueAt,
        createdAt = createdAt,
        completedAt = completedAt
    )
}