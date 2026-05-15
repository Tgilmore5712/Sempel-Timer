package com.pmc.timer.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings

object AlarmSoundPlayer {
    private const val DEFAULT_PLAY_DURATION_MS = 8_000L

    fun resolveAlarmUri(context: Context): Uri {
        return AlarmPreferences.getSavedUri(context)
            ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: Settings.System.DEFAULT_ALARM_ALERT_URI
    }

    fun playOnce(context: Context, durationMs: Long = DEFAULT_PLAY_DURATION_MS) {
        try {
            val ringtone = RingtoneManager.getRingtone(context, resolveAlarmUri(context)) ?: return
            ringtone.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            // Loop so short ringtones repeat for the full duration
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                ringtone.isLooping = true
            }
            ringtone.play()

            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    ringtone.stop()
                } catch (_: Exception) {
                }
            }, durationMs)
        } catch (_: Exception) {
        }
    }
}