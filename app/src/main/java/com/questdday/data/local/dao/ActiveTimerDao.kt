package com.questdday.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.questdday.data.local.entity.ActiveTimerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActiveTimerDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTimer(timer: ActiveTimerEntity): Long

    @Query("SELECT * FROM active_timers WHERE quest_id = :questId")
    suspend fun getTimerByQuestId(questId: Long): ActiveTimerEntity?

    @Query("SELECT * FROM active_timers WHERE alarm_fired_at IS NOT NULL")
    fun getPendingConfirmationTimers(): Flow<List<ActiveTimerEntity>>

    @Query("SELECT * FROM active_timers WHERE alarm_fired_at IS NULL")
    fun getRunningTimers(): Flow<List<ActiveTimerEntity>>

    @Query("UPDATE active_timers SET alarm_fired_at = :firedAt WHERE quest_id = :questId")
    suspend fun markAlarmFired(questId: Long, firedAt: String)

    @Query("DELETE FROM active_timers WHERE quest_id = :questId")
    suspend fun deleteTimer(questId: Long)

    @Query("SELECT COUNT(*) FROM active_timers WHERE quest_id = :questId")
    suspend fun timerExistsForQuest(questId: Long): Int
}
