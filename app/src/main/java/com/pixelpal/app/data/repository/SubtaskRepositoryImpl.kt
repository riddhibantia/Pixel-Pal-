package com.pixelpal.app.data.repository

import com.pixelpal.app.data.local.db.dao.SubtaskDao
import com.pixelpal.app.data.local.db.dao.TaskDao
import com.pixelpal.app.data.local.db.entity.SubtaskEntity
import com.pixelpal.app.data.remote.firebase.FirestoreSyncEngine
import com.pixelpal.app.domain.model.Subtask
import com.pixelpal.app.domain.repository.SubtaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubtaskRepositoryImpl @Inject constructor(
    private val dao: SubtaskDao,
    private val taskDao: TaskDao,
    private val syncEngine: FirestoreSyncEngine
) : SubtaskRepository {

    /** Cloud pushes are keyed by the PARENT task's cloudId; null when unpushed. */
    private suspend fun parentCloudId(taskId: Long): String? =
        taskDao.getTaskById(taskId)?.cloudId?.takeIf { it.isNotBlank() }

    override fun getByTask(taskId: Long): Flow<List<Subtask>> {
        return dao.getByTask(taskId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun add(taskId: Long, title: String) {
        if (title.isBlank()) return
        val existing = dao.getByTaskDirect(taskId)
        val entity = SubtaskEntity(
            taskId = taskId,
            title = title.trim(),
            sortOrder = existing.size,
            updatedAt = System.currentTimeMillis()
        )
        val id = dao.insert(entity)
        parentCloudId(taskId)?.let { parent ->
            syncEngine.pushSubtaskAsync(entity.copy(id = id), parent)
        }
    }

    override suspend fun toggle(subtask: Subtask) {
        val entity = dao.getById(subtask.id) ?: return
        val now = System.currentTimeMillis()
        val updated = if (entity.isDone) {
            entity.copy(isDone = false, completedAt = null, updatedAt = now)
        } else {
            entity.copy(isDone = true, completedAt = now, updatedAt = now)
        }
        dao.setDone(entity.id, updated.isDone, updated.completedAt, updated.updatedAt)
        parentCloudId(entity.taskId)?.let { parent ->
            syncEngine.pushSubtaskAsync(updated, parent)
        }
    }

    override suspend fun rename(subtaskId: Long, title: String) {
        if (title.isBlank()) return
        val entity = dao.getById(subtaskId) ?: return
        val updated = entity.copy(title = title.trim(), updatedAt = System.currentTimeMillis())
        dao.update(updated)
        parentCloudId(entity.taskId)?.let { parent ->
            syncEngine.pushSubtaskAsync(updated, parent)
        }
    }

    override suspend fun delete(subtask: Subtask) {
        val entity = dao.getById(subtask.id)
        if (entity != null) {
            dao.delete(entity)
            syncEngine.deleteSubtaskAsync(entity.cloudId)
        } else {
            dao.delete(subtask.toEntity())
        }
    }

    private fun SubtaskEntity.toDomain() = Subtask(
        id = id,
        taskId = taskId,
        title = title,
        isDone = isDone,
        sortOrder = sortOrder,
        createdAt = createdAt,
        completedAt = completedAt
    )

    private fun Subtask.toEntity() = SubtaskEntity(
        id = id,
        taskId = taskId,
        title = title,
        isDone = isDone,
        sortOrder = sortOrder,
        createdAt = createdAt,
        completedAt = completedAt
    )
}
