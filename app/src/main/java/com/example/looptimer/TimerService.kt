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
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
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

    private fun startSession(durationMs: Long) {
        stopAlerts()
        tickerJob?.cancel()

        val now = android.os.SystemClock.elapsedRealtime()
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

        val now = android.os.SystemClock.elapsedRealtime()
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
                val now = android.os.SystemClock.elapsedRealtime()
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
                val attrs = VibrationAttributes
