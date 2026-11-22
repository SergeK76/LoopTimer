package com.example.looptimer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(val code: String, val nativeName: String) {
    RU("ru", "Русский"),
    EN("en", "English"),
    ES("es", "Español"),
    PT("pt", "Português"),
    DE("de", "Deutsch"),
    FR("fr", "Français"),
    ZH("zh", "简体中文"),
    HI("hi", "हिन्दी"),
    IT("it", "Italiano");

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return entries.firstOrNull { it.code == code } ?: RU
        }
    }
}

data class AppStrings(
    val languageLabel: String,
    val changeTheme: String,
    val hours: String,
    val minutes: String,
    val seconds: String,
    val lapPrefix: String,
    val waitingOkPrefix: String,
    val totalElapsed: String,
    val okButton: String,
    val stopButton: String,
    val startButton: String,
    val animationTitle: String,
    val animationOrbit3d: String,
    val animationClassic: String,
    val soundTitle: String,
    val noSound: String,
    val customAudioFile: String,
    val vibrateTitle: String,
    val vibrateSubtitle: String,
    val flashTitle: String,
    val flashSubtitle: String,
    val wakeScreenTitle: String,
    val wakeScreenSubtitle: String,
    val keepScreenOnTitle: String,
    val keepScreenOnSubtitle: String,
    val stopDialogTitle: String,
    val stopDialogBody: String,
    val cancel: String,
    val select: String,
    val soundSheetTitle: String,
    val soundSheetHint: String,
    val pickFromDevice: String,
    val playingNow: String,
    val previewAction: String,
    val notifChannelName: String,
    val notifWaitingText: String,
    val notifRunningText: String
)

private val RuStrings = AppStrings(
    languageLabel = "Язык",
    changeTheme = "Сменить тему",
    hours = "Часы",
    minutes = "Мин",
    seconds = "Сек",
    lapPrefix = "КРУГ #",
    waitingOkPrefix = "ЖДЁТ ОК • КРУГ #",
    totalElapsed = "Общее время",
    okButton = "ОК",
    stopButton = "СТОП",
    startButton = "СТАРТ",
    animationTitle = "Анимация отсчёта",
    animationOrbit3d = "3D-орбита с объёмом",
    animationClassic = "Классические цифры",
    soundTitle = "Мелодия сигнала",
    noSound = "Без звука",
    customAudioFile = "Файл с устройства",
    vibrateTitle = "Вибрация",
    vibrateSubtitle = "При включении — проверочный сигнал",
    flashTitle = "Моргание вспышкой",
    flashSubtitle = "При включении — короткий блинк",
    wakeScreenTitle = "Зажигать экран при сигнале",
    wakeScreenSubtitle = "Чтобы сразу нажать ОК",
    keepScreenOnTitle = "Не выключать экран",
    keepScreenOnSubtitle = "Пока открыт экран приложения",
    stopDialogTitle = "Остановить таймер?",
    stopDialogBody = "Текущий отсчёт и счётчик кругов будут сброшены.",
    cancel = "Отмена",
    select = "Выбрать",
    soundSheetTitle = "Выбор мелодии",
    soundSheetHint = "Нажми на мелодию, чтобы прослушать, затем нажми «Выбрать»",
    pickFromDevice = "Выбрать мелодию / файл с устройства",
    playingNow = "Воспроизводится...",
    previewAction = "Прослушать",
    notifChannelName = "Сигналы таймера",
    notifWaitingText = "Время вышло — нажмите ОК для нового круга",
    notifRunningText = "Таймер работает в фоне"
)

