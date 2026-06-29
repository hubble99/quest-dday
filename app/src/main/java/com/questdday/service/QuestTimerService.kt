package com.questdday.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.questdday.data.local.AppDatabase
import com.questdday.data.local.entity.ActiveTimerEntity
import com.questdday.receiver.QuestAlarmReceiver
import com.questdday.util.AlarmScheduler
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class QuestTimerService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var countdownJob: Job? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "quest_timer_channel"
        private const val CHANNEL_NAME = "Active Quest Timers"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Foreground notification WAJIB ditampilkan segera di onStartCommand sebelum operasi lain
        val initialNotification = buildNotification("Timer is starting...")
        startForeground(NOTIFICATION_ID, initialNotification)

        startCountdownLoop()

        return START_STICKY // WAJIB
    }

    private fun startCountdownLoop() {
        countdownJob?.cancel()
        countdownJob = serviceScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val activeTimerDao = db.activeTimerDao()
            val questDao = db.questDao()

            while (isActive) {
                // Selalu baca state dari active_timers di DB — jangan andalkan in-memory state
                val runningTimers = activeTimerDao.getRunningTimersOnce()
                if (runningTimers.isEmpty()) {
                    stopSelf()
                    break
                }

                var mostUrgentTimer: ActiveTimerEntity? = null
                var minRemainingSeconds = Long.MAX_VALUE
                var mostUrgentQuestTitle = "Quest"

                val nowEpoch = System.currentTimeMillis()

                for (timer in runningTimers) {
                    val startedAtMillis = parseDateTimeToMillis(timer.startedAt)
                    val targetDurationMillis = timer.targetDurationSeconds * 1000L
                    val triggerAtMillis = startedAtMillis + targetDurationMillis
                    val remainingSeconds = (triggerAtMillis - nowEpoch) / 1000L

                    if (remainingSeconds <= 0) {
                        // Jika remaining <= 0 saat service restart: langsung fire alarm, jangan tunggu
                        fireAlarm(timer.questId)
                    } else {
                        // AlarmManager WAJIB pakai setExactAndAllowWhileIdle() via AlarmScheduler
                        AlarmScheduler.scheduleQuestAlarm(applicationContext, timer.questId, triggerAtMillis)

                        if (remainingSeconds < minRemainingSeconds) {
                            minRemainingSeconds = remainingSeconds
                            mostUrgentTimer = timer
                        }
                    }
                }

                // Check again in case all timers expired and were fired in this loop
                val stillRunning = activeTimerDao.getRunningTimersOnce()
                if (stillRunning.isEmpty()) {
                    stopSelf()
                    break
                }

                if (mostUrgentTimer != null) {
                    val quest = questDao.getQuestById(mostUrgentTimer.questId)
                    if (quest != null) {
                        mostUrgentQuestTitle = quest.title
                    }
                    val formattedTime = formatTime(minRemainingSeconds)
                    val notificationText = if (stillRunning.size > 1) {
                        "$mostUrgentQuestTitle: $formattedTime (+${stillRunning.size - 1} more)"
                    } else {
                        "$mostUrgentQuestTitle: $formattedTime remaining"
                    }
                    updateNotification(notificationText)
                }

                delay(1000L)
            }
        }
    }

    private fun fireAlarm(questId: Long) {
        val alarmIntent = Intent(applicationContext, QuestAlarmReceiver::class.java).apply {
            putExtra("quest_id", questId)
        }
        sendBroadcast(alarmIntent)
    }

    private fun parseDateTimeToMillis(dateTimeStr: String): Long {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val localDateTime = LocalDateTime.parse(dateTimeStr, formatter)
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun formatTime(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", mins, secs)
    }

    private fun buildNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Quest Timer Running")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notification = buildNotification(contentText)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of active quest timers"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
