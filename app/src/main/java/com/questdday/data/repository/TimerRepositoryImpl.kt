package com.questdday.data.repository

import androidx.room.withTransaction
import com.questdday.data.local.AppDatabase
import com.questdday.data.local.dao.ActiveTimerDao
import com.questdday.data.local.dao.QuestDao
import com.questdday.data.local.dao.QuestLogDao
import com.questdday.data.local.entity.ActiveTimerEntity
import com.questdday.data.local.entity.QuestLogEntity
import com.questdday.data.local.entity.toDomain
import com.questdday.domain.exception.QuestValidationException
import com.questdday.domain.model.ActiveTimer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TimerRepositoryImpl(
    private val activeTimerDao: ActiveTimerDao,
    private val questDao: QuestDao,
    private val questLogDao: QuestLogDao,
    private val db: AppDatabase
) : TimerRepository {

    private fun now(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    override suspend fun startTimer(questId: Long, targetDurationSeconds: Int): Boolean {
        // Validation constraints
        if (targetDurationSeconds <= 0) {
            throw QuestValidationException("Target duration must be greater than 0")
        }

        val quest = questDao.getQuestById(questId)
            ?: throw QuestValidationException("Quest not found")

        if (quest.completionMode != "timer") {
            throw QuestValidationException("Quest completion mode must be 'timer'")
        }

        if (quest.status != "active") {
            throw QuestValidationException("Quest must be active to start a timer")
        }

        // Return false if a timer already exists for this quest
        if (activeTimerDao.timerExistsForQuest(questId) > 0) {
            return false
        }

        // Insert new timer
        val timerEntity = ActiveTimerEntity(
            questId = questId,
            startedAt = now(),
            targetDurationSeconds = targetDurationSeconds,
            alarmFiredAt = null
        )
        activeTimerDao.insertTimer(timerEntity)
        return true
    }

    override suspend fun cancelTimer(questId: Long) {
        activeTimerDao.deleteTimer(questId)
    }

    override suspend fun markAlarmFired(questId: Long) {
        activeTimerDao.markAlarmFired(questId, now())
    }

    override suspend fun getTimerByQuestId(questId: Long): ActiveTimer? {
        return activeTimerDao.getTimerByQuestId(questId)?.toDomain()
    }

    override fun getPendingConfirmationTimers(): Flow<List<ActiveTimer>> {
        return activeTimerDao.getPendingConfirmationTimers().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRunningTimers(): Flow<List<ActiveTimer>> {
        return activeTimerDao.getRunningTimers().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun confirmTimerComplete(
        questId: Long,
        userId: Long,
        logDate: String,
        actualDurationSeconds: Int,
        expAwarded: Double
    ) {
        db.withTransaction {
            activeTimerDao.deleteTimer(questId)
            val logEntity = QuestLogEntity(
                questId = questId,
                userId = userId,
                logDate = logDate,
                actualDurationSeconds = actualDurationSeconds,
                expAwarded = expAwarded,
                isEpicFinaleBonus = 0,
                completedAt = now()
            )
            questLogDao.insertLog(logEntity)
        }
    }
}
