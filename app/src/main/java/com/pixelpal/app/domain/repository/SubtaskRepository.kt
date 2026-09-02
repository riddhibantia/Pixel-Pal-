package com.pixelpal.app.domain.repository

import com.pixelpal.app.domain.model.Subtask
import kotlinx.coroutines.flow.Flow

interface SubtaskRepository {
    fun getByTask(taskId: Long): Flow<List<Subtask>>

    suspend fun add(taskId: Long, title: String)

    suspend fun toggle(subtask: Subtask)

    suspend fun delete(subtask: Subtask)

    suspend fun rename(subtaskId: Long, title: String)
}
