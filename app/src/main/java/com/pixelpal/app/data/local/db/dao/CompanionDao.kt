package com.pixelpal.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pixelpal.app.data.local.db.entity.CompanionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanionDao {
    @Query("SELECT * FROM companion WHERE id = 1")
    fun getCompanion(): Flow<CompanionEntity?>

    @Query("SELECT * FROM companion WHERE id = 1")
    suspend fun getCompanionDirect(): CompanionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(companion: CompanionEntity)
}
