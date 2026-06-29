package com.questdday.data.repository

import kotlinx.coroutines.flow.Flow
import com.questdday.data.local.entity.QuestLogEntity
import com.questdday.data.local.entity.QuestHistoryEntity

interface QuestLogRepository {
    fun getQuestLogs(): Flow<List<QuestLogEntity>>
    fun getQuestHistory(): Flow<List<QuestHistoryEntity>>
    // other operations...
}
