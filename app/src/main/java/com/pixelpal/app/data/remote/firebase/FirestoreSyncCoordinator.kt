package com.pixelpal.app.data.remote.firebase

import com.pixelpal.app.data.local.db.dao.BondDao
import com.pixelpal.app.data.local.db.dao.CompanionDao
import com.pixelpal.app.data.local.db.dao.ReminderDao
import com.pixelpal.app.data.local.db.dao.SubtaskDao
import com.pixelpal.app.data.local.db.dao.TaskDao
import com.pixelpal.app.data.local.db.entity.BondEntity
import com.pixelpal.app.data.local.db.entity.CompanionEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates true 2-way Cloud Firestore synchronization:
 *
 *  - Cloud → local: full pull-on-login plus real-time snapshot listeners that
 *    apply remote tasks/reminders into Room with last-write-wins on `updatedAt`.
 *    Remote changes are written through the DAOs directly (never the
 *    repositories), so they never echo back to the cloud.
 *  - Local → cloud: repositories push each write through the engine, and
 *    [fullSync] batch-pushes everything (also the hourly worker path).
 *  - Companion & bond are singleton documents restored from the cloud only
 *    when the newer side wins on `updatedAt` (last-write-wins).
 *  - [signOut] wipes all local user data BEFORE auth sign-out so the next
 *    account (or guest) never inherits the previous user's data.
 */
