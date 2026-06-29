package com.questdday.util

import com.questdday.domain.model.Quest
import com.questdday.domain.model.QuestLog
import java.time.LocalDate

/**
 * Pure utility functions for calculating missed sessions and inactive scheduled days.
 * No Android dependency — testable as plain JVM unit tests.
 *
 * These functions receive pre-computed data from the repository layer;
 * they do NOT access the database directly.
 */
object MissedSessionCalculator {

    /**
     * Calculate consecutive missed sessions for a single quest,
     * counting trailing misses from the most recent date backwards.
     *
     * Iterates scheduledDates from back (newest) to front (oldest).
     * Stops and returns the count as soon as a completed date is found.
     *
     * @param scheduledDates Ordered list of dates the quest was scheduled (oldest to newest).
     *                       Typically produced by ScheduleCalculator.getScheduledDatesInRange.
     *                       Today is EXCLUDED from this list by the caller.
     * @param completedDates Set of dates that have a quest_log entry (for O(1) lookup).
     * @return Number of consecutive missed sessions from the most recent scheduled date.
     *         Returns 0 if scheduledDates is empty or all sessions are completed.
     */
    fun calculateConsecutiveMissedSessions(
        scheduledDates: List<LocalDate>,
        completedDates: Set<LocalDate>
    ): Int {
        if (scheduledDates.isEmpty()) return 0

        var consecutiveMisses = 0
        // Iterate from back (most recent) to front (oldest)
        for (i in scheduledDates.indices.reversed()) {
            if (scheduledDates[i] in completedDates) {
                // Found a completion — stop counting
                break
            }
            consecutiveMisses++
        }
        return consecutiveMisses
    }

    /**
     * Determine if a specific day is an "inactive scheduled day" for the user.
     * An inactive scheduled day is one where:
     * - At least one quest is scheduled on that day, AND
     * - No quest completions (quest_logs) exist for that day.
     *
     * Days with no scheduled quests are NOT counted as inactive (free days).
     * Used for tracking users.consecutive_inactive_scheduled_days (EXP Decay trigger).
     *
     * @param date The date to evaluate
     * @param questsScheduledOnDate List of active quests scheduled on this date
     * @param completionsOnDate List of quest_logs recorded on this date
     * @return true if quests were scheduled but none were completed; false otherwise
     */
    fun isDayInactiveScheduled(
        date: LocalDate,
        questsScheduledOnDate: List<Quest>,
        completionsOnDate: List<QuestLog>
    ): Boolean {
        // No quests scheduled → free day → not inactive
        if (questsScheduledOnDate.isEmpty()) return false

        // At least one completion exists → active day
        if (completionsOnDate.isNotEmpty()) return false

        // Quests scheduled but none completed → inactive scheduled day
        return true
    }
}
