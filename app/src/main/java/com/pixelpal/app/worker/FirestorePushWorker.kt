package com.pixelpal.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pixelpal.app.data.local.db.PixelPalDatabase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Retrying push for a single entity. Replaces the old fire-and-forget
 * `scope.launch { syncX() }` so offline writes are not silently lost.
 * WorkManager retries with exponential backoff until NetworkType.CONNECTED.
 */
@HiltWorker
class FirestorePushWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val database: PixelPalDatabase,
    private val syncEngine: com.pixelpal.app.data.remote.firebase.FirestoreSyncEngine
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val type = inputData.getString(KEY_TYPE) ?: return Result.failure()
        val cloudId = inputData.getString(KEY_CLOUD_ID) ?: return Result.failure()
        if (cloudId.isBlank()) return Result.success()
        if (syncEngine.currentUserId == null) return Result.retry()

        return try {
            val res = when (type) {
                TYPE_TASK -> {
                    val entity = database.taskDao().getByCloudId(cloudId) ?: return Result.success()
                    syncEngine.syncTaskToCloud(entity)
                }
                TYPE_REMINDER -> {
                    val entity = database.reminderDao().getByCloudId(cloudId) ?: return Result.success()
                    syncEngine.syncReminderToCloud(entity)
                }
                TYPE_SUBTASK -> {
                    val subtask = database.subtaskDao().getByCloudId(cloudId) ?: return Result.success()
                    val parent = database.taskDao().getTaskById(subtask.taskId) ?: return Result.success()
                    syncEngine.syncSubtaskToCloud(subtask, parent.cloudId)
                }
                TYPE_DELETE_TASK -> syncEngine.deleteTaskFromCloud(cloudId)
                TYPE_DELETE_SUBTASK -> syncEngine.deleteSubtaskFromCloud(cloudId)
                else -> return Result.failure()
            }
            if (res.isSuccess) Result.success() else Result.retry()
        } catch (e: Exception) {
            Timber.e(e, "FirestorePushWorker $type $cloudId failed")
            Result.retry()
        }
    }

    companion object {
        const val KEY_TYPE = "push_type"
        const val KEY_CLOUD_ID = "cloud_id"
        const val TYPE_TASK = "task"
        const val TYPE_REMINDER = "reminder"
        const val TYPE_SUBTASK = "subtask"
        const val TYPE_DELETE_TASK = "delete_task"
        const val TYPE_DELETE_SUBTASK = "delete_subtask"
    }
}
