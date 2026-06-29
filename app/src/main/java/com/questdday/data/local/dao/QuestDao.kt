package com.questdday.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.questdday.data.local.entity.QuestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuest(quest: QuestEntity): Long

    @Query("SELECT * FROM quests WHERE user_id = :userId AND status = 'active' AND is_container = 0")
    fun getActiveExecutableQuests(userId: Long): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests WHERE user_id = :userId AND is_container = 1 AND status = 'active'")
    fun getActiveEpicContainers(userId: Long): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests WHERE parent_quest_id = :parentId AND status = 'active'")
    fun getActiveSubQuests(parentId: Long): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests WHERE id = :id")
    suspend fun getQuestById(id: Long): QuestEntity?

    @Query("""
        SELECT * FROM quests 
        WHERE user_id = :userId 
        AND status = 'active' 
        AND is_container = 0
        AND schedule_type = 'daily'
        AND date(:today) >= date(start_date)
        AND (end_date IS NULL OR date(:today) <= date(end_date))
    """)
    fun getDailyQuestsForToday(userId: Long, today: String): Flow<List<QuestEntity>>

    @Query("""
        SELECT * FROM quests 
        WHERE user_id = :userId 
        AND status = 'active' 
        AND is_container = 0
        AND schedule_type = 'custom_days'
        AND date(:today) >= date(start_date)
        AND (end_date IS NULL OR date(:today) <= date(end_date))
    """)
    fun getCustomScheduleQuests(userId: Long, today: String): Flow<List<QuestEntity>>

    @Query("UPDATE quests SET status = :status, updated_at = :now WHERE id = :id")
    suspend fun updateQuestStatus(id: Long, status: String, now: String)

    @Query("""
        UPDATE quests 
        SET consecutive_missed_sessions = :sessions, updated_at = :now 
        WHERE id = :id
    """)
    suspend fun updateMissedSessions(id: Long, sessions: Int, now: String)

    @Query("UPDATE quests SET end_date = :newEndDate, updated_at = :now WHERE id = :id")
    suspend fun updateEndDate(id: Long, newEndDate: String, now: String)

    @Query("""
        UPDATE quests 
        SET stacked_duration_seconds = stacked_duration_seconds + :amount, updated_at = :now 
        WHERE id = :id
    """)
    suspend fun addStackedDuration(id: Long, amount: Int, now: String)

    @Query("UPDATE quests SET stacked_duration_seconds = 0, updated_at = :now WHERE id = :id")
    suspend fun resetStackedDuration(id: Long, now: String)

    @Query("UPDATE quests SET last_completed_at = :now, updated_at = :now WHERE id = :id")
    suspend fun updateLastCompleted(id: Long, now: String)

    @Query("DELETE FROM quests WHERE id = :id")
    suspend fun deleteQuest(id: Long)

    @Query("SELECT COUNT(*) FROM quests WHERE attribute_id = :attributeId AND status = 'active'")
    suspend fun countActiveQuestsByAttribute(attributeId: Long): Int

    /** One-shot variant for lazy evaluation (inside @Transaction, cannot use Flow). */
    @Query("SELECT * FROM quests WHERE user_id = :userId AND status = 'active' AND is_container = 0")
    suspend fun getActiveExecutableQuestsOnce(userId: Long): List<QuestEntity>

    /** Return ALL sub-quests under parent (any status) — for cascade archival. */
    @Query("SELECT * FROM quests WHERE parent_quest_id = :parentId")
    suspend fun getAllSubQuestsOnce(parentId: Long): List<QuestEntity>

    /** Sub-quests that have breached the failure threshold. */
    @Query("""
        SELECT * FROM quests 
        WHERE user_id = :userId 
        AND status = 'active' 
        AND is_container = 0 
        AND parent_quest_id IS NOT NULL 
        AND consecutive_missed_sessions >= :threshold
    """)
    suspend fun getFailedSubQuests(userId: Long, threshold: Int): List<QuestEntity>

    /** Standalone quests that have breached the failure threshold. */
    @Query("""
        SELECT * FROM quests 
        WHERE user_id = :userId 
        AND status = 'active' 
        AND is_container = 0 
        AND parent_quest_id IS NULL 
        AND consecutive_missed_sessions >= :threshold
    """)
    suspend fun getFailedStandaloneQuests(userId: Long, threshold: Int): List<QuestEntity>
}
