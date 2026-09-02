package com.pixelpal.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pixelpal.app.data.local.db.entity.CompanionEntity
import kotlinx.coroutines.flow.Flow

/** Single-companion data access: one primary row is expected at all times. */
@Dao
interface CompanionDao {
    @Query("SELECT * FROM companions ORDER BY isFavorite DESC, lastUsedAt DESC, id ASC LIMIT 1")
    fun getPrimary(): Flow<CompanionEntity?>

    @Query("SELECT * FROM companions ORDER BY isFavorite DESC, lastUsedAt DESC, id ASC LIMIT 1")
    suspend fun getPrimaryDirect(): CompanionEntity?

    @Query("SELECT * FROM companions WHERE id = :id")
    fun getById(id: Long): Flow<CompanionEntity?>

    @Query("SELECT * FROM companions WHERE id = :id")
    suspend fun getByIdDirect(id: Long): CompanionEntity?

    @Query("SELECT * FROM companions")
    suspend fun getAllDirect(): List<CompanionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(companion: CompanionEntity): Long

    @Update
    suspend fun update(companion: CompanionEntity)

    @Query("UPDATE companions SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE companions SET lastUsedAt = :time WHERE id = :id")
    suspend fun setLastUsed(id: Long, time: Long)

    @Query("DELETE FROM companions")
    suspend fun deleteAll()
}