private val EnStrings = AppStrings(
    languageLabel = "Language",
    changeTheme = "Toggle theme",
    hours = "Hours",
    minutes = "Min",
    seconds = "Sec",
    lapPrefix = "LAP #",
    waitingOkPrefix = "WAITING OK • LAP #",
    totalElapsed = "Total time",
    okButton = "OK",
    stopButton = "STOP",
    startButton = "START",
    animationTitle = "Countdown animation",
    animationOrbit3d = "3D orbit with depth",
    animationClassic = "Classic digits",
    soundTitle = "Alert sound",
    noSound = "Silent",
    customAudioFile = "Device audio file",
    vibrateTitle = "Vibration",
    vibrateSubtitle = "Short test pulse when enabled",
    flashTitle = "Camera flash blink",
    flashSubtitle = "Short test blink when enabled",
    wakeScreenTitle = "Wake screen on alert",
    wakeScreenSubtitle = "Quickly tap OK when time is up",
    keepScreenOnTitle = "Keep screen awake",
    keepScreenOnSubtitle = "While the app screen is open",
    stopDialogTitle = "Stop timer?",
    stopDialogBody = "Current countdown and lap counter will be reset.",
    cancel = "Cancel",
    select = "Select",
    soundSheetTitle = "Choose sound",
    soundSheetHint = "Tap a sound to preview, then tap Select",
    pickFromDevice = "Pick sound / audio file from device",
    playingNow = "Playing...",
    previewAction = "Preview",
    notifChannelName = "Timer alerts",
    notifWaitingText = "Time is up — tap OK for next lap",
    notifRunningText = "Timer running in background"
)

private val EsStrings = AppStrings(
    languageLabel = "Idioma",
    changeTheme = "Cambiar tema",
    hours = "Horas",
    minutes = "Min",
    seconds = "Seg",
    lapPrefix = "VUELTA #",
    waitingOkPrefix = "ESPERANDO OK • VUELTA #",
    totalElapsed = "Tiempo total",
    okButton = "OK",
    stopButton = "DETENER",
    startButton = "INICIAR",
    animationTitle = "Animación de cuenta atrás",
    animationOrbit3d = "Órbita 3D con volumen",
    animationClassic = "Dígitos clásicos",
    soundTitle = "Tono de aviso",
    noSound = "Sin sonido",
    customAudioFile = "Archivo del dispositivo",
    vibrateTitle = "Vibración",
    vibrateSubtitle = "Pulso de prueba al activar",
    flashTitle = "Parpadeo con linterna",
    flashSubtitle = "Destello de prueba al activar",
    wakeScreenTitle = "Encender pantalla al sonar",
    wakeScreenSubtitle = "Para pulsar OK al instante",
    keepScreenOnTitle = "Mantener pantalla encendida",
    keepScreenOnSubtitle = "Mientras la app esté abierta",
    stopDialogTitle = "¿Detener el temporizador?",
    stopDialogBody = "Se reiniciarán la cuenta atrás y las vueltas.",
    cancel = "Cancelar",
    select = "Elegir",
    soundSheetTitle = "Seleccionar tono",
    soundSheetHint = "Toca un tono para escucharlo y luego pulsa Elegir",
    pickFromDevice = "Elegir tono / archivo del dispositivo",
    playingNow = "Reproduciendo...",
    previewAction = "Escuchar",
    notifChannelName = "Avisos del temporizador",
    notifWaitingText = "Tiempo cumplido: pulsa OK para la siguiente vuelta",
    notifRunningText = "Temporizador activo en segundo plano"
)

