package com.questdday.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class AlarmSchedulerTest {

    private val context = mockk<Context>(relaxed = true)
    private val alarmManager = mockk<AlarmManager>(relaxed = true)
    private val pendingIntent = mockk<PendingIntent>(relaxed = true)
    private val extras = mutableMapOf<String, Any>()

    @Before
    fun setUp() {
        mockkStatic(PendingIntent::class)
        mockkConstructor(Intent::class)
        
        every { context.getSystemService(Context.ALARM_SERVICE) } returns alarmManager

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
        unmockkStatic(PendingIntent::class)
        unmockkConstructor(Intent::class)
    }

    @Test
    fun `scheduleQuestAlarm creates PendingIntent with correct questId as request code`() {
        // Arrange
        val questId = 123L
        val triggerAtMillis = 10000L
        val intentSlot = slot<Intent>()

        every {
            PendingIntent.getBroadcast(
                context,
                questId.toInt(),
                capture(intentSlot),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } returns pendingIntent

        // Act
        AlarmScheduler.scheduleQuestAlarm(context, questId, triggerAtMillis)

        // Assert
        verify(exactly = 1) {
            PendingIntent.getBroadcast(
                context,
                123, // request code is questId.toInt()
                any(),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        verify(exactly = 1) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
        // Ensure intent target is QuestAlarmReceiver and has the quest_id extra
        assert(intentSlot.captured.component?.className?.contains("QuestAlarmReceiver") == true)
        assert(intentSlot.captured.getLongExtra("quest_id", -1L) == 123L)
    }

    @Test
    fun `cancelQuestAlarm cancels existing PendingIntent`() {
        // Arrange
        val questId = 123L
        every {
            PendingIntent.getBroadcast(
                context,
                questId.toInt(),
                any(),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
        } returns pendingIntent

        // Act
        AlarmScheduler.cancelQuestAlarm(context, questId)

        // Assert
        verify(exactly = 1) {
            PendingIntent.getBroadcast(
                context,
                123,
                any(),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
        }
        verify(exactly = 1) {
            alarmManager.cancel(pendingIntent)
        }
        verify(exactly = 1) {
            pendingIntent.cancel()
        }
    }
}
