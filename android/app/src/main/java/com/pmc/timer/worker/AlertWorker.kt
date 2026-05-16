package com.sempel.timer.worker

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.sempel.timer.audio.AlarmSoundPlayer

class AlertWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val title = inputData.getString("title") ?: "Boil Alert"
        val message = inputData.getString("message") ?: "Time to drop in next item!"
        val itemName = inputData.getString("itemName") ?: ""
        val announcement = if (itemName.isNotBlank()) "Time to add $itemName" else message

        val appInForeground = isAppInForeground(applicationContext)

        // Only show notification and play sound from worker when app is in background.
        // When foregrounded, TimerViewModel handles both UI alert and sound to avoid double-play.
        if (!appInForeground) {
            showNotification(title, message)
            AlarmSoundPlayer.playWithAnnouncement(applicationContext, announcement)
        }

        return Result.success()
    }

    private fun isAppInForeground(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.runningAppProcesses?.any {
            it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                it.processName == context.packageName
        } ?: false
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "boil_timer_alarm_channel"
        val alarmUri = AlarmSoundPlayer.resolveAlarmUri(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Boil Timer Alerts", NotificationManager.IMPORTANCE_HIGH)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            channel.setSound(alarmUri, audioAttributes)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(alarmUri)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
