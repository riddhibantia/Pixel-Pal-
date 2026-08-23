package com.pixelpal.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pixelpal.app.data.local.db.entity.CompanionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanionDao {
    @Query("SELECT * FROM companions WHERE isArchived = 0 ORDER BY isFavorite DESC, lastUsedAt DESC, id ASC")
    fun getAllActive(): Flow<List<CompanionEntity>>

    @Query("SELECT * FROM companions WHERE isArchived = 0 ORDER BY isFavorite DESC, lastUsedAt DESC, id ASC")
    suspend fun getAllActiveDirect(): List<CompanionEntity>

    @Query("SELECT * FROM companions ORDER BY isArchived ASC, isFavorite DESC, createdAt ASC")
    fun getAll(): Flow<List<CompanionEntity>>

    @Query("SELECT * FROM companions ORDER BY isArchived ASC, isFavorite DESC, createdAt ASC")
    suspend fun getAllDirect(): List<CompanionEntity>

    @Query("SELECT * FROM companions WHERE isArchived = 1 ORDER BY createdAt DESC")
    fun getArchived(): Flow<List<CompanionEntity>>

    @Query("SELECT * FROM companions WHERE id = :id")
    fun getById(id: Long): Flow<CompanionEntity?>

    @Query("SELECT * FROM companions WHERE id = :id")
    suspend fun getByIdDirect(id: Long): CompanionEntity?

    @Query("SELECT COUNT(*) FROM companions WHERE isArchived = 0")
    suspend fun countActive(): Int

    @Query("SELECT * FROM companions WHERE isArchived = 0 LIMIT 1")
    suspend fun firstActiveDirect(): CompanionEntity?

    @Query("SELECT * FROM companions LIMIT 1")
    suspend fun firstAnyDirect(): CompanionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(companion: CompanionEntity): Long

    @Update
    suspend fun update(companion: CompanionEntity)

    @Query("UPDATE companions SET isArchived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Query("UPDATE companions SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE companions SET lastUsedAt = :time WHERE id = :id")
    suspend fun setLastUsed(id: Long, time: Long)
}