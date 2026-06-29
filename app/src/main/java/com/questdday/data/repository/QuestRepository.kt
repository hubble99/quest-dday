package com.questdday.data.repository

import com.questdday.domain.model.Quest
import kotlinx.coroutines.flow.Flow

interface QuestRepository {

    /**
     * Returns quests scheduled for [today]:
     * - All 'daily' schedule_type quests active on that date.
     * - 'custom_days' quests filtered by whether [today]'s day-of-week is in schedule_days.
     */
    fun getTodayQuests(userId: Long, today: String): Flow<List<Quest>>

    /** Returns all active Epic containers (is_container = 1) for [userId]. */
    fun getActiveEpicContainers(userId: Long): Flow<List<Quest>>

    /** Returns all active sub-quests under [parentId]. */
    fun getSubQuests(parentId: Long): Flow<List<Quest>>

    /** Returns a single quest by [id], or null if not found. */
    suspend fun getQuestById(id: Long): Quest?

    /**
     * Validates and inserts a standalone (non-container, no parent) quest.
     * Throws [com.questdday.domain.exception.QuestValidationException] if validation fails.
     */
    suspend fun insertStandaloneQuest(quest: Quest): Long

    /**
     * Validates and inserts an Epic container together with its first sub-quest
     * in a single atomic transaction. Throws [com.questdday.domain.exception.QuestValidationException]
     * if either fails validation or if sub-quest end_date exceeds epic end_date.
     */
    suspend fun insertEpicWithFirstSubQuest(epic: Quest, subQuest: Quest): Long

    /**
     * Validates and inserts a new sub-quest under an existing parent quest.
     * Throws [com.questdday.domain.exception.QuestValidationException] if validation fails.
     */
    suspend fun insertSubQuest(subQuest: Quest, parentId: Long): Long

    /** Updates the status field of quest [id]. */
    suspend fun updateQuestStatus(id: Long, status: String)

    /** Updates the consecutive_missed_sessions field of quest [id]. */
    suspend fun updateMissedSessions(id: Long, sessions: Int)

    /** Updates end_date of quest [id] (Mode Shift logic — caller computes new date). */
    suspend fun updateEndDateShift(id: Long, newEndDate: String)

    /** Adds [amount] to stacked_duration_seconds of quest [id] (Mode Stack). */
    suspend fun addStackedDuration(id: Long, amount: Int)

    /** Resets stacked_duration_seconds to 0 for quest [id] after completion. */
    suspend fun resetStackedDuration(id: Long)

    /**
     * Deletes quest [id] from the quests table.
     * ON DELETE CASCADE in the DB automatically removes all sub-quests.
     */
    suspend fun deleteQuest(id: Long)

    /**
     * Archives a quest to quest_history and then deletes it from quests,
     * all within a single atomic transaction.
     * ON DELETE CASCADE handles sub-quest cleanup.
     */
    suspend fun archiveQuestToHistory(
        quest: Quest,
        finalStatus: String,
        totalDaysCompleted: Int,
        totalExpEarned: Double
    )
}
