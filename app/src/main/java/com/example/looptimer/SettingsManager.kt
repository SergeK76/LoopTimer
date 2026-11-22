package com.example.looptimer

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri

enum class DisplayMode(val key: String) {
    CLASSIC("classic"),
    ORBIT_3D("orbit3d");

    companion object {
        fun fromKey(key: String?): DisplayMode =
            entries.firstOrNull { it.key == key } ?: ORBIT_3D
    }
}

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

    var wakeOnAlert: Boolean
        get() = prefs.getBoolean("wake_on_alert", true)
        set(value) = prefs.edit().putBoolean("wake_on_alert", value).apply()

    var isDarkTheme: Boolean
        get() = prefs.getBoolean("dark_theme", true)
        set(value) = prefs.edit().putBoolean("dark_theme", value).apply()

    var displayMode: DisplayMode
        get() = DisplayMode.fromKey(prefs.getString("display_mode", DisplayMode.ORBIT_3D.key))
        set(value) = prefs.edit().putString("display_mode", value.key).apply()

    var ringtoneUri: String?
        get() = prefs.getString(
            "ringtone_uri",
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString()
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)?.toString()
        )
        set(value) = prefs.edit().putString("ringtone_uri", value).apply()

    var customRingtoneTitle: String?
        get() = prefs.getString("ringtone_title", null)
        set(value) = prefs.edit().putString("ringtone_title", value).apply()

    fun getRingtoneTitle(context: Context): String {
        val savedTitle = customRingtoneTitle
        if (!savedTitle.isNullOrBlank()) return savedTitle

        val uriString = ringtoneUri ?: return "Без звука"
        return try {
            val ringtone = RingtoneManager.getRingtone(context, Uri.parse(uriString))
            ringtone?.getTitle(context) ?: "Системный сигнал"
        } catch (_: Exception) {
            "Системный сигнал"
        }
    }
}
