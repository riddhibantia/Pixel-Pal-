package com.pixelpal.app.data.repository

import com.pixelpal.app.data.local.db.dao.ActivityEventDao
import com.pixelpal.app.data.local.db.entity.ActivityEventEntity
import com.pixelpal.app.domain.model.ActivityEvent
import com.pixelpal.app.domain.model.ActivityType
import com.pixelpal.app.domain.repository.ActivityEventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityEventRepositoryImpl @Inject constructor(
    private val dao: ActivityEventDao
) : ActivityEventRepository {

    override fun getForCompanion(companionId: Long, limit: Int): Flow<List<ActivityEvent>> {
        return dao.getForCompanion(companionId, limit).map { list -> list.map { it.toDomain() } }
    }

    override fun getRecent(limit: Int): Flow<List<ActivityEvent>> {
        return dao.getRecent(limit).map { list -> list.map { it.toDomain() } }
    }

    override fun getCenterEvents(limit: Int): Flow<List<ActivityEvent>> {
        return dao.getCenterEvents(limit).map { list -> list.map { it.toDomain() } }
    }

    override fun getCenterEventsForCompanion(companionId: Long, limit: Int): Flow<List<ActivityEvent>> {
        return dao.getCenterEventsForCompanion(companionId, limit).map { list -> list.map { it.toDomain() } }
    }

    override fun unreadCount(): Flow<Int> = dao.unreadCount()

    override suspend fun markAllRead() = dao.markAllRead()

    override suspend fun record(
        companionId: Long,
        type: ActivityType,
        title: String,
        description: String?
    ) {
        dao.insert(
            ActivityEventEntity(
                companionId = companionId,
                type = type.id,
                title = title,
                description = description,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun insert(event: ActivityEvent) {
        dao.insert(
            ActivityEventEntity(
                companionId = event.companionId,
                type = event.type.id,
                title = event.title,
                description = event.description,
                createdAt = event.createdAt,
                isRead = event.isRead
            )
        )
    }

    private fun ActivityEventEntity.toDomain() = ActivityEvent(
        id = id,
        companionId = companionId,
        type = ActivityType.fromId(type),
        title = title,
        description = description,
        createdAt = createdAt,
        isRead = isRead
    )
}