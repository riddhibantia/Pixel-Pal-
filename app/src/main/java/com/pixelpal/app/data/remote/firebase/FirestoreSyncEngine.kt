package com.pixelpal.app.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.pixelpal.app.domain.model.Bond
import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.model.Reminder
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
 * 2-Way Real-time Cloud Sync Engine using Firebase Cloud Firestore.
 * Ensures offline-first local persistence with seamless cloud backup and replication.
 */
@Singleton
class FirestoreSyncEngine @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    val currentUserId: String?
        get() = auth.currentUser?.uid

    val currentUserEmail: String?
        get() = auth.currentUser?.email

    val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    val isAnonymousUser: Boolean
        get() = auth.currentUser?.isAnonymous == true

    // ==========================================
    // COMPANION SYNC
    // ==========================================

    /**
     * Uploads the active companion to `users/{userId}/companion/primary`.
     */
    suspend fun syncCompanionToCloud(companion: Companion): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        return withContext(Dispatchers.IO) {
            try {
                val cloudCompanion = companion.toFirestore()
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
     * Fetches the current companion from Cloud Firestore.
     */
    suspend fun pullCompanionFromCloud(): Result<FirestoreCompanion?> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("users")
                    .document(uid)
                    .collection("companion")
                    .document("primary")
                    .get()
                    .await()
                if (snapshot.exists()) {
                    Result.success(snapshot.toObject(FirestoreCompanion::class.java))
                } else {
                    Result.success(null)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to pull companion from cloud")
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
                trySend(snapshot.toObject(FirestoreCompanion::class.java))
            } else {
                trySend(null)
            }
        }

        awaitClose { listener.remove() }
    }

    // ==========================================
    // TASKS SYNC
    // ==========================================

    /**
     * Syncs a single task to `users/{userId}/tasks/{taskId}`.
     */
    suspend fun syncTaskToCloud(task: Task): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        return withContext(Dispatchers.IO) {
            try {
                val cloudTask = task.toFirestore()
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

    /**
     * Fetches all cloud tasks.
     */
    suspend fun pullTasksFromCloud(): Result<List<FirestoreTask>> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("users")
                    .document(uid)
                    .collection("tasks")
                    .get()
                    .await()
                val tasks = snapshot.documents.mapNotNull { it.toObject(FirestoreTask::class.java) }
                Result.success(tasks)
            } catch (e: Exception) {
                Timber.e(e, "Failed to pull tasks from cloud")
                Result.failure(e)
            }
        }
    }

    /**
     * Observes real-time changes to the cloud tasks collection.
     */
    fun observeCloudTasks(): Flow<List<FirestoreTask>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val colRef = firestore.collection("users")
            .document(uid)
            .collection("tasks")

        val listener = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Timber.e(error, "Firestore tasks listener error")
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(FirestoreTask::class.java) } ?: emptyList()
            trySend(list)
        }

        awaitClose { listener.remove() }
    }

    // ==========================================
    // REMINDERS SYNC
    // ==========================================

    /**
     * Syncs a single reminder to `users/{userId}/reminders/{reminderId}`.
     */
    suspend fun syncReminderToCloud(reminder: Reminder): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        return withContext(Dispatchers.IO) {
            try {
                val cloudReminder = reminder.toFirestore()
                firestore.collection("users")
                    .document(uid)
                    .collection("reminders")
                    .document(reminder.id.toString())
                    .set(cloudReminder, SetOptions.merge())
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync reminder to cloud")
                Result.failure(e)
            }
        }
    }

    /**
     * Deletes a reminder from Cloud Firestore.
     */
    suspend fun deleteReminderFromCloud(reminderId: Long): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        return withContext(Dispatchers.IO) {
            try {
                firestore.collection("users")
                    .document(uid)
                    .collection("reminders")
                    .document(reminderId.toString())
                    .delete()
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete reminder from cloud")
                Result.failure(e)
            }
        }
    }

    /**
     * Fetches all cloud reminders.
     */
    suspend fun pullRemindersFromCloud(): Result<List<FirestoreReminder>> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("users")
                    .document(uid)
                    .collection("reminders")
                    .get()
                    .await()
                val reminders = snapshot.documents.mapNotNull { it.toObject(FirestoreReminder::class.java) }
                Result.success(reminders)
            } catch (e: Exception) {
                Timber.e(e, "Failed to pull reminders from cloud")
                Result.failure(e)
            }
        }
    }

    /**
     * Observes real-time changes to the cloud reminders collection.
     */
    fun observeCloudReminders(): Flow<List<FirestoreReminder>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val colRef = firestore.collection("users")
            .document(uid)
            .collection("reminders")

        val listener = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Timber.e(error, "Firestore reminders listener error")
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(FirestoreReminder::class.java) } ?: emptyList()
            trySend(list)
        }

        awaitClose { listener.remove() }
    }

    // ==========================================
    // BOND & STREAKS SYNC
    // ==========================================

    /**
     * Syncs bond & streak metrics to `users/{userId}/metrics/bond`.
     */
    suspend fun syncBondToCloud(bond: Bond): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        return withContext(Dispatchers.IO) {
            try {
                val cloudBond = bond.toFirestore()
                firestore.collection("users")
                    .document(uid)
                    .collection("metrics")
                    .document("bond")
                    .set(cloudBond, SetOptions.merge())
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync bond to cloud")
                Result.failure(e)
            }
        }
    }

    /**
     * Fetches cloud bond metrics.
     */
    suspend fun pullBondFromCloud(): Result<FirestoreBond?> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("users")
                    .document(uid)
                    .collection("metrics")
                    .document("bond")
                    .get()
                    .await()
                if (snapshot.exists()) {
                    Result.success(snapshot.toObject(FirestoreBond::class.java))
                } else {
                    Result.success(null)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to pull bond from cloud")
                Result.failure(e)
            }
        }
    }

    // ==========================================
    // FULL SYNC ALL
    // ==========================================

    /**
     * Pushes all local state (companion, tasks, reminders, bond) to Cloud Firestore in a batch.
     */
    suspend fun syncAllLocalToCloud(
        companion: Companion?,
        tasks: List<Task>,
        reminders: List<Reminder>,
        bond: Bond?
    ): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        return withContext(Dispatchers.IO) {
            try {
                val batch = firestore.batch()

                if (companion != null) {
                    val compRef = firestore.collection("users").document(uid).collection("companion").document("primary")
                    batch.set(compRef, companion.toFirestore(), SetOptions.merge())
                }

                tasks.forEach { task ->
                    val taskRef = firestore.collection("users").document(uid).collection("tasks").document(task.id.toString())
                    batch.set(taskRef, task.toFirestore(), SetOptions.merge())
                }

                reminders.forEach { reminder ->
                    val reminderRef = firestore.collection("users").document(uid).collection("reminders").document(reminder.id.toString())
                    batch.set(reminderRef, reminder.toFirestore(), SetOptions.merge())
                }

                if (bond != null) {
                    val bondRef = firestore.collection("users").document(uid).collection("metrics").document("bond")
                    batch.set(bondRef, bond.toFirestore(), SetOptions.merge())
                }

                batch.commit().await()
                Timber.d("Successfully synced all local state to Cloud Firestore")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed full local-to-cloud sync")
                Result.failure(e)
            }
        }
    }
}
