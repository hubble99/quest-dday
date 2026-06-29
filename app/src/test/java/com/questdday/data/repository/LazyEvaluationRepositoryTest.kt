package com.questdday.data.repository

import androidx.room.withTransaction
import com.questdday.data.local.AppDatabase
import com.questdday.data.local.dao.ActiveTimerDao
import com.questdday.data.local.dao.QuestDao
import com.questdday.data.local.dao.QuestHistoryDao
import com.questdday.data.local.dao.QuestLogDao
import com.questdday.data.local.dao.UserDao
import com.questdday.data.local.entity.ActiveTimerEntity
import com.questdday.data.local.entity.QuestEntity
import com.questdday.data.local.entity.QuestHistoryEntity
import com.questdday.data.local.entity.QuestLogEntity
import com.questdday.data.local.entity.UserEntity
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class LazyEvaluationRepositoryTest {

    // ---- Mocks ----
    private lateinit var db: AppDatabase
    private lateinit var userDao: UserDao
    private lateinit var questDao: QuestDao
    private lateinit var questLogDao: QuestLogDao
    private lateinit var questHistoryDao: QuestHistoryDao
    private lateinit var activeTimerDao: ActiveTimerDao
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var expRepository: ExpRepository

    private lateinit var repo: LazyEvaluationRepositoryImpl

    // ---- Test constants ----
    private val userId = 1L
    private val today = LocalDate.of(2026, 6, 29)
    private val todayStr = "2026-06-29"
    private val yesterdayStr = "2026-06-28"
    private val twoDaysAgoStr = "2026-06-27"
    private val nowTimestamp = "2026-06-29 12:00:00"

    @Before
    fun setup() {
        db = mockk(relaxed = true)
        userDao = mockk(relaxed = true)
        questDao = mockk(relaxed = true)
        questLogDao = mockk(relaxed = true)
        questHistoryDao = mockk(relaxed = true)
        activeTimerDao = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        expRepository = mockk(relaxed = true)

        // Mock Room withTransaction to just execute the lambda directly
        io.mockk.mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { db.withTransaction(any<suspend () -> Any?>()) } coAnswers {
            secondArg<suspend () -> Any?>().invoke()
        }

        repo = LazyEvaluationRepositoryImpl(
            db = db,
            userDao = userDao,
            questDao = questDao,
            questLogDao = questLogDao,
            questHistoryDao = questHistoryDao,
            activeTimerDao = activeTimerDao,
            settingsRepository = settingsRepository,
            expRepository = expRepository
        )
    }

    // ------------------------------------------------------------------
    // Helper factories
    // ------------------------------------------------------------------

    private fun makeUser(
        lastEvaluatedDate: String? = yesterdayStr,
        consecutiveInactiveDays: Int = 0
    ) = UserEntity(
        id = userId,
        username = "Hero",
        lastActiveAt = nowTimestamp,
        lastEvaluatedDate = lastEvaluatedDate,
        consecutiveInactiveScheduledDays = consecutiveInactiveDays,
        totalExpEarnedLifetime = 0.0,
        hasSeenWelcome = 1,
        createdAt = "2026-01-01 00:00:00"
    )

    private fun makeQuest(
        id: Long = 1L,
        parentQuestId: Long? = null,
        scheduleType: String? = "daily",
        scheduleDays: String? = null,
        status: String = "active",
        consecutiveMissedSessions: Int = 0,
        durationType: String = "endless",
        absenceMode: String? = null,
        endDate: String? = null,
        targetDurationSeconds: Int? = 300,
        startDate: String = "2026-01-01"
    ) = QuestEntity(
        id = id,
        userId = userId,
        parentQuestId = parentQuestId,
        attributeId = 1L,
        title = "Quest $id",
        description = null,
        isContainer = 0,
        completionMode = "instant",
        targetDurationSeconds = targetDurationSeconds,
        durationType = durationType,
        durationInputType = null,
        targetDays = null,
        startDate = startDate,
        endDate = endDate,
        absenceMode = absenceMode,
        stackedDurationSeconds = 0,
        scheduleType = scheduleType,
        scheduleDays = scheduleDays,
        status = status,
        consecutiveMissedSessions = consecutiveMissedSessions,
        lastCompletedAt = null,
        createdAt = "2026-01-01 00:00:00",
        updatedAt = "2026-01-01 00:00:00"
    )

    private fun makeEpicContainer(id: Long = 100L) = QuestEntity(
        id = id,
        userId = userId,
        parentQuestId = null,
        attributeId = 1L,
        title = "Epic $id",
        description = null,
        isContainer = 1,
        completionMode = null,
        targetDurationSeconds = null,
        durationType = "endless",
        durationInputType = null,
        targetDays = null,
        startDate = "2026-01-01",
        endDate = null,
        absenceMode = null,
        stackedDurationSeconds = 0,
        scheduleType = null,
        scheduleDays = null,
        status = "active",
        consecutiveMissedSessions = 0,
        lastCompletedAt = null,
        createdAt = "2026-01-01 00:00:00",
        updatedAt = "2026-01-01 00:00:00"
    )

    private fun makeTimer(
        id: Long = 1L,
        questId: Long = 1L,
        alarmFiredAt: String? = null
    ) = ActiveTimerEntity(
        id = id,
        questId = questId,
        startedAt = "2026-06-29 10:00:00",
        targetDurationSeconds = 600,
        alarmFiredAt = alarmFiredAt
    )

    /**
     * Setup default mocks for a standard runLayerA evaluation scenario.
     * Caller can override specific mocks after calling this.
     */
    private fun setupDefaultLayerAMocks(
        lastEvaluatedDate: String? = twoDaysAgoStr,
        consecutiveInactiveDays: Int = 0,
        activeQuests: List<QuestEntity> = emptyList()
    ) {
        coEvery { userDao.getUserOnce(userId) } returns makeUser(
            lastEvaluatedDate = lastEvaluatedDate,
            consecutiveInactiveDays = consecutiveInactiveDays
        )
        coEvery { questDao.getActiveExecutableQuestsOnce(userId) } returns activeQuests
        coEvery { questDao.getFailedSubQuests(userId, any()) } returns emptyList()
        coEvery { questDao.getFailedStandaloneQuests(userId, any()) } returns emptyList()
        coEvery { settingsRepository.getFailureThresholdSessions() } returns 7
        coEvery { settingsRepository.getDecayRateR() } returns null
        coEvery { settingsRepository.getDecayGracePeriodDays() } returns null
        coEvery { userDao.updateEvaluationDate(any(), any(), any()) } just Runs
        coEvery { userDao.updateInactiveDays(any(), any()) } just Runs
        coEvery { questDao.updateMissedSessions(any(), any(), any()) } just Runs
        coEvery { questLogDao.getCompletedDatesForQuest(any(), any(), any()) } returns emptyList()
        coEvery { questLogDao.getLogsForUserOnDateOnce(any(), any()) } returns emptyList()
        coEvery { questLogDao.getTotalCompletionsForQuest(any()) } returns 0
        coEvery { questLogDao.getTotalExpForQuest(any()) } returns 0.0
        coEvery { questHistoryDao.insertHistory(any()) } returns 1L
        coEvery { questDao.deleteQuest(any()) } just Runs
    }

    // ==================================================================
    // TEST 1: runLayerA returns AlreadyEvaluated when last_evaluated_date equals today
    // ==================================================================

    @Test
    fun `runLayerA returns AlreadyEvaluated when last_evaluated_date equals today`() = runTest {
        // Arrange
        coEvery { userDao.getUserOnce(userId) } returns makeUser(lastEvaluatedDate = todayStr)

        // Act
        val result = repo.runLayerA(userId, today)

        // Assert
        assertTrue(result is LayerAResult.AlreadyEvaluated)
        coVerify(exactly = 0) { db.withTransaction<Any>(any()) }
    }

    // ==================================================================
    // TEST 2: runLayerA returns FirstTime when last_evaluated_date is null
    // ==================================================================

    @Test
    fun `runLayerA returns FirstTime when last_evaluated_date is null`() = runTest {
        // Arrange
        coEvery { userDao.getUserOnce(userId) } returns makeUser(lastEvaluatedDate = null)

        // Act
        val result = repo.runLayerA(userId, today)

        // Assert
        assertTrue(result is LayerAResult.FirstTime)
        coVerify { userDao.updateEvaluationDate(userId, todayStr, any()) }
        // No evaluation steps should have run
        coVerify(exactly = 0) { questDao.getActiveExecutableQuestsOnce(any()) }
    }

    // ==================================================================
    // TEST 3: runLayerA skips decay when decay_rate_R is not configured
    // ==================================================================

    @Test
    fun `runLayerA skips decay when decay_rate_R is not configured`() = runTest {
        // Arrange
        setupDefaultLayerAMocks(consecutiveInactiveDays = 5)
        coEvery { settingsRepository.getDecayRateR() } returns null
        coEvery { settingsRepository.getDecayGracePeriodDays() } returns 1

        // Act
        val result = repo.runLayerA(userId, today)

        // Assert
        assertTrue(result is LayerAResult.Evaluated)
        assertFalse((result as LayerAResult.Evaluated).decayApplied)
        coVerify(exactly = 0) { expRepository.applyDecay(any(), any(), any(), any()) }
    }

    // ==================================================================
    // TEST 4: runLayerA skips decay when grace_period_days is not configured
    // ==================================================================

    @Test
    fun `runLayerA skips decay when grace_period_days is not configured`() = runTest {
        // Arrange
        setupDefaultLayerAMocks(consecutiveInactiveDays = 5)
        coEvery { settingsRepository.getDecayRateR() } returns 10.0
        coEvery { settingsRepository.getDecayGracePeriodDays() } returns null

        // Act
        val result = repo.runLayerA(userId, today)

        // Assert
        assertTrue(result is LayerAResult.Evaluated)
        assertFalse((result as LayerAResult.Evaluated).decayApplied)
        coVerify(exactly = 0) { expRepository.applyDecay(any(), any(), any(), any()) }
    }

    // ==================================================================
    // TEST 5: runLayerA processes cascade failure before absence_mode
    // ==================================================================

    @Test
    fun `runLayerA processes cascade failure before absence_mode`() = runTest {
        // Arrange
        // A sub-quest that has breached threshold → will be cascade-failed in Step 3
        val failedSubQuest = makeQuest(
            id = 10L,
            parentQuestId = 100L,
            consecutiveMissedSessions = 7
        )
        val epicParent = makeEpicContainer(id = 100L)

        // A time_bound quest that survives → processed in Step 4
        val survivingQuest = makeQuest(
            id = 20L,
            durationType = "time_bound",
            absenceMode = "shift",
            endDate = "2026-07-15",
            scheduleType = "daily"
        )

        setupDefaultLayerAMocks(activeQuests = listOf(failedSubQuest, survivingQuest))

        // Step 3 setup
        coEvery { questDao.getFailedSubQuests(userId, 7) } returns listOf(failedSubQuest)
        coEvery { questDao.getQuestById(100L) } returns epicParent
        coEvery { questDao.getAllSubQuestsOnce(100L) } returns emptyList()

        // Step 4: After cascade, re-query returns only the surviving quest
        coEvery { questDao.getActiveExecutableQuestsOnce(userId) } returnsMany listOf(
            listOf(failedSubQuest, survivingQuest),  // first call in Step 2
            listOf(survivingQuest)                    // second call in Step 4
        )

        // Act
        val result = repo.runLayerA(userId, today)

        // Assert
        assertTrue(result is LayerAResult.Evaluated)
        val evaluated = result as LayerAResult.Evaluated
        assertTrue(evaluated.failedQuestIds.contains(10L))
        assertTrue(evaluated.failedQuestIds.contains(100L))

        // Verify cascade (Step 3) happened — archive was called for failed sub-quest
        coVerify { questHistoryDao.insertHistory(match { it.originalQuestId == 10L && it.finalStatus == "failed_abandoned" }) }
        // Verify cascade — archive was called for epic parent
        coVerify { questHistoryDao.insertHistory(match { it.originalQuestId == 100L && it.finalStatus == "failed_abandoned" }) }
        // Verify DELETE of epic parent (cascade deletes sub-quests)
        coVerify { questDao.deleteQuest(100L) }
    }

    // ==================================================================
    // TEST 6: runLayerA archives sibling as completed when sibling status is completed
    // ==================================================================

    @Test
    fun `runLayerA archives sibling as completed when sibling status is completed`() = runTest {
        // Arrange
        val failedSubQuest = makeQuest(id = 10L, parentQuestId = 100L, consecutiveMissedSessions = 7)
        val completedSibling = makeQuest(id = 11L, parentQuestId = 100L, status = "completed")
        val epicParent = makeEpicContainer(id = 100L)

        setupDefaultLayerAMocks(activeQuests = listOf(failedSubQuest))

        coEvery { questDao.getFailedSubQuests(userId, 7) } returns listOf(failedSubQuest)
        coEvery { questDao.getQuestById(100L) } returns epicParent
        coEvery { questDao.getAllSubQuestsOnce(100L) } returns listOf(completedSibling)

        // Act
        repo.runLayerA(userId, today)

        // Assert — sibling with status "completed" archived as "completed"
        coVerify {
            questHistoryDao.insertHistory(match {
                it.originalQuestId == 11L && it.finalStatus == "completed"
            })
        }
    }

    // ==================================================================
    // TEST 7: runLayerA archives sibling as failed_via_parent when sibling status is active
    // ==================================================================

    @Test
    fun `runLayerA archives sibling as failed_via_parent when sibling status is active`() = runTest {
        // Arrange
        val failedSubQuest = makeQuest(id = 10L, parentQuestId = 100L, consecutiveMissedSessions = 7)
        val activeSibling = makeQuest(id = 12L, parentQuestId = 100L, status = "active")
        val epicParent = makeEpicContainer(id = 100L)

        setupDefaultLayerAMocks(activeQuests = listOf(failedSubQuest))

        coEvery { questDao.getFailedSubQuests(userId, 7) } returns listOf(failedSubQuest)
        coEvery { questDao.getQuestById(100L) } returns epicParent
        coEvery { questDao.getAllSubQuestsOnce(100L) } returns listOf(activeSibling)

        // Act
        repo.runLayerA(userId, today)

        // Assert — sibling with status "active" archived as "failed_via_parent"
        coVerify {
            questHistoryDao.insertHistory(match {
                it.originalQuestId == 12L && it.finalStatus == "failed_via_parent"
            })
        }
    }

    // ==================================================================
    // TEST 8: runLayerA applies Shift by calling findNextScheduledDay not calendar day increment
    // ==================================================================

    @Test
    fun `runLayerA applies Shift by calling findNextScheduledDay not calendar day increment`() = runTest {
        // Arrange
        // Quest scheduled Mon/Wed/Fri (1,3,5), time_bound, shift mode
        // end_date = 2026-07-10 (Friday)
        // Evaluation range: yesterday (2026-06-28, Saturday) — 1 missed session on Friday 2026-06-27?
        // Let's set lastEvaluatedDate to 2026-06-25 (Wednesday) so range = 2026-06-26 to 2026-06-28
        // 2026-06-26 = Thursday (not scheduled)
        // 2026-06-27 = Friday (scheduled, but missed)
        // 2026-06-28 = Saturday (not scheduled)
        // → 1 missed session → shift end_date by 1 via findNextScheduledDay
        // findNextScheduledDay("2026-07-10"=Friday, [1,3,5]) → Monday 2026-07-13
        val questWithShift = makeQuest(
            id = 30L,
            durationType = "time_bound",
            absenceMode = "shift",
            endDate = "2026-07-10",
            scheduleType = "custom_days",
            scheduleDays = "1,3,5",
            startDate = "2026-01-01"
        )

        val lastEval = "2026-06-25"
        setupDefaultLayerAMocks(
            lastEvaluatedDate = lastEval,
            activeQuests = listOf(questWithShift)
        )

        // After Step 3 (no failures), re-query returns same quest
        coEvery { questDao.getActiveExecutableQuestsOnce(userId) } returns listOf(questWithShift)

        val endDateSlot = slot<String>()
        coEvery { questDao.updateEndDate(30L, capture(endDateSlot), any()) } just Runs

        // Act
        repo.runLayerA(userId, today)

        // Assert — end_date should be shifted to next scheduled day after 2026-07-10
        // 2026-07-10 is Friday. Next scheduled day for [1,3,5] is Monday 2026-07-13
        coVerify { questDao.updateEndDate(30L, any(), any()) }
        assertEquals("2026-07-13", endDateSlot.captured)
    }

    // ==================================================================
    // TEST 9: runLayerA applies Stack by adding missed_sessions * target_duration_seconds
    // ==================================================================

    @Test
    fun `runLayerA applies Stack by adding missed_sessions times target_duration_seconds`() = runTest {
        // Arrange
        // Daily quest, time_bound, stack mode, target_duration = 300 seconds
        // lastEvaluatedDate = 2026-06-26, today = 2026-06-29
        // Range: 2026-06-27 to 2026-06-28 → 2 scheduled days, 0 completions → 2 misses
        // Stack amount = 2 × 300 = 600
        val questWithStack = makeQuest(
            id = 40L,
            durationType = "time_bound",
            absenceMode = "stack",
            endDate = "2026-07-15",
            scheduleType = "daily",
            targetDurationSeconds = 300,
            startDate = "2026-01-01"
        )

        val lastEval = "2026-06-26"
        setupDefaultLayerAMocks(
            lastEvaluatedDate = lastEval,
            activeQuests = listOf(questWithStack)
        )

        coEvery { questDao.getActiveExecutableQuestsOnce(userId) } returns listOf(questWithStack)

        val stackAmountSlot = slot<Int>()
        coEvery { questDao.addStackedDuration(40L, capture(stackAmountSlot), any()) } just Runs

        // Act
        repo.runLayerA(userId, today)

        // Assert — 2 missed sessions × 300 = 600
        coVerify { questDao.addStackedDuration(40L, any(), any()) }
        assertEquals(600, stackAmountSlot.captured)
    }

    // ==================================================================
    // TEST 10: runLayerA updates last_evaluated_date after successful evaluation
    // ==================================================================

    @Test
    fun `runLayerA updates last_evaluated_date after successful evaluation`() = runTest {
        // Arrange
        setupDefaultLayerAMocks()

        // Act
        repo.runLayerA(userId, today)

        // Assert
        coVerify { userDao.updateEvaluationDate(userId, todayStr, any()) }
    }

    // ==================================================================
    // TEST 11: runLayerB always runs regardless of last_evaluated_date
    // ==================================================================

    @Test
    fun `runLayerB always runs regardless of last_evaluated_date`() = runTest {
        // Arrange
        val pendingTimer = makeTimer(id = 1L, questId = 1L, alarmFiredAt = "2026-06-29 11:00:00")
        val runningTimer = makeTimer(id = 2L, questId = 2L, alarmFiredAt = null)

        coEvery { activeTimerDao.getPendingConfirmationTimersOnce() } returns listOf(pendingTimer)
        coEvery { activeTimerDao.getRunningTimersOnce() } returns listOf(runningTimer)

        // Act — no user needed, no guard check
        val result = repo.runLayerB(userId)

        // Assert
        assertEquals(1, result.pendingConfirmationTimers.size)
        assertEquals(1, result.runningTimers.size)
        // Verify DAO calls were made without any guard checks
        coVerify { activeTimerDao.getPendingConfirmationTimersOnce() }
        coVerify { activeTimerDao.getRunningTimersOnce() }
        // No user DAO interaction needed for Layer B
        coVerify(exactly = 0) { userDao.getUserOnce(any()) }
    }

    // ==================================================================
    // TEST 12: runLayerB returns correct pending and running timer lists
    // ==================================================================

    @Test
    fun `runLayerB returns correct pending and running timer lists`() = runTest {
        // Arrange
        val pending1 = makeTimer(id = 1L, questId = 10L, alarmFiredAt = "2026-06-29 09:00:00")
        val pending2 = makeTimer(id = 2L, questId = 20L, alarmFiredAt = "2026-06-29 10:00:00")
        val running1 = makeTimer(id = 3L, questId = 30L, alarmFiredAt = null)

        coEvery { activeTimerDao.getPendingConfirmationTimersOnce() } returns listOf(pending1, pending2)
        coEvery { activeTimerDao.getRunningTimersOnce() } returns listOf(running1)

        // Act
        val result = repo.runLayerB(userId)

        // Assert
        assertEquals(2, result.pendingConfirmationTimers.size)
        assertEquals(10L, result.pendingConfirmationTimers[0].questId)
        assertEquals(20L, result.pendingConfirmationTimers[1].questId)
        assertEquals(1, result.runningTimers.size)
        assertEquals(30L, result.runningTimers[0].questId)
    }

    @org.junit.After
    fun tearDown() {
        io.mockk.unmockkStatic("androidx.room.RoomDatabaseKt")
    }
}
