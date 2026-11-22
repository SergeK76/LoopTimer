package com.example.looptimer

import android.app.ActivityManager
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
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TimerUiState(
    val isRunning: Boolean = false,
    val isWaitingOk: Boolean = false,
    val remainingMillis: Long = 0L,
    val totalIntervalMillis: Long = 0L,
    val lapCount: Int = 1,
    val totalElapsedMillis: Long = 0L
)

class TimerService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_OK = "ACTION_OK"
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

    private var currentIntervalMs: Long = 60_000L
    private var loopStartTime: Long = 0L
    private var lapStartTime: Long = 0L
    private var currentLap: Int = 1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val intervalMs = intent.getLongExtra(EXTRA_INTERVAL_MS, 60_000L)
                if (intervalMs > 0L) {
                    startTimer(intervalMs)
                }
            }
            ACTION_OK -> {
                confirmOkAndStartNextLap()
            }
            ACTION_STOP -> {
                stopTimer()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTimer(intervalMs: Long) {
        timerJob?.cancel()
        alertJob?.cancel()
        stopAlerts()
        acquireWakeLock()

        currentIntervalMs = intervalMs
        val now = SystemClock.elapsedRealtime()
        loopStartTime = now
        lapStartTime = now
        currentLap = 1

        _state.value = TimerUiState(
            isRunning = true,
            isWaitingOk = false,
            remainingMillis = intervalMs,
            totalIntervalMillis = intervalMs,
            lapCount = currentLap,
            totalElapsedMillis = 0L
        )

        startForeground(NOTIFICATION_ID, buildNotification(intervalMs, currentLap, isWaitingOk = false))
        launchCountdownLoop()
    }

    private fun launchCountdownLoop() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            var lastNotificationSecond = -1L

            while (isActive) {
                val now = SystemClock.elapsedRealtime()
                val totalElapsed = now - loopStartTime

                if (_state.value.isWaitingOk) {
                    _state.value = _state.value.copy(
                        isRunning = true,
                        isWaitingOk = true,
                        remainingMillis = 0L,
                        totalElapsedMillis = totalElapsed
                    )
                    if (lastNotificationSecond != -2L) {
                        lastNotificationSecond = -2L
                        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        manager.notify(
                            NOTIFICATION_ID,
                            buildNotification(0L, currentLap, isWaitingOk = true)
                        )
                    }
                    delay(150L)
                    continue
                }

                val remaining = (currentIntervalMs - (now - lapStartTime)).coerceAtLeast(0L)
                _state.value = _state.value.copy(
                    isRunning = true,
                    isWaitingOk = false,
                    remainingMillis = remaining,
                    totalIntervalMillis = currentIntervalMs,
                    lapCount = currentLap,
                    totalElapsedMillis = totalElapsed
                )

                val currentSec = (remaining + 999L) / 1000L
                if (currentSec != lastNotificationSecond) {
                    lastNotificationSecond = currentSec
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(
                        NOTIFICATION_ID,
                        buildNotification(remaining, currentLap, isWaitingOk = false)
                    )
                }

                if (remaining <= 0L) {
                    enterWaitingOkState()
                    break
                }

                delay(100L)
            }
        }
    }

    private fun enterWaitingOkState() {
        val settings = SettingsManager(this)
        _state.value = _state.value.copy(
            isRunning = true,
            isWaitingOk = true,
            remainingMillis = 0L,
            totalElapsedMillis = SystemClock.elapsedRealtime() - loopStartTime
        )

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(0L, currentLap, isWaitingOk = true))

        if (settings.wakeOnAlert) {
            wakeScreenAndOpenApp()
        }

        startLoopingAlert(settings)
    }

    private fun confirmOkAndStartNextLap() {
        alertJob?.cancel()
        stopAlerts()

        if (!_state.value.isRunning) return

        currentLap++
        val now = SystemClock.elapsedRealtime()
        lapStartTime = now

        _state.value = TimerUiState(
            isRunning = true,
            isWaitingOk = false,
            remainingMillis = currentIntervalMs,
            totalIntervalMillis = currentIntervalMs,
            lapCount = currentLap,
            totalElapsedMillis = now - loopStartTime
        )

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(
            NOTIFICATION_ID,
            buildNotification(currentIntervalMs, currentLap, isWaitingOk = false)
        )
        launchCountdownLoop()
    }

    private fun stopTimer() {
        timerJob?.cancel()
        alertJob?.cancel()
        stopAlerts()
        releaseWakeLock()
        _state.value = TimerUiState(isRunning = false, isWaitingOk = false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun wakeScreenAndOpenApp() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            val screenWake = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "LoopTimer::ScreenWake"
            )
            screenWake.acquire(5000L)
        } catch (_: Exception) {
        }

        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val isForeground = am.runningAppProcesses?.any {
                it.processName == packageName &&
                    it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
            } == true

            if (!isForeground) {
                val intent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(intent)
            }
        } catch (_: Exception) {
        }
    }

    private fun startLoopingAlert(settings: SettingsManager) {
        alertJob?.cancel()
        stopAlerts()

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
            while (currentCoroutineContext().isActive && _state.value.isWaitingOk) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val attrs = VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_ALARM)
                        .build()
                    vibrator.vibrate(effect, attrs)
                } else {
                    @Suppress("DEPRECATION")
                    val audioAttrs = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(effect, audioAttrs)
                }
                delay(1100L)
            }
        } catch (_: Exception) {
        }
    }

    private suspend fun loopFlash() {
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
            while (currentCoroutineContext().isActive && _state.value.isWaitingOk) {
                cameraManager.setTorchMode(cameraId, true)
                delay(180L)
                cameraManager.setTorchMode(cameraId, false)
                delay(920L)
            }
        } catch (_: Exception) {
        } finally {
            turnOffFlashSafely()
        }
    }

    private fun turnOffFlashSafely() {
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull()
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, false)
            }
        } catch (_: Exception) {
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
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
        }
    }

    private fun stopAlerts() {
        stopSound()
        turnOffFlashSafely()
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
        } catch (_: Exception) {
        }
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

    private fun buildNotification(remainingMs: Long, lap: Int, isWaitingOk: Boolean): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val okIntent = PendingIntent.getService(
            this, 2,
            Intent(this, TimerService::class.java).apply { action = ACTION_OK },
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, TimerService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )

        val formatted = formatDuration(remainingMs)
        val title = if (isWaitingOk) {
            "ВРЕМЯ ВЫШЛО • Круг #$lap • Нажми ОК"
        } else {
            "Круг #$lap • Осталось: $formatted"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(
                if (isWaitingOk) "Ожидание нажатия ОК для запуска следующего круга"
                else "Циклический таймер работает"
            )
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(openIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)

        if (isWaitingOk) {
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "ОК", okIntent)
        }
        builder.addAction(android.R.drawable.ic_media_pause, "Стоп", stopIntent)

        return builder.build()
    }

    override fun onDestroy() {
        stopTimer()
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
