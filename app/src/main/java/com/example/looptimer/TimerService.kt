package com.example.looptimer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TimerUiState(
    val isRunning: Boolean = false,
    val isWaitingOk: Boolean = false,
    val lapCount: Int = 1,
    val totalIntervalMillis: Long = 0L,
    val remainingMillis: Long = 0L,
    val totalElapsedMillis: Long = 0L
)

class TimerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var settings: SettingsManager

    private var tickerJob: Job? = null
    private var alertJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    private var ringtone: Ringtone? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var intervalMillis: Long = 0L
    private var lapStartElapsed: Long = 0L
    private var sessionStartElapsed: Long = 0L
    private var accumulatedPausedMillis: Long = 0L
    private var waitingStartElapsed: Long = 0L
    private var currentLap: Int = 1
    private var isWaitingOk: Boolean = false

    override fun onCreate() {
        super.onCreate()
        settings = SettingsManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val requested = intent.getLongExtra(EXTRA_INTERVAL_MS, 0L)
                val fallback = ((settings.hours * 3600L) + (settings.minutes * 60L) + settings.seconds) * 1000L
                val chosen = if (requested > 0L) requested else fallback
                if (chosen > 0L) {
                    startSession(chosen)
                }
            }
            ACTION_OK -> {
                confirmNextLap()
            }
            ACTION_STOP -> {
                stopSession()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        tickerJob?.cancel()
        stopAlerts()
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startSession(durationMs: Long) {
        stopAlerts()
        tickerJob?.cancel()

        val now = SystemClock.elapsedRealtime()
        intervalMillis = durationMs
        currentLap = 1
        isWaitingOk = false
        lapStartElapsed = now
        sessionStartElapsed = now
        accumulatedPausedMillis = 0L
        waitingStartElapsed = 0L

        acquireWakeLock()
        startForeground(NOTIFICATION_ID, buildNotification(durationMs))

        _state.value = TimerUiState(
            isRunning = true,
            isWaitingOk = false,
            lapCount = currentLap,
            totalIntervalMillis = intervalMillis,
            remainingMillis = intervalMillis,
            totalElapsedMillis = 0L
        )

        startTicker()
    }

    private fun confirmNextLap() {
        if (!_state.value.isRunning || !isWaitingOk) return

        val now = SystemClock.elapsedRealtime()
        if (waitingStartElapsed > 0L) {
            accumulatedPausedMillis += (now - waitingStartElapsed).coerceAtLeast(0L)
            waitingStartElapsed = 0L
        }

        stopAlerts()
        isWaitingOk = false
        currentLap += 1
        lapStartElapsed = now

        _state.value = _state.value.copy(
            isWaitingOk = false,
            lapCount = currentLap,
            remainingMillis = intervalMillis
        )
        updateNotification(intervalMillis)
    }

    private fun stopSession() {
        tickerJob?.cancel()
        stopAlerts()
        releaseWakeLock()

        isWaitingOk = false
        currentLap = 1
        _state.value = TimerUiState()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive) {
                val now = SystemClock.elapsedRealtime()
                val totalElapsed = (now - sessionStartElapsed - accumulatedPausedMillis).coerceAtLeast(0L)

                if (!isWaitingOk) {
                    val elapsedInLap = (now - lapStartElapsed).coerceAtLeast(0L)
                    val remaining = (intervalMillis - elapsedInLap).coerceAtLeast(0L)

                    _state.value = _state.value.copy(
                        isRunning = true,
                        isWaitingOk = false,
                        lapCount = currentLap,
                        totalIntervalMillis = intervalMillis,
                        remainingMillis = remaining,
                        totalElapsedMillis = totalElapsed
                    )

                    if (remaining <= 0L) {
                        enterWaitingOkState(now, totalElapsed)
                    } else {
                        updateNotification(remaining)
                    }
                } else {
                    _state.value = _state.value.copy(
                        isRunning = true,
                        isWaitingOk = true,
                        lapCount = currentLap,
                        totalIntervalMillis = intervalMillis,
                        remainingMillis = 0L,
                        totalElapsedMillis = totalElapsed
                    )
                }

                delay(200L)
            }
        }
    }

    private fun enterWaitingOkState(nowElapsed: Long, totalElapsed: Long) {
        isWaitingOk = true
        waitingStartElapsed = nowElapsed
        _state.value = _state.value.copy(
            isRunning = true,
            isWaitingOk = true,
            lapCount = currentLap,
            totalIntervalMillis = intervalMillis,
            remainingMillis = 0L,
            totalElapsedMillis = totalElapsed
        )
        updateNotification(0L)
        triggerAlerts()
    }

    private fun triggerAlerts() {
        stopAlerts()

        if (settings.wakeOnAlert) {
            wakeScreenBriefly()
        }

        val uriStr = settings.ringtoneUri
        if (!uriStr.isNullOrBlank()) {
            val uri = Uri.parse(uriStr)
            try {
                val mp = MediaPlayer().apply {
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
                mediaPlayer = mp
            } catch (_: Exception) {
                try {
                    val rt = RingtoneManager.getRingtone(applicationContext, uri)
                    if (rt != null) {
                        rt.audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            rt.isLooping = true
                        }
                        rt.play()
                        ringtone = rt
                    }
                } catch (_: Exception) {
                }
            }
        }

        alertJob = serviceScope.launch {
            while (isActive && isWaitingOk) {
                if (settings.vibrateEnabled) {
                    vibratePulse()
                }
                if (settings.flashEnabled) {
                    blinkTorch()
                }
                delay(1100L)
            }
        }
    }

    private fun stopAlerts() {
        alertJob?.cancel()
        alertJob = null
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) mp.stop()
                mp.release()
            }
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
        }
        try {
            ringtone?.stop()
        } catch (_: Exception) {
        } finally {
            ringtone = null
        }
        setTorchOffSafe()
    }

    private fun vibratePulse() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            val effect = VibrationEffect.createOneShot(450L, VibrationEffect.DEFAULT_AMPLITUDE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val attrs = VibrationAttributes.Builder()
                    .setUsage(VibrationAttributes.USAGE_ALARM)
                    .build()
                vibrator.vibrate(effect, attrs)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(effect)
            }
        } catch (_: Exception) {
        }
    }

    private fun findFlashCameraId(cameraManager: CameraManager): String? {
        return cameraManager.cameraIdList.firstOrNull { id ->
            try {
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } catch (_: Exception) {
                false
            }
        } ?: cameraManager.cameraIdList.firstOrNull()
    }

    private suspend fun blinkTorch() {
        withContext(Dispatchers.IO) {
            try {
                val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val cameraId = findFlashCameraId(cameraManager) ?: return@withContext
                cameraManager.setTorchMode(cameraId, true)
                delay(250L)
                cameraManager.setTorchMode(cameraId, false)
            } catch (_: Exception) {
            }
        }
    }

    private fun setTorchOffSafe() {
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = findFlashCameraId(cameraManager) ?: return
            cameraManager.setTorchMode(cameraId, false)
        } catch (_: Exception) {
        }
    }

    private fun wakeScreenBriefly() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            val wl = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "LoopTimer:AlertWakeLock"
            )
            wl.acquire(3_000L)
        } catch (_: Exception) {
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LoopTimer:ServiceWakeLock").apply {
                acquire(12 * 60 * 60 * 1000L)
            }
        } catch (_: Exception) {
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        } finally {
            wakeLock = null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                Strings.get(settings.language, "channel_name"),
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(remainingMs: Long): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val okIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_OK
        }
        val okPendingIntent = PendingIntent.getService(
            this,
            2,
            okIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val totalSeconds = ((remainingMs + 999L) / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        val formattedTime = if (hours > 0L) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }

        val contentText = if (isWaitingOk) {
            "${Strings.get(settings.language, "waiting_ok")} • #${currentLap}"
        } else {
            "$formattedTime • #${currentLap}"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_3d_fg)
            .setContentTitle(Strings.get(settings.language, "app_name"))
            .setContentText(contentText)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        if (isWaitingOk) {
            builder.addAction(0, Strings.get(settings.language, "ok"), okPendingIntent)
        }
        builder.addAction(0, Strings.get(settings.language, "stop"), stopPendingIntent)

        return builder.build()
    }

    private fun updateNotification(remainingMs: Long) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(remainingMs))
    }

    companion object {
        const val ACTION_START = "com.example.looptimer.action.START"
        const val ACTION_OK = "com.example.looptimer.action.OK"
        const val ACTION_STOP = "com.example.looptimer.action.STOP"
        const val EXTRA_INTERVAL_MS = "com.example.looptimer.extra.INTERVAL_MS"

        const val CHANNEL_ID = "looptimer_channel"
        const val NOTIFICATION_ID = 1001

        private val _state = MutableStateFlow(TimerUiState())
        val state: StateFlow<TimerUiState> = _state.asStateFlow()
    }
}