private val PtStrings = AppStrings(
    languageLabel = "Idioma",
    changeTheme = "Alternar tema",
    hours = "Horas",
    minutes = "Min",
    seconds = "Seg",
    lapPrefix = "VOLTA #",
    waitingOkPrefix = "AGUARDANDO OK • VOLTA #",
    totalElapsed = "Tempo total",
    okButton = "OK",
    stopButton = "PARAR",
    startButton = "INICIAR",
    animationTitle = "Animação da contagem",
    animationOrbit3d = "Órbita 3D com volume",
    animationClassic = "Dígitos clássicos",
    soundTitle = "Som do alerta",
    noSound = "Sem som",
    customAudioFile = "Arquivo do aparelho",
    vibrateTitle = "Vibração",
    vibrateSubtitle = "Pulso de teste ao ativar",
    flashTitle = "Piscar lanterna",
    flashSubtitle = "Flash de teste ao ativar",
    wakeScreenTitle = "Acender tela no alerta",
    wakeScreenSubtitle = "Para tocar em OK rapidamente",
    keepScreenOnTitle = "Manter tela ligada",
    keepScreenOnSubtitle = "Enquanto a tela do app estiver aberta",
    stopDialogTitle = "Parar o timer?",
    stopDialogBody = "A contagem atual e as voltas serão zeradas.",
    cancel = "Cancelar",
    select = "Selecionar",
    soundSheetTitle = "Escolher som",
    soundSheetHint = "Toque para ouvir e depois toque em Selecionar",
    pickFromDevice = "Escolher som / arquivo do aparelho",
    playingNow = "Reproduzindo...",
    previewAction = "Ouvir",
    notifChannelName = "Alertas do timer",
    notifWaitingText = "Tempo esgotado — toque em OK para a próxima volta",
    notifRunningText = "Timer em execução em segundo plano"
)

private val DeStrings = AppStrings(
    languageLabel = "Sprache",
    changeTheme = "Design wechseln",
    hours = "Std",
    minutes = "Min",
    seconds = "Sek",
    lapPrefix = "RUNDE #",
    waitingOkPrefix = "WARTET AUF OK • RUNDE #",
    totalElapsed = "Gesamtzeit",
    okButton = "OK",
    stopButton = "STOPP",
    startButton = "START",
    animationTitle = "Countdown-Animation",
    animationOrbit3d = "3D-Orbit mit Tiefe",
    animationClassic = "Klassische Ziffern",
    soundTitle = "Signalton",
    noSound = "Ohne Ton",
    customAudioFile = "Audiodatei vom Gerät",
    vibrateTitle = "Vibration",
    vibrateSubtitle = "Testimpuls beim Aktivieren",
    flashTitle = "Kamerablitz blinken",
    flashSubtitle = "Kurzer Testblitz beim Aktivieren",
    wakeScreenTitle = "Bildschirm bei Signal wecken",
    wakeScreenSubtitle = "Für schnelles Tippen auf OK",
    keepScreenOnTitle = "Bildschirm anlassen",
    keepScreenOnSubtitle = "Solange die App geöffnet ist",
    stopDialogTitle = "Timer stoppen?",
    stopDialogBody = "Aktueller Countdown und Rundenzähler werden zurückgesetzt.",
    cancel = "Abbrechen",
    select = "Auswählen",
    soundSheetTitle = "Ton wählen",
    soundSheetHint = "Ton antippen zum Anhören, dann Auswählen tippen",
    pickFromDevice = "Ton / Audiodatei vom Gerät wählen",
    playingNow = "Wird abgespielt...",
    previewAction = "Anhören",
    notifChannelName = "Timer-Signale",
    notifWaitingText = "Zeit abgelaufen — OK für nächste Runde tippen",
    notifRunningText = "Timer läuft im Hintergrund"
)

