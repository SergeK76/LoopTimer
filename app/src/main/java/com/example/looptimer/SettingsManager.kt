package com.example.looptimer

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns

enum class DisplayMode {
    CLASSIC,
    ORBIT_3D
}

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("loop_timer_prefs", Context.MODE_PRIVATE)
    private val appContext = context.applicationContext

    var hours: Int
        get() = prefs.getInt("hours", 0)
        set(value) = prefs.edit().putInt("hours", value.coerceIn(0, 23)).apply()

    var minutes: Int
        get() = prefs.getInt("minutes", 1)
        set(value) = prefs.edit().putInt("minutes", value.coerceIn(0, 59)).apply()

    var seconds: Int
        get() = prefs.getInt("seconds", 0)
        set(value) = prefs.edit().putInt("seconds", value.coerceIn(0, 59)).apply()

    var language: AppLanguage
        get() = AppLanguage.fromCode(prefs.getString("language", AppLanguage.RU.code))
        set(value) = prefs.edit().putString("language", value.code).apply()

    var vibrateEnabled: Boolean
        get() = prefs.getBoolean("vibrate_enabled", true)
        set(value) = prefs.edit().putBoolean("vibrate_enabled", value).apply()

    var flashEnabled: Boolean
        get() = prefs.getBoolean("flash_enabled", false)
        set(value) = prefs.edit().putBoolean("flash_enabled", value).apply()

    var keepScreenOn: Boolean
        get() = prefs.getBoolean("keep_screen_on", false)
        set(value) = prefs.edit().putBoolean("keep_screen_on", value).apply()

    var wakeOnAlert: Boolean
        get() = prefs.getBoolean("wake_on_alert", true)
        set(value) = prefs.edit().putBoolean("wake_on_alert", value).apply()

    var isDarkTheme: Boolean
        get() = prefs.getBoolean("is_dark_theme", true)
        set(value) = prefs.edit().putBoolean("is_dark_theme", value).apply()

    var displayMode: DisplayMode
        get() = try {
            DisplayMode.valueOf(prefs.getString("display_mode", DisplayMode.ORBIT_3D.name) ?: DisplayMode.ORBIT_3D.name)
        } catch (_: Exception) {
            DisplayMode.ORBIT_3D
        }
        set(value) = prefs.edit().putString("display_mode", value.name).apply()

    var ringtoneUri: String?
        get() = if (prefs.contains("ringtone_uri")) {
            prefs.getString("ringtone_uri", null)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString()
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)?.toString()
        }
        set(value) = prefs.edit().putString("ringtone_uri", value).apply()

    var customRingtoneTitle: String?
        get() = prefs.getString("custom_ringtone_title", null)
        set(value) = prefs.edit().putString("custom_ringtone_title", value).apply()

    fun getStrings(): AppStrings = stringsFor(language)

    fun getRingtoneTitle(context: Context = appContext, strings: AppStrings = getStrings()): String {
        val uriStr = ringtoneUri ?: return strings.noSound
        if (!customRingtoneTitle.isNullOrBlank()) {
            return customRingtoneTitle!!
        }
        return try {
            val uri = Uri.parse(uriStr)
            val rt = RingtoneManager.getRingtone(context, uri)
            val title = rt?.getTitle(context)
            if (!title.isNullOrBlank()) {
                title
            } else {
                resolveDisplayName(context, uri) ?: strings.customAudioFile
            }
        } catch (_: Exception) {
            strings.customAudioFile
        }
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            }
        } catch (_: Exception) {
            null
        }
    }
}

fun formatDuration(millis: Long): String {
    val totalSec = (millis / 1000L).coerceAtLeast(0L)
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return if (h > 0L) {
        String.format("%02d:%02d:%02d", h, m, s)
    } else {
        String.format("%02d:%02d", m, s)
    }
}
