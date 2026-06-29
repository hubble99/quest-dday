package com.questdday.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.questdday.data.local.AppDatabase
import com.questdday.service.QuestTimerService
import com.questdday.util.AlarmScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class BootReceiver : BroadcastReceiver() {

    companion object {
        var dispatcher: CoroutineDispatcher = Dispatchers.IO
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = try {
            goAsync()
        } catch (t: Throwable) {
            null
        }

        CoroutineScope(dispatcher).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val activeTimerDao = db.activeTimerDao()
                val runningTimers = activeTimerDao.getRunningTimersOnce()

                var hasRescheduled = false
                val nowEpoch = System.currentTimeMillis()

                for (timer in runningTimers) {
                    val startedAtMillis = parseDateTimeToMillis(timer.startedAt)
                    val targetDurationMillis = timer.targetDurationSeconds * 1000L
                    val triggerAtMillis = startedAtMillis + targetDurationMillis

                    if (triggerAtMillis <= nowEpoch) {
                        // Past duration elapsed during reboot, fire alarm immediately
                        val alarmIntent = Intent(context, QuestAlarmReceiver::class.java).apply {
                            putExtra("quest_id", timer.questId)
                        }
                        context.sendBroadcast(alarmIntent)
                    } else {
                        // Future trigger, reschedule alarm via AlarmScheduler
                        AlarmScheduler.scheduleQuestAlarm(context, timer.questId, triggerAtMillis)
                        hasRescheduled = true
                    }
                }

                // If any timer is still running or rescheduled, restore QuestTimerService
                if (hasRescheduled || runningTimers.any { parseDateTimeToMillis(it.startedAt) + it.targetDurationSeconds * 1000L > nowEpoch }) {
                    val serviceIntent = Intent(context, QuestTimerService::class.java)
                    context.startForegroundService(serviceIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult?.finish()
            }
        }
    }

    private fun parseDateTimeToMillis(dateTimeStr: String): Long {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val localDateTime = LocalDateTime.parse(dateTimeStr, formatter)
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
