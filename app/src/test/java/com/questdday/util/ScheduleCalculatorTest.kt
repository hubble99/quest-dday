package com.questdday.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ScheduleCalculatorTest {

    // =======================================================================
    // parseScheduleDays
    // =======================================================================

    @Test
    fun `parseScheduleDays returns empty list for null input`() {
        // Arrange
        val input: String? = null

        // Act
        val result = ScheduleCalculator.parseScheduleDays(input)

        // Assert
        assertEquals(emptyList<Int>(), result)
    }

    @Test
    fun `parseScheduleDays returns empty list for empty string`() {
        // Arrange
        val input = ""

        // Act
        val result = ScheduleCalculator.parseScheduleDays(input)

        // Assert
        assertEquals(emptyList<Int>(), result)
    }

    @Test
    fun `parseScheduleDays returns empty list for blank string`() {
        // Arrange
        val input = "   "

        // Act
        val result = ScheduleCalculator.parseScheduleDays(input)

        // Assert
        assertEquals(emptyList<Int>(), result)
    }

    @Test
    fun `parseScheduleDays parses 1,3,5 correctly to list of 1 3 5`() {
        // Arrange
        val input = "1,3,5"

        // Act
        val result = ScheduleCalculator.parseScheduleDays(input)

        // Assert
        assertEquals(listOf(1, 3, 5), result)
    }

    @Test
    fun `parseScheduleDays ignores non-integer values in CSV`() {
        // Arrange
        val input = "1,,3,abc,5"

        // Act
        val result = ScheduleCalculator.parseScheduleDays(input)

        // Assert
        assertEquals(listOf(1, 3, 5), result)
    }

    @Test
    fun `parseScheduleDays filters out values outside 1-7 range`() {
        // Arrange
        val input = "0,1,3,8,7,10"

        // Act
        val result = ScheduleCalculator.parseScheduleDays(input)

        // Assert
        assertEquals(listOf(1, 3, 7), result)
    }

    // =======================================================================
    // isScheduledOnDate
    // =======================================================================

    @Test
    fun `isScheduledOnDate returns true when day matches scheduleDays`() {
        // Arrange — 2026-06-29 is a Monday (DayOfWeek.MONDAY = 1)
        val date = LocalDate.of(2026, 6, 29)
        val scheduleDays = listOf(1, 3, 5) // Mon, Wed, Fri

        // Act
        val result = ScheduleCalculator.isScheduledOnDate(date, scheduleDays)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `isScheduledOnDate returns false when day not in scheduleDays`() {
        // Arrange — 2026-06-30 is a Tuesday (DayOfWeek.TUESDAY = 2)
        val date = LocalDate.of(2026, 6, 30)
        val scheduleDays = listOf(1, 3, 5) // Mon, Wed, Fri

        // Act
        val result = ScheduleCalculator.isScheduledOnDate(date, scheduleDays)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `isScheduledOnDate returns false when scheduleDays is empty`() {
        // Arrange
        val date = LocalDate.of(2026, 6, 29)
        val scheduleDays = emptyList<Int>()

        // Act
        val result = ScheduleCalculator.isScheduledOnDate(date, scheduleDays)

        // Assert
        assertFalse(result)
    }

    // =======================================================================
    // getScheduledDatesInRange
    // =======================================================================

    @Test
    fun `getScheduledDatesInRange daily returns all days inclusive`() {
        // Arrange — 7 days from Mon Jun 29 to Sun Jul 5
        val fromDate = LocalDate.of(2026, 6, 29)
        val toDate = LocalDate.of(2026, 7, 5)

        // Act
        val result = ScheduleCalculator.getScheduledDatesInRange(
            fromDate, toDate, "daily", emptyList()
        )

        // Assert
        assertEquals(7, result.size)
        assertEquals(fromDate, result.first())
        assertEquals(toDate, result.last())
    }

    @Test
    fun `getScheduledDatesInRange custom returns only scheduled days`() {
        // Arrange — Mon Jun 29 to Sun Jul 5, schedule Mon(1) Wed(3) Fri(5)
        val fromDate = LocalDate.of(2026, 6, 29) // Monday
        val toDate = LocalDate.of(2026, 7, 5)     // Sunday
        val scheduleDays = listOf(1, 3, 5)

        // Act
        val result = ScheduleCalculator.getScheduledDatesInRange(
            fromDate, toDate, "custom_days", scheduleDays
        )

        // Assert — Mon Jun 29, Wed Jul 1, Fri Jul 3
        assertEquals(3, result.size)
        assertEquals(LocalDate.of(2026, 6, 29), result[0]) // Monday
        assertEquals(LocalDate.of(2026, 7, 1), result[1])  // Wednesday
        assertEquals(LocalDate.of(2026, 7, 3), result[2])  // Friday
    }

    @Test
    fun `getScheduledDatesInRange returns empty list when fromDate after toDate`() {
        // Arrange
        val fromDate = LocalDate.of(2026, 7, 5)
        val toDate = LocalDate.of(2026, 6, 29)

        // Act
        val result = ScheduleCalculator.getScheduledDatesInRange(
            fromDate, toDate, "daily", emptyList()
        )

        // Assert
        assertEquals(emptyList<LocalDate>(), result)
    }

    @Test
    fun `getScheduledDatesInRange returns single day when fromDate equals toDate and is scheduled`() {
        // Arrange — Mon Jun 29 is a Monday (1), scheduled on Mon(1)
        val date = LocalDate.of(2026, 6, 29)
        val scheduleDays = listOf(1) // Monday only

        // Act
        val result = ScheduleCalculator.getScheduledDatesInRange(
            date, date, "custom_days", scheduleDays
        )

        // Assert
        assertEquals(1, result.size)
        assertEquals(date, result[0])
    }

    // =======================================================================
    // findNextScheduledDay
    // =======================================================================

    @Test
    fun `findNextScheduledDay returns next scheduled day after fromDate`() {
        // Arrange — fromDate is Mon Jun 29, schedule Mon(1) Wed(3) Fri(5)
        // Next scheduled after Mon is Wed Jul 1
        val fromDate = LocalDate.of(2026, 6, 29) // Monday
        val scheduleDays = listOf(1, 3, 5)

        // Act
        val result = ScheduleCalculator.findNextScheduledDay(fromDate, scheduleDays)

        // Assert
        assertEquals(LocalDate.of(2026, 7, 1), result) // Wednesday
    }

    @Test
    fun `findNextScheduledDay skips non-scheduled days correctly`() {
        // Arrange — fromDate is Tue Jun 30, schedule only Fri(5)
        // Next Fri after Tue Jun 30 is Fri Jul 3
        val fromDate = LocalDate.of(2026, 6, 30) // Tuesday
        val scheduleDays = listOf(5) // Friday only

        // Act
        val result = ScheduleCalculator.findNextScheduledDay(fromDate, scheduleDays)

        // Assert
        assertEquals(LocalDate.of(2026, 7, 3), result) // Friday
    }

    @Test
    fun `findNextScheduledDay with count=3 returns third next scheduled day`() {
        // Arrange — fromDate is Mon Jun 29, schedule Mon(1) Wed(3) Fri(5)
        // 1st: Wed Jul 1, 2nd: Fri Jul 3, 3rd: Mon Jul 6
        val fromDate = LocalDate.of(2026, 6, 29) // Monday
        val scheduleDays = listOf(1, 3, 5)

        // Act
        val result = ScheduleCalculator.findNextScheduledDay(fromDate, scheduleDays, count = 3)

        // Assert
        assertEquals(LocalDate.of(2026, 7, 6), result) // Monday (3rd scheduled slot)
    }

    @Test
    fun `findNextScheduledDay never returns fromDate itself even if fromDate is scheduled`() {
        // Arrange — fromDate is Mon Jun 29 (Monday=1), schedule includes Mon(1)
        val fromDate = LocalDate.of(2026, 6, 29) // Monday
        val scheduleDays = listOf(1) // Monday only

        // Act
        val result = ScheduleCalculator.findNextScheduledDay(fromDate, scheduleDays)

        // Assert — must return NEXT Monday (Jul 6), NOT fromDate itself
        assertEquals(LocalDate.of(2026, 7, 6), result)
        assertTrue(result.isAfter(fromDate))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `findNextScheduledDay throws for empty scheduleDays`() {
        // Arrange
        val fromDate = LocalDate.of(2026, 6, 29)
        val scheduleDays = emptyList<Int>()

        // Act — should throw IllegalArgumentException
        ScheduleCalculator.findNextScheduledDay(fromDate, scheduleDays)
    }
}