private val FrStrings = AppStrings(
    languageLabel = "Langue",
    changeTheme = "Changer le thème",
    hours = "Heures",
    minutes = "Min",
    seconds = "Sec",
    lapPrefix = "TOUR #",
    waitingOkPrefix = "ATTENTE OK • TOUR #",
    totalElapsed = "Temps total",
    okButton = "OK",
    stopButton = "ARRÊTER",
    startButton = "DÉMARRER",
    animationTitle = "Animation du décompte",
    animationOrbit3d = "Orbite 3D en relief",
    animationClassic = "Chiffres classiques",
    soundTitle = "Sonnerie d'alerte",
    noSound = "Sans son",
    customAudioFile = "Fichier audio de l'appareil",
    vibrateTitle = "Vibration",
    vibrateSubtitle = "Impulsion test à l'activation",
    flashTitle = "Clignotement du flash",
    flashSubtitle = "Flash test à l'activation",
    wakeScreenTitle = "Allumer l'écran à l'alerte",
    wakeScreenSubtitle = "Pour appuyer sur OK aussitôt",
    keepScreenOnTitle = "Garder l'écran allumé",
    keepScreenOnSubtitle = "Tant que l'écran de l'app est ouvert",
    stopDialogTitle = "Arrêter le minuteur ?",
    stopDialogBody = "Le décompte en cours et les tours seront réinitialisés.",
    cancel = "Annuler",
    select = "Choisir",
    soundSheetTitle = "Choisir un son",
    soundSheetHint = "Touchez un son pour l'écouter, puis touchez Choisir",
    pickFromDevice = "Choisir un son / fichier de l'appareil",
    playingNow = "Lecture en cours...",
    previewAction = "Écouter",
    notifChannelName = "Alertes du minuteur",
    notifWaitingText = "Temps écoulé — appuyez sur OK pour le tour suivant",
    notifRunningText = "Minuteur actif en arrière-plan"
)

private val ZhStrings = AppStrings(
    languageLabel = "语言",
    changeTheme = "切换主题",
    hours = "时",
    minutes = "分",
    seconds = "秒",
    lapPrefix = "第 #",
    waitingOkPrefix = "等待确认 • 第 #",
    totalElapsed = "总用时",
    okButton = "确定",
    stopButton = "停止",
    startButton = "开始",
    animationTitle = "倒计时动画",
    animationOrbit3d = "立体 3D 轨道",
    animationClassic = "经典数字",
    soundTitle = "提示音",
    noSound = "静音",
    customAudioFile = "设备音频文件",
    vibrateTitle = "振动",
    vibrateSubtitle = "开启时播放测试振动",
    flashTitle = "闪光灯闪烁",
    flashSubtitle = "开启时闪光测试",
    wakeScreenTitle = "提醒时亮屏",
    wakeScreenSubtitle = "方便立即点击确定",
    keepScreenOnTitle = "保持屏幕常亮",
    keepScreenOnSubtitle = "在应用界面停留期间",
    stopDialogTitle = "停止计时器？",
    stopDialogBody = "当前倒计时和计圈次数将被重置。",
    cancel = "取消",
    select = "选择",
    soundSheetTitle = "选择提示音",
    soundSheetHint = "点击试听提示音，然后点击“选择”",
    pickFromDevice = "从设备选择铃声 / 音频文件",
    playingNow = "正在播放...",
    previewAction = "试听",
    notifChannelName = "计时器提醒",
    notifWaitingText = "时间到 — 点击确定开始下一轮",
    notifRunningText = "计时器正在后台运行"
)

private val HiStrings = AppStrings(
    languageLabel = "भाषा",
    changeTheme = "थीम बदलें",
    hours = "घंटे",
    minutes = "मिनट",
    seconds = "सेकंड",
    lapPrefix = "लैप #",
    waitingOkPrefix = "OK की प्रतीक्षा • लैप #",
    totalElapsed = "कुल समय",
    okButton = "OK",
    stopButton = "रोकें",
    startButton = "शुरू करें",
    animationTitle = "काउंटडाउन एनीमेशन",
    animationOrbit3d = "3D ऑर्बिट",
    animationClassic = "क्लासिक अंक",
    soundTitle = "अलर्ट ध्वनि",
    noSound = "मौन",
    customAudioFile = "डिवाइस ऑडियो फ़ाइल",
    vibrateTitle = "कंपन",
    vibrateSubtitle = "चालू करने पर परीक्षण कंपन",
    flashTitle = "फ़्लैश ब्लिंक",
    flashSubtitle = "चालू करने पर हल्का फ़्लैश",
    wakeScreenTitle = "अलर्ट पर स्क्रीन जगाएं",
    wakeScreenSubtitle = "तुरंत OK दबाने के लिए",
    keepScreenOnTitle = "स्क्रीन चालू रखें",
    keepScreenOnSubtitle = "जब ऐप स्क्रीन खुली हो",
    stopDialogTitle = "टाइमर रोकें?",
    stopDialogBody = "मौजूदा काउंटडाउन और लैप गिनती रीसेट हो जाएगी।",
    cancel = "रद्द करें",
    select = "चुनें",
    soundSheetTitle = "ध्वनि चुनें",
    soundSheetHint = "सुनने के लिए ध्वनि टैप करें, फिर चुनें दबाएं",
    pickFromDevice = "डिवाइस से ध्वनि / फ़ाइल चुनें",
    playingNow = "चल रहा है...",
    previewAction = "सुनें",
    notifChannelName = "टाइमर अलर्ट",
    notifWaitingText = "समय समाप्त — अगले लैप के लिए OK दबाएं",
    notifRunningText = "टाइमर बैकग्राउंड में चल रहा है"
)

