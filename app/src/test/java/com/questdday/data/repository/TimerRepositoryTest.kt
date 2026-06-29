package com.questdday.data.repository

import androidx.room.withTransaction
import com.questdday.data.local.AppDatabase
import com.questdday.data.local.dao.ActiveTimerDao
import com.questdday.data.local.dao.QuestDao
import com.questdday.data.local.dao.QuestLogDao
import com.questdday.data.local.entity.ActiveTimerEntity
import com.questdday.data.local.entity.QuestEntity
import com.questdday.data.local.entity.toDomain
import com.questdday.domain.exception.QuestValidationException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TimerRepositoryTest {

    private lateinit var activeTimerDao: ActiveTimerDao
    private lateinit var questDao: QuestDao
    private lateinit var questLogDao: QuestLogDao
    private lateinit var db: AppDatabase
    private lateinit var repository: TimerRepositoryImpl

    private fun buildQuestEntity(
        id: Long = 10L,
        completionMode: String? = "timer",
        status: String = "active"
    ) = QuestEntity(
        id = id,
        userId = 1L,
        parentQuestId = null,
        attributeId = 1L,
        title = "Study Math",
        description = null,
        isContainer = 0,
        completionMode = completionMode,
        targetDurationSeconds = 1800,
        durationType = "endless",
        durationInputType = null,
        targetDays = null,
        startDate = "2026-06-29",
        endDate = null,
        absenceMode = null,
        stackedDurationSeconds = 0,
        scheduleType = "daily",
        scheduleDays = null,
        status = status,
        consecutiveMissedSessions = 0,
        lastCompletedAt = null,
        createdAt = "2026-06-29 10:00:00",
        updatedAt = "2026-06-29 10:00:00"
    )

    @Before
    fun setup() {
        activeTimerDao = mockk(relaxed = true)
        questDao = mockk(relaxed = true)
        questLogDao = mockk(relaxed = true)
        db = mockk(relaxed = true)

        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { db.withTransaction(any<suspend () -> Any?>()) } coAnswers {
            secondArg<suspend () -> Any?>().invoke()
        }

        repository = TimerRepositoryImpl(activeTimerDao, questDao, questLogDao, db)
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.room.RoomDatabaseKt")
    }

    @Test
    fun `startTimer returns false when timer already exists for quest`() = runTest {
        // Arrange
        val questId = 10L
        val quest = buildQuestEntity(id = questId)
        coEvery { questDao.getQuestById(questId) } returns quest
        coEvery { activeTimerDao.timerExistsForQuest(questId) } returns 1

        // Act
        val result = repository.startTimer(questId, 1800)

        // Assert
        assertFalse(result)
        coVerify(exactly = 1) { activeTimerDao.timerExistsForQuest(questId) }
        coVerify(exactly = 0) { activeTimerDao.insertTimer(any()) }
    }

    @Test
    fun `startTimer returns true and inserts when no existing timer`() = runTest {
        // Arrange
        val questId = 10L
        val quest = buildQuestEntity(id = questId)
        coEvery { questDao.getQuestById(questId) } returns quest
        coEvery { activeTimerDao.timerExistsForQuest(questId) } returns 0
        coEvery { activeTimerDao.insertTimer(any()) } returns 1L

        // Act
        val result = repository.startTimer(questId, 1800)

        // Assert
        assertTrue(result)
        coVerify(exactly = 1) { activeTimerDao.timerExistsForQuest(questId) }
        coVerify(exactly = 1) {
            activeTimerDao.insertTimer(
                withArg { timer ->
                    assertEquals(questId, timer.questId)
                    assertEquals(1800, timer.targetDurationSeconds)
                    assertNull(timer.alarmFiredAt)
                }
            )
        }
    }

    @Test(expected = QuestValidationException::class)
    fun `startTimer throws QuestValidationException when target duration is zero or negative`() = runTest {
        // Arrange
        val questId = 10L

        // Act & Assert
        repository.startTimer(questId, 0)
    }

    @Test(expected = QuestValidationException::class)
    fun `startTimer throws QuestValidationException when quest not found`() = runTest {
        // Arrange
        val questId = 999L
        coEvery { questDao.getQuestById(questId) } returns null

        // Act & Assert
        repository.startTimer(questId, 1800)
    }

    @Test(expected = QuestValidationException::class)
    fun `startTimer throws QuestValidationException when quest completion mode is not timer`() = runTest {
        // Arrange
        val questId = 10L
        val quest = buildQuestEntity(id = questId, completionMode = "instant")
        coEvery { questDao.getQuestById(questId) } returns quest

        // Act & Assert
        repository.startTimer(questId, 1800)
    }

    @Test(expected = QuestValidationException::class)
    fun `startTimer throws QuestValidationException when quest status is not active`() = runTest {
        // Arrange
        val questId = 10L
        val quest = buildQuestEntity(id = questId, status = "completed")
        coEvery { questDao.getQuestById(questId) } returns quest

        // Act & Assert
        repository.startTimer(questId, 1800)
    }

    @Test
    fun `cancelTimer deletes timer without inserting quest log`() = runTest {
        // Arrange
        val questId = 10L

        // Act
        repository.cancelTimer(questId)

        // Assert
        coVerify(exactly = 1) { activeTimerDao.deleteTimer(questId) }
        coVerify(exactly = 0) { questLogDao.insertLog(any()) }
    }

    @Test
    fun `confirmTimerComplete deletes timer and inserts log in transaction`() = runTest {
        // Arrange
        val questId = 10L
        val userId = 1L
        val logDate = "2026-06-29"
        val actualDurationSeconds = 1800
        val expAwarded = 75.0

        // Act
        repository.confirmTimerComplete(
            questId = questId,
            userId = userId,
            logDate = logDate,
            actualDurationSeconds = actualDurationSeconds,
            expAwarded = expAwarded
        )

        // Assert
        coVerify(exactly = 1) { db.withTransaction(any<suspend () -> Any?>()) }
        coVerify(exactly = 1) { activeTimerDao.deleteTimer(questId) }
        coVerify(exactly = 1) {
            questLogDao.insertLog(
                withArg { log ->
                    assertEquals(questId, log.questId)
                    assertEquals(userId, log.userId)
                    assertEquals(logDate, log.logDate)
                    assertEquals(actualDurationSeconds, log.actualDurationSeconds)
                    assertEquals(expAwarded, log.expAwarded, 0.0)
                    assertEquals(0, log.isEpicFinaleBonus)
                }
            )
        }
    }

    @Test
    fun `markAlarmFired calls dao with correct questId and timestamp`() = runTest {
        // Arrange
        val questId = 10L

        // Act
        repository.markAlarmFired(questId)

        // Assert
        coVerify(exactly = 1) { activeTimerDao.markAlarmFired(questId, any()) }
    }

    @Test
    fun `getTimerByQuestId returns mapped domain timer or null`() = runTest {
        // Arrange
        val questId = 10L
        val entity = ActiveTimerEntity(
            id = 1L,
            questId = questId,
            startedAt = "2026-06-29 10:00:00",
            targetDurationSeconds = 1800,
            alarmFiredAt = null
        )
        coEvery { activeTimerDao.getTimerByQuestId(questId) } returns entity
        coEvery { activeTimerDao.getTimerByQuestId(999L) } returns null

        // Act
        val timer = repository.getTimerByQuestId(questId)
        val emptyTimer = repository.getTimerByQuestId(999L)

        // Assert
        assertNotNull(timer)
        assertEquals(entity.toDomain(), timer)
        assertNull(emptyTimer)
    }

    @Test
    fun `getRunningTimers maps list of running timers`() = runTest {
        // Arrange
        val entities = listOf(
            ActiveTimerEntity(
                id = 1L,
                questId = 10L,
                startedAt = "2026-06-29 10:00:00",
                targetDurationSeconds = 1800,
                alarmFiredAt = null
            )
        )
        every { activeTimerDao.getRunningTimers() } returns flowOf(entities)

        // Act
        val result = repository.getRunningTimers().first()

        // Assert
        assertEquals(1, result.size)
        assertEquals(entities[0].toDomain(), result[0])
    }

    @Test
    fun `getPendingConfirmationTimers maps list of pending confirmation timers`() = runTest {
        // Arrange
        val entities = listOf(
            ActiveTimerEntity(
                id = 1L,
                questId = 10L,
                startedAt = "2026-06-29 10:00:00",
                targetDurationSeconds = 1800,
                alarmFiredAt = "2026-06-29 10:30:00"
            )
        )
        every { activeTimerDao.getPendingConfirmationTimers() } returns flowOf(entities)

        // Act
        val result = repository.getPendingConfirmationTimers().first()

        // Assert
        assertEquals(1, result.size)
        assertEquals(entities[0].toDomain(), result[0])
    }
}
