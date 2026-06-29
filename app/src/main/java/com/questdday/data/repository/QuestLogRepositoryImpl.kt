package com.questdday.data.repository

import com.questdday.data.local.dao.QuestHistoryDao
import com.questdday.data.local.dao.QuestLogDao
import com.questdday.data.local.entity.QuestLogEntity
import com.questdday.data.local.entity.toDomainModel
import com.questdday.domain.model.QuestHistory
import com.questdday.domain.model.QuestLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class QuestLogRepositoryImpl(
    private val questLogDao: QuestLogDao,
    private val questHistoryDao: QuestHistoryDao
) : QuestLogRepository {

    private fun now(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    override fun getQuestLogs(): Flow<List<QuestLog>> {
        return questLogDao.getLogsForUser(userId = 1L).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getQuestHistory(): Flow<List<QuestHistory>> {
        return questHistoryDao.getHistoryByUser(userId = 1L).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun insertLog(
        questId: Long,
        userId: Long,
        logDate: String,
        actualDurationSeconds: Int?,
        expAwarded: Double,
        isEpicFinaleBonus: Boolean
    ): Long {
        val logEntity = QuestLogEntity(
            questId = questId,
            userId = userId,
            logDate = logDate,
            actualDurationSeconds = actualDurationSeconds,
            expAwarded = expAwarded,
            isEpicFinaleBonus = if (isEpicFinaleBonus) 1 else 0,
            completedAt = now()
        )
        return questLogDao.insertLog(logEntity)
    }

    override fun getLogsForDate(userId: Long, date: String): Flow<List<QuestLog>> {
        return questLogDao.getLogsForUserOnDate(userId, date).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun hasLogForDate(questId: Long, date: String): Boolean {
        return questLogDao.getLogForDate(questId, date) != null
    }

    override suspend fun getTotalCompletions(questId: Long): Int {
        return questLogDao.getTotalCompletionsForQuest(questId)
    }

    override suspend fun getTotalExpForQuest(questId: Long): Double {
        return questLogDao.getTotalExpForQuest(questId)
    }
}
