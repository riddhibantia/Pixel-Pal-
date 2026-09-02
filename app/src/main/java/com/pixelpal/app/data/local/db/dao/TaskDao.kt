package com.pixelpal.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.pixelpal.app.data.local.db.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE companionId = :companionId ORDER BY isDone ASC, createdAt DESC")
    fun getTasks(companionId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE companionId = :companionId ORDER BY isDone ASC, createdAt DESC")
    suspend fun getTasksDirect(companionId: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTaskFlow(id: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE cloudId = :cloudId")
    suspend fun getByCloudId(cloudId: String): TaskEntity?

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    suspend fun getAllTasksDirect(): List<TaskEntity>

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("UPDATE tasks SET isDone = 1, completedAt = :completedAt, updatedAt = :completedAt WHERE id = :id")
    suspend fun markDone(id: Long, completedAt: Long)

    @Query("UPDATE tasks SET isDone = 0, completedAt = NULL, updatedAt = :undoneAt WHERE id = :id")
    suspend fun markUndone(id: Long, undoneAt: Long)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(task: TaskEntity)
}