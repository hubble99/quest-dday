package com.questdday.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.questdday.data.local.entity.QuestHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: QuestHistoryEntity): Long

    @Query("SELECT * FROM quest_history WHERE user_id = :userId ORDER BY ended_at DESC")
    fun getHistoryByUser(userId: Long): Flow<List<QuestHistoryEntity>>
}
