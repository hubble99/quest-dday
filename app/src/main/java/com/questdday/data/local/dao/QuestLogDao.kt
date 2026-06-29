package com.questdday.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.questdday.data.local.entity.QuestLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: QuestLogEntity): Long

    @Query("SELECT * FROM quest_logs WHERE quest_id = :questId AND log_date = :date")
    suspend fun getLogForDate(questId: Long, date: String): QuestLogEntity?

    @Query("SELECT * FROM quest_logs WHERE user_id = :userId AND log_date = :date")
    fun getLogsForUserOnDate(userId: Long, date: String): Flow<List<QuestLogEntity>>

    @Query("SELECT COUNT(*) FROM quest_logs WHERE quest_id = :questId")
    suspend fun getTotalCompletionsForQuest(questId: Long): Int

    @Query("SELECT COALESCE(SUM(exp_awarded), 0) FROM quest_logs WHERE quest_id = :questId")
    suspend fun getTotalExpForQuest(questId: Long): Double

    @Query("SELECT * FROM quest_logs WHERE user_id = :userId")
    fun getLogsForUser(userId: Long): Flow<List<QuestLogEntity>>

    /** Distinct completed dates for a quest within a range — for missed session calculation. */
    @Query("""
        SELECT DISTINCT log_date FROM quest_logs 
        WHERE quest_id = :questId 
        AND log_date >= :fromDate 
        AND log_date <= :toDate
    """)
    suspend fun getCompletedDatesForQuest(questId: Long, fromDate: String, toDate: String): List<String>

    /** One-shot variant for lazy evaluation (inside @Transaction, cannot use Flow). */
    @Query("SELECT * FROM quest_logs WHERE user_id = :userId AND log_date = :date")
    suspend fun getLogsForUserOnDateOnce(userId: Long, date: String): List<QuestLogEntity>
}
