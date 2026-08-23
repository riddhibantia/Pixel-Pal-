package com.pixelpal.app.domain.usecase.task

import com.pixelpal.app.domain.model.Task
import com.pixelpal.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    fun getTasks(companionId: Long): Flow<List<Task>> = taskRepository.getTasks(companionId)
}