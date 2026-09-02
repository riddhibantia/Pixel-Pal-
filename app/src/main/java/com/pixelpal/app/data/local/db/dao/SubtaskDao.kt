package com.pixelpal.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pixelpal.app.data.local.db.entity.SubtaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtaskDao {
    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY sortOrder ASC, createdAt ASC")
    fun getByTask(taskId: Long): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getByTaskDirect(taskId: Long): List<SubtaskEntity>

    @Query("SELECT * FROM subtasks WHERE id = :id")
    suspend fun getById(id: Long): SubtaskEntity?

    @Query("SELECT * FROM subtasks WHERE cloudId = :cloudId")
    suspend fun getByCloudId(cloudId: String): SubtaskEntity?

    @Query("SELECT * FROM subtasks")
    suspend fun getAllDirect(): List<SubtaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subtask: SubtaskEntity): Long

    @Update
    suspend fun update(subtask: SubtaskEntity)

    @Delete
    suspend fun delete(subtask: SubtaskEntity)

    @Query("DELETE FROM subtasks")
    suspend fun deleteAll()

    @Query("UPDATE subtasks SET isDone = :isDone, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setDone(id: Long, isDone: Boolean, completedAt: Long?, updatedAt: Long)
}
