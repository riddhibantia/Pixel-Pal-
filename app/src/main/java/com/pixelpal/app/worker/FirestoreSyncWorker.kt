package com.pixelpal.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pixelpal.app.data.remote.firebase.FirestoreSyncCoordinator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Background WorkManager worker for the periodic full 2-way Cloud Firestore
 * synchronization (pull cloud → local, then push local → cloud).
 */
@HiltWorker
class FirestoreSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncCoordinator: FirestoreSyncCoordinator
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!syncCoordinator.isUserLoggedIn) {
            Timber.d("FirestoreSyncWorker skipped: user not logged in")
            return Result.success()
        }

        return try {
            val result = syncCoordinator.fullSync()
            if (result.isSuccess) {
                Timber.d("FirestoreSyncWorker finished successfully")
                Result.success()
            } else {
                Timber.w(
                    "FirestoreSyncWorker failed: %s",
                    result.exceptionOrNull()?.message
                )
                Result.retry()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error executing FirestoreSyncWorker")
            Result.retry()
        }
    }
}
