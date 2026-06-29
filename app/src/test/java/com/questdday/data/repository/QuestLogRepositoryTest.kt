package com.questdday.data.repository

import com.questdday.data.local.dao.QuestHistoryDao
import com.questdday.data.local.dao.QuestLogDao
import com.questdday.data.local.entity.QuestHistoryEntity
import com.questdday.data.local.entity.QuestLogEntity
import com.questdday.data.local.entity.toDomainModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QuestLogRepositoryTest {

    private lateinit var questLogDao: QuestLogDao
    private lateinit var questHistoryDao: QuestHistoryDao
    private lateinit var repository: QuestLogRepositoryImpl

    @Before
    fun setup() {
        questLogDao = mockk(relaxed = true)
        questHistoryDao = mockk(relaxed = true)
        repository = QuestLogRepositoryImpl(questLogDao, questHistoryDao)
    }

    @Test
    fun `insertLog calls dao with correct parameters`() = runTest {
        // Arrange
        val questId = 10L
        val userId = 1L
        val logDate = "2026-06-29"
        val actualDurationSeconds = 1200
        val expAwarded = 50.0
        val isEpicFinaleBonus = true

        coEvery { questLogDao.insertLog(any()) } returns 42L

        // Act
        val result = repository.insertLog(
            questId = questId,
            userId = userId,
            logDate = logDate,
            actualDurationSeconds = actualDurationSeconds,
            expAwarded = expAwarded,
            isEpicFinaleBonus = isEpicFinaleBonus
        )

        // Assert
        assertEquals(42L, result)
        coVerify(exactly = 1) {
            questLogDao.insertLog(
                withArg { log ->
                    assertEquals(questId, log.questId)
                    assertEquals(userId, log.userId)
                    assertEquals(logDate, log.logDate)
                    assertEquals(actualDurationSeconds, log.actualDurationSeconds)
                    assertEquals(expAwarded, log.expAwarded, 0.0)
                    assertEquals(1, log.isEpicFinaleBonus)
                }
            )
        }
    }

    @Test
    fun `hasLogForDate returns true when log exists for date`() = runTest {
        // Arrange
        val questId = 10L
        val date = "2026-06-29"
        val entity = QuestLogEntity(
            id = 1L,
            questId = questId,
            userId = 1L,
            logDate = date,
            actualDurationSeconds = 1200,
            expAwarded = 50.0,
            isEpicFinaleBonus = 0,
            completedAt = "2026-06-29 10:00:00"
        )
        coEvery { questLogDao.getLogForDate(questId, date) } returns entity

        // Act
        val result = repository.hasLogForDate(questId, date)

        // Assert
        assertTrue(result)
        coVerify(exactly = 1) { questLogDao.getLogForDate(questId, date) }
    }

    @Test
    fun `hasLogForDate returns false when no log for date`() = runTest {
        // Arrange
        val questId = 10L
        val date = "2026-06-29"
        coEvery { questLogDao.getLogForDate(questId, date) } returns null

        // Act
        val result = repository.hasLogForDate(questId, date)

        // Assert
        assertFalse(result)
        coVerify(exactly = 1) { questLogDao.getLogForDate(questId, date) }
    }

    @Test
    fun `getLogsForDate maps list of entities to domain models`() = runTest {
        // Arrange
        val userId = 1L
        val date = "2026-06-29"
        val entities = listOf(
            QuestLogEntity(
                id = 1L,
                questId = 10L,
                userId = userId,
                logDate = date,
                actualDurationSeconds = 1200,
                expAwarded = 50.0,
                isEpicFinaleBonus = 0,
                completedAt = "2026-06-29 10:00:00"
            )
        )
        every { questLogDao.getLogsForUserOnDate(userId, date) } returns flowOf(entities)

        // Act
        val result = repository.getLogsForDate(userId, date).first()

        // Assert
        assertEquals(1, result.size)
        assertEquals(entities[0].toDomainModel(), result[0])
        coVerify(exactly = 1) { questLogDao.getLogsForUserOnDate(userId, date) }
    }

    @Test
    fun `getTotalCompletions returns correct count`() = runTest {
        // Arrange
        val questId = 10L
        coEvery { questLogDao.getTotalCompletionsForQuest(questId) } returns 5

        // Act
        val result = repository.getTotalCompletions(questId)

        // Assert
        assertEquals(5, result)
        coVerify(exactly = 1) { questLogDao.getTotalCompletionsForQuest(questId) }
    }

    @Test
    fun `getTotalExpForQuest returns correct total`() = runTest {
        // Arrange
        val questId = 10L
        coEvery { questLogDao.getTotalExpForQuest(questId) } returns 250.0

        // Act
        val result = repository.getTotalExpForQuest(questId)

        // Assert
        assertEquals(250.0, result, 0.0)
        coVerify(exactly = 1) { questLogDao.getTotalExpForQuest(questId) }
    }

    @Test
    fun `getQuestLogs retrieves and maps successfully`() = runTest {
        // Arrange
        val entities = listOf(
            QuestLogEntity(
                id = 1L,
                questId = 10L,
                userId = 1L,
                logDate = "2026-06-29",
                actualDurationSeconds = 1200,
                expAwarded = 50.0,
                isEpicFinaleBonus = 0,
                completedAt = "2026-06-29 10:00:00"
            )
        )
        every { questLogDao.getLogsForUser(1L) } returns flowOf(entities)

        // Act
        val result = repository.getQuestLogs().first()

        // Assert
        assertEquals(1, result.size)
        assertEquals(entities[0].toDomainModel(), result[0])
        coVerify(exactly = 1) { questLogDao.getLogsForUser(1L) }
    }

    @Test
    fun `getQuestHistory retrieves and maps successfully`() = runTest {
        // Arrange
        val entities = listOf(
            QuestHistoryEntity(
                id = 1L,
                originalQuestId = 10L,
                userId = 1L,
                title = "Test Quest",
                finalStatus = "completed",
                totalDaysCompleted = 5,
                totalExpEarned = 250.0,
                startedAt = "2026-06-20 10:00:00",
                endedAt = "2026-06-29 10:00:00"
            )
        )
        every { questHistoryDao.getHistoryByUser(1L) } returns flowOf(entities)

        // Act
        val result = repository.getQuestHistory().first()

        // Assert
        assertEquals(1, result.size)
        assertEquals(entities[0].toDomainModel(), result[0])
        coVerify(exactly = 1) { questHistoryDao.getHistoryByUser(1L) }
    }
}
