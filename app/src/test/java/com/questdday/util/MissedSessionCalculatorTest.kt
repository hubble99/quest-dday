package com.questdday.util

import com.questdday.domain.model.Quest
import com.questdday.domain.model.QuestLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MissedSessionCalculatorTest {

    // =======================================================================
    // Helper factories for domain models
    // =======================================================================

    private fun createQuest(
        id: Long = 1L,
        title: String = "Test Quest",
        scheduleType: String = "daily"
    ): Quest = Quest(
        id = id,
        userId = 1L,
        parentQuestId = null,
        attributeId = 1L,
        title = title,
        description = null,
        isContainer = false,
        completionMode = "instant",
        targetDurationSeconds = null,
        durationType = "endless",
        durationInputType = null,
        targetDays = null,
        startDate = "2026-01-01",
        endDate = null,
        absenceMode = null,
        stackedDurationSeconds = 0,
        scheduleType = scheduleType,
        scheduleDays = null,
        status = "active",
        consecutiveMissedSessions = 0,
        lastCompletedAt = null,
        createdAt = "2026-01-01T00:00:00",
        updatedAt = "2026-01-01T00:00:00"
    )

    private fun createQuestLog(
        questId: Long = 1L,
        logDate: String = "2026-06-29"
    ): QuestLog = QuestLog(
        id = 1L,
        questId = questId,
        userId = 1L,
        logDate = logDate,
        actualDurationSeconds = null,
        expAwarded = 10.0,
        isEpicFinaleBonus = false,
        completedAt = "2026-06-29T10:00:00"
    )

    // =======================================================================
    // calculateConsecutiveMissedSessions
    // =======================================================================

    @Test
    fun `calculateConsecutiveMissedSessions returns 0 when all sessions completed`() {
        // Arrange
        val scheduledDates = listOf(
            LocalDate.of(2026, 6, 23), // Mon
            LocalDate.of(2026, 6, 25), // Wed
            LocalDate.of(2026, 6, 27)  // Fri
        )
        val completedDates = setOf(
            LocalDate.of(2026, 6, 23),
            LocalDate.of(2026, 6, 25),
            LocalDate.of(2026, 6, 27)
        )

        // Act
        val result = MissedSessionCalculator.calculateConsecutiveMissedSessions(
            scheduledDates, completedDates
        )

        // Assert
        assertEquals(0, result)
    }

    @Test
    fun `calculateConsecutiveMissedSessions returns correct count for trailing misses`() {
        // Arrange — completed Mon and Wed, missed Fri and Sun (2 trailing misses)
        val scheduledDates = listOf(
            LocalDate.of(2026, 6, 23), // Mon — completed
            LocalDate.of(2026, 6, 25), // Wed — completed
            LocalDate.of(2026, 6, 27), // Fri — missed
            LocalDate.of(2026, 6, 29)  // Mon — missed (today excluded by caller, but present here for test)
        )
        val completedDates = setOf(
            LocalDate.of(2026, 6, 23),
            LocalDate.of(2026, 6, 25)
        )

        // Act
        val result = MissedSessionCalculator.calculateConsecutiveMissedSessions(
            scheduledDates, completedDates
        )

        // Assert — 2 trailing misses (Jun 29 and Jun 27)
        assertEquals(2, result)
    }

    @Test
    fun `calculateConsecutiveMissedSessions returns 0 when no scheduled dates`() {
        // Arrange
        val scheduledDates = emptyList<LocalDate>()
        val completedDates = emptySet<LocalDate>()

        // Act
        val result = MissedSessionCalculator.calculateConsecutiveMissedSessions(
            scheduledDates, completedDates
        )

        // Assert
        assertEquals(0, result)
    }

    @Test
    fun `calculateConsecutiveMissedSessions resets count when completion found in sequence`() {
        // Arrange — pattern: missed, completed, missed, missed (from oldest to newest)
        // Trailing from back: missed, missed, then hits completed → count = 2
        val scheduledDates = listOf(
            LocalDate.of(2026, 6, 22), // Mon — missed
            LocalDate.of(2026, 6, 24), // Wed — completed (acts as reset)
            LocalDate.of(2026, 6, 26), // Fri — missed
            LocalDate.of(2026, 6, 28)  // Sun — missed
        )
        val completedDates = setOf(
            LocalDate.of(2026, 6, 24)
        )

        // Act
        val result = MissedSessionCalculator.calculateConsecutiveMissedSessions(
            scheduledDates, completedDates
        )

        // Assert — only trailing 2 misses count (Jun 28 and Jun 26), reset at Jun 24
        assertEquals(2, result)
    }

    @Test
    fun `calculateConsecutiveMissedSessions returns total scheduled count when none completed`() {
        // Arrange — 3 scheduled dates, none completed
        val scheduledDates = listOf(
            LocalDate.of(2026, 6, 23),
            LocalDate.of(2026, 6, 25),
            LocalDate.of(2026, 6, 27)
        )
        val completedDates = emptySet<LocalDate>()

        // Act
        val result = MissedSessionCalculator.calculateConsecutiveMissedSessions(
            scheduledDates, completedDates
        )

        // Assert — all 3 are trailing misses
        assertEquals(3, result)
    }

    @Test
    fun `calculateConsecutiveMissedSessions returns 0 when scheduledDates empty and completedDates empty`() {
        // Arrange
        val scheduledDates = emptyList<LocalDate>()
        val completedDates = emptySet<LocalDate>()

        // Act
        val result = MissedSessionCalculator.calculateConsecutiveMissedSessions(
            scheduledDates, completedDates
        )

        // Assert
        assertEquals(0, result)
    }

    // =======================================================================
    // isDayInactiveScheduled
    // =======================================================================

    @Test
    fun `isDayInactiveScheduled returns false when no quests scheduled on date`() {
        // Arrange — free day, no quests scheduled
        val date = LocalDate.of(2026, 6, 29)
        val questsScheduledOnDate = emptyList<Quest>()
        val completionsOnDate = emptyList<QuestLog>()

        // Act
        val result = MissedSessionCalculator.isDayInactiveScheduled(
            date, questsScheduledOnDate, completionsOnDate
        )

        // Assert — free day is NOT counted as inactive
        assertFalse(result)
    }

    @Test
    fun `isDayInactiveScheduled returns false when at least one completion exists`() {
        // Arrange — quest scheduled and at least one completed
        val date = LocalDate.of(2026, 6, 29)
        val questsScheduledOnDate = listOf(createQuest(id = 1L), createQuest(id = 2L))
        val completionsOnDate = listOf(createQuestLog(questId = 1L, logDate = "2026-06-29"))

        // Act
        val result = MissedSessionCalculator.isDayInactiveScheduled(
            date, questsScheduledOnDate, completionsOnDate
        )

        // Assert — at least one completion → active day
        assertFalse(result)
    }

    @Test
    fun `isDayInactiveScheduled returns true when quests scheduled but none completed`() {
        // Arrange — 2 quests scheduled, zero completions
        val date = LocalDate.of(2026, 6, 29)
        val questsScheduledOnDate = listOf(createQuest(id = 1L), createQuest(id = 2L))
        val completionsOnDate = emptyList<QuestLog>()

        // Act
        val result = MissedSessionCalculator.isDayInactiveScheduled(
            date, questsScheduledOnDate, completionsOnDate
        )

        // Assert — quests existed but none done → inactive scheduled day
        assertTrue(result)
    }

    @Test
    fun `isDayInactiveScheduled returns false when both lists are empty`() {
        // Arrange — no quests, no completions
        val date = LocalDate.of(2026, 6, 29)
        val questsScheduledOnDate = emptyList<Quest>()
        val completionsOnDate = emptyList<QuestLog>()

        // Act
        val result = MissedSessionCalculator.isDayInactiveScheduled(
            date, questsScheduledOnDate, completionsOnDate
        )

        // Assert — no quests scheduled → not inactive
        assertFalse(result)
    }
}
