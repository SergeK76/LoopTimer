package com.example.looptimer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
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
                    val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                        cameraManager.getCameraCharacteristics(id)
                            .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    } ?: return@withContext
                    cameraManager.setTorchMode(cameraId, true)
                    delay(140L)
                    cameraManager.setTorchMode(cameraId, false)
                } catch (_: Exception) {
                }
            }
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
                            text = "${strings.totalElapsed}: ${formatDuration(timerState.totalElapsedMillis)}",
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
                                        flashEnabled = enabled
                                        settings.flashEnabled = enabled
                                        if (enabled) {
                                            triggerTestBlink()
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
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitAngle"
    )

    val primaryColor = if (isWaitingOk) Color(0xFFEF4444) else Color(0xFF38BDF8)
    val secondaryColor = if (isWaitingOk) Color(0xFFF97316) else Color(0xFF34D399)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(284.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(264.dp)) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f + 6f
            val radius = (minOf(w, h) * 0.38f) * (if (isWaitingOk) pulse else 1f)

            drawOval(
                color = Color.Black.copy(alpha = 0.35f),
                topLeft = Offset(cx - radius * 0.85f, cy + radius * 0.72f),
                size = Size(radius * 1.7f, radius * 0.34f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.18f),
                        Color(0xFF0F172A).copy(alpha = 0.85f),
                        Color(0xFF020617).copy(alpha = 0.95f)
                    ),
                    center = Offset(cx - radius * 0.25f, cy - radius * 0.25f),
                    radius = radius * 1.15f
                ),
                radius = radius,
                center = Offset(cx, cy)
            )

            drawArc(
                color = Color.White.copy(alpha = 0.12f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(cx - radius, cy - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = 14f, cap = StrokeCap.Round)
            )

            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(secondaryColor, primaryColor, secondaryColor)
                ),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(cx - radius, cy - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = 14f, cap = StrokeCap.Round)
            )

            rotate(degrees = -18f, pivot = Offset(cx, cy)) {
                drawOval(
                    color = primaryColor.copy(alpha = 0.35f),
                    topLeft = Offset(cx - radius * 1.12f, cy - radius * 0.42f),
                    size = Size(radius * 2.24f, radius * 0.84f),
                    style = Stroke(width = 3f)
                )

                val rad = Math.toRadians(orbitAngle.toDouble())
                val dotX = cx + (radius * 1.12f * cos(rad)).toFloat()
                val dotY = cy + (radius * 0.42f * sin(rad)).toFloat()

                drawCircle(
                    color = Color.White,
                    radius = 7f,
                    center = Offset(dotX, dotY)
                )
                drawCircle(
                    color = primaryColor.copy(alpha = 0.45f),
                    radius = 14f,
                    center = Offset(dotX, dotY)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = lapText,
                style = MaterialTheme.typography.labelSmall,
                color = primaryColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = remainingText,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundBrowserSheet(
    settings: SettingsManager,
    currentTitle: String,
    onDismiss: () -> Unit,
    onSelected: (String, String?) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var previewRingtone by remember { mutableStateOf<Ringtone?>(null) }
    var playingUri by remember { mutableStateOf<String?>(null) }

    val soundOptions = remember(strings) {
        val list = mutableListOf(
            SoundOption(title = strings.noSound, uriString = null)
        )
        val manager = RingtoneManager(context).apply {
            setType(
                RingtoneManager.TYPE_ALARM or
                    RingtoneManager.TYPE_NOTIFICATION or
                    RingtoneManager.TYPE_RINGTONE
            )
        }
        val cursor = try {
            manager.cursor
        } catch (_: Exception) {
            null
        }
        if (cursor != null && cursor.moveToFirst()) {
            val seenUris = mutableSetOf<String>()
            do {
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX) ?: continue
                val uri = manager.getRingtoneUri(cursor.position)?.toString() ?: continue
                if (seenUris.add(uri)) {
                    list.add(SoundOption(title = title, uriString = uri))
                }
            } while (cursor.moveToNext())
        }
        list
    }

    var selectedTitle by remember { mutableStateOf(currentTitle) }
    var selectedUri by remember {
        mutableStateOf(
            if (currentTitle == strings.noSound) null else settings.ringtoneUri
        )
    }

    fun stopPreview() {
        try {
            previewPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.release()
            }
        } catch (_: Exception) {
        } finally {
            previewPlayer = null
        }
        try {
            previewRingtone?.stop()
        } catch (_: Exception) {
        } finally {
            previewRingtone = null
        }
        playingUri = null
    }

    fun playPreview(uriString: String?) {
        val wasSamePlaying = !uriString.isNullOrBlank() && playingUri == uriString
        stopPreview()
        if (uriString.isNullOrBlank() || wasSamePlaying) return

        val uri = Uri.parse(uriString)
        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, uri)
                isLooping = false
                setOnCompletionListener {
                    playingUri = null
                }
                prepare()
                start()
            }
            previewPlayer = mp
            playingUri = uriString
        } catch (_: Exception) {
            stopPreview()
            try {
                val rt = RingtoneManager.getRingtone(context, uri)
                if (rt != null) {
                    rt.audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                    rt.play()
                    previewRingtone = rt
                    playingUri = uriString
                }
            } catch (_: Exception) {
                stopPreview()
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            val fileName = resolveFileName(context, uri) ?: strings.customAudioFile
            val uriStr = uri.toString()
            selectedTitle = fileName
            selectedUri = uriStr
            playPreview(uriStr)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopPreview()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            stopPreview()
            onDismiss()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = strings.soundSheetTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = strings.soundSheetHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = {
                    stopPreview()
                    filePickerLauncher.launch(arrayOf("audio/*"))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.pickFromDevice)
            }

            if (selectedUri != null && soundOptions.none { it.uriString == selectedUri }) {
                val isPlayingCustom = playingUri == selectedUri
                ListItem(
                    headlineContent = { Text(selectedTitle, fontWeight = FontWeight.SemiBold) },
                    supportingContent = {
                        Text(if (isPlayingCustom) strings.playingNow else strings.customAudioFile)
                    },
                    leadingContent = {
                        RadioButton(
                            selected = true,
                            onClick = { playPreview(selectedUri) }
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = { playPreview(selectedUri) }) {
                            Icon(
                                imageVector = if (isPlayingCustom) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = strings.previewAction
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.clickable {
                        playPreview(selectedUri)
                    }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                items(soundOptions) { item ->
                    val isSelected = if (item.uriString == null) {
                        selectedUri == null
                    } else {
                        item.uriString == selectedUri
                    }
                    val isPlayingThis = !item.uriString.isNullOrBlank() && playingUri == item.uriString

                    ListItem(
                        headlineContent = {
                            Text(
                                text = item.title,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        supportingContent = if (isPlayingThis) {
                            { Text(strings.playingNow, color = MaterialTheme.colorScheme.primary) }
                        } else {
                            null
                        },
                        leadingContent = {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedTitle = item.title
                                    selectedUri = item.uriString
                                    playPreview(item.uriString)
                                }
                            )
                        },
                        trailingContent = {
                            if (item.uriString != null) {
                                IconButton(
                                    onClick = {
                                        selectedTitle = item.title
                                        selectedUri = item.uriString
                                        playPreview(item.uriString)
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingThis) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = strings.previewAction,
                                        tint = if (isPlayingThis)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        modifier = Modifier.clickable {
                            selectedTitle = item.title
                            selectedUri = item.uriString
                            playPreview(item.uriString)
                        }
                    )
                    HorizontalDivider()
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        stopPreview()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(strings.cancel)
                }
                Button(
                    onClick = {
                        stopPreview()
                        onSelected(selectedTitle, selectedUri)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.select, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun resolveFileName(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                cursor.getString(nameIndex)
            } else {
                null
            }
        }
    } catch (_: Exception) {
        null
    }
}

private fun vibrateTestPulse(context: Context) {
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val effect = VibrationEffect.createOneShot(220L, VibrationEffect.DEFAULT_AMPLITUDE)
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
    } catch (_: Exception) {
    }
}

@Composable
fun PresetChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(label, fontWeight = FontWeight.SemiBold)
            }
        },
        modifier = modifier
    )
}

@Composable
fun WheelPickerColumn(
    label: String,
    value: Int,
    range: IntRange,
    textColor: Int,
    onValueChange: (Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AndroidView(
            modifier = Modifier
                .width(76.dp)
                .height(140.dp),
            factory = { ctx ->
                NumberPicker(ctx).apply {
                    minValue = range.first
                    maxValue = range.last
                    this.value = value
                    setFormatter { itemValue -> String.format("%02d", itemValue) }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        this.textColor = textColor
                        this.textSize = 68f
                    }
                    setOnValueChangedListener { _, _, newVal ->
                        onValueChange(newVal)
                    }
                }
            },
            update = { picker ->
                if (picker.value != value) {
                    picker.value = value
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    picker.textColor = textColor
                }
            }
        )
    }
}
