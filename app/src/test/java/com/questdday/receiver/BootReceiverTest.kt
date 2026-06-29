package com.questdday.receiver

import android.content.Context
import android.content.Intent
import com.questdday.data.local.AppDatabase
import com.questdday.data.local.dao.ActiveTimerDao
import com.questdday.data.local.entity.ActiveTimerEntity
import com.questdday.util.AlarmScheduler
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class BootReceiverTest {

    private val context = mockk<Context>(relaxed = true)
    private val db = mockk<AppDatabase>(relaxed = true)
    private val activeTimerDao = mockk<ActiveTimerDao>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()
    private val extras = mutableMapOf<String, Any>()

    @Before
    fun setUp() {
        mockkObject(AppDatabase.Companion)
        mockkObject(AlarmScheduler)
        mockkConstructor(Intent::class)
        
        every { AppDatabase.getDatabase(any()) } returns db
        every { db.activeTimerDao() } returns activeTimerDao
        BootReceiver.dispatcher = testDispatcher

        // Dynamic mock for Intent extras and component
        val component = mockk<android.content.ComponentName>()
        every { component.className } returns "QuestAlarmReceiver"
        every { anyConstructed<Intent>().component } returns component

        extras.clear()
        every { anyConstructed<Intent>().putExtra(any<String>(), any<Long>()) } answers {
            val key = firstArg<String>()
            val value = secondArg<Long>()
            extras[key] = value
            self as Intent
        }
        every { anyConstructed<Intent>().getLongExtra(any<String>(), any<Long>()) } answers {
            val key = firstArg<String>()
            val default = secondArg<Long>()
            (extras[key] as? Long) ?: default
        }
    }

    @After
    fun tearDown() {
        unmockkObject(AppDatabase.Companion)
        unmockkObject(AlarmScheduler)
        unmockkConstructor(Intent::class)
        BootReceiver.dispatcher = kotlinx.coroutines.Dispatchers.IO
    }

    @Test
    fun `onReceive reschedules timers where alarm not yet fired and trigger in future`() = runTest(testDispatcher) {
        // Arrange
        val intent = mockk<Intent>()
        every { intent.action } returns Intent.ACTION_BOOT_COMPLETED

        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val startedAtStr = now.minusMinutes(5).format(formatter) // started 5 mins ago
        
        // Target duration is 10 mins (600 seconds) -> trigger time is in 5 mins (future)
        val futureTimer = ActiveTimerEntity(
            id = 1L,
            questId = 101L,
            startedAt = startedAtStr,
            targetDurationSeconds = 600,
            alarmFiredAt = null
        )

        coEvery { activeTimerDao.getRunningTimersOnce() } returns listOf(futureTimer)
        every { AlarmScheduler.scheduleQuestAlarm(any(), any(), any()) } just Runs

        val bootReceiver = BootReceiver()

        // Act
        bootReceiver.onReceive(context, intent)

        // Assert
        val localDateTime = LocalDateTime.parse(startedAtStr, formatter)
        val expectedTrigger = localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() + 600 * 1000L

        verify(exactly = 1) {
            AlarmScheduler.scheduleQuestAlarm(context, 101L, expectedTrigger)
        }
        verify(exactly = 0) {
            context.sendBroadcast(any())
        }
    }

    @Test
    fun `onReceive marks alarm fired for timers where trigger already passed`() = runTest(testDispatcher) {
        // Arrange
        val intent = mockk<Intent>()
        every { intent.action } returns Intent.ACTION_BOOT_COMPLETED

        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val startedAtStr = now.minusMinutes(15).format(formatter) // started 15 mins ago
        
        // Target duration is 10 mins (600 seconds) -> trigger time was 5 mins ago (past)
        val pastTimer = ActiveTimerEntity(
            id = 2L,
            questId = 102L,
            startedAt = startedAtStr,
            targetDurationSeconds = 600,
            alarmFiredAt = null
        )

        coEvery { activeTimerDao.getRunningTimersOnce() } returns listOf(pastTimer)
        val intentSlot = slot<Intent>()
        every { context.sendBroadcast(capture(intentSlot)) } returns Unit

        val bootReceiver = BootReceiver()

        // Act
        bootReceiver.onReceive(context, intent)

        // Assert
        verify(exactly = 0) {
            AlarmScheduler.scheduleQuestAlarm(any(), any(), any())
        }
        verify(exactly = 1) {
            context.sendBroadcast(any())
        }
        assert(intentSlot.captured.component?.className?.contains("QuestAlarmReceiver") == true)
        assert(intentSlot.captured.getLongExtra("quest_id", -1L) == 102L)
    }

    @Test
    fun `onReceive ignores non-BOOT_COMPLETED intents`() = runTest(testDispatcher) {
        // Arrange
        val intent = mockk<Intent>()
        every { intent.action } returns Intent.ACTION_MY_PACKAGE_REPLACED

        val bootReceiver = BootReceiver()

        // Act
        bootReceiver.onReceive(context, intent)

        // Assert
        coVerify(exactly = 0) {
            activeTimerDao.getRunningTimersOnce()
        }
    }
}
