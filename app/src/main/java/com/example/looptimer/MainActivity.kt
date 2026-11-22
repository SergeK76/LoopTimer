package com.example.looptimer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.OpenableColumns
import android.view.WindowManager
import android.widget.NumberPicker
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

data class SoundOption(
    val title: String,
    val uriString: String?
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = SettingsManager(this)

        setContent {
            var isDarkTheme by remember { mutableStateOf(settings.isDarkTheme) }
            var keepScreenOn by remember { mutableStateOf(settings.keepScreenOn) }
            var wakeOnAlert by remember { mutableStateOf(settings.wakeOnAlert) }
            var currentLanguage by remember { mutableStateOf(settings.language) }

            LaunchedEffect(keepScreenOn) {
                if (keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            val colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()

            ProvideAppStrings(language = currentLanguage) {
                MaterialTheme(colorScheme = colorScheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        LoopTimerScreen(
                            settings = settings,
                            currentLanguage = currentLanguage,
                            onLanguageChange = { lang ->
                                currentLanguage = lang
                                settings.language = lang
                            },
                            isDarkTheme = isDarkTheme,
                            onThemeToggle = {
                                isDarkTheme = it
                                settings.isDarkTheme = it
                            },
                            keepScreenOn = keepScreenOn,
                            onKeepScreenOnToggle = {
                                keepScreenOn = it
                                settings.keepScreenOn = it
                            },
                            wakeOnAlert = wakeOnAlert,
                            onWakeOnAlertToggle = {
                                wakeOnAlert = it
                                settings.wakeOnAlert = it
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoopTimerScreen(
    settings: SettingsManager,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    isDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    keepScreenOn: Boolean,
    onKeepScreenOnToggle: (Boolean) -> Unit,
    wakeOnAlert: Boolean,
    onWakeOnAlertToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = LocalAppStrings.current
    val timerState by TimerService.state.collectAsState()

    var hours by remember { mutableIntStateOf(settings.hours) }
    var minutes by remember { mutableIntStateOf(settings.minutes) }
    var seconds by remember { mutableIntStateOf(settings.seconds) }

    var vibrateEnabled by remember { mutableStateOf(settings.vibrateEnabled) }
    var flashEnabled by remember { mutableStateOf(settings.flashEnabled) }
    var displayMode by remember { mutableStateOf(settings.displayMode) }
    var ringtoneTitle by remember { mutableStateOf(settings.getRingtoneTitle(context, strings)) }
    var showSoundSheet by remember { mutableStateOf(false) }
    var showStopDialog by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }

    LaunchedEffect(currentLanguage, settings.ringtoneUri, settings.customRingtoneTitle) {
        ringtoneTitle = settings.getRingtoneTitle(context, strings)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        }
    }

    fun triggerTestBlink() {
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                    val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return@withContext
                    cameraManager.setTorchMode(cameraId, true)
                    delay(140L)
                    cameraManager.setTorchMode(cameraId, false)
                } catch (_: Exception) {
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        flashEnabled = granted
        settings.flashEnabled = granted
        if (granted) {
            triggerTestBlink()
        }
    }

    fun applyTime(h: Int, m: Int, s: Int) {
        hours = h
        minutes = m
        seconds = s
        settings.hours = h
        settings.minutes = m
        settings.seconds = s
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LoopTimer", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { onThemeToggle(!isDarkTheme) }) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = strings.changeTheme
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            if (!timerState.isRunning) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetChip(label = "15m", modifier = Modifier.weight(1f)) { applyTime(0, 15, 0) }
                    PresetChip(label = "30m", modifier = Modifier.weight(1f)) { applyTime(0, 30, 0) }
                    PresetChip(label = "45m", modifier = Modifier.weight(1f)) { applyTime(0, 45, 0) }
                    PresetChip(label = "1h", modifier = Modifier.weight(1f)) { applyTime(1, 0, 0) }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (timerState.isRunning) {
                        val progress = if (timerState.totalIntervalMillis > 0L) {
                            (timerState.remainingMillis.toFloat() / timerState.totalIntervalMillis.toFloat())
                                .coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        val lapLabel = if (timerState.isWaitingOk) {
                            "${strings.waitingOkPrefix}${timerState.lapCount}"
                        } else {
                            "${strings.lapPrefix}${timerState.lapCount}"
                        }

                        if (displayMode == DisplayMode.ORBIT_3D) {
                            Orbit3DCountdown(
                                remainingText = formatDuration(timerState.remainingMillis),
                                lapText = lapLabel,
                                progress = progress,
                                isWaitingOk = timerState.isWaitingOk
                            )
                        } else {
                            Text(
                                text = lapLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (timerState.isWaitingOk)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = formatDuration(timerState.remainingMillis),
                                fontSize = 56.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "${strings.totalElapsed}:${formatDuration(timerState.totalElapsedMillis)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WheelPickerColumn(
                                label = strings.hours,
                                value = hours,
                                range = 0..23,
                                textColor = MaterialTheme.colorScheme.onSurface.toArgb(),
                                onValueChange = { newVal ->
                                    hours = newVal
                                    settings.hours = newVal
                                }
                            )
                            Text(":", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            WheelPickerColumn(
                                label = strings.minutes,
                                value = minutes,
                                range = 0..59,
                                textColor = MaterialTheme.colorScheme.onSurface.toArgb(),
                                onValueChange = { newVal ->
                                    minutes = newVal
                                    settings.minutes = newVal
                                }
                            )
                            Text(":", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            WheelPickerColumn(
                                label = strings.seconds,
                                value = seconds,
                                range = 0..59,
                                textColor = MaterialTheme.colorScheme.onSurface.toArgb(),
                                onValueChange = { newVal ->
                                    seconds = newVal
                                    settings.seconds = newVal
                                }
                            )
                        }
                    }
                }
            }

            val totalMillis = ((hours * 3600L) + (minutes * 60L) + seconds) * 1000L

            if (timerState.isRunning && timerState.isWaitingOk) {
                Button(
                    onClick = {
                        val okIntent = Intent(context, TimerService::class.java).apply {
                            action = TimerService.ACTION_OK
                        }
                        context.startService(okIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.okButton,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = { showStopDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.stopButton, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = {
                        if (timerState.isRunning) {
                            showStopDialog = true
                        } else if (totalMillis > 0L) {
                            val startIntent = Intent(context, TimerService::class.java).apply {
                                action = TimerService.ACTION_START
                                putExtra(TimerService.EXTRA_INTERVAL_MS, totalMillis)
                            }
                            ContextCompat.startForegroundService(context, startIntent)
                        }
                    },
                    enabled = timerState.isRunning || totalMillis > 0L,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (timerState.isRunning)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (timerState.isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (timerState.isRunning) strings.stopButton else strings.startButton,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(
                visible = !timerState.isRunning,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Box {
                            ListItem(
                                headlineContent = { Text(strings.languageLabel) },
                                supportingContent = { Text(currentLanguage.nativeName) },
                                leadingContent = {
                                    Icon(Icons.Default.Language, contentDescription = null)
                                },
                                trailingContent = {
                                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                                },
                                modifier = Modifier.clickable {
                                    showLanguageMenu = true
                                }
                            )
                            DropdownMenu(
                                expanded = showLanguageMenu,
                                onDismissRequest = { showLanguageMenu = false }
                            ) {
                                AppLanguage.entries.forEach { lang ->
                                    DropdownMenuItem(
                                        text = { Text(lang.nativeName) },
                                        trailingIcon = {
                                            if (lang == currentLanguage) {
                                                Icon(Icons.Default.Check, contentDescription = null)
                                            }
                                        },
                                        onClick = {
                                            showLanguageMenu = false
                                            onLanguageChange(lang)
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        ListItem(
                            headlineContent = { Text(strings.animationTitle) },
                            supportingContent = {
                                Text(
                                    if (displayMode == DisplayMode.ORBIT_3D)
                                        strings.animationOrbit3d
                                    else
                                        strings.animationClassic
                                )
                            },
                            leadingContent = {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                            },
                            trailingContent = {
                                Switch(
                                    checked = displayMode == DisplayMode.ORBIT_3D,
                                    onCheckedChange = { is3d ->
                                        val mode = if (is3d) DisplayMode.ORBIT_3D else DisplayMode.CLASSIC
                                        displayMode = mode
                                        settings.displayMode = mode
                                    }
                                )
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        ListItem(
                            headlineContent = { Text(strings.soundTitle) },
                            supportingContent = { Text(ringtoneTitle) },
                            leadingContent = {
                                Icon(Icons.Default.Notifications, contentDescription = null)
                            },
                            trailingContent = {
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            },
                            modifier = Modifier.clickable {
                                showSoundSheet = true
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        ListItem(
                            headlineContent = { Text(strings.vibrateTitle) },
                            supportingContent = { Text(strings.vibrateSubtitle) },
                            leadingContent = {
                                Icon(Icons.Default.Settings, contentDescription = null)
                            },
                            trailingContent = {
                                Switch(
                                    checked = vibrateEnabled,
                                    onCheckedChange = { enabled ->
                                        vibrateEnabled = enabled
                                        settings.vibrateEnabled = enabled
                                        if (enabled) {
                                            vibrateTestPulse(context)
                                        }
                                    }
                                )
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        ListItem(
                            headlineContent = { Text(strings.flashTitle) },
                            supportingContent = { Text(strings.flashSubtitle) },
                            leadingContent = {
                                Icon(Icons.Default.FlashlightOn, contentDescription = null)
                            },
                            trailingContent = {
                                Switch(
                                    checked = flashEnabled,
                                    onCheckedChange = { enabled ->
                                        if (enabled) {
                                            if (ContextCompat.checkSelfPermission(
                                                    context, Manifest.permission.CAMERA
                                                ) != PackageManager.PERMISSION_GRANTED
                                            ) {
                                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            } else {
                                                flashEnabled = true
                                                settings.flashEnabled = true
                                                triggerTestBlink()
                                            }
                                        } else {
                                            flashEnabled = false
                                            settings.flashEnabled = false
                                        }
                                    }
                                )
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        ListItem(
                            headlineContent = { Text(strings.wakeScreenTitle) },
                            supportingContent = { Text(strings.wakeScreenSubtitle) },
                            leadingContent = {
                                Icon(Icons.Default.Notifications, contentDescription = null)
                            },
                            trailingContent = {
                                Switch(
                                    checked = wakeOnAlert,
                                    onCheckedChange = onWakeOnAlertToggle
                                )
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        ListItem(
                            headlineContent = { Text(strings.keepScreenOnTitle) },
                            supportingContent = { Text(strings.keepScreenOnSubtitle) },
                            leadingContent = {
                                Icon(Icons.Default.Settings, contentDescription = null)
                            },
                            trailingContent = {
                                Switch(
                                    checked = keepScreenOn,
                                    onCheckedChange = onKeepScreenOnToggle
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text(strings.stopDialogTitle) },
            text = { Text(strings.stopDialogBody) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStopDialog = false
                        val stopIntent = Intent(context, TimerService::class.java).apply {
                            action = TimerService.ACTION_STOP
                        }
                        context.startService(stopIntent)
                    }
                ) {
                    Text(strings.stopButton, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showSoundSheet) {
        SoundBrowserSheet(
            settings = settings,
            currentTitle = ringtoneTitle,
            onDismiss = { showSoundSheet = false },
            onSelected = { title, uriStr ->
                settings.ringtoneUri = uriStr
                settings.customRingtoneTitle = title
                ringtoneTitle = title
                showSoundSheet = false
            }
        )
    }
}

@Composable
fun Orbit3DCountdown(
    remainingText: String,
    lapText: String,
    progress: Float,
    isWaitingOk: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbit3d")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isWaitingOk) 450 else 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
