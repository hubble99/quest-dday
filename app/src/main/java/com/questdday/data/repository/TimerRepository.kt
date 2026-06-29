package com.questdday.data.repository

import kotlinx.coroutines.flow.Flow
import com.questdday.domain.model.ActiveTimer

interface TimerRepository {
    suspend fun startTimer(questId: Long, targetDurationSeconds: Int): Boolean
    suspend fun cancelTimer(questId: Long)
    suspend fun markAlarmFired(questId: Long)
    suspend fun getTimerByQuestId(questId: Long): ActiveTimer?
    fun getPendingConfirmationTimers(): Flow<List<ActiveTimer>>
    fun getRunningTimers(): Flow<List<ActiveTimer>>
    
    suspend fun confirmTimerComplete(
        questId: Long,
        userId: Long,
        logDate: String,
        actualDurationSeconds: Int,
        expAwarded: Double
    )
}
