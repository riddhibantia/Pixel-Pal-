package com.pixelpal.app.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.pixelpal.app.data.local.db.PixelPalDatabase
import com.pixelpal.app.data.local.db.dao.ActivityEventDao
import com.pixelpal.app.data.local.db.dao.SubtaskDao
import com.pixelpal.app.data.local.db.dao.TaskDao
import com.pixelpal.app.data.local.db.entity.ActivityEventEntity
import com.pixelpal.app.data.local.db.entity.TaskEntity
import com.pixelpal.app.data.remote.firebase.FirestoreSyncEngine
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
    private val subtaskDao: SubtaskDao,
    private val activityEventDao: ActivityEventDao,
    private val syncEngine: FirestoreSyncEngine
) : TaskRepository {

    override fun getTasks(companionId: Long): Flow<List<Task>> {
        return taskDao.getTasks(companionId).map { list -> list.map { it.toDomain() } }
    }

    override fun getTask(taskId: Long): Flow<Task?> {
        return taskDao.getTaskFlow(taskId).map { it?.toDomain() }
    }

    override suspend fun updateTask(task: Task) {
        val existing = taskDao.getTaskById(task.id) ?: return
        val entity = task.copy(id = existing.id)
            .toEntity()
            .copy(
                cloudId = existing.cloudId,
                updatedAt = System.currentTimeMillis()
            )
        taskDao.update(entity)
        syncEngine.pushTaskAsync(entity)
    }

    override suspend fun addTask(task: Task): Long {
        val entity = task.toEntity(updatedAt = System.currentTimeMillis())
        val id = taskDao.insert(entity)
        syncEngine.pushTaskAsync(entity.copy(id = id))
        TasksWidgetProvider.updateAllWidgets(context)
        HomeWidgetProvider.updateAllWidgets(context)
        return id
    }

    override suspend fun completeTask(task: Task): Boolean {
        if (task.isDone) return false
        val now = System.currentTimeMillis()
        database.withTransaction {
            taskDao.markDone(task.id, now)
            activityEventDao.insert(
                ActivityEventEntity(
                    companionId = task.companionId,
                    type = ActivityType.TASK_COMPLETED.id,
                    title = "Completed \"${task.title}\"",
                    createdAt = now
                )
            )
        }
        taskDao.getTaskById(task.id)?.let(syncEngine::pushTaskAsync)
        TasksWidgetProvider.updateAllWidgets(context)
        HomeWidgetProvider.updateAllWidgets(context)
        return true
    }

    override suspend fun toggleTask(task: Task) {
        if (task.isDone) {
            taskDao.markUndone(task.id, System.currentTimeMillis())
            taskDao.getTaskById(task.id)?.let(syncEngine::pushTaskAsync)
        } else {
            completeTask(task)
            return
        }
        TasksWidgetProvider.updateAllWidgets(context)
        HomeWidgetProvider.updateAllWidgets(context)
    }

    override suspend fun deleteTask(task: Task) {
        val entity = taskDao.getTaskById(task.id)
        if (entity != null) {
            // Delete cloud subtasks first (local rows cascade with the task).
            subtaskDao.getByTaskDirect(entity.id).forEach { subtask ->
                syncEngine.deleteSubtaskAsync(subtask.cloudId)
            }
            taskDao.delete(entity)
            syncEngine.deleteTaskAsync(entity.cloudId)
        } else {
            taskDao.delete(task.toEntity())
        }
        TasksWidgetProvider.updateAllWidgets(context)
        HomeWidgetProvider.updateAllWidgets(context)
    }

    override suspend fun reinsertTask(task: Task): Long {
        val entity = task.toEntity(updatedAt = System.currentTimeMillis())
        val id = taskDao.insert(entity)
        syncEngine.pushTaskAsync(entity.copy(id = id))
        TasksWidgetProvider.updateAllWidgets(context)
        HomeWidgetProvider.updateAllWidgets(context)
        return id
    }

    private fun TaskEntity.toDomain() = Task(
        id = id,
        companionId = companionId,
        title = title,
        description = description,
        isDone = isDone,
        dueAt = dueAt,
        createdAt = createdAt,
        completedAt = completedAt
    )

    private fun Task.toEntity(updatedAt: Long = 0L) = TaskEntity(
        id = id,
        companionId = companionId,
        title = title,
        description = description,
        isDone = isDone,
        dueAt = dueAt,
        createdAt = createdAt,
        completedAt = completedAt,
        updatedAt = updatedAt
    )
}
