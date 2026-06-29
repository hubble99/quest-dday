package com.questdday.util

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Pure utility functions for schedule calculations.
 * No Android dependency — testable as plain JVM unit tests.
 *
 * Convention: 1=Monday..7=Sunday (ISO DayOfWeek.value)
 * CSV format in DB: "1,3,5" = Monday, Wednesday, Friday
 */
object ScheduleCalculator {

    /**
     * Parse CSV schedule_days string to List<Int>.
     * Input: "1,3,5" → Output: [1, 3, 5] (1=Monday..7=Sunday)
     *
     * Returns empty list for null, empty, or blank input.
     * Ignores non-integer and out-of-range (not 1–7) values.
     */
    fun parseScheduleDays(scheduleDays: String?): List<Int> {
        if (scheduleDays.isNullOrBlank()) return emptyList()

        return scheduleDays.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
    }

    /**
     * Check if a date falls on a scheduled day.
     *
     * @param date The date to check
     * @param scheduleDays List of scheduled day-of-week values (1=Monday..7=Sunday)
     * @return true if the day of week is in scheduleDays; false if scheduleDays is empty
     */
    fun isScheduledOnDate(date: LocalDate, scheduleDays: List<Int>): Boolean {
        if (scheduleDays.isEmpty()) return false
        return date.dayOfWeek.value in scheduleDays
    }

    /**
     * Get all scheduled dates within an inclusive range.
     *
     * @param fromDate Start of range (inclusive)
     * @param toDate End of range (inclusive)
     * @param scheduleType "daily" or "custom_days"
     * @param scheduleDays List of day-of-week values (used only when scheduleType is "custom_days")
     * @return List of LocalDate that are scheduled within the range
     */
    fun getScheduledDatesInRange(
        fromDate: LocalDate,
        toDate: LocalDate,
        scheduleType: String,
        scheduleDays: List<Int>
    ): List<LocalDate> {
        if (fromDate.isAfter(toDate)) return emptyList()

        val result = mutableListOf<LocalDate>()
        var current = fromDate
        while (!current.isAfter(toDate)) {
            when (scheduleType) {
                "daily" -> result.add(current)
                "custom_days" -> {
                    if (isScheduledOnDate(current, scheduleDays)) {
                        result.add(current)
                    }
                }
            }
            current = current.plusDays(1)
        }
        return result
    }

    /**
     * Find the Nth next scheduled day AFTER fromDate.
     * fromDate itself is NEVER returned — always strictly after.
     *
     * Used by Mode Shift to calculate new end_date.
     *
     * @param fromDate The reference date (exclusive — result is always after this)
     * @param scheduleDays List of scheduled day-of-week values (1=Monday..7=Sunday)
     * @param count How many scheduled slots to skip forward (default: 1)
     * @return The Nth next scheduled LocalDate after fromDate
     * @throws IllegalArgumentException if scheduleDays is empty (infinite loop prevention)
     */
    fun findNextScheduledDay(
        fromDate: LocalDate,
        scheduleDays: List<Int>,
        count: Int = 1
    ): LocalDate {
        require(scheduleDays.isNotEmpty()) {
            "scheduleDays must not be empty — cannot find next scheduled day without a schedule"
        }

        var found = 0
        var current = fromDate.plusDays(1) // Start AFTER fromDate
        while (found < count) {
            if (current.dayOfWeek.value in scheduleDays) {
                found++
                if (found == count) return current
            }
            current = current.plusDays(1)
        }
        // Unreachable given non-empty scheduleDays, but satisfies compiler
        return current
    }
}
