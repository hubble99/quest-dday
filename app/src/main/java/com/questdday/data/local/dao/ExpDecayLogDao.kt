package com.questdday.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.questdday.data.local.entity.ExpDecayLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpDecayLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecayLog(log: ExpDecayLogEntity): Long

    @Query("SELECT * FROM exp_decay_log ORDER BY processed_at DESC LIMIT :limit")
    fun getRecentDecayLogs(limit: Int): Flow<List<ExpDecayLogEntity>>
}
