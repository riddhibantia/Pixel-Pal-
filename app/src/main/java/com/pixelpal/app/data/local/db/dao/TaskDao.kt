package com.pixelpal.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.pixelpal.app.data.local.db.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE companionId = :companionId ORDER BY isDone ASC, createdAt DESC")
    fun getTasks(companionId: Long): Flow<List<TaskEntity>>

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Query("UPDATE tasks SET isDone = 1, completedAt = :completedAt WHERE id = :id")
    suspend fun markDone(id: Long, completedAt: Long)

    @Query("UPDATE tasks SET isDone = 0, completedAt = NULL WHERE id = :id")
    suspend fun markUndone(id: Long)

    @Delete
    suspend fun delete(task: TaskEntity)
}