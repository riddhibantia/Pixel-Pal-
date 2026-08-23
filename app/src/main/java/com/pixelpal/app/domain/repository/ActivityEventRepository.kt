package com.pixelpal.app.domain.repository

import com.pixelpal.app.domain.model.ActivityEvent
import com.pixelpal.app.domain.model.ActivityType
import kotlinx.coroutines.flow.Flow

interface ActivityEventRepository {
    fun getForCompanion(companionId: Long, limit: Int = 50): Flow<List<ActivityEvent>>
    fun getRecent(limit: Int = 20): Flow<List<ActivityEvent>>

    /** Meaningful events only (TAP/FEED excluded) — the Activity Center source. */
    fun getCenterEvents(limit: Int = 50): Flow<List<ActivityEvent>>
    fun getCenterEventsForCompanion(companionId: Long, limit: Int = 50): Flow<List<ActivityEvent>>

    /** Count of unread meaningful events, for the Home bell badge. */
    fun unreadCount(): Flow<Int>
    suspend fun markAllRead()

    suspend fun record(
        companionId: Long,
        type: ActivityType,
        title: String,
        description: String? = null
    )
    suspend fun insert(event: ActivityEvent)
}