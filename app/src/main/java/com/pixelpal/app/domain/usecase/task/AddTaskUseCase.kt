package com.pixelpal.app.domain.usecase.task

import com.pixelpal.app.domain.model.ActivityType
import com.pixelpal.app.domain.model.Task
import com.pixelpal.app.domain.repository.ActivityEventRepository
import com.pixelpal.app.domain.repository.TaskRepository
import javax.inject.Inject

class AddTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val activityEventRepository: ActivityEventRepository
) {
    suspend operator fun invoke(companionId: Long, title: String, dueAt: Long? = null): Long {
        val id = taskRepository.addTask(Task(companionId = companionId, title = title, dueAt = dueAt))
        activityEventRepository.record(
            companionId = companionId,
            type = ActivityType.TASK_ADDED,
            title = "Added \"$title\""
        )
        return id
    }
}