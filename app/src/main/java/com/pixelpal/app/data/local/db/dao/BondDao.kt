package com.pixelpal.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pixelpal.app.data.local.db.entity.BondEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BondDao {
    @Query("SELECT * FROM bond WHERE companionId = :companionId")
    fun getBond(companionId: Long): Flow<BondEntity?>

    @Query("SELECT * FROM bond WHERE companionId = :companionId")
    suspend fun getBondDirect(companionId: Long): BondEntity?

    @Query("SELECT * FROM bond")
    suspend fun getAllDirect(): List<BondEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(bond: BondEntity)
}