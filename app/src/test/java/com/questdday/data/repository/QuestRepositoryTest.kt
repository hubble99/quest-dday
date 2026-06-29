package com.questdday.data.repository

import androidx.room.withTransaction
import com.questdday.data.local.AppDatabase
import com.questdday.data.local.dao.QuestDao
import com.questdday.data.local.dao.QuestHistoryDao
import com.questdday.data.local.entity.QuestEntity
import com.questdday.domain.exception.QuestValidationException
import com.questdday.domain.model.Quest
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QuestRepositoryTest {

    private lateinit var questDao: QuestDao
    private lateinit var questHistoryDao: QuestHistoryDao
    private lateinit var db: AppDatabase
    private lateinit var repository: QuestRepositoryImpl

    // ------------------------------------------------------------------
    // Helpers — build minimal Quest domain models for tests
    // ------------------------------------------------------------------

    private fun buildBaseQuest(
        id: Long = 0L,
        userId: Long = 1L,
        title: String = "Morning Run",
        attributeId: Long? = 1L,
        isContainer: Boolean = false,
        completionMode: String? = "instant",
        targetDurationSeconds: Int? = null,
        durationType: String = "endless",
        endDate: String? = null,
        targetDays: Int? = null,
        absenceMode: String? = null,
        scheduleType: String? = "daily",
        scheduleDays: String? = null,
        parentQuestId: Long? = null,
        createdAt: String = "2026-01-01 00:00:00",
        updatedAt: String = "2026-01-01 00:00:00",
        startDate: String = "2026-01-01"
    ) = Quest(
        id = id,
        userId = userId,
        parentQuestId = parentQuestId,
        attributeId = attributeId,
        title = title,
        description = null,
        isContainer = isContainer,
        completionMode = completionMode,
        targetDurationSeconds = targetDurationSeconds,
        durationType = durationType,
        durationInputType = null,
        targetDays = targetDays,
        startDate = startDate,
        endDate = endDate,
        absenceMode = absenceMode,
        stackedDurationSeconds = 0,
        scheduleType = scheduleType,
        scheduleDays = scheduleDays,
        status = "active",
        consecutiveMissedSessions = 0,
        lastCompletedAt = null,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun buildQuestEntity(
        id: Long = 1L,
        userId: Long = 1L,
        title: String = "Morning Run",
        scheduleType: String = "daily",
        scheduleDays: String? = null,
        status: String = "active",
        isContainer: Int = 0,
        durationType: String = "endless",
        completionMode: String? = "instant",
        endDate: String? = null,
        parentQuestId: Long? = null
    ) = QuestEntity(
        id = id,
        userId = userId,
        parentQuestId = parentQuestId,
        attributeId = 1L,
        title = title,
        description = null,
        isContainer = isContainer,
        completionMode = completionMode,
        targetDurationSeconds = null,
        durationType = durationType,
        durationInputType = null,
        targetDays = null,
        startDate = "2026-01-01",
        endDate = endDate,
        absenceMode = null,
        stackedDurationSeconds = 0,
        scheduleType = scheduleType,
        scheduleDays = scheduleDays,
        status = status,
        consecutiveMissedSessions = 0,
        lastCompletedAt = null,
        createdAt = "2026-01-01 00:00:00",
        updatedAt = "2026-01-01 00:00:00"
    )

    @Before
    fun setup() {
        questDao = mockk(relaxed = true)
        questHistoryDao = mockk(relaxed = true)
        db = mockk(relaxed = true)

        // CRITICAL: withTransaction is a top-level Kotlin extension function in Room
        // (androidx.room.RoomDatabase.kt → compiled to androidx.room.RoomDatabaseKt).
        // Without mockkStatic, MockK cannot intercept the static call and the real
        // Room implementation runs — which tries to open a database connection and hangs.
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { db.withTransaction(any<suspend () -> Any?>()) } coAnswers {
            // With mockkStatic: args[0] = receiver (AppDatabase), args[1] = block lambda.
            // Must use secondArg, not firstArg, to get the actual suspend block.
            secondArg<suspend () -> Any?>().invoke()
        }

        repository = QuestRepositoryImpl(questDao, questHistoryDao, db)
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.room.RoomDatabaseKt")
    }

    // ==================================================================
    // getTodayQuests
    // ==================================================================

    @Test
    fun `getTodayQuests includes daily quests regardless of day`() = runTest {
        // Arrange — 2026-06-29 is a Monday (dayOfWeek = 1)
        val dailyEntity = buildQuestEntity(id = 1L, scheduleType = "daily")
        every { questDao.getDailyQuestsForToday(1L, "2026-06-29") } returns flowOf(listOf(dailyEntity))
        every { questDao.getCustomScheduleQuests(1L, "2026-06-29") } returns flowOf(emptyList())

        // Act
        val result = repository.getTodayQuests(userId = 1L, today = "2026-06-29").first()

        // Assert
        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)
    }

    @Test
    fun `getTodayQuests filters custom_days correctly for matching day of week`() = runTest {
        // Arrange — 2026-06-29 is Monday (dayOfWeek = 1); schedule = Mon/Wed/Fri = "1,3,5"
        val customEntity = buildQuestEntity(id = 2L, scheduleType = "custom_days", scheduleDays = "1,3,5")
        every { questDao.getDailyQuestsForToday(1L, "2026-06-29") } returns flowOf(emptyList())
        every { questDao.getCustomScheduleQuests(1L, "2026-06-29") } returns flowOf(listOf(customEntity))

        // Act
        val result = repository.getTodayQuests(userId = 1L, today = "2026-06-29").first()

        // Assert — Monday (1) is in "1,3,5" → quest should be included
        assertEquals(1, result.size)
        assertEquals(2L, result[0].id)
    }

    @Test
    fun `getTodayQuests excludes custom_days quest when day does not match`() = runTest {
        // Arrange — 2026-06-30 is Tuesday (dayOfWeek = 2); schedule = Mon/Wed/Fri = "1,3,5"
        val customEntity = buildQuestEntity(id = 3L, scheduleType = "custom_days", scheduleDays = "1,3,5")
        every { questDao.getDailyQuestsForToday(1L, "2026-06-30") } returns flowOf(emptyList())
        every { questDao.getCustomScheduleQuests(1L, "2026-06-30") } returns flowOf(listOf(customEntity))

        // Act
        val result = repository.getTodayQuests(userId = 1L, today = "2026-06-30").first()

        // Assert — Tuesday (2) is NOT in "1,3,5" → quest excluded
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getTodayQuests returns empty list when no quests available`() = runTest {
        // Arrange
        every { questDao.getDailyQuestsForToday(any(), any()) } returns flowOf(emptyList())
        every { questDao.getCustomScheduleQuests(any(), any()) } returns flowOf(emptyList())

        // Act
        val result = repository.getTodayQuests(userId = 1L, today = "2026-06-29").first()

        // Assert
        assertTrue(result.isEmpty())
    }

    // ==================================================================
    // getQuestById
    // ==================================================================

    @Test
    fun `getQuestById returns mapped domain model when quest exists`() = runTest {
        // Arrange
        val entity = buildQuestEntity(id = 10L, title = "Study Kotlin")
        coEvery { questDao.getQuestById(10L) } returns entity

        // Act
        val result = repository.getQuestById(10L)

        // Assert
        assertNotNull(result)
        assertEquals(10L, result!!.id)
        assertEquals("Study Kotlin", result.title)
    }

    @Test
    fun `getQuestById returns null when quest does not exist`() = runTest {
        // Arrange
        coEvery { questDao.getQuestById(999L) } returns null

        // Act
        val result = repository.getQuestById(999L)

        // Assert
        assertNull(result)
    }

    // ==================================================================
    // insertStandaloneQuest — validation tests
    // ==================================================================

    @Test(expected = QuestValidationException::class)
    fun `insertStandaloneQuest throws when title is empty`() = runTest {
        // Arrange
        val quest = buildBaseQuest(title = "   ")

        // Act — should throw
        repository.insertStandaloneQuest(quest)
    }

    @Test(expected = QuestValidationException::class)
    fun `insertStandaloneQuest throws when title exceeds 100 characters`() = runTest {
        // Arrange
        val longTitle = "A".repeat(101)
        val quest = buildBaseQuest(title = longTitle)

        // Act — should throw
        repository.insertStandaloneQuest(quest)
    }

    @Test(expected = QuestValidationException::class)
    fun `insertStandaloneQuest throws when absence_mode set for endless quest`() = runTest {
        // Arrange — endless quest must NOT have absence_mode (validation-rules section 2)
        val quest = buildBaseQuest(durationType = "endless", absenceMode = "shift")

        // Act — should throw
        repository.insertStandaloneQuest(quest)
    }

    @Test(expected = QuestValidationException::class)
    fun `insertStandaloneQuest throws when custom_days selected but schedule_days empty`() = runTest {
        // Arrange
        val quest = buildBaseQuest(scheduleType = "custom_days", scheduleDays = null)

        // Act — should throw
        repository.insertStandaloneQuest(quest)
    }

    @Test(expected = QuestValidationException::class)
    fun `insertStandaloneQuest throws when time_bound but no end_date or target_days`() = runTest {
        // Arrange — time_bound with neither end_date nor target_days
        val quest = buildBaseQuest(
            durationType = "time_bound",
            endDate = null,
            targetDays = null,
            absenceMode = "shift"
        )

        // Act — should throw
        repository.insertStandaloneQuest(quest)
    }

    @Test(expected = QuestValidationException::class)
    fun `insertStandaloneQuest throws when timer mode but no target duration`() = runTest {
        // Arrange — completion_mode='timer' requires target_duration_seconds > 0
        val quest = buildBaseQuest(completionMode = "timer", targetDurationSeconds = null)

        // Act — should throw
        repository.insertStandaloneQuest(quest)
    }

    @Test(expected = QuestValidationException::class)
    fun `insertStandaloneQuest throws when time_bound but absence_mode is null`() = runTest {
        // Arrange — time_bound requires absence_mode
        val quest = buildBaseQuest(
            durationType = "time_bound",
            endDate = "2026-12-31",
            absenceMode = null
        )

        // Act — should throw
        repository.insertStandaloneQuest(quest)
    }

    @Test
    fun `insertStandaloneQuest succeeds for valid endless instant daily quest`() = runTest {
        // Arrange
        val quest = buildBaseQuest(
            completionMode = "instant",
            durationType = "endless",
            scheduleType = "daily",
            scheduleDays = null
        )
        coEvery { questDao.insertQuest(any()) } returns 42L

        // Act
        val result = repository.insertStandaloneQuest(quest)

        // Assert
        assertEquals(42L, result)
        coVerify(exactly = 1) { questDao.insertQuest(any()) }
    }

    // ==================================================================
    // insertEpicWithFirstSubQuest — validation & atomicity tests
    // ==================================================================

    @Test(expected = QuestValidationException::class)
    fun `insertEpicWithFirstSubQuest throws when sub-quest end_date exceeds epic end_date`() = runTest {
        // Arrange — sub-quest end_date AFTER epic end_date (invalid)
        val epic = buildBaseQuest(
            isContainer = true,
            completionMode = null,
            scheduleType = null,
            scheduleDays = null,
            durationType = "time_bound",
            endDate = "2026-06-30",
            absenceMode = "shift"
        )
        val subQuest = buildBaseQuest(
            durationType = "time_bound",
            endDate = "2026-12-31",  // exceeds epic end_date
            absenceMode = "shift"
        )

        // Act — should throw
        repository.insertEpicWithFirstSubQuest(epic, subQuest)
    }

    @Test
    fun `insertEpicWithFirstSubQuest succeeds when sub-quest end_date equals epic end_date`() = runTest {
        // Arrange — boundary value: subQuest.endDate == epic.endDate (must be allowed)
        val epic = buildBaseQuest(
            isContainer = true,
            completionMode = null,
            scheduleType = null,
            scheduleDays = null,
            durationType = "time_bound",
            endDate = "2026-12-31",
            absenceMode = "shift"
        )
        val subQuest = buildBaseQuest(
            durationType = "time_bound",
            endDate = "2026-12-31",  // equals — boundary case
            absenceMode = "shift"
        )
        coEvery { questDao.insertQuest(any()) } returnsMany listOf(10L, 11L)

        // Act
        val epicId = repository.insertEpicWithFirstSubQuest(epic, subQuest)

        // Assert — epic was inserted (returned id 10)
        assertEquals(10L, epicId)
        coVerify(exactly = 2) { questDao.insertQuest(any()) }
    }

    @Test
    fun `insertEpicWithFirstSubQuest calls withTransaction`() = runTest {
        // Arrange
        val epic = buildBaseQuest(
            isContainer = true,
            completionMode = null,
            scheduleType = null,
            scheduleDays = null,
            durationType = "endless",
            absenceMode = null
        )
        val subQuest = buildBaseQuest(durationType = "endless")
        coEvery { questDao.insertQuest(any()) } returnsMany listOf(1L, 2L)

        // Act
        repository.insertEpicWithFirstSubQuest(epic, subQuest)

        // Assert — transaction was invoked exactly once (atomicity guaranteed)
        coVerify(exactly = 1) { db.withTransaction(any<suspend () -> Any?>()) }
    }

    @Test
    fun `insertEpicWithFirstSubQuest sets parentQuestId on sub-quest`() = runTest {
        // Arrange
        val epic = buildBaseQuest(
            isContainer = true,
            completionMode = null,
            scheduleType = null,
            scheduleDays = null,
            durationType = "endless",
            absenceMode = null
        )
        val subQuest = buildBaseQuest(durationType = "endless", parentQuestId = null)

        // Capture all insertQuest calls in order to inspect the sub-quest entity
        val capturedEntities = mutableListOf<QuestEntity>()
        coEvery { questDao.insertQuest(any()) } coAnswers {
            val entity = firstArg<QuestEntity>()
            capturedEntities.add(entity)
            // First insert (epic) returns 99L; second (sub-quest) returns 100L
            if (entity.isContainer == 1) 99L else 100L
        }

        // Act
        repository.insertEpicWithFirstSubQuest(epic, subQuest)

        // Assert — exactly 2 inserts: epic then sub-quest
        assertEquals(2, capturedEntities.size)
        // Sub-quest (second insert, index 1) must have parentQuestId = 99 (the epicId)
        val subQuestEntity = capturedEntities[1]
        assertEquals(99L, subQuestEntity.parentQuestId)
    }

    // ==================================================================
    // archiveQuestToHistory
    // ==================================================================

    @Test
    fun `archiveQuestToHistory inserts to history and deletes quest`() = runTest {
        // Arrange
        val quest = buildBaseQuest(id = 5L, title = "Conquered Quest", createdAt = "2026-01-01 00:00:00")

        // Act
        repository.archiveQuestToHistory(
            quest = quest,
            finalStatus = "completed",
            totalDaysCompleted = 30,
            totalExpEarned = 300.0
        )

        // Assert — history insert and quest delete both called
        coVerify(exactly = 1) { questHistoryDao.insertHistory(any()) }
        coVerify(exactly = 1) { questDao.deleteQuest(5L) }
    }

    @Test
    fun `archiveQuestToHistory runs in single transaction`() = runTest {
        // Arrange
        val quest = buildBaseQuest(id = 7L)

        // Act
        repository.archiveQuestToHistory(
            quest = quest,
            finalStatus = "failed_abandoned",
            totalDaysCompleted = 0,
            totalExpEarned = 0.0
        )

        // Assert — db.withTransaction called exactly once (both ops are atomic)
        coVerify(exactly = 1) { db.withTransaction(any<suspend () -> Any?>()) }
    }

    // ==================================================================
    // Delegation checks
    // ==================================================================

    @Test
    fun `updateQuestStatus delegates to dao with correct id and status`() = runTest {
        // Act
        repository.updateQuestStatus(id = 3L, status = "completed")

        // Assert
        coVerify(exactly = 1) { questDao.updateQuestStatus(id = 3L, status = "completed", now = any()) }
    }

    @Test
    fun `updateMissedSessions delegates to dao with correct parameters`() = runTest {
        // Act
        repository.updateMissedSessions(id = 4L, sessions = 3)

        // Assert
        coVerify(exactly = 1) { questDao.updateMissedSessions(id = 4L, sessions = 3, now = any()) }
    }
}
