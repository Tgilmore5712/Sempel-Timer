package com.sempel.timer.audio

import android.content.Context
import android.net.Uri

object AlarmPreferences {
    private const val PREFS_NAME = "alarm_prefs"
    private const val KEY_RINGTONE_URI = "ringtone_uri"

    fun getSavedUri(context: Context): Uri? {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_RINGTONE_URI, null) ?: return null
        return Uri.parse(raw)
    }

    fun saveUri(context: Context, uri: Uri?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_RINGTONE_URI, uri?.toString())
            .apply()
    }
}
