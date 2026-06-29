package com.questdday.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.questdday.data.local.AppDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class QuestAlarmReceiver : BroadcastReceiver() {

    companion object {
        var dispatcher: CoroutineDispatcher = Dispatchers.IO
    }

    override fun onReceive(context: Context, intent: Intent) {
        val questId = intent.getLongExtra("quest_id", -1L)
        if (questId == -1L) return

        val pendingResult = try {
            goAsync()
        } catch (t: Throwable) {
            null
        }

        CoroutineScope(dispatcher).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val activeTimerDao = db.activeTimerDao()
                val questDao = db.questDao()

                // 1. Update active_timers SET alarm_fired_at = now
                val nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                activeTimerDao.markAlarmFired(questId, nowStr)

                // Get quest title for notification
                val quest = questDao.getQuestById(questId)
                val questTitle = quest?.title ?: "Quest"

                // 2. Tampilkan notifikasi sistem Android (bunyi + getar)
                showAlarmNotification(context, questId, questTitle)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult?.finish()
            }
        }
    }

    private fun showAlarmNotification(context: Context, questId: Long, questTitle: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "quest_alerts_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Quest Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a quest timer finishes"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = if (launchIntent != null) {
            PendingIntent.getActivity(
                context,
                questId.toInt(),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            null
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Quest Selesai!")
            .setContentText("Quest '$questTitle' selesai! Tap untuk konfirmasi")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(questId.toInt(), notification)
    }
}
