package com.example.looptimer

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("loop_timer_prefs", Context.MODE_PRIVATE)

    var hours: Int
        get() = prefs.getInt("hours", 0)
        set(value) = prefs.edit().putInt("hours", value).apply()

    var minutes: Int
        get() = prefs.getInt("minutes", 30)
        set(value) = prefs.edit().putInt("minutes", value).apply()

    var seconds: Int
        get() = prefs.getInt("seconds", 0)
        set(value) = prefs.edit().putInt("seconds", value).apply()

    var vibrateEnabled: Boolean
        get() = prefs.getBoolean("vibrate", true)
        set(value) = prefs.edit().putBoolean("vibrate", value).apply()

    var flashEnabled: Boolean
        get() = prefs.getBoolean("flash", false)
        set(value) = prefs.edit().putBoolean("flash", value).apply()

    var keepScreenOn: Boolean
        get() = prefs.getBoolean("keep_screen_on", false)
        set(value) = prefs.edit().putBoolean("keep_screen_on", value).apply()

    var isDarkTheme: Boolean
        get() = prefs.getBoolean("dark_theme", true)
        set(value) = prefs.edit().putBoolean("dark_theme", value).apply()

    var ringtoneUri: String?
        get() = prefs.getString(
            "ringtone_uri",
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)?.toString()
        )
        set(value) = prefs.edit().putString("ringtone_uri", value).apply()

    fun getRingtoneTitle(context: Context): String {
        val uriString = ringtoneUri ?: return "Без звука"
        return try {
            val ringtone = RingtoneManager.getRingtone(context, Uri.parse(uriString))
            ringtone?.getTitle(context) ?: "Системный сигнал"
        } catch (e: Exception) {
            "Системный сигнал"
        }
    }
}