private val ItStrings = AppStrings(
    languageLabel = "Lingua",
    changeTheme = "Cambia tema",
    hours = "Ore",
    minutes = "Min",
    seconds = "Sec",
    lapPrefix = "GIRO #",
    waitingOkPrefix = "ATTESA OK • GIRO #",
    totalElapsed = "Tempo totale",
    okButton = "OK",
    stopButton = "FERMA",
    startButton = "AVVIA",
    animationTitle = "Animazione conto alla rovescia",
    animationOrbit3d = "Orbita 3D con volume",
    animationClassic = "Cifre classiche",
    soundTitle = "Suono avviso",
    noSound = "Senza suono",
    customAudioFile = "File audio del dispositivo",
    vibrateTitle = "Vibrazione",
    vibrateSubtitle = "Impulso di prova all'attivazione",
    flashTitle = "Lampeggio flash",
    flashSubtitle = "Lampo di prova all'attivazione",
    wakeScreenTitle = "Riattiva schermo all'avviso",
    wakeScreenSubtitle = "Per toccare subito OK",
    keepScreenOnTitle = "Mantieni schermo attivo",
    keepScreenOnSubtitle = "Finché la schermata dell'app è aperta",
    stopDialogTitle = "Fermare il timer?",
    stopDialogBody = "Il conto alla rovescia e i giri verranno azzerati.",
    cancel = "Annulla",
    select = "Seleziona",
    soundSheetTitle = "Scegli suono",
    soundSheetHint = "Tocca un suono per ascoltarlo, poi tocca Seleziona",
    pickFromDevice = "Scegli suono / file dal dispositivo",
    playingNow = "In riproduzione...",
    previewAction = "Ascolta",
    notifChannelName = "Avvisi timer",
    notifWaitingText = "Tempo scaduto — tocca OK per il prossimo giro",
    notifRunningText = "Timer in esecuzione in background"
)

fun stringsFor(language: AppLanguage): AppStrings {
    return when (language) {
        AppLanguage.RU -> RuStrings
        AppLanguage.EN -> EnStrings
        AppLanguage.ES -> EsStrings
        AppLanguage.PT -> PtStrings
        AppLanguage.DE -> DeStrings
        AppLanguage.FR -> FrStrings
        AppLanguage.ZH -> ZhStrings
        AppLanguage.HI -> HiStrings
        AppLanguage.IT -> ItStrings
    }
}

val LocalAppStrings = staticCompositionLocalOf { RuStrings }

@Composable
fun ProvideAppStrings(language: AppLanguage, content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalAppStrings provides stringsFor(language),
        content = content
    )
}

object Strings {
    fun get(language: AppLanguage, key: String): String {
        val s = stringsFor(language)
        return when (key) {
            "channel_name" -> s.notifChannelName
            "waiting_ok" -> s.notifWaitingText
            "running_text" -> s.notifRunningText
            "app_name" -> "LoopTimer"
            "ok" -> s.okButton
            "stop" -> s.stopButton
            else -> ""
        }
    }
}
