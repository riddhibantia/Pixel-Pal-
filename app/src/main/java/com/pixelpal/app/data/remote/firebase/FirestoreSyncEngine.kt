package com.pixelpal.app.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.pixelpal.app.data.local.db.entity.ReminderEntity
import com.pixelpal.app.data.local.db.entity.SubtaskEntity
import com.pixelpal.app.data.local.db.entity.TaskEntity
import com.pixelpal.app.domain.model.Bond
import com.pixelpal.app.domain.model.Companion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real-time Cloud Firestore replication: offline-first local persistence with
 * cloud backup and last-write-wins conflict resolution.
 *
 * Cloud document ids for tasks/reminders are the local row's stable `cloudId`
 * UUID — never the device-local Room autoincrement id, which collides across
 * devices.
 */
@Singleton
class FirestoreSyncEngine @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    /** Engine-owned scope so pushes never block the caller (UI coroutine) on network I/O. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val currentUserEmail: String?
        get() = auth.currentUser?.email

    val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    val isAnonymousUser: Boolean
        get() = auth.currentUser?.isAnonymous == true

    private fun tasksRef(uid: String) =
        firestore.collection("users").document(uid).collection("tasks")

    private fun remindersRef(uid: String) =
        firestore.collection("users").document(uid).collection("reminders")

    private fun subtasksRef(uid: String) =
        firestore.collection("users").document(uid).collection("subtasks")

    // ==========================================
    // COMPANION SYNC
    // ==========================================

    /**
     * Uploads the active companion to `users/{userId}/companion/primary`.
     */
    suspend fun syncCompanionToCloud(
        companion: Companion,
        updatedAt: Long = System.currentTimeMillis()
    ): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        return withContext(Dispatchers.IO) {
            try {
                val cloudCompanion = companion.toFirestore(updatedAt)
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

    fun pushCompanionAsync(companion: Companion) {
        scope.launch { syncCompanionToCloud(companion) }
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
     * Syncs a single task to `users/{userId}/tasks/{cloudId}`.
     */
    suspend fun syncTaskToCloud(task: TaskEntity): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        if (task.cloudId.isBlank()) {
            return Result.failure(IllegalStateException("Task ${task.id} has no cloudId"))
        }
        return withContext(Dispatchers.IO) {
            try {
                tasksRef(uid).document(task.cloudId)
                    .set(task.toFirestore(), SetOptions.merge())
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync task to cloud")
                Result.failure(e)
            }
        }
    }

    fun pushTaskAsync(task: TaskEntity) {
        scope.launch { syncTaskToCloud(task) }
    }

    /**
     * Deletes a task from cloud Firestore by its stable cloudId.
     */
    suspend fun deleteTaskFromCloud(cloudId: String): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        if (cloudId.isBlank()) return Result.success(Unit)
        return withContext(Dispatchers.IO) {
            try {
                tasksRef(uid).document(cloudId).delete().await()
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete task from cloud")
                Result.failure(e)
            }
        }
    }

    fun deleteTaskAsync(cloudId: String) {
        scope.launch { deleteTaskFromCloud(cloudId) }
    }

    /**
     * Fetches all cloud tasks.
     */
    suspend fun pullTasksFromCloud(): Result<List<FirestoreTask>> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = tasksRef(uid).get().await()
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

        val listener = tasksRef(uid).addSnapshotListener { snapshot, error ->
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
     * Syncs a single reminder to `users/{userId}/reminders/{cloudId}`.
     */
    suspend fun syncReminderToCloud(reminder: ReminderEntity): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        if (reminder.cloudId.isBlank()) {
            return Result.failure(IllegalStateException("Reminder ${reminder.id} has no cloudId"))
        }
        return withContext(Dispatchers.IO) {
            try {
                remindersRef(uid).document(reminder.cloudId)
                    .set(reminder.toFirestore(), SetOptions.merge())
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync reminder to cloud")
                Result.failure(e)
            }
        }
    }

    fun pushReminderAsync(reminder: ReminderEntity) {
        scope.launch { syncReminderToCloud(reminder) }
    }

    /**
     * Deletes a reminder from Cloud Firestore by its stable cloudId.
     */
    suspend fun deleteReminderFromCloud(cloudId: String): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        if (cloudId.isBlank()) return Result.success(Unit)
        return withContext(Dispatchers.IO) {
            try {
                remindersRef(uid).document(cloudId).delete().await()
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete reminder from cloud")
                Result.failure(e)
            }
        }
    }

    fun deleteReminderAsync(cloudId: String) {
        scope.launch { deleteReminderFromCloud(cloudId) }
    }

    /**
     * Fetches all cloud reminders.
     */
    suspend fun pullRemindersFromCloud(): Result<List<FirestoreReminder>> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = remindersRef(uid).get().await()
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

        val listener = remindersRef(uid).addSnapshotListener { snapshot, error ->
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
    suspend fun syncBondToCloud(
        bond: Bond,
        updatedAt: Long = System.currentTimeMillis()
    ): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        return withContext(Dispatchers.IO) {
            try {
                val cloudBond = bond.toFirestore(updatedAt)
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

    fun pushBondAsync(bond: Bond) {
        scope.launch { syncBondToCloud(bond) }
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
    // SUBTASKS SYNC
    // ==========================================

    /**
     * Syncs a single subtask to `users/{userId}/subtasks/{cloudId}`,
     * linked to its task via parentCloudId.
     */
    suspend fun syncSubtaskToCloud(subtask: SubtaskEntity, parentCloudId: String): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        if (subtask.cloudId.isBlank() || parentCloudId.isBlank()) {
            return Result.failure(IllegalStateException("Subtask ${subtask.id} has no cloudId or parent"))
        }
        return withContext(Dispatchers.IO) {
            try {
                subtasksRef(uid).document(subtask.cloudId)
                    .set(subtask.toFirestore(parentCloudId), SetOptions.merge())
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync subtask to cloud")
                Result.failure(e)
            }
        }
    }

    fun pushSubtaskAsync(subtask: SubtaskEntity, parentCloudId: String) {
        scope.launch { syncSubtaskToCloud(subtask, parentCloudId) }
    }

    suspend fun deleteSubtaskFromCloud(cloudId: String): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        if (cloudId.isBlank()) return Result.success(Unit)
        return withContext(Dispatchers.IO) {
            try {
                subtasksRef(uid).document(cloudId).delete().await()
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete subtask from cloud")
                Result.failure(e)
            }
        }
    }

    fun deleteSubtaskAsync(cloudId: String) {
        scope.launch { deleteSubtaskFromCloud(cloudId) }
    }

    suspend fun pullSubtasksFromCloud(): Result<List<FirestoreSubtask>> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = subtasksRef(uid).get().await()
                val subtasks = snapshot.documents.mapNotNull { it.toObject(FirestoreSubtask::class.java) }
                Result.success(subtasks)
            } catch (e: Exception) {
                Timber.e(e, "Failed to pull subtasks from cloud")
                Result.failure(e)
            }
        }
    }

    fun observeCloudSubtasks(): Flow<List<FirestoreSubtask>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = subtasksRef(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Timber.e(error, "Firestore subtasks listener error")
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(FirestoreSubtask::class.java) } ?: emptyList()
            trySend(list)
        }

        awaitClose { listener.remove() }
    }

    // ==========================================
    // FULL PUSH ALL
    // ==========================================

    /**
     * Pushes all local state (companion, tasks, reminders, bond) to Cloud
     * Firestore in a single batch, keyed by stable cloud ids.
     */
    suspend fun pushAllLocalToCloud(
        companion: Companion?,
        tasks: List<TaskEntity>,
        subtasks: List<SubtaskEntity> = emptyList(),
        reminders: List<ReminderEntity>,
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

                tasks.filter { it.cloudId.isNotBlank() }.forEach { task ->
                    batch.set(tasksRef(uid).document(task.cloudId), task.toFirestore(), SetOptions.merge())
                }

                reminders.filter { it.cloudId.isNotBlank() }.forEach { reminder ->
                    batch.set(remindersRef(uid).document(reminder.cloudId), reminder.toFirestore(), SetOptions.merge())
                }

                val taskCloudIdById = tasks.associate { it.id to it.cloudId }
                subtasks.filter { it.cloudId.isNotBlank() }.forEach { subtask ->
                    val parentCloudId = taskCloudIdById[subtask.taskId]
                    if (!parentCloudId.isNullOrBlank()) {
                        batch.set(
                            subtasksRef(uid).document(subtask.cloudId),
                            subtask.toFirestore(parentCloudId),
                            SetOptions.merge()
                        )
                    }
                }

                if (bond != null) {
                    val bondRef = firestore.collection("users").document(uid).collection("metrics").document("bond")
                    batch.set(bondRef, bond.toFirestore(), SetOptions.merge())
                }

                batch.commit().await()
                Timber.d(
                    "Pushed %d tasks, %d subtasks and %d reminders to Cloud Firestore",
                    tasks.size, subtasks.size, reminders.size
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed full local-to-cloud push")
                Result.failure(e)
            }
        }
    }
}
