package com.pixelpal.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pixelpal.app.data.local.db.entity.PersonalityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalityDao {
    @Query("SELECT * FROM personality WHERE companionId = :companionId")
    fun getPersonality(companionId: Long): Flow<PersonalityEntity?>

    @Query("SELECT * FROM personality WHERE companionId = :companionId")
    suspend fun getPersonalityDirect(companionId: Long): PersonalityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(personality: PersonalityEntity)
}