package com.pixelpal.app.domain.repository

import com.pixelpal.app.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    /** Reactive single-task stream for the Task Detail screen. */
    fun getTask(taskId: Long): Flow<Task?>

    /** Edits title/description and syncs the change to the cloud. */
    suspend fun updateTask(task: Task)

    fun getTasks(companionId: Long): Flow<List<Task>>
    suspend fun addTask(task: Task): Long
    /**
     * Marks the task done and records the completion in the same transaction.
     * Returns true when the task was transitioned to done.
     */
    suspend fun completeTask(task: Task): Boolean
    suspend fun toggleTask(task: Task)
    suspend fun deleteTask(task: Task)
    suspend fun reinsertTask(task: Task): Long
}