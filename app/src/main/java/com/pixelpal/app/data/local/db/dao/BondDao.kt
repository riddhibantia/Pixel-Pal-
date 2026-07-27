package com.pixelpal.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pixelpal.app.data.local.db.entity.BondEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BondDao {
    @Query("SELECT * FROM bond WHERE id = 1")
    fun getBond(): Flow<BondEntity?>

    @Query("SELECT * FROM bond WHERE id = 1")
    suspend fun getBondDirect(): BondEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(bond: BondEntity)

    @Query("UPDATE bond SET level = :level WHERE id = 1")
    suspend fun updateLevel(level: Int)

    @Query("UPDATE bond SET tapsToday = tapsToday + 1, totalInteractions = totalInteractions + 1, lastInteractionTime = :time WHERE id = 1")
    suspend fun recordTap(time: Long = System.currentTimeMillis())

    @Query("UPDATE bond SET feedsToday = feedsToday + 1, totalInteractions = totalInteractions + 1, lastInteractionTime = :time WHERE id = 1")
    suspend fun recordFeed(time: Long = System.currentTimeMillis())

    @Query("UPDATE bond SET tapsToday = 0, feedsToday = 0 WHERE id = 1")
    suspend fun resetDailyCounts()
}
