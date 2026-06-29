package com.questdday.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.questdday.receiver.QuestAlarmReceiver

object AlarmScheduler {
    fun scheduleQuestAlarm(context: Context, questId: Long, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, QuestAlarmReceiver::class.java).apply {
            putExtra("quest_id", questId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            questId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    fun cancelQuestAlarm(context: Context, questId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, QuestAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            questId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }
}
