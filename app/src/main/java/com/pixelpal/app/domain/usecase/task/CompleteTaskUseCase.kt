package com.pixelpal.app.domain.usecase.task

import com.pixelpal.app.domain.engine.BondEngine
import com.pixelpal.app.domain.model.Task
import com.pixelpal.app.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * Completes a task and coordinates its side effects:
 *  - marks the task done + records the completion activity (in one transaction),
 *  - applies the bond reward to the companion.
 */
class CompleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val bondEngine: BondEngine
) {
    suspend operator fun invoke(task: Task): Boolean {
        val completed = taskRepository.completeTask(task)
        if (completed) {
            bondEngine.recordTaskCompleted(task.companionId)
        }
        return completed
    }
}