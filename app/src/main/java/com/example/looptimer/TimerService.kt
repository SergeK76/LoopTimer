package com.example.looptimer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TimerUiState(
    val isRunning: Boolean = false,
    val isAlerting: Boolean = false,
    val remainingMillis: Long = 0L,
    val totalIntervalMillis: Long = 0L,
    val lapCount: Int = 1,
    val totalElapsedMillis: Long = 0L
)

class TimerService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_MUTE = "ACTION_MUTE"
        const val EXTRA_INTERVAL_MS = "EXTRA_INTERVAL_MS"
        private const val CHANNEL_ID = "loop_timer_channel"
        private const val NOTIFICATION_ID = 101

        private val _state = MutableStateFlow(TimerUiState())
        val state = _state.asStateFlow()
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null
    private var alertJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var mediaPlayer: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val intervalMs = intent.getLongExtra(EXTRA_INTERVAL_MS, 60_000L)
                if (intervalMs > 0) {
                    startLoop(intervalMs)
                }
            }
            ACTION_MUTE -> {
                muteAlert()
            }
            ACTION_STOP -> {
                stopLoop()
            }
        }
        return START_NOT_STICKY
    }

    private fun startLoop(intervalMs: Long) {
        timerJob?.cancel()
        alertJob?.cancel()
        stopAlerts()
        acquireWakeLock()

        val startTime = SystemClock.elapsedRealtime()
        var currentLap = 1
        var lapStartTime = startTime

        startForeground(NOTIFICATION_ID, buildNotification(intervalMs, currentLap, isAlerting = false))

        timerJob = serviceScope.launch {
            var lastNotificationSecond = -1L

            while (isActive) {
                val now = SystemClock.elapsedRealtime()
                val elapsedInLap = now - lapStartTime
                val remaining = intervalMs - elapsedInLap

                if (remaining <= 0L) {
                    val overshoot = -remaining
                    lapStartTime = now - (overshoot % intervalMs)
                    currentLap++
                    startLoopingAlert()
                }

                val updatedRemaining = (intervalMs - (SystemClock.elapsedRealtime() - lapStartTime)).coerceAtLeast(0L)
                val totalElapsed = SystemClock.elapsedRealtime() - startTime
                val alertingNow = _state.value.isAlerting

                _state.value = TimerUiState(
                    isRunning = true,
                    isAlerting = alertingNow,
                    remainingMillis = updatedRemaining,
                    totalIntervalMillis = intervalMs,
                    lapCount = currentLap,
                    totalElapsedMillis = totalElapsed
                )

                val currentSec = updatedRemaining / 1000L
                if (currentSec != lastNotificationSecond) {
                    lastNotificationSecond = currentSec
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(
                        NOTIFICATION_ID,
                        buildNotification(updatedRemaining, currentLap, alertingNow)
                    )
                }

                delay(100L)
            }
        }
    }

    private fun muteAlert() {
        alertJob?.cancel()
        stopAlerts()
        val current = _state.value
        _state.value = current.copy(isAlerting = false)
        if (current.isRunning) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(
                NOTIFICATION_ID,
                buildNotification(current.remainingMillis, current.lapCount, isAlerting = false)
            )
        }
    }

    private fun stopLoop() {
        timerJob?.cancel()
        alertJob?.cancel()
        stopAlerts()
        releaseWakeLock()
        _state.value = TimerUiState(isRunning = false, isAlerting = false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startLoopingAlert() {
        val settings = SettingsManager(this)
        alertJob?.cancel()
        stopAlerts()

        val currentState = _state.value
        _state.value = currentState.copy(isAlerting = true)

        alertJob = serviceScope.launch {
            settings.ringtoneUri?.let { uriStr ->
                startLoopingSound(Uri.parse(uriStr))
            }

            if (settings.vibrateEnabled) {
                launch {
                    loopVibration()
                }
            }

            if (settings.flashEnabled) {
                launch {
                    loopFlash()
                }
            }
        }
    }

    private fun startLoopingSound(uri: Uri) {
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(applicationContext, uri)
                isLooping = true
                prepare()
                start()
            }
            mediaPlayer = player
        } catch (_: Exception) {
            stopSound()
        }
    }

    private suspend fun loopVibration() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            val effect = VibrationEffect.createOneShot(350L, VibrationEffect.DEFAULT_AMPLITUDE)
            while (currentCoroutineContext().isActive && _state.value.isAlerting) {
                vibrator.vibrate(effect)
                delay(1200L)
            }
        } catch (_: Exception) { }
    }

    private suspend fun loopFlash() {
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
            while (currentCoroutineContext().isActive && _state.value.isAlerting) {
                cameraManager.setTorchMode(cameraId, true)
                delay(180L)
                cameraManager.setTorchMode(cameraId, false)
                delay(1020L)
            }
        } catch (_: Exception) { } finally {
            try {
                val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val cameraId = cameraManager.cameraIdList.firstOrNull()
                if (cameraId != null) {
                    cameraManager.setTorchMode(cameraId, false)
                }
            } catch (_: Exception) { }
        }
    }

    private fun stopSound() {
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.release()
            }
        } catch (_: Exception) { } finally {
            mediaPlayer = null
        }
    }

    private fun stopAlerts() {
        stopSound()
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull()
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, false)
            }
        } catch (_: Exception) { }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LoopTimer::WakeLock")
        }
        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire(12 * 60 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) { }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Активный циклический таймер",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(remainingMs: Long, lap: Int, isAlerting: Boolean): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val muteIntent = PendingIntent.getService(
            this, 2,
            Intent(this, TimerService::class.java).apply { action = ACTION_MUTE },
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, TimerService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )

        val formatted = formatDuration(remainingMs)
        val title = if (isAlerting) {
            "СИГНАЛ • Круг #$lap • Осталось: $formatted"
        } else {
            "Круг #$lap • Осталось: $formatted"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Циклический таймер работает")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(openIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)

        if (isAlerting) {
            builder.addAction(android.R.drawable.ic_lock_silent_mode_off, "ОК / Мьют", muteIntent)
        }
        builder.addAction(android.R.drawable.ic_media_pause, "Стоп", stopIntent)

        return builder.build()
    }

    override fun onDestroy() {
        stopLoop()
        serviceScope.cancel()
        super.onDestroy()
    }
}

fun formatDuration(millis: Long): String {
    val totalSeconds = (millis + 999) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
