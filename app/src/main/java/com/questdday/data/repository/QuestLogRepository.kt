package com.questdday.data.repository

import kotlinx.coroutines.flow.Flow
import com.questdday.domain.model.QuestLog
import com.questdday.domain.model.QuestHistory

interface QuestLogRepository {
    fun getQuestLogs(): Flow<List<QuestLog>>
    fun getQuestHistory(): Flow<List<QuestHistory>>
    
    suspend fun insertLog(
        questId: Long,
        userId: Long,
        logDate: String,
        actualDurationSeconds: Int?,
        expAwarded: Double,
        isEpicFinaleBonus: Boolean
    ): Long

    fun getLogsForDate(userId: Long, date: String): Flow<List<QuestLog>>

    suspend fun hasLogForDate(questId: Long, date: String): Boolean

    suspend fun getTotalCompletions(questId: Long): Int

    suspend fun getTotalExpForQuest(questId: Long): Double
}
