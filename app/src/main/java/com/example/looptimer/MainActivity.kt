package com.example.looptimer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.NumberPicker
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = SettingsManager(this)

        setContent {
            var isDarkTheme by remember { mutableStateOf(settings.isDarkTheme) }
            var keepScreenOn by remember { mutableStateOf(settings.keepScreenOn) }

            LaunchedEffect(keepScreenOn) {
                if (keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            val colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()

            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LoopTimerScreen(
                        settings = settings,
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = {
                            isDarkTheme = it
                            settings.isDarkTheme = it
                        },
                        keepScreenOn = keepScreenOn,
                        onKeepScreenOnToggle = {
                            keepScreenOn = it
                            settings.keepScreenOn = it
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoopTimerScreen(
    settings: SettingsManager,
    isDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    keepScreenOn: Boolean,
    onKeepScreenOnToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val timerState by TimerService.state.collectAsState()

    var hours by remember { mutableIntStateOf(settings.hours) }
    var minutes by remember { mutableIntStateOf(settings.minutes) }
    var seconds by remember { mutableIntStateOf(settings.seconds) }

    var vibrateEnabled by remember { mutableStateOf(settings.vibrateEnabled) }
    var flashEnabled by remember { mutableStateOf(settings.flashEnabled) }
    var ringtoneTitle by remember { mutableStateOf(settings.getRingtoneTitle(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                needed.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        flashEnabled = granted
        settings.flashEnabled = granted
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            settings.ringtoneUri = uri?.toString()
            ringtoneTitle = settings.getRingtoneTitle(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Циклический таймер", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { onThemeToggle(!isDarkTheme) }) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Сменить тему"
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (timerState.isRunning) {
                        Text(
                            text = if (timerState.isAlerting)
                                "СИГНАЛ • КРУГ #${timerState.lapCount}"
                            else
                                "КРУГ #${timerState.lapCount}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (timerState.isAlerting)
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
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Общее время: ${formatDuration(timerState.totalElapsedMillis)}",
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
                                label = "Часы",
                                value = hours,
                                range = 0..23,
                                textColor = MaterialTheme.colorScheme.onSurface.toArgb(),
                                onValueChange = {
                                    hours = it
                                    settings.hours = it
                                }
                            )
                            Text(":", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            WheelPickerColumn(
                                label = "Мин",
                                value = minutes,
                                range = 0..59,
                                textColor = MaterialTheme.colorScheme.onSurface.toArgb(),
                                onValueChange = {
                                    minutes = it
                                    settings.minutes = it
                                }
                            )
                            Text(":", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            WheelPickerColumn(
                                label = "Сек",
                                value = seconds,
                                range = 0..59,
                                textColor = MaterialTheme.colorScheme.onSurface.toArgb(),
                                onValueChange = {
                                    seconds = it
                                    settings.seconds = it
                                }
                            )
                        }
                    }
                }
            }

            val totalMillis = ((hours * 3600L) + (minutes * 60L) + seconds) * 1000L

            if (timerState.isRunning && timerState.isAlerting) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val muteIntent = Intent(context, TimerService::class.java).apply {
                                action = TimerService.ACTION_MUTE
                            }
                            context.startService(muteIntent)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ОК / МЬЮТ",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            val stopIntent = Intent(context, TimerService::class.java).apply {
                                action = TimerService.ACTION_STOP
                            }
                            context.startService(stopIntent)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "СТОП",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Button(
                    onClick = {
                        if (timerState.isRunning) {
                            val stopIntent = Intent(context, TimerService::class.java).apply {
                                action = TimerService.ACTION_STOP
                            }
                            context.startService(stopIntent)
                        } else if (totalMillis > 0) {
                            val startIntent = Intent(context, TimerService::class.java).apply {
                                action = TimerService.ACTION_START
                                putExtra(TimerService.EXTRA_INTERVAL_MS, totalMillis)
                            }
                            ContextCompat.startForegroundService(context, startIntent)
                        }
                    },
                    enabled = timerState.isRunning || totalMillis > 0,
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
                        text = if (timerState.isRunning) "СТОП" else "СТАРТ",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    ListItem(
                        headlineContent = { Text("Звук сигнала") },
                        supportingContent = { Text(ringtoneTitle) },
                        leadingContent = { Icon(Icons.Default.NotificationsActive, contentDescription = null) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                        modifier = Modifier.clickable {
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Выберите сигнал таймера")
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                settings.ringtoneUri?.let {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it))
                                }
                            }
                            ringtonePickerLauncher.launch(intent)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text("Вибрация") },
                        leadingContent = { Icon(Icons.Default.Vibration, contentDescription = null) },
                        trailingContent = {
                            Switch(
                                checked = vibrateEnabled,
                                onCheckedChange = {
                                    vibrateEnabled = it
                                    settings.vibrateEnabled = it
                                }
                            )
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text("Моргание вспышкой") },
                        leadingContent = { Icon(Icons.Default.FlashlightOn, contentDescription = null) },
                        trailingContent = {
                            Switch(
                                checked = flashEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.CAMERA
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    } else {
                                        flashEnabled = enabled
                                        settings.flashEnabled = enabled
                                    }
                                }
                            )
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text("Не выключать экран") },
                        supportingContent = { Text("Пока открыто приложение") },
                        leadingContent = { Icon(Icons.Default.ScreenLockPortrait, contentDescription = null) },
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
    }
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
                    setFormatter { String.format("%02d", it) }
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