@Singleton
class FirestoreSyncCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val companionDao: CompanionDao,
    private val taskDao: TaskDao,
    private val reminderDao: ReminderDao,
    private val subtaskDao: SubtaskDao,
    private val bondDao: BondDao,
    private val syncEngine: FirestoreSyncEngine,
    private val authManager: FirebaseAuthManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncError = MutableStateFlow<String?>(null)
    /** Last full-sync failure message (null when the last sync succeeded). */
    val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

    val isUserLoggedIn: Boolean
        get() = syncEngine.isUserLoggedIn

    val isAnonymousUser: Boolean
        get() = syncEngine.isAnonymousUser

    val currentUserEmail: String?
        get() = syncEngine.currentUserEmail

    /** Bumped on every sign-out so a stale in-flight sync can abort instead of clobbering. */
    private var sessionEpoch = 0

    private var realtimeUid: String? = null
    private var realtimeJobs = emptyList<kotlinx.coroutines.Job>()

    /**
     * Starts the real-time cloud → local listeners, pinned to the CURRENT
     * authenticated uid. Safe to call repeatedly; no-ops when signed out so a
     * later [onUserAuthenticated] can start them once auth is ready.
     *
     * Previously this captured the uid once at Application start (usually
     * signed-out) and never rebound after login, so post-login realtime sync
     * silently never ran.
     */
    fun startRealtimeSync() {
        val uid = syncEngine.currentUserId
        if (uid.isNullOrBlank()) {
            Timber.d("[FIRESTORE] Realtime sync deferred: no authenticated user yet")
            return
        }
        if (realtimeUid == uid && realtimeJobs.any { it.isActive }) return
        cancelRealtimeSync()
        realtimeUid = uid
        Timber.d("[FIRESTORE] Starting realtime listeners for uid=%s", uid)

        realtimeJobs = listOf(
            scope.launch {
                syncEngine.observeCloudTasksFor(uid).collect { remoteTasks ->
                    runCatching { remoteTasks.forEach { applyCloudTask(it) } }
                        .onFailure { Timber.e(it, "[FIRESTORE] Failed applying cloud tasks") }
                }
            },
            scope.launch {
                syncEngine.observeCloudRemindersFor(uid).collect { remoteReminders ->
                    runCatching { remoteReminders.forEach { applyCloudReminder(it) } }
                        .onFailure { Timber.e(it, "[FIRESTORE] Failed applying cloud reminders") }
                }
            },
            scope.launch {
                syncEngine.observeCloudSubtasksFor(uid).collect { remoteSubtasks ->
                    runCatching { remoteSubtasks.forEach { applyCloudSubtask(it) } }
                        .onFailure { Timber.e(it, "[FIRESTORE] Failed applying cloud subtasks") }
                }
            }
        )
    }

    /**
     * Call immediately after Firebase Auth succeeds (uid available): rebinds
     * realtime listeners to the new uid and runs a full pull → push restore.
     * Awaiting the returned Result before navigating guarantees the companion
     * and metrics are in Room before Home renders.
     */
    suspend fun onUserAuthenticated(): Result<Unit> {
        val uid = syncEngine.currentUserId
        Timber.d("[AUTH] Session established, uid=%s. Rebinding sync.", uid)
        startRealtimeSync()
        return fullSync()
    }

    private fun cancelRealtimeSync() {
        realtimeJobs.forEach { it.cancel() }
        realtimeJobs = emptyList()
        realtimeUid = null
    }

    /**
     * Full bidirectional sync: pull cloud → local, then push local → cloud.
     * Used after sign-in, by the periodic worker, and by "Sync Now".
     */
    suspend fun fullSync(): Result<Unit> {
        val uid = syncEngine.currentUserId
        if (uid.isNullOrBlank()) {
            Timber.w("[FIRESTORE] fullSync skipped: no authenticated user")
            return Result.failure(IllegalStateException("No user logged in"))
        }
        val epoch = sessionEpoch
        _isSyncing.value = true
        try {
            Timber.d("[FIRESTORE] fullSync start for uid=%s", uid)
            pullAndApplyCloud()
            if (epoch != sessionEpoch) {
                Timber.w("[FIRESTORE] fullSync aborted: session changed mid-sync")
                return Result.failure(java.util.concurrent.CancellationException("Session changed"))
            }

            val primary = companionDao.getPrimaryDirect()
            val tasks = primary?.let { taskDao.getTasksDirect(it.id) } ?: emptyList()
            val subtasks = subtaskDao.getAllDirect()
            val reminders = reminderDao.getAllRemindersDirect()
            val bond = primary?.let { bondDao.getBondDirect(it.id) }

            val pushResult = syncEngine.pushAllLocalToCloud(
                companion = primary?.toDomainModel(),
                tasks = tasks,
                subtasks = subtasks,
                reminders = reminders,
                bond = bond?.toDomainModel()
            )
            if (pushResult.isFailure) {
                val msg = pushResult.exceptionOrNull()?.message
                _lastSyncError.value = msg
                Timber.w("[FIRESTORE] fullSync push failed for uid=%s: %s", uid, msg)
            } else {
                _lastSyncError.value = null
                Timber.d(
                    "[FIRESTORE] fullSync complete for uid=%s (companion=%s)",
                    uid, primary?.name
                )
            }
            return pushResult
        } finally {
            _isSyncing.value = false
        }
    }

    /** Fire-and-forget full sync (sign-in path). */
    fun syncInBackground() {
        scope.launch { fullSync() }
    }

    /**
     * Pull-on-login restore: applies cloud tasks/reminders with last-write-wins,
     * and restores companion/bond from the cloud when the cloud copy is newer.
     */
    private suspend fun pullAndApplyCloud() {
        syncEngine.pullTasksFromCloud().onSuccess { remoteTasks ->
            remoteTasks.forEach { applyCloudTask(it) }
        }.onFailure { Timber.w(it, "Pull tasks failed") }

        syncEngine.pullRemindersFromCloud().onSuccess { remoteReminders ->
            remoteReminders.forEach { applyCloudReminder(it) }
        }.onFailure { Timber.w(it, "Pull reminders failed") }

        syncEngine.pullSubtasksFromCloud().onSuccess { remoteSubtasks ->
            remoteSubtasks.forEach { applyCloudSubtask(it) }
        }.onFailure { Timber.w(it, "Pull subtasks failed") }

        applyCloudCompanion()
        applyCloudBond()
    }

    private suspend fun applyCloudTask(cloud: FirestoreTask) {
        if (cloud.id.isBlank()) return
        val local = taskDao.getByCloudId(cloud.id)
        if (local == null) {
            val companionId = companionDao.getPrimaryDirect()?.id ?: return
            taskDao.insert(cloud.toEntity(localId = 0, companionId = companionId))
        } else if (cloud.updatedAt > local.updatedAt) {
            taskDao.update(cloud.toEntity(localId = local.id, companionId = local.companionId))
        }
    }

    private suspend fun applyCloudReminder(cloud: FirestoreReminder) {
        if (cloud.id.isBlank()) return
        val local = reminderDao.getByCloudId(cloud.id)
        if (local == null) {
            val companionId = companionDao.getPrimaryDirect()?.id
            reminderDao.insert(cloud.toEntity(localId = 0, companionId = companionId))
        } else if (cloud.updatedAt > local.updatedAt) {
            reminderDao.update(cloud.toEntity(localId = local.id, companionId = local.companionId))
        }
    }

    private suspend fun applyCloudSubtask(cloud: FirestoreSubtask) {
        if (cloud.id.isBlank() || cloud.parentCloudId.isBlank()) return
        val local = subtaskDao.getByCloudId(cloud.id)
        val parent = taskDao.getByCloudId(cloud.parentCloudId) ?: return
        if (local == null) {
            subtaskDao.insert(cloud.toSubtaskEntity(localId = 0, taskId = parent.id))
        } else if (local.taskId != parent.id || cloud.updatedAt > local.updatedAt) {
            subtaskDao.update(cloud.toSubtaskEntity(localId = local.id, taskId = parent.id))
        }
    }

    private suspend fun applyCloudCompanion() {
        val cloudResult = syncEngine.pullCompanionFromCloud()
        val cloud = cloudResult.getOrNull()
        if (cloud == null) {
            if (cloudResult.isFailure) {
                Timber.w(
                    "[FIRESTORE] Companion pull failed: %s",
                    cloudResult.exceptionOrNull()?.message
                )
            } else {
                Timber.d("[FIRESTORE] No cloud companion yet (new user)")
            }
            return
        }
        val local = companionDao.getPrimaryDirect()
        if (local == null) {
            companionDao.insert(cloud.toEntityWithTimestamps(cloudUpdatedAt = cloud.updatedAt))
            Timber.d("[FIRESTORE] Companion restored from cloud (name=%s)", cloud.name)
        } else if (cloud.updatedAt > local.updatedAt || isFreshUneditedLocal(local)) {
            // A fresh bootstrap seed (createdAt == updatedAt, never edited) always
            // loses to an existing cloud row: otherwise a post-wipe reseed with a
            // newer timestamp would permanently hide the user's real companion.
            companionDao.update(cloud.toEntityWithTimestamps(cloudUpdatedAt = cloud.updatedAt, local = local))
            Timber.d("[FIRESTORE] Companion restored from cloud (name=%s)", cloud.name)
        }
    }

    /** True when the local row is a just-seeded default the user never edited. */
    private fun isFreshUneditedLocal(local: CompanionEntity): Boolean =
        local.createdAt == local.updatedAt

    private fun CompanionEntity.toDomainModel() = com.pixelpal.app.domain.model.Companion(
        id = id,
        name = name,
        petType = petType,
        role = com.pixelpal.app.domain.model.CompanionRole.fromId(role),
        description = description,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt,
        isFavorite = isFavorite,
        isArchived = isArchived,
        hatId = hatId,
        outfitId = outfitId,
        accessoryId = accessoryId,
        species = species,
        color = color,
        pattern = pattern
    )

    private fun BondEntity.toDomainModel() = com.pixelpal.app.domain.model.Bond(
        companionId = companionId,
        level = level,
        totalInteractions = totalInteractions,
        tapsToday = tapsToday,
        feedsToday = feedsToday,
        lastInteractionTime = lastInteractionTime,
        streakDays = streakDays,
        lastStreakDate = lastStreakDate
    )

    private suspend fun FirestoreCompanion.toEntityWithTimestamps(
        cloudUpdatedAt: Long,
        local: CompanionEntity? = null
    ): CompanionEntity {
        val domain = toDomain(id = local?.id ?: 0)
        return CompanionEntity(
            id = domain.id,
            name = domain.name,
            petType = domain.petType,
            role = domain.role.id,
            description = domain.description,
            createdAt = local?.createdAt ?: System.currentTimeMillis(),
            lastUsedAt = local?.lastUsedAt,
            isFavorite = domain.isFavorite,
            isArchived = local?.isArchived ?: false,
            hatId = domain.hatId,
            outfitId = domain.outfitId,
            accessoryId = domain.accessoryId,
            species = domain.species,
            color = domain.color,
            pattern = domain.pattern,
            updatedAt = cloudUpdatedAt
        )
    }

    private suspend fun applyCloudBond() {
        val cloudResult = syncEngine.pullBondFromCloud()
        val cloud = cloudResult.getOrNull()
        if (cloud == null) {
            if (cloudResult.isFailure) {
                Timber.w(
                    "[FIRESTORE] Bond pull failed: %s",
                    cloudResult.exceptionOrNull()?.message
                )
            }
            return
        }
        val primary = companionDao.getPrimaryDirect() ?: return
        val local = bondDao.getBondDirect(primary.id)
        if (local == null) {
            bondDao.insertOrUpdate(
                BondEntity(
                    companionId = primary.id,
                    level = cloud.level,
                    totalInteractions = cloud.totalInteractions,
                    tapsToday = cloud.tapsToday,
                    feedsToday = cloud.feedsToday,
                    lastInteractionTime = cloud.lastInteractionTime,
                    streakDays = cloud.streakDays,
                    lastStreakDate = cloud.lastStreakDate,
                    updatedAt = cloud.updatedAt
                )
            )
            Timber.d("[FIRESTORE] Bond metrics restored from cloud (level=%d)", cloud.level)
        } else if (cloud.updatedAt > local.updatedAt || isFreshDefaultBond(local)) {
            bondDao.insertOrUpdate(
                local.copy(
                    level = cloud.level,
                    totalInteractions = cloud.totalInteractions,
                    tapsToday = cloud.tapsToday,
                    feedsToday = cloud.feedsToday,
                    lastInteractionTime = cloud.lastInteractionTime,
                    streakDays = cloud.streakDays,
                    lastStreakDate = cloud.lastStreakDate,
                    updatedAt = cloud.updatedAt
                )
            )
        }
    }

    /** True when the local bond row is an untouched ensureExists() default. */
    private fun isFreshDefaultBond(local: BondEntity): Boolean =
        local.totalInteractions == 0 && local.level <= 0 && local.updatedAt == 0L

    /**
     * Wipes ALL local user data, then signs out of Firebase. Guarantees the
     * next sign-in (any account, including guest) starts clean instead of
     * pushing the previous user's data under the new uid.
     *
     * The wipe runs BEFORE auth sign-out in the same coroutine, the realtime
     * listeners are cancelled, and [sessionEpoch] is bumped so any in-flight
     * fullSync aborts instead of re-pushing stale rows under the next uid.
     */
    fun signOut() {
        scope.launch { signOutSuspend() }
    }

    suspend fun signOutSuspend() {
        sessionEpoch++
        cancelRealtimeSync()
        try {
            clearAllLocalData()
        } catch (e: Exception) {
            Timber.e(e, "[AUTH] Failed to clear local data on sign-out")
        }
        Timber.d("[AUTH] Signed out, local session cleared")
        authManager.signOut()
    }

    private suspend fun clearAllLocalData() {
        subtaskDao.deleteAll()
        taskDao.deleteAll()
        reminderDao.deleteAll()
        bondDao.deleteAll()
        companionDao.deleteAll()
        Timber.d("Local user data cleared for sign-out")
    }
}
