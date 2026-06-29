package com.questdday.data.local.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.questdday.data.local.AppDatabase
import com.questdday.data.local.entity.ActiveTimerEntity
import com.questdday.data.local.entity.QuestEntity
import com.questdday.data.local.entity.UserEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActiveTimerDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var activeTimerDao: ActiveTimerDao
    private lateinit var questDao: QuestDao
    private lateinit var userDao: UserDao

    private val testUserId = 1L
    private val testQuestId = 10L

    @Before
    fun setup() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        activeTimerDao = db.activeTimerDao()
        questDao = db.questDao()
        userDao = db.userDao()

        userDao.insertUser(UserEntity(
            id = testUserId,
            lastActiveAt = "2026-06-29",
            lastEvaluatedDate = null,
            createdAt = "2026-06-29"
        ))
        questDao.insertQuest(
            QuestEntity(
                id = testQuestId,
                userId = testUserId,
                parentQuestId = null,
                attributeId = null,
                title = "Timer Quest",
                description = null,
                completionMode = null,
                targetDurationSeconds = 1800,
                durationType = "endless",
                durationInputType = null,
                targetDays = null,
                startDate = "2026-06-29",
                endDate = null,
                absenceMode = null,
                scheduleDays = null,
                lastCompletedAt = null,
                createdAt = "2026-06-29",
                updatedAt = "2026-06-29"
            )
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `insertTimer_succeeds_for_new_questId`() = runTest {
        // Arrange
        val timer = ActiveTimerEntity(
            questId = testQuestId,
            startedAt = "2026-06-29 10:00:00",
            targetDurationSeconds = 1800
        )

        // Act
        val id = activeTimerDao.insertTimer(timer)
        val result = activeTimerDao.getTimerByQuestId(testQuestId)

        // Assert
        assertNotNull(result)
        assertEquals(testQuestId, result?.questId)
        assertNull(result?.alarmFiredAt)
    }

    @Test
    fun `insertTimer_throws_for_duplicate_questId_due_to_ABORT_strategy`() = runTest {
        // Arrange
        val timer = ActiveTimerEntity(
            questId = testQuestId,
            startedAt = "2026-06-29 10:00:00",
            targetDurationSeconds = 1800
        )
        activeTimerDao.insertTimer(timer)

        // Act & Assert
        try {
            activeTimerDao.insertTimer(timer.copy(id = 0))
            org.junit.Assert.fail("Expected SQLiteConstraintException")
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            // Expected
        }
    }

    @Test
    fun `getTimerByQuestId_returns_null_if_no_timer_exists`() = runTest {
        // Act
        val result = activeTimerDao.getTimerByQuestId(999L)

        // Assert
        assertNull(result)
    }

    @Test
    fun `getPendingConfirmationTimers_returns_only_timers_with_alarm_fired_at_NOT_NULL`() = runTest {
        // Arrange
        val quest2Id = 11L
        questDao.insertQuest(
            QuestEntity(
                id = quest2Id,
                userId = testUserId,
                parentQuestId = null,
                attributeId = null,
                title = "Timer Quest 2",
                description = null,
                completionMode = null,
                targetDurationSeconds = 1800,
                durationType = "endless",
                durationInputType = null,
                targetDays = null,
                startDate = "2026-06-29",
                endDate = null,
                absenceMode = null,
                scheduleDays = null,
                lastCompletedAt = null,
                createdAt = "2026-06-29",
                updatedAt = "2026-06-29"
            )
        )
        activeTimerDao.insertTimer(ActiveTimerEntity(questId = testQuestId, startedAt = "now", targetDurationSeconds = 1800, alarmFiredAt = "fired"))
        activeTimerDao.insertTimer(ActiveTimerEntity(questId = quest2Id, startedAt = "now", targetDurationSeconds = 1800, alarmFiredAt = null))

        // Act
        val pendingTimers = activeTimerDao.getPendingConfirmationTimers().first()

        // Assert
        assertEquals(1, pendingTimers.size)
        assertEquals(testQuestId, pendingTimers[0].questId)
    }

    @Test
    fun `getRunningTimers_returns_only_timers_with_alarm_fired_at_IS_NULL`() = runTest {
        // Arrange
        val quest2Id = 11L
        questDao.insertQuest(
            QuestEntity(
                id = quest2Id,
                userId = testUserId,
                parentQuestId = null,
                attributeId = null,
                title = "Timer Quest 2",
                description = null,
                completionMode = null,
                targetDurationSeconds = 1800,
                durationType = "endless",
                durationInputType = null,
                targetDays = null,
                startDate = "2026-06-29",
                endDate = null,
                absenceMode = null,
                scheduleDays = null,
                lastCompletedAt = null,
                createdAt = "2026-06-29",
                updatedAt = "2026-06-29"
            )
        )
        activeTimerDao.insertTimer(ActiveTimerEntity(questId = testQuestId, startedAt = "now", targetDurationSeconds = 1800, alarmFiredAt = "fired"))
        activeTimerDao.insertTimer(ActiveTimerEntity(questId = quest2Id, startedAt = "now", targetDurationSeconds = 1800, alarmFiredAt = null))

        // Act
        val runningTimers = activeTimerDao.getRunningTimers().first()

        // Assert
        assertEquals(1, runningTimers.size)
        assertEquals(quest2Id, runningTimers[0].questId)
    }

    @Test
    fun `markAlarmFired_sets_alarm_fired_at_correctly`() = runTest {
        // Arrange
        activeTimerDao.insertTimer(ActiveTimerEntity(questId = testQuestId, startedAt = "now", targetDurationSeconds = 1800, alarmFiredAt = null))
        val firedTime = "2026-06-29 10:30:00"

        // Act
        activeTimerDao.markAlarmFired(testQuestId, firedTime)
        val result = activeTimerDao.getTimerByQuestId(testQuestId)

        // Assert
        assertEquals(firedTime, result?.alarmFiredAt)
    }

    @Test
    fun `deleteTimer_removes_timer_from_table`() = runTest {
        // Arrange
        activeTimerDao.insertTimer(ActiveTimerEntity(questId = testQuestId, startedAt = "now", targetDurationSeconds = 1800))

        // Act
        activeTimerDao.deleteTimer(testQuestId)
        val result = activeTimerDao.getTimerByQuestId(testQuestId)

        // Assert
        assertNull(result)
    }

    @Test
    fun `timerExistsForQuest_returns_0_when_no_timer_and_1_when_exists`() = runTest {
        // Act (no timer)
        val countBefore = activeTimerDao.timerExistsForQuest(testQuestId)
        
        // Arrange
        activeTimerDao.insertTimer(ActiveTimerEntity(questId = testQuestId, startedAt = "now", targetDurationSeconds = 1800))
        
        // Act (has timer)
        val countAfter = activeTimerDao.timerExistsForQuest(testQuestId)

        // Assert
        assertEquals(0, countBefore)
        assertEquals(1, countAfter)
    }
}
