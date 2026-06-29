package com.questdday.data.repository

import kotlinx.coroutines.flow.Flow
import com.questdday.data.local.entity.ActiveTimerEntity

interface TimerRepository {
    fun getActiveTimerForQuest(questId: Long): Flow<ActiveTimerEntity?>
    suspend fun insertActiveTimer(timer: ActiveTimerEntity)
    // other operations...
}
