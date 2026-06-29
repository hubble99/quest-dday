package com.questdday.data.repository

import kotlinx.coroutines.flow.Flow
import com.questdday.data.local.entity.QuestEntity

interface QuestRepository {
    fun getActiveQuests(): Flow<List<QuestEntity>>
    suspend fun insertQuest(quest: QuestEntity)
    // other operations...
}
