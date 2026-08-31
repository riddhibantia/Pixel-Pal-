package com.pixelpal.app.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sync engine managing real-time cloud replication and local synchronization with Firestore.
 */
@Singleton
class FirestoreSyncEngine @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val currentUserId: String?
        get() = auth.currentUser?.uid

    /**
     * Uploads or syncs the active companion state to `users/{userId}/companion/primary`.
     */
    suspend fun syncCompanionToCloud(companion: Companion): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        return withContext(Dispatchers.IO) {
            try {
                val cloudCompanion = FirestoreCompanion(
                    name = companion.name,
                    petType = companion.petType,
                    role = companion.role,
                    description = companion.description,
                    species = companion.species,
                    color = companion.color,
                    pattern = companion.pattern,
                    hatId = companion.hatId,
                    outfitId = companion.outfitId,
                    accessoryId = companion.accessoryId,
                    isFavorite = companion.isFavorite,
                    updatedAt = System.currentTimeMillis()
                )
                firestore.collection("users")
                    .document(uid)
                    .collection("companion")
                    .document("primary")
                    .set(cloudCompanion, SetOptions.merge())
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync companion to cloud")
                Result.failure(e)
            }
        }
    }

    /**
     * Observes real-time changes to the cloud companion.
     */
    fun observeCloudCompanion(): Flow<FirestoreCompanion?> = callbackFlow {
        val uid = currentUserId
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val docRef = firestore.collection("users")
            .document(uid)
            .collection("companion")
            .document("primary")

        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Timber.e(error, "Firestore companion listener error")
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val companion = snapshot.toObject(FirestoreCompanion::class.java)
                trySend(companion)
            } else {
                trySend(null)
            }
        }

        awaitClose { listener.remove() }
    }

    /**
     * Syncs a task to `users/{userId}/tasks/{taskId}`.
     */
    suspend fun syncTaskToCloud(task: Task): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        return withContext(Dispatchers.IO) {
            try {
                val cloudTask = FirestoreTask(
                    id = task.id.toString(),
                    title = task.title,
                    isDone = task.isDone,
                    dueAt = task.dueAt,
                    createdAt = task.createdAt,
                    completedAt = task.completedAt
                )
                firestore.collection("users")
                    .document(uid)
                    .collection("tasks")
                    .document(task.id.toString())
                    .set(cloudTask, SetOptions.merge())
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync task to cloud")
                Result.failure(e)
            }
        }
    }

    /**
     * Deletes a task from cloud Firestore.
     */
    suspend fun deleteTaskFromCloud(taskId: Long): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        return withContext(Dispatchers.IO) {
            try {
                firestore.collection("users")
                    .document(uid)
                    .collection("tasks")
                    .document(taskId.toString())
                    .delete()
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete task from cloud")
                Result.failure(e)
            }
        }
    }
}
