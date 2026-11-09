package com.brendon978.menulauncher.pet

import com.brendon978.menulauncher.home.LocalBorderWidth
import com.brendon978.menulauncher.home.LocalLineScale
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.key
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.core.VectorConverter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.layout
import com.brendon978.menulauncher.music.MediaListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** Altura do robozinho na tela. */
val ROBOT_HEIGHT = 84.dp
private val TOUCH_PAD = 14.dp
/** Segurar parado este tempo pega no colo; antes disso, mexer o dedo é carinho. */
private const val PICKUP_HOLD_MS = 650L

/**
 * O robozinho andando pela tela inicial inteira, sem pisar no que é tocável ([obstacles]) e dentro de [area]
 * (coordenadas do próprio container, em px). Humor pelo celular e reação ao toque, tudo só visual.
 * Muito bravo, ele para e aponta a AK de brinquedo para você; guarda quando se acalma. Nunca atira.
 * As animações vivem do relógio de quadros do Compose, que para quando a tela inicial não está visível.
 */
@Composable
fun RobotPet(
    color: Color,
    musicPlaying: Boolean,
    homeOnTop: Boolean = true,
    comments: Boolean = true,
    mischief: Boolean = true,
    birthday: String = "",
    city: String = "",
    buddy: Buddy = Buddy.NONE,
    /** Jardim e torrinha só aparecem durante a brincadeira de regar ou empilhar. */
    calmGarden: Boolean = false,
    aiPackage: String? = null,
    named: Map<String, Area> = emptyMap(),
    area: Area,
    obstacles: List<Area>,
    accessory: Accessory = Accessory.NONE,
) {
    val density = LocalDensity.current
    val lw = LocalLineScale.current
    val heightPx = with(density) { ROBOT_HEIGHT.toPx() }
    val widthPx = heightPx * ROBOT_ASPECT
    val ballPx = with(density) { 22.dp.toPx() }

    // ---- entradas do celular ----
    val battery = rememberBattery()
    val headset = rememberHeadset()
    val dnd = rememberDoNotDisturb()
    val online = rememberOnline()
    val notifications by MediaListener.count.collectAsState()
    var prevNotif by remember { mutableStateOf<Int?>(null) }
    var happyUntil by remember { mutableLongStateOf(0L) }
    LaunchedEffect(notifications) {
        val n = notifications ?: return@LaunchedEffect
        happyUntil = happyUntil(prevNotif, n, System.currentTimeMillis(), happyUntil)
        prevNotif = n
    }

    // ---- relógio de animação (só corre com a tela inicial visível) ----
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameMillis { it }
        while (true) withFrameMillis { frame ->
            t = (frame - start) / 1000f
            now = System.currentTimeMillis()
        }
    }
    // sente o celular, pose de print, data especial
    var tiltNow by remember { mutableStateOf(Tilt.LEVEL) }
    var tiltShown by remember { mutableStateOf(Tilt.LEVEL) }
    var poseUntil by remember { mutableLongStateOf(0L) }
    var lastComment by remember { mutableLongStateOf(0L) }
    var photoUntil by remember { mutableLongStateOf(0L) }
    var photoPoseKind by remember { mutableStateOf(PhotoPose.VICTORY) }
    var debugSpecial by remember { mutableStateOf<SpecialDay?>(null) }
    val mischiefNow by rememberUpdatedState(mischief)
    val commentsNow by rememberUpdatedState(comments)
    var game by remember { mutableStateOf<GameKind?>(null) }
    val weather = GlitchWeather.current.value
    val talking = GlitchTalk.state.value
    // visita de outro Glitch
    var visitor by remember { mutableStateOf<Visitor?>(null) }
    var visitScene by remember { mutableStateOf<VisitScene?>(null) }
    var visitAt by remember { mutableLongStateOf(0L) }
    var guestStand by remember { mutableStateOf(Offset.Zero) }
    var guestEdge by remember { mutableStateOf(Offset.Zero) }
    var visitHandsNow by remember { mutableStateOf(Hand.ROCK to Hand.PAPER) }
    var guestLine by remember { mutableStateOf<String?>(null) }
    var guestLineUntil by remember { mutableLongStateOf(0L) }
    // os dois juntos
    var fetchLanding by remember { mutableStateOf(Offset.Zero) }
    var buddyLine by remember { mutableStateOf<String?>(null) }
    var buddyLineUntil by remember { mutableLongStateOf(0L) }
    var buddyHeartUntil by remember { mutableLongStateOf(0L) }
    var buddyConsoled by remember { mutableStateOf(false) }
    fun buddySpeak(text: String, ms: Long = 1_400) { buddyLine = text; buddyLineUntil = System.currentTimeMillis() + ms }
    // shows, jardim, grafite e bichinho
    var show by remember { mutableStateOf<ShowRun?>(null) }
    var lastShow by remember { mutableStateOf<Show?>(null) }
    var pendingShow by remember { mutableStateOf<Show?>(null) }
    var debugVariant by remember { mutableIntStateOf(0) }
    var plantWatered by remember { mutableIntStateOf(0) }
    var lastWaterDay by remember { mutableIntStateOf(0) }
    var towerBlocks by remember { mutableIntStateOf(0) }
    var graffiti by remember { mutableStateOf<Graffiti?>(null) }
    var graffitiAt by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    var graffitiShown by remember { mutableFloatStateOf(1f) }
    var buddyAt by remember { mutableStateOf(Offset.Unspecified) }
    var battleLeft by remember { mutableStateOf(Offset.Zero) }
    var battleRight by remember { mutableStateOf(Offset.Zero) }
    var battleEvil by remember { mutableStateOf(Offset.Zero) }
    // peteleco, cócegas, memória e voo
    var shoveUntil by remember { mutableLongStateOf(0L) }
    var tickleUntil by remember { mutableLongStateOf(0L) }
    var shoveDir by remember { mutableFloatStateOf(1f) }
    var memorySeq by remember { mutableStateOf(listOf<Int>()) }
    var memoryPos by remember { mutableIntStateOf(0) }
    var memoryLit by remember { mutableStateOf<Int?>(null) }
    var memoryYourTurn by remember { mutableStateOf(false) }
    var memoryBest by remember { mutableIntStateOf(0) }
    var memoryLost by remember { mutableStateOf(false) }
    var memoryTapAt by remember { mutableLongStateOf(0L) }
    var fly by remember { mutableStateOf<FlyState?>(null) }
    var flyWorld by remember { mutableStateOf<FlyWorld?>(null) }
    var flyBest by remember { mutableIntStateOf(0) }
    // barriga, poleiro, caça aos bugs e duelo
    var belly by remember { mutableFloatStateOf(0f) }
    var eatUntil by remember { mutableLongStateOf(0L) }
    var perch by remember { mutableStateOf<Perch?>(null) }
    var bugs by remember { mutableStateOf(listOf<Bug>()) }
    var bugSeq by remember { mutableIntStateOf(0) }
    var bugsUntil by remember { mutableLongStateOf(0L) }
    var bugsYou by remember { mutableIntStateOf(0) }
    var bugsHim by remember { mutableIntStateOf(0) }
    var duelMessage by remember { mutableStateOf("") }
    var duelYou by remember { mutableIntStateOf(0) }
    var duelHim by remember { mutableIntStateOf(0) }
    var duelBest by remember { mutableLongStateOf(0L) }
    var duelGoAt by remember { mutableLongStateOf(0L) }
    var duelTapped by remember { mutableLongStateOf(0L) }
    var duelEarly by remember { mutableStateOf(false) }
    val currentNamed by rememberUpdatedState(named)
    var debugNight by remember { mutableStateOf(false) } // só para testar de dia
    val special = remember(now / 60_000, debugSpecial, birthday) {
        debugSpecial ?: Calendar.getInstance().let { specialDay(it.get(Calendar.DAY_OF_MONTH), it.get(Calendar.MONTH) + 1, birthday) }
    }
    val hour = remember(now / 60_000, debugNight) { if (debugNight) SLEEP_FROM_HOUR else Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    // não perturbe: dorme junto (como se fosse noite)
    val phoneMood = if (dnd) Mood.SLEEPING else baseMood(hour, battery.level, battery.charging, musicPlaying, happyUntil, now)

    // ---- cérebro do Glitch: vontades, vínculo e o que ele decidiu fazer ----
    val context = LocalContext.current
    val store = remember { GlitchStore(context.applicationContext) }
    var drives by remember { mutableStateOf(Drives()) }
    var bond by remember { mutableFloatStateOf(.5f) }
    var activity by remember { mutableStateOf(Activity.WANDER) }
    var loaded by remember { mutableStateOf(false) }
    var resumed by remember { mutableStateOf(true) }
    var lastSeen by remember { mutableLongStateOf(0L) }
    var pokeExpect by remember { mutableFloatStateOf(0f) }
    var diary by remember { mutableStateOf(Diary(todayInt())) }
    var lastMorning by remember { mutableIntStateOf(0) }
    var lastNightNag by remember { mutableLongStateOf(0L) }
    fun log(f: (Diary) -> Diary) { diary = f(diary.forDay(todayInt())) }
    val brainRandom = remember { Random(System.nanoTime() + 3) }
    var speech by remember { mutableStateOf<String?>(null) }
    var speechUntil by remember { mutableLongStateOf(0L) }
    // fala de IA não pode ser cortada por bom dia, trabalho ou resmungo
    var aiSpeechUntil by remember { mutableLongStateOf(0L) }
    fun say(line: String, ms: Long = 2_200) {
        if (System.currentTimeMillis() < aiSpeechUntil) return
        speech = line
        speechUntil = System.currentTimeMillis() + ms
    }
    val brainScope = androidx.compose.runtime.rememberCoroutineScope()
    fun save() {
        if (GlitchLive.importing) return
        val m = GlitchMemory(drives, bond, System.currentTimeMillis(), pokeExpect, diary, lastMorning, lastNightNag, duelBest, plantWatered, lastWaterDay, towerBlocks, memoryBest, flyBest)
        brainScope.launch { store.save(m) }
    }
    fun greetIfAway(awayFrom: Long) {
        if (awayFrom <= 0L) return
        val (d, greeting) = onReturn(drives, System.currentTimeMillis() - awayFrom, bond)
        drives = d
        greeting?.let { say(GREETING_LINES.getValue(it).random(brainRandom), 2_800) }
    }
    LaunchedEffect(Unit) {
        val m = store.load()
        GlitchLive.importing = false
        drives = m.drives
        bond = m.bond
        pokeExpect = m.pokeExpect
        diary = m.diary.forDay(todayInt())
        lastMorning = m.lastMorning
        lastNightNag = m.lastNightNag
        duelBest = m.duelBest
        plantWatered = m.plantWatered
        lastWaterDay = m.lastWaterDay
        towerBlocks = m.towerBlocks
        memoryBest = m.memoryBest
        flyBest = m.flyBest
        loaded = true
        greetIfAway(m.lastSeen)
    }
    // tela inicial saiu de vista: guarda a memória; voltou depois de muito tempo: cumprimenta
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            when (e) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> { resumed = false; lastSeen = System.currentTimeMillis(); if (loaded) save() }
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> { resumed = true; if (loaded) greetIfAway(lastSeen) }
                else -> Unit
            }
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs); if (loaded) save() }
    }
    val night = phoneMood == Mood.SLEEPING
    val batteryTired = phoneMood == Mood.TIRED
    // os efeitos longos abaixo não reiniciam: leem daqui o valor de agora
    val nightNow by rememberUpdatedState(night)
    val tiredNow by rememberUpdatedState(batteryTired)
    val musicNow by rememberUpdatedState(musicPlaying)
    val chargingNow by rememberUpdatedState(battery.charging)
    // vontades andam com o tempo (só com a tela inicial à vista)
    LaunchedEffect(loaded) {
        if (!loaded) return@LaunchedEffect
        var n = 0
        while (true) {
            delay(1_000)
            if (!resumed) continue
            drives = tick(drives, 1f, TickCtx(napping = activity == Activity.NAP || nightNow, night = nightNow, batteryLow = tiredNow, music = musicNow))
            if (chargingNow) drives = drives.copy(energy = drives.energy + 1f / 300f).clamped()
            if (++n % 30 == 0) save()
        }
    }
    // decide o que fazer de tempos em tempos
    LaunchedEffect(loaded) {
        if (!loaded) return@LaunchedEffect
        while (true) {
            if (activity != Activity.PERCH && activity != Activity.EAT && activity != Activity.SHOW) activity = nextActivity(activity, drives, bond, musicNow, brainRandom).let { a ->
                when {
                    a != Activity.WANDER || game != null -> a
                    GlitchSignals.badgeSpots.isNotEmpty() && belly < 1f && brainRandom.nextInt(3) == 0 -> Activity.EAT
                    brainRandom.nextInt(5) == 0 -> Activity.PERCH
                    brainRandom.nextInt(3) == 0 -> Activity.SHOW
                    else -> a
                }
            }
            delay(brainRandom.nextLong(7_000, 13_000))
        }
    }
    // novidades: notificação nova e música começando deixam ele curioso (e a música faz ele repensar)
    var prevCount by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(notifications) {
        val n = notifications ?: return@LaunchedEffect
        if (prevCount != null && n > prevCount!!) drives = onEvent(drives, GlitchEvent.NOTIFICATION)
        prevCount = n
    }
    LaunchedEffect(musicPlaying, loaded) {
        if (!musicPlaying || !loaded) return@LaunchedEffect
        drives = onEvent(drives, GlitchEvent.MUSIC_START)
        activity = decide(drives, bond, true, brainRandom)
        if (activity == Activity.SULK) say(REFUSE_DANCE_LINES.random(brainRandom))
    }

    // humor final: noite e bateria mandam; senão, o que ele decidiu
    val mood = when {
        night || batteryTired -> phoneMood
        activity == Activity.NAP -> Mood.SLEEPING
        activity == Activity.DANCE && musicPlaying -> Mood.DANCING
        phoneMood == Mood.HAPPY -> Mood.HAPPY
        else -> Mood.NORMAL
    }
    val sulking = activity == Activity.SULK && mood == Mood.NORMAL

    // ---- toque ----
    var annoyance by remember { mutableStateOf(Annoyance()) }
    var reaction by remember { mutableStateOf<Reaction?>(null) }
    var reactionUntil by remember { mutableLongStateOf(0L) }
    var fleeSignal by remember { mutableIntStateOf(0) }
    val reacting = reaction != null && now < reactionUntil
    val calming = annoyance.levelReaction(now)
    val menacing = (reacting && reaction == Reaction.MENACE) || calming == Reaction.MENACE

    // depois de guardar a mira, sai marchando com a arma por um tempo
    var marchUntil by remember { mutableLongStateOf(0L) }
    var wasMenacing by remember { mutableStateOf(false) }
    LaunchedEffect(menacing) {
        marchUntil = marchUntil(wasMenacing, menacing, System.currentTimeMillis(), marchUntil)
        wasMenacing = menacing
    }
    val marching = !menacing && now < marchUntil

    val speechRandom = brainRandom
    LaunchedEffect(marching) {
        if (!marching) return@LaunchedEffect
        while (true) {
            delay(speechRandom.nextLong(2_500, 5_000))
            speech = MARCH_LINES.random(speechRandom)
            speechUntil = System.currentTimeMillis() + 2_000
        }
    }

    LaunchedEffect(activity, mood) {
        when {
            activity == Activity.SEEK_ATTENTION && mood == Mood.NORMAL -> while (true) {
                delay(brainRandom.nextLong(3_000, 6_000)); say(ATTENTION_LINES.random(brainRandom))
            }
            activity == Activity.SAD && mood == Mood.NORMAL -> while (true) {
                delay(brainRandom.nextLong(5_000, 9_000)); say(SAD_LINES.random(brainRandom))
            }
            activity == Activity.SULK && mood == Mood.NORMAL -> while (true) {
                delay(brainRandom.nextLong(6_000, 11_000)); say(SULK_LINES.random(brainRandom))
            }
        }
    }

    // ---- dança: troca de passo a cada DANCE_MOVE_MS ----
    var danceMove by remember { mutableStateOf(DanceMove.SWAY) }
    val danceRandom = remember { Random(System.nanoTime() + 2) }
    LaunchedEffect(mood == Mood.DANCING) {
        if (mood != Mood.DANCING) return@LaunchedEffect
        while (true) {
            delay(DANCE_MOVE_MS)
            danceMove = nextDanceMove(danceRandom, danceMove)
        }
    }

    // ---- carinho, colo, voo e tontura ----
    var petting by remember { mutableStateOf(false) }
    var held by remember { mutableStateOf(false) }
    var flying by remember { mutableStateOf(false) }
    var flyVx by remember { mutableFloatStateOf(0f) }
    var dizzyUntil by remember { mutableLongStateOf(0L) }
    val dizzy = now < dizzyUntil
    var debugHold by remember { mutableStateOf(false) }
    // fogos, protesto e castigo
    var fireworksUntil by remember { mutableLongStateOf(0L) }
    var protestMeter by remember { mutableStateOf(ProtestMeter()) }
    var protestUntil by remember { mutableLongStateOf(0L) }
    var protestSign by remember { mutableStateOf(PROTEST_SIGNS.first()) }
    var punishment by remember { mutableStateOf<Punishment?>(null) }
    var punishSign by remember { mutableStateOf<String?>(null) }
    var corner by remember { mutableStateOf<Area?>(null) } // placa do castigo (aparece no colo)
    var overCorner by remember { mutableStateOf(false) }
    // bola
    var ball by remember { mutableStateOf<Body?>(null) }
    var ballHeld by remember { mutableStateOf(false) }
    var ballSpin by remember { mutableFloatStateOf(0f) }
    var kicks by remember { mutableIntStateOf(0) }
    // brincadeiras que você começa (toque duplo no Glitch)
    var pendingGame by remember { mutableStateOf<GameKind?>(null) }
    var pendingProtest by remember { mutableIntStateOf(0) }
    var pendingPhoto by remember { mutableIntStateOf(0) }
    var debugBattle by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var lastTapAt by remember { mutableLongStateOf(0L) }
    var lastBallTouch by remember { mutableLongStateOf(0L) }
    var hideSpot by remember { mutableStateOf<Area?>(null) }
    var hideAntennaX by remember { mutableFloatStateOf(0f) }
    var hideTries by remember { mutableIntStateOf(0) }
    var hint by remember { mutableStateOf<Pair<Offset, String>?>(null) }
    var hintUntil by remember { mutableLongStateOf(0L) }
    var tagUntil by remember { mutableLongStateOf(0L) }
    var finger by remember { mutableStateOf<Offset?>(null) }
    var jkChoosing by remember { mutableStateOf(true) }
    var jkGlitch by remember { mutableStateOf<Hand?>(null) }
    var jkRevealUntil by remember { mutableLongStateOf(0L) }
    var jkCounting by remember { mutableStateOf(false) }
    var jkOutcome by remember { mutableStateOf<Outcome?>(null) }
    var bubbles by remember { mutableStateOf(listOf<Bubble>()) }
    var pops by remember { mutableStateOf(listOf<Pair<Offset, Long>>()) }
    var bubblesUntil by remember { mutableLongStateOf(0L) }
    var bubbleSeq by remember { mutableIntStateOf(0) }
    // travessuras, trabalho, pião e encolher
    var notes by remember { mutableStateOf(listOf<PostItNote>()) }
    var noteSeq by remember { mutableIntStateOf(0) }
    var workUntil by remember { mutableLongStateOf(0L) }
    var spinDeg by remember { mutableFloatStateOf(0f) }
    var spinning by remember { mutableStateOf(false) }
    var flinchUntil by remember { mutableLongStateOf(0L) }
    var letterUntil by remember { mutableLongStateOf(0L) }
    var lastLetterAt by remember { mutableLongStateOf(0L) }
    // protesto em estágios, rancor, drama e medo da placa
    var protestTimes by remember { mutableStateOf(listOf<Long>()) }
    var protestStage by remember { mutableStateOf(ProtestStage.COMPLAINT) }
    var grudge by remember { mutableLongStateOf(0L) }
    var faintUntil by remember { mutableLongStateOf(0L) }
    var papers by remember { mutableStateOf(listOf<Body>()) }
    var signHeight by remember { mutableIntStateOf(0) }
    var rootBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }

    // ---- andar ----
    val pos = remember { Animatable(Offset.Unspecified, Offset.VectorConverter) }
    var placed by remember { mutableStateOf(false) }
    var walking by remember { mutableStateOf(false) }
    var facingLeft by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var visible by remember { mutableFloatStateOf(1f) }
    val currentArea by rememberUpdatedState(area)
    val currentObstacles by rememberUpdatedState(obstacles)
    // post-it consome o toque: nunca em cima de algo que se usa
    fun noteFits(x: Float, y: Float, w: Float, h: Float) = currentObstacles.none { it.intersects(Area(x, y, x + w, y + h)) }
    // marchando, ele anda mesmo que estivesse dormindo, cansado ou dançando
    val protesting = now < protestUntil
    val punished = punishment?.active(now) == true
    val celebrating = now < fireworksUntil
    val sad = activity == Activity.SAD && mood == Mood.NORMAL
    val playing = activity == Activity.PLAY && mood == Mood.NORMAL
    val fainted = now < faintUntil
    val striking = protesting && protestStage == ProtestStage.STRIKE
    val revolting = protesting && protestStage == ProtestStage.REVOLUTION
    val working = now < workUntil
    val hidden = game == GameKind.HIDE && hideSpot != null
    val flinching = now < flinchUntil
    val posing = now < poseUntil
    val eating = now < eatUntil
    val dueling = game == GameKind.DUEL || game == GameKind.MEMORY || game == GameKind.FLY
    val shoved = now < shoveUntil
    val tickled = now < tickleUntil
    val asleep = mood == Mood.SLEEPING
    val visiting = visitor != null
    val stillMood = visiting || shoved || tickled || dueling || eating || tiltShown != Tilt.LEVEL || posing || fainted || striking || menuOpen || game == GameKind.JOKENPO || hidden || working || spinning || flinching || punished || celebrating || debugHold || petting || held || flying || dizzy || menacing || (!marching && game == null && (mood == Mood.SLEEPING || mood == Mood.TIRED || mood == Mood.DANCING || sulking))
    val random = remember { Random(System.nanoTime()) }

    fun spot(from: Offset?, minDistance: Float): Offset? =
        freeSpot(random, currentArea, widthPx, heightPx, currentObstacles, from?.let { it.x to it.y }, minDistance)
            ?.let { Offset(it.first, it.second) }

    // vários lugares pedem glitch: dois ao mesmo tempo fariam ele piscar duas vezes e ir para outro destino
    var teleporting by remember { mutableStateOf(false) }

    suspend fun glitchTo(target: Offset) {
        if (teleporting) return
        teleporting = true
        // pisca, some, reaparece no outro lugar. Se for cancelado no meio (a tela mudou), ele não pode ficar invisível.
        try {
            repeat(3) { visible = .3f; delay(50); visible = 1f; delay(60) }
            visible = 0f
            delay(220)
            pos.snapTo(target)
            visible = .5f; delay(60)
        } finally {
            visible = 1f
            teleporting = false
        }
    }

    // moonwalk: atravessa a tela de verdade, deslizando para trás (olha para um lado e vai para o outro)
    var moonFacingLeft by remember { mutableStateOf(false) }
    val moonwalking = mood == Mood.DANCING && danceMove == DanceMove.MOONWALK && !menacing && !fainted && !striking
    LaunchedEffect(moonwalking) {
        if (!moonwalking || !pos.value.isSpecified) return@LaunchedEffect
        val here = pos.value
        val a = currentArea
        val maxX = a.r - widthPx
        val target = (0 until 15).map { a.l + random.nextFloat() * (maxX - a.l) }
            .filter { abs(it - here.x) > widthPx * 1.5f && pathClear(here.x to here.y, it to here.y, widthPx, heightPx, currentObstacles) }
            .maxByOrNull { abs(it - here.x) }
            ?: return@LaunchedEffect
        moonFacingLeft = target > here.x // olha para o lado contrário de onde vai
        pos.animateTo(Offset(target, here.y), tween((DANCE_MOVE_MS - 300).toInt(), easing = LinearEasing))
    }

    // primeira posição, e fugir de onde a tela mudou (lista cresceu, IA veio para cá)
    LaunchedEffect(area, obstacles) {
        if (held || flying) return@LaunchedEffect
        val p = pos.value
        val me = if (p.isSpecified) Area(p.x, p.y, p.x + widthPx, p.y + heightPx) else null
        val inside = me != null && me.l >= area.l && me.t >= area.t && me.r <= area.r && me.b <= area.b
        if (me == null || !inside || obstacles.any { it.intersects(me) }) {
            val target = spot(null, 0f) ?: return@LaunchedEffect
            if (!placed) { pos.snapTo(target); placed = true } else glitchTo(target)
        }
    }

    // lugar livre perto de um ponto (vai até o meio da tela para chamar atenção, ou até algo para explorar)
    fun spotNear(target: Offset): Offset? =
        (0 until 20).mapNotNull { spot(pos.value, widthPx) }.minByOrNull { (it + Offset(widthPx / 2, heightPx / 2) - target).getDistance() }

    // o mais perto de um ponto sem pisar em nada: as brincadeiras miram em qualquer lugar da tela
    fun freeNear(target: Offset): Offset? =
        nearestFree(target.x, target.y, widthPx, heightPx, currentArea, currentObstacles)?.let { Offset(it.first, it.second) }

    // um glitch no meio toma a posição e cancela a animação: isso não pode encerrar quem estava andando
    suspend fun glide(target: Offset, ms: Int) {
        try {
            pos.animateTo(target, tween(ms, easing = LinearEasing))
        } catch (e: kotlinx.coroutines.CancellationException) {
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
        } finally {
            walking = false
        }
    }

    // anda até lá sem atravessar o que é tocável; sem caminho livre, vai num glitch
    suspend fun dash(target: Offset, speed: Float, maxMs: Int = Int.MAX_VALUE) {
        val here = pos.value
        if (!pathClear(here.x to here.y, target.x to target.y, widthPx, heightPx, currentObstacles)) return glitchTo(target)
        facingLeft = target.x < here.x
        walking = true
        val ms = ((target - here).getDistance() / speed * 1000).roundToInt().coerceIn(1, maxMs)
        glide(target, ms)
    }

    // pisou em algo tocável: se foi pouco, dá um passo para o lado; se foi muito, vai num glitch até o lugar livre
    suspend fun stepOff() {
        val here = pos.value
        val free = freeNear(here) ?: return
        if ((free - here).getDistance() < widthPx) {
            walking = true
            glide(free, 260)
        } else {
            glitchTo(free)
        }
    }

    fun onSomething(): Boolean {
        val p = pos.value
        return p.isSpecified && currentObstacles.any { it.intersects(Area(p.x, p.y, p.x + widthPx, p.y + heightPx)) }
    }

    suspend fun walkTo(target: Offset) = dash(target, with(density) { 38.dp.toPx() })

    // em cima de algo tocável o passeio não acha caminho e ele ficaria ali para sempre: sai num glitch
    val mayLeave by rememberUpdatedState(!hidden && !menuOpen && show == null)
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            if (placed && !held && !flying && !walking && !teleporting && mayLeave && onSomething()) stepOff()
        }
    }

    /** Roda um show do começo ao fim; cancelado no meio, ele volta ao normal sem resto na tela. */
    suspend fun runShow(run: ShowRun) {
        val a = currentArea
        val start = pos.value
        val gardenSpot = currentNamed["list"]?.let { Offset((it.r - widthPx * 2.1f).coerceAtLeast(a.l), it.t - heightPx - 16f) }
        try {
            when (run.kind) {
                Show.PARACHUTE, Show.DROP -> {
                    pos.snapTo(Offset(start.x, a.t - heightPx))
                    show = run
                    SHOW_LINES[run.kind]?.let { say(it, 1_600) }
                    pos.animateTo(start, tween((run.kind.ms * .9f).toInt(), easing = if (run.kind == Show.DROP) androidx.compose.animation.core.FastOutLinearInEasing else LinearEasing))
                    delay((run.kind.ms * .1f).toLong())
                }
                Show.JETPACK, Show.SKATE, Show.SCOOTER -> {
                    show = run
                    SHOW_LINES[run.kind]?.let { say(it, 1_800) }
                    val toRight = start.x < (a.l + a.r) / 2
                    facingLeft = !toRight
                    val y = if (run.kind == Show.JETPACK) (start.y - heightPx).coerceAtLeast(a.t) else start.y
                    pos.animateTo(Offset(if (toRight) a.r - widthPx else a.l, y), tween(run.kind.ms.toInt(), easing = LinearEasing))
                }
                Show.CLONES -> {
                    // o Glitch fica num ponto; o clone do mal à frente, e um clone do bem de cada lado
                    val ahead = if (start.x + widthPx * 3.2f < a.r) 1f else -1f
                    facingLeft = ahead < 0f
                    fun clamp(o: Offset) = Offset(o.x.coerceIn(a.l, a.r - widthPx), o.y.coerceIn(a.t, a.b - heightPx))
                    battleEvil = clamp(start + Offset(ahead * widthPx * 2f, 0f))
                    battleLeft = clamp(start + Offset(ahead * widthPx * 1f, -heightPx * .75f))
                    battleRight = clamp(start + Offset(ahead * widthPx * 1f, heightPx * .75f))
                    show = run
                    val ms = run.kind.ms
                    say(BATTLE_APPEAR_LINES.random(brainRandom), 2_000)
                    if (!run.evil) {
                        delay((ms * .55f).toLong())
                        say(BATTLE_NO_EVIL_LINES.random(brainRandom), 1_800)
                        delay((ms * .45f).toLong())
                    } else {
                        delay((ms * .28f).toLong())
                        say(BATTLE_EVIL_LINES.random(brainRandom), 1_500)
                        delay((ms * .12f).toLong())
                        say(BATTLE_ALLY_LINES.random(brainRandom), 1_800)
                        delay((ms * .18f).toLong())
                        say(pickFinisher(run.variant).line, 1_800)
                        delay((ms * .3f).toLong())
                        say(BATTLE_WIN_LINES.random(brainRandom), 2_000)
                        delay((ms * .12f).toLong())
                    }
                }
                Show.ON_HEAD -> { show = run; say(ON_HEAD_LINES.random(brainRandom), 2_400); delay(run.kind.ms) }
                Show.FETCH -> {
                    val toRight = start.x < (a.l + a.r) / 2
                    facingLeft = !toRight
                    fetchLanding = Offset((start.x + (if (toRight) widthPx * 3.2f else -widthPx * 2.6f)).coerceIn(a.l + 20f, a.r - 20f), start.y + heightPx * .9f)
                    show = run
                    say(FETCH_LINES.random(brainRandom), 1_600)
                    delay(run.kind.ms)
                    say("Bom garoto!", 1_500)
                }
                Show.SCARE -> {
                    show = run
                    delay((run.kind.ms * .42f).toLong())
                    buddySpeak("BU!")
                    say(SCARE_LINES.random(brainRandom), 2_000)
                    delay((run.kind.ms * .58f).toLong())
                }
                Show.KNOCK -> {
                    show = run
                    delay((run.kind.ms * .55f).toLong())
                    towerBlocks = 0
                    save()
                    say(KNOCK_LINES.random(brainRandom), 2_200)
                    buddySpeak(BUDDY_VOICE[buddy]?.first() ?: "Bip!")
                    delay((run.kind.ms * .45f).toLong())
                }
                Show.NAP_TOGETHER -> { show = run; say(NAP_LINES.random(brainRandom), 2_200); delay(run.kind.ms) }
                Show.BUDDY -> {
                    show = run
                    if (buddyChase(run.variant)) {
                        // corre atrás dele pela linha em que está, indo e voltando
                        say(if (buddySteals(run.variant)) "Devolve meu parafuso!" else BUDDY_CHASE_LINES.random(brainRandom), 2_000)
                        val span = widthPx * 2.2f
                        val left = (start.x - span).coerceAtLeast(a.l)
                        val right = (start.x + span).coerceAtMost(a.r - widthPx)
                        repeat(3) { i ->
                            val to = if (i % 2 == 0) right else left
                            val dest = Offset(to, start.y)
                            facingLeft = to < pos.value.x
                            if (pathClear(pos.value.x to start.y, to to start.y, widthPx, heightPx, currentObstacles)) pos.animateTo(dest, tween((run.kind.ms / 3).toInt(), easing = LinearEasing))
                            else delay(run.kind.ms / 3)
                        }
                    } else {
                        say(BUDDY_PET_LINES.random(brainRandom), 2_400)
                        delay(run.kind.ms)
                    }
                }
                Show.WATER -> {
                    gardenSpot?.let { glitchTo(it) }
                    show = run.copy(at = System.currentTimeMillis())
                    SHOW_LINES[run.kind]?.let { say(it, 1_800) }
                    delay(run.kind.ms)
                    plantWatered++
                    lastWaterDay = todayInt()
                    save()
                }
                Show.BLOCKS -> {
                    gardenSpot?.let { glitchTo(it) }
                    show = run.copy(at = System.currentTimeMillis())
                    SHOW_LINES[run.kind]?.let { say(it, 1_600) }
                    delay(run.kind.ms)
                    val (n, fell) = addBlock(towerBlocks)
                    towerBlocks = n
                    if (fell) { say("NÃO! MINHA TORRE!", 2_400); dizzyUntil = System.currentTimeMillis() + 2_000 }
                    save()
                }
                Show.MAGIC -> {
                    show = run
                    say(SHOW_LINES.getValue(run.kind), 2_000)
                    delay((run.kind.ms * .55f).toLong())
                    if (run.variant == 0) {
                        say(MAGIC_FAIL_LINE, 2_000)
                        delay((run.kind.ms * .3f).toLong())
                        spot(null, 0f)?.let { glitchTo(it) }
                    } else {
                        say(MAGIC_ITEMS.random(brainRandom), 2_000)
                        delay((run.kind.ms * .45f).toLong())
                    }
                }
                Show.GRAFFITI -> {
                    val s = with(density) { 96.dp.toPx() }
                    val spotFor = (0 until 20).mapNotNull { spot(null, 0f) }.map { Offset((it.x + widthPx + 8f).coerceAtMost(a.r - s), it.y) }
                        .firstOrNull { noteFits(it.x, it.y, s, s) }
                    show = run
                    SHOW_LINES[run.kind]?.let { say(it, 1_800) }
                    if (spotFor != null) {
                        graffitiAt = androidx.compose.ui.geometry.Rect(spotFor, androidx.compose.ui.geometry.Size(s, s))
                        graffiti = Graffiti.entries[run.variant.mod(Graffiti.entries.size)]
                        graffitiShown = 0f
                        val begin = System.currentTimeMillis()
                        while (System.currentTimeMillis() - begin < run.kind.ms) {
                            graffitiShown = ((System.currentTimeMillis() - begin).toFloat() / run.kind.ms).coerceIn(0f, 1f)
                            delay(50)
                        }
                        graffitiShown = 1f
                    } else delay(run.kind.ms)
                }
                else -> {
                    show = run
                    SHOW_LINES[run.kind]?.let { say(it, 2_000) }
                    delay(run.kind.ms)
                }
            }
        } finally {
            show = null
            // interrompido caindo de paraquedas: não pode ficar fora da tela
            if (pos.value.y < a.t) kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { pos.snapTo(start) }
        }
    }

    LaunchedEffect(stillMood, placed, marching, activity, protesting, game) {
        if (stillMood || !placed) {
            walking = false
            return@LaunchedEffect
        }
        if (!marching && !protesting && activity == Activity.SAD) {
            // vai para o canto livre mais perto e fica lá, triste
            val a = currentArea
            val corners = listOf(Offset(a.l, a.t), Offset(a.r, a.t), Offset(a.l, a.b), Offset(a.r, a.b))
            corners.mapNotNull { spotNear(it)?.let { s2 -> s2 to (s2 - it).getDistance() } }.minByOrNull { it.second }?.first?.let { walkTo(it) }
            // deixa um bilhete triste do lado (um por vez)
            if (notes.none { it.text in SAD_NOTES }) {
                val nw = with(density) { 110.dp.toPx() }
                val nh = with(density) { 60.dp.toPx() }
                val p = pos.value
                val self = Area(p.x, p.y, p.x + widthPx, p.y + heightPx)
                // ao lado, embaixo ou em cima dele: nunca por cima dele nem do que se usa
                listOf(Offset(p.x - nw, p.y), Offset(p.x + widthPx, p.y), Offset(p.x, p.y + heightPx), Offset(p.x, p.y - nh))
                    .map { Offset(it.x.coerceIn(a.l, a.r - nw), it.y.coerceIn(a.t, a.b - nh)) }
                    .firstOrNull { !self.intersects(Area(it.x, it.y, it.x + nw, it.y + nh)) && noteFits(it.x, it.y, nw, nh) }
                    ?.let { notes = (notes + PostItNote(++noteSeq, SAD_NOTES.random(brainRandom), it.x, it.y, brainRandom.nextFloat() * 8f - 4f)).takeLast(4) }
            }
            return@LaunchedEffect
        }
        if (!marching && !protesting && activity == Activity.PRANK && game == null) {
            // travessura: vai até um lugar livre e cola um post-it
            // às vezes, em vez de bilhete, entorta o relógio ou empurra um favorito
            if (mischiefNow && brainRandom.nextBoolean() && GlitchMischief.current.value == null) {
                GlitchMischief.current.value = Mischief.entries.random(brainRandom)
                say(MISCHIEF_LINES.random(brainRandom), 1_400)
                delay(1_500)
                drives = drives.copy(boredom = drives.boredom - .4f).clamped()
                activity = Activity.WANDER
                return@LaunchedEffect
            }
            spot(pos.value, widthPx)?.let { walkTo(it) }
            val a = currentArea
            val nw = with(density) { 110.dp.toPx() }
            val nh = with(density) { 60.dp.toPx() }
            val at = Offset((pos.value.x - nw / 3).coerceIn(a.l, a.r - nw), (pos.value.y - nh * .2f).coerceIn(a.t, a.b - nh))
            if (noteFits(at.x, at.y, nw, nh)) {
                notes = (notes + PostItNote(++noteSeq, POSTIT_LINES.random(brainRandom), at.x, at.y, brainRandom.nextFloat() * 10f - 5f)).takeLast(4)
                say("Hehe...", 1_200)
            }
            delay(1_500)
            drives = drives.copy(boredom = drives.boredom - .4f).clamped()
            activity = Activity.WANDER
            return@LaunchedEffect
        }
        if (game == GameKind.TAG) {
            // pega-pega: foge do dedo; encurralado, dá glitch
            while (System.currentTimeMillis() < tagUntil) {
                val f = finger
                val me = pos.value + Offset(widthPx / 2, heightPx / 2)
                if (f != null && (f - me).getDistance() < with(density) { 120.dp.toPx() }) {
                    val away = (me - f).let { it / it.getDistance().coerceAtLeast(1f) }
                    val want = me + away * with(density) { 130.dp.toPx() } - Offset(widthPx / 2, heightPx / 2)
                    val a = currentArea
                    val clamped = freeNear(Offset(want.x.coerceIn(a.l, a.r - widthPx), want.y.coerceIn(a.t, a.b - heightPx))) ?: pos.value
                    if ((clamped - pos.value).getDistance() < widthPx * .4f) {
                        spot(null, 0f)?.let { glitchTo(it) }
                        if (brainRandom.nextInt(2) == 0) say(TAG_LINES.random(brainRandom), 1_000)
                    } else {
                        dash(clamped, with(density) { 260.dp.toPx() }, maxMs = 500)
                    }
                } else delay(80)
            }
            if (game == GameKind.TAG) { say("Lento demais! Ganhei!", 2_200); game = null }
            return@LaunchedEffect
        }
        if (!marching && !protesting && game == null && activity == Activity.SHOW) {
            val kind = pendingShow ?: pickShow(lastShow, hour, canWater(lastWaterDay, todayInt()), brainRandom, buddy != Buddy.NONE, towerBlocks)
            pendingShow = null
            lastShow = kind
            runShow(ShowRun(kind, System.currentTimeMillis(), debugVariant++ + brainRandom.nextInt(12), kind == Show.CLONES && (debugBattle || battleHasEvil(brainRandom))))
            debugBattle = false
            activity = Activity.WANDER
            return@LaunchedEffect
        }
        if (!marching && !protesting && game == null && activity == Activity.EAT) {
            // vai até uma bolinha de notificação e come (só visual: a notificação continua lá, e a bolinha volta)
            val (pkg, r) = GlitchSignals.badgeSpots.entries.randomOrNull(brainRandom)?.toPair() ?: (null to null)
            if (pkg != null && r != null && homeOnTop) {
                val c = currentArea
                // bolinha no meio da lista: fica no lugar livre mais perto dela
                val target = freeNear(Offset((r.center.x - widthPx / 2).coerceIn(c.l, c.r - widthPx), (r.top - heightPx - 8f).coerceIn(c.t, c.b - heightPx))) ?: pos.value
                glitchTo(target)
                GlitchSignals.eat(pkg)
                belly = bellyAfterMeal(belly)
                say((if (belly >= 1f) STUFFED_LINES else EAT_LINES).random(brainRandom), 1_600)
                // nesta ordem: eatUntil reinicia este efeito, e ele não pode voltar ainda em EAT
                activity = Activity.WANDER
                eatUntil = System.currentTimeMillis() + 1_300
                return@LaunchedEffect
            }
            activity = Activity.WANDER
            return@LaunchedEffect
        }
        if (!marching && !protesting && game == null && activity == Activity.PERCH) {
            // senta em cima da lista ou se pendura embaixo do relógio: lugares onde não cobre nada tocável
            val c = currentArea
            val list = currentNamed["list"]?.takeIf { it.r - it.l > widthPx * 1.5f }
            val top = currentNamed["top"]?.takeIf { it.r - it.l > widthPx * 1.5f }
            val clear = with(density) { 16.dp.toPx() } // folga para a área de toque dele não entrar no que é tocável
            val others = currentNamed.filterKeys { it != "list" && it != "top" }.values
            val options = buildList {
                list?.let { add(Perch.SIT to Offset(it.l + brainRandom.nextFloat() * (it.r - it.l - widthPx), it.t - heightPx - clear)) }
                top?.let { add(Perch.HANG to Offset(it.l + brainRandom.nextFloat() * (it.r - it.l - widthPx), it.b + clear)) }
            }.filter { (_, o) ->
                val me = Area(o.x - clear, o.y - clear, o.x + widthPx + clear, o.y + heightPx + clear)
                o.y in c.t..(c.b - heightPx) && others.none { it.intersects(me) }
            }
            val choice = options.randomOrNull(brainRandom)
            if (choice != null) {
                glitchTo(choice.second)
                perch = choice.first
                try { delay(brainRandom.nextLong(14_000, 24_000)) } finally { perch = null }
                spot(null, 0f)?.let { glitchTo(it) }
            }
            activity = Activity.WANDER
            return@LaunchedEffect
        }
        if (game == GameKind.BUGS) {
            // caça aos bugs: disputa com você quem pega mais
            while (System.currentTimeMillis() < bugsUntil) {
                val me = pos.value + Offset(widthPx / 2, heightPx / 2)
                val target = bugs.minByOrNull { (Offset(it.x, it.y) - me).getDistance() }
                if (target == null) { delay(120); continue }
                val tc = Offset(target.x, target.y)
                val a = currentArea
                val dest = freeNear(Offset((tc.x - widthPx / 2).coerceIn(a.l, a.r - widthPx), (tc.y - heightPx / 2).coerceIn(a.t, a.b - heightPx))) ?: pos.value
                // bug em cima de um item: ele chega o mais perto que pode e pega de lá
                if ((tc - me).getDistance() < with(density) { 40.dp.toPx() } || (dest - pos.value).getDistance() < widthPx * .25f) {
                    bugs = bugs.filter { it.id != target.id }
                    pops = pops + (tc to System.currentTimeMillis())
                    bugsHim++
                    delay(420) // respiro para você ter chance
                    continue
                }
                dash(dest, with(density) { 95.dp.toPx() }, maxMs = 220)
            }
            if (game == GameKind.BUGS) { say(bugScoreLine(bugsYou, bugsHim), 2_800); game = null; bugs = emptyList() }
            return@LaunchedEffect
        }
        if (game == GameKind.BUBBLES) {
            // bolhas: corre até a mais perto e estoura
            while (System.currentTimeMillis() < bubblesUntil) {
                val me = pos.value + Offset(widthPx / 2, heightPx / 2)
                val target = bubbles.minByOrNull { (Offset(it.x, it.y) - me).getDistance() }
                if (target == null) { delay(150); continue }
                val tc = Offset(target.x, target.y)
                val a = currentArea
                val dest = freeNear(Offset((tc.x - widthPx / 2).coerceIn(a.l, a.r - widthPx), (tc.y - heightPx / 2).coerceIn(a.t, a.b - heightPx))) ?: pos.value
                // bolha em cima de um item: ele chega o mais perto que pode e estoura de lá
                if ((tc - me).getDistance() < with(density) { 44.dp.toPx() } || (dest - pos.value).getDistance() < widthPx * .25f) {
                    bubbles = bubbles.filter { it.id != target.id }
                    pops = pops + (tc to System.currentTimeMillis())
                    if (brainRandom.nextInt(3) == 0) say(BUBBLE_LINES.random(brainRandom), 900)
                    continue
                }
                dash(dest, with(density) { 150.dp.toPx() }, maxMs = 250)
            }
            if (game == GameKind.BUBBLES) { say("Ufa, cansei!", 1_800); game = null; bubbles = emptyList() }
            return@LaunchedEffect
        }
        if (!marching && !protesting && (activity == Activity.PLAY || game == GameKind.PASS)) {
            // solta a bola do alto e corre atrás, chutando
            if (ball == null) {
                val a = currentArea
                ball = Body(a.l + brainRandom.nextFloat() * (a.r - a.l - ballPx), a.t + 10f, 0f, 0f)
                kicks = 0
                say(PLAY_LINES.first())
            }
            val toYou = game == GameKind.PASS
            if (toYou) lastBallTouch = System.currentTimeMillis()
            while (if (toYou) game == GameKind.PASS && System.currentTimeMillis() - lastBallTouch < 40_000 else kicks < 7) {
                val b = ball ?: break
                if (ballHeld) { delay(200); continue }
                val ballCenter = Offset(b.x + ballPx / 2, b.y + ballPx / 2)
                val me = pos.value + Offset(widthPx / 2, heightPx / 2)
                // pés na altura do chão da bola, ao lado dela
                val side = if (me.x < ballCenter.x) -1f else 1f
                val target = freeNear(
                    Offset(
                        (ballCenter.x + side * (widthPx * .55f) - widthPx / 2).coerceIn(currentArea.l, currentArea.r - widthPx),
                        (b.y + ballPx - heightPx).coerceIn(currentArea.t, currentArea.b - heightPx),
                    ),
                ) ?: pos.value
                if ((target - pos.value).getDistance() > widthPx * .25f) {
                    dash(target, with(density) { 90.dp.toPx() }, maxMs = 700)
                    continue
                }
                // chute: manda a bola para o lado contrário de onde ele está, para cima
                facingLeft = side > 0f
                ball = if (toYou) {
                    // devolve para você: mira no meio da tela, para cima
                    val aimX = (currentArea.l + currentArea.r) / 2 - b.x
                    b.copy(vx = (aimX * 1.4f).coerceIn(-1_600f, 1_600f), vy = -1_300f)
                } else b.copy(vx = -side * (900f + brainRandom.nextFloat() * 700f), vy = -900f - brainRandom.nextFloat() * 500f)
                kicks++
                if (brainRandom.nextInt(3) == 0) say(PLAY_LINES.random(brainRandom), 1_400)
                delay(450)
            }
            ball = null
            drives = drives.copy(boredom = drives.boredom - .5f).clamped()
            activity = Activity.WANDER
            if (toYou && game == GameKind.PASS) { say("Cansei, valeu pelo jogo!", 2_000); game = null }
            return@LaunchedEffect
        }
        if (!marching && activity == Activity.SEEK_ATTENTION) {
            // vem para o meio da tela, de frente para você, e acena
            spotNear(Offset((currentArea.l + currentArea.r) / 2, (currentArea.t + currentArea.b) / 2))?.let { walkTo(it) }
            facingLeft = pos.value.x + widthPx / 2 > (currentArea.l + currentArea.r) / 2
            return@LaunchedEffect
        }
        if (!marching && activity == Activity.EXPLORE) {
            // vai até alguma coisa da tela e escaneia
            val thing = currentObstacles.randomOrNull(brainRandom)
            val target = thing?.let { Offset((it.l + it.r) / 2, (it.t + it.b) / 2) }
            target?.let { spotNear(it) }?.let { walkTo(it) }
            say(EXPLORE_LINES.random(brainRandom))
            scanning = true
            try { delay(3_000) } finally { scanning = false }
            drives = drives.copy(curiosity = drives.curiosity - .4f).clamped()
            activity = decide(drives, bond, musicNow, brainRandom)
            return@LaunchedEffect
        }
        while (true) {
            val target = spot(pos.value, minDistance = widthPx * 1.5f)
            if (target == null) {
                // preso onde não dá para sair andando (em cima da lista, depois de comer)
                if (onSomething()) stepOff() else spot(null, 0f)?.let { glitchTo(it) }
                delay(1_500)
                continue
            }
            facingLeft = target.x < pos.value.x
            walking = true
            val speed = with(density) { (if (marching || protesting) 48.dp else 38.dp).toPx() } // px por segundo
            val ms = ((target - pos.value).getDistance() / speed * 1000).roundToInt().coerceAtLeast(1)
            glide(target, ms)
            delay(if (marching || protesting) random.nextLong(300, 700) else random.nextLong(1_500, 4_000))
            when (random.nextInt(100)) {
                in 0..11 -> { scanning = true; delay(2_500); scanning = false }
                in 12..15 -> spot(null, widthPx * 3)?.let { glitchTo(it) }
            }
        }
    }

    // fugir de quem fica importunando: o lugar livre mais longe que achar
    LaunchedEffect(fleeSignal) {
        if (fleeSignal == 0) return@LaunchedEffect
        val here = pos.value
        val far = (0 until 12).mapNotNull { spot(null, 0f) }.maxByOrNull { (it - here).getDistance() }
        if (far != null) glitchTo(far)
    }

    LaunchedEffect(drives, bond, activity, mood, menacing, marching, game, workUntil, diary) {
        val doing = when {
            game != null -> "Brincando: ${game!!.label}"
            System.currentTimeMillis() < workUntil -> "Trabalhando junto"
            punishment?.active(System.currentTimeMillis()) == true -> "De castigo"
            System.currentTimeMillis() < faintUntil -> "Fingindo que morreu"
            System.currentTimeMillis() < protestUntil -> when (protestStage) {
                ProtestStage.COMPLAINT -> "Em protesto"
                ProtestStage.STRIKE -> "De greve"
                ProtestStage.REVOLUTION -> "Fazendo revolução"
            }
            menacing -> "Apontando a arma pra você"
            marching -> "Marchando, bravo"
            dnd -> "Dormindo (não perturbe)"
            night -> "Dormindo (é noite)"
            batteryTired -> "Cansado (bateria baixa)"
            mood == Mood.DANCING -> "Dançando"
            else -> when (activity) {
                Activity.WANDER -> "Passeando"
                Activity.NAP -> "Cochilando"
                Activity.SEEK_ATTENTION -> "Pedindo atenção"
                Activity.EXPLORE -> "Explorando"
                Activity.SULK -> "Emburrado"
                Activity.DANCE -> "Querendo dançar"
                Activity.SAD -> "Triste no cantinho"
                Activity.PLAY -> "Jogando bola"
                Activity.PRANK -> "Aprontando"
                Activity.EAT -> "Comendo notificação"
                Activity.PERCH -> "De boa, olhando a tela"
                Activity.SHOW -> "Dando um show"
            }
        }
        GlitchLive.publish(GlitchSnapshot(drives, bond, activity, annoyance.levelAt(System.currentTimeMillis()) / 6f, doing, diaryText(diary.forDay(todayInt()))))
    }

    LaunchedEffect(punishment) {
        val p = punishment ?: return@LaunchedEffect
        while (p.active(System.currentTimeMillis())) {
            delay(brainRandom.nextLong(3_000, 5_000))
            punishSign = PUNISH_SIGNS.random(brainRandom)
            delay(2_200)
            punishSign = null
        }
        if (!p.over) { say("Tá, aprendi. Posso sair?", 2_000); grudge = grudgeUntil(System.currentTimeMillis()) }
        if (!held) corner = null
    }

    // chegou notificação nova: ele aparece com uma cartinha (no máximo uma a cada 20 s)
    LaunchedEffect(loaded) {
        if (!loaded) return@LaunchedEffect
        MediaListener.arrived.collect { pkg ->
            val n = System.currentTimeMillis()
            if (!resumed || game != null || nightNow || n - lastLetterAt < 20_000) return@collect
            lastLetterAt = n
            letterUntil = n + 2_800
            val label = runCatching {
                context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg, 0)).toString()
            }.getOrDefault("alguém")
            say(listOf("Chegou coisa do $label!", "Cartinha do $label!", "Ó, $label te chamando.").random(brainRandom), 2_400)
        }
    }

    // uma IA falando pelo Glitch (IRIS, Eloy...): sai num balão maior, com o nome de quem fala
    val incoming by com.brendon978.menulauncher.ai.SayReceiver.pending.collectAsState()
    LaunchedEffect(incoming, resumed, loaded, homeOnTop, hidden) {
        val sp = incoming ?: return@LaunchedEffect
        // busca ou ajustes por cima, ou escondido: guarda para quando dê para ver
        if (!resumed || !loaded || !homeOnTop || hidden) return@LaunchedEffect
        com.brendon978.menulauncher.ai.SayReceiver.consumed(sp)
        // fala velha (mais de 10 min) não vale mais
        if (System.currentTimeMillis() - sp.at > 10 * 60_000L) return@LaunchedEffect
        val line = sp.from?.let { "$it: ${sp.text}" } ?: sp.text
        aiSpeechUntil = 0L
        GlitchTalk.state.value = null
        say(line, (4_500L + line.length * 55L).coerceAtMost(12_000L))
        aiSpeechUntil = speechUntil
    }

    // rotina: bom dia de manhã, "vai dormir" de madrugada
    LaunchedEffect(resumed, loaded) {
        if (!resumed || !loaded) return@LaunchedEffect
        delay(1_200)
        val n = System.currentTimeMillis()
        when (routineFor(Calendar.getInstance().get(Calendar.HOUR_OF_DAY), todayInt(n), lastMorning, lastNightNag, n)) {
            Routine.GOOD_MORNING -> {
                lastMorning = todayInt(n)
                say(special?.line ?: GOOD_MORNING_LINES.random(brainRandom), 2_600)
                if (special?.party == true) fireworksUntil = n + 2_400
                save()
            }
            Routine.GO_TO_SLEEP -> { lastNightNag = n; say(GO_TO_SLEEP_LINES.random(brainRandom), 2_600); save() }
            null -> Unit
        }
    }
    // sem internet: fica confuso de vez em quando
    LaunchedEffect(online, loaded) {
        if (online || !loaded) return@LaunchedEffect
        while (true) {
            say(listOf("Cadê a internet?", "Caiu a net?!", "Sem sinal...").random(brainRandom), 2_000)
            delay(brainRandom.nextLong(20_000, 40_000))
        }
    }
    // plugou no carregador: "nham, energia"; completou: festa (uma vez por carga)
    var celebratedFull by remember { mutableStateOf(false) }
    LaunchedEffect(battery.charging) {
        if (battery.charging && loaded) say(listOf("Nham, energia!", "Hmm, eletricidade fresquinha.", "Recarregando...").random(brainRandom), 2_000)
        if (!battery.charging) celebratedFull = false
    }
    LaunchedEffect(battery.level, battery.charging) {
        if (battery.charging && battery.level == 100 && !celebratedFull) {
            celebratedFull = true
            fireworksUntil = System.currentTimeMillis() + 2_400
            say("Carga completa!", 2_000)
        }
    }

    // sente o celular: chacoalhou, fica tonto; inclinou, escorrega; de cabeça para baixo, reclama
    MotionEffect(
        active = resumed && homeOnTop && loaded,
        onTilt = { tiltNow = it },
        onShake = {
            if (!held && game == null) {
                dizzyUntil = System.currentTimeMillis() + 2_500
                say(SHAKE_LINES.random(brainRandom), 1_600)
            }
        },
    )
    // tirou print: faz pose
    LaunchedEffect(Unit) {
        GlitchSignals.screenshots.collect {
            if (System.currentTimeMillis() < photoUntil) return@collect // foi ele quem tirou
            photoPoseKind = pickPhotoPose(photoPoseKind, brainRandom)
            poseUntil = System.currentTimeMillis() + POSE_MS
            say(POSE_LINES.random(brainRandom), POSE_MS)
        }
    }
    // clima: só com a cidade escrita nos Ajustes; comenta quando muda
    LaunchedEffect(resumed, city) {
        if (!resumed) return@LaunchedEffect
        delay(1_000) // enquanto você digita a cidade, espera terminar
        GlitchWeather.refresh(city, hour)
    }
    LaunchedEffect(weather, loaded) {
        if (weather != null && loaded) { delay(2_500); say(weather.line, 2_200) }
    }
    // falando com a IA: mostra que ouviu e que ela está pensando
    LaunchedEffect(talking) {
        if (talking != null) say(talking, 25_000) else if (speech == "Ouvindo..." || speech == "Pensando...") speechUntil = 0L
    }
    DisposableEffect(Unit) { onDispose { GlitchMischief.current.value = null } }
    // travessura não fica para sempre
    val mischiefState = GlitchMischief.current.value
    LaunchedEffect(mischiefState) {
        if (mischiefState == null) return@LaunchedEffect
        delay(3 * 60_000L)
        GlitchMischief.current.value = null
    }
    // voltou de um app de trabalho depois de um tempo: ele "trabalhou junto"; de outro, às vezes comenta
    LaunchedEffect(resumed) {
        if (!resumed) return@LaunchedEffect
        // ficou um tempo fora: às vezes chega de um jeito especial
        if (lastSeen > 0L && game == null && mood != Mood.SLEEPING) {
            entranceFor(System.currentTimeMillis() - lastSeen, brainRandom)?.let { pendingShow = it; activity = Activity.SHOW }
        }
        GlitchSignals.lastLaunch?.let { (pkg, at) ->
            val n = System.currentTimeMillis()
            if (isWorkApp(pkg) && n - at > 2 * 60_000L) {
                workUntil = n + 20_000
                say(WORK_LINES.random(brainRandom), 2_600)
            } else if (commentsNow && commentNow(lastComment, n, brainRandom)) {
                val label = runCatching {
                    context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg, 0)).toString()
                }.getOrDefault("app")
                returnComment(pkg, label, n - at, brainRandom)?.let { lastComment = n; say(it, 2_400) }
            }
        }
        GlitchSignals.lastLaunch = null
    }

    // visita: o outro Glitch chega, fazem as cenas sorteadas e ele vai embora
    LaunchedEffect(visitor) {
        val v = visitor ?: return@LaunchedEffect
        val a = currentArea
        try {
            val here = pos.value
            val fromRight = here.x + widthPx * 2.6f < a.r
            guestStand = Offset(if (fromRight) here.x + widthPx * 1.6f else here.x - widthPx * 1.6f, here.y)
            guestEdge = Offset(if (fromRight) a.r else a.l - widthPx, here.y)
            for (scene in visitPlan(brainRandom)) {
                if (scene == VisitScene.JOKENPO) visitHandsNow = visitHands(brainRandom)
                visitScene = scene
                visitAt = System.currentTimeMillis()
                val (mine, theirs) = visitLines(scene, v.name, brainRandom)
                say(mine, 1_700)
                delay(1_100)
                guestLine = theirs
                guestLineUntil = System.currentTimeMillis() + 1_800
                delay((scene.ms - 1_100).coerceAtLeast(0L))
                if (scene == VisitScene.JOKENPO) {
                    val r = jokenpo(visitHandsNow.first, visitHandsNow.second)
                    say(if (r == Outcome.WIN) "Ganhei! A tela é minha!" else if (r == Outcome.DRAW) "Empate, de novo!" else "Roubou!", 1_600)
                    delay(900)
                }
            }
        } finally {
            visitScene = null
            visitor = null
        }
    }
    // saiu da tela inicial: jogo de reflexo não fica esperando
    LaunchedEffect(resumed) {
        if (!resumed && (game == GameKind.MEMORY || game == GameKind.FLY)) game = null
    }
    // memória: ele mostra, você repete; cada rodada aumenta um
    LaunchedEffect(game == GameKind.MEMORY) {
        if (game != GameKind.MEMORY) return@LaunchedEffect
        memorySeq = emptyList()
        memoryLost = false
        // sai de cima dos blocos: toque neles não pode cair nele
        val c = currentArea
        val center = Offset((c.l + c.r) / 2, (c.t + c.b) / 2)
        (0 until 20).mapNotNull { spot(null, 0f) }.maxByOrNull { (it - center).getDistance() }?.let { glitchTo(it) }
        while (game == GameKind.MEMORY && !memoryLost) {
            memorySeq = memorySeq + brainRandom.nextInt(4)
            memoryPos = 0
            memoryYourTurn = false
            delay(650)
            val flash = memoryFlashMs(memorySeq.size)
            for (pad in memorySeq) {
                memoryLit = pad
                delay(flash)
                memoryLit = null
                delay(flash / 2)
            }
            memoryYourTurn = true
            val seq = memorySeq
            memoryTapAt = System.currentTimeMillis()
            while (memoryYourTurn && !memoryLost && memorySeq === seq && game == GameKind.MEMORY) {
                delay(30)
                if (System.currentTimeMillis() - memoryTapAt > 12_000) memoryLost = true // largou o jogo: não fica preso na tela
            }
        }
        val score = (memorySeq.size - 1).coerceAtLeast(0)
        if (score > memoryBest) { memoryBest = score; save(); say("Recorde: $score!", 2_400) } else say("Você foi até $score. Tenta de novo!", 2_400)
        memoryYourTurn = false
        game = null
    }
    // voo: tocar sobe, soltar cai; desvia dos muros
    LaunchedEffect(game == GameKind.FLY) {
        if (game != GameKind.FLY) return@LaunchedEffect
        val a = currentArea
        val world = FlyWorld(a, a.l + widthPx * .8f, widthPx, heightPx)
        flyWorld = world
        pos.snapTo(Offset(world.x, (a.t + a.b) / 2))
        fly = FlyState(pos.value.y, started = false)
        say("Toca pra começar!", 2_000)
        val waitFrom = System.currentTimeMillis()
        var last = 0L
        while (game == GameKind.FLY) {
            val dt = androidx.compose.runtime.withFrameNanos { nanos ->
                val d = if (last == 0L) 1f / 60f else ((nanos - last) / 1e9f).coerceIn(0f, .05f)
                last = nanos
                d
            }
            val s = flyStep(fly ?: break, dt, world, brainRandom)
            fly = s
            if (!s.started && System.currentTimeMillis() - waitFrom > 10_000) { game = null; break } // ninguém jogou
            pos.snapTo(Offset(world.x, s.y))
            if (s.crashed) {
                dizzyUntil = System.currentTimeMillis() + 2_000
                if (s.score > flyBest) { flyBest = s.score; save(); say("Recorde: ${s.score} muros!", 2_400) } else say("${s.score} muros. Bati!", 2_400)
                game = null
            }
        }
        fly = null
        flyWorld = null
        spot(null, 0f)?.let { glitchTo(it) } // acabou o voo: não fica em cima de nada
    }
    // falas espontâneas, de vez em quando
    LaunchedEffect(resumed, loaded) {
        if (!resumed || !loaded) return@LaunchedEffect
        while (true) {
            delay(nextIdleGapMs(brainRandom))
            if (commentsNow && game == null && show == null && !asleep && now >= speechUntil) say(idleLine(hour, bond, brainRandom), 2_400)
        }
    }
    // barriga cheia vai descendo sozinha
    LaunchedEffect(belly > 0f) {
        while (belly > 0f) { delay(1_000); belly = bellyAfterTime(belly, 1f, walking) }
    }
    LaunchedEffect(game == GameKind.BUGS) {
        if (game != GameKind.BUGS) return@LaunchedEffect
        var last = 0L
        var nextBug = 0L
        while (game == GameKind.BUGS) {
            androidx.compose.runtime.withFrameNanos { nanos ->
                val dt = if (last == 0L) 1f / 60f else ((nanos - last) / 1e9f).coerceIn(0f, .05f)
                last = nanos
                val n = System.currentTimeMillis()
                val a = currentArea
                if (n > nextBug && bugs.size < 7) {
                    nextBug = n + 800
                    val speed = with(density) { (46 + brainRandom.nextInt(40)).dp.toPx() }
                    val ang = brainRandom.nextFloat() * 6.283f
                    bugs = bugs + Bug(++bugSeq, a.l + brainRandom.nextFloat() * (a.r - a.l), a.t + brainRandom.nextFloat() * (a.b - a.t), speed * kotlin.math.cos(ang), speed * sin(ang))
                }
                bugs = bugs.map { moveBug(it, dt, a) }
                pops = pops.filter { n - it.second < 400 }
            }
        }
    }
    // duelo: espera o "JÁ!" e vê quem é mais rápido
    LaunchedEffect(game == GameKind.DUEL) {
        if (game != GameKind.DUEL) return@LaunchedEffect
        duelYou = 0
        duelHim = 0
        repeat(DUEL_ROUNDS) {
            duelMessage = "Espera..."
            duelGoAt = 0L
            duelTapped = 0L
            duelEarly = false
            delay(brainRandom.nextLong(1_500, 4_000))
            if (game != GameKind.DUEL) return@LaunchedEffect
            if (!duelEarly) {
                duelMessage = "JÁ!"
                // o cronômetro só parte quando o "JÁ!" já foi desenhado, não quando foi pedido
                androidx.compose.runtime.withFrameNanos { }
                androidx.compose.runtime.withFrameNanos { }
                duelGoAt = System.currentTimeMillis()
                val deadline = duelGoAt + 1_200
                while (duelTapped == 0L && System.currentTimeMillis() < deadline) delay(10)
            }
            val tapMs = duelTapped.takeIf { it > 0L && !duelEarly }?.let { it - duelGoAt }
            val glitchMs = duelGlitchRange(duelBest).random(brainRandom)
            when (duelRound(tapMs, glitchMs, duelEarly)) {
                DuelRound.YOU -> duelYou++
                DuelRound.HIM -> duelHim++
            }
            if (tapMs != null) { duelBest = bestTime(duelBest, tapMs); save() }
            duelMessage = when {
                duelEarly -> "Queimou!"
                tapMs == null -> "Dormiu?"
                else -> "$tapMs x $glitchMs ms"
            }
            delay(1_300)
        }
        say(duelEndLine(duelYou, duelHim), 2_800)
        game = null
    }

    LaunchedEffect(game == GameKind.BUBBLES) {
        if (game != GameKind.BUBBLES) return@LaunchedEffect
        var last = 0L
        while (game == GameKind.BUBBLES) {
            androidx.compose.runtime.withFrameNanos { nanos ->
                val dt = if (last == 0L) 1f / 60f else ((nanos - last) / 1e9f).coerceIn(0f, .05f)
                last = nanos
                val n = System.currentTimeMillis()
                bubbles = bubbles.mapNotNull { b ->
                    val ny = b.y - 70f * dt
                    if (ny < currentArea.t - b.r) null else b.copy(y = ny, x = b.x + kotlin.math.sin((n - b.born) / 300f) * 30f * dt)
                }
                pops = pops.filter { n - it.second < 400 }
            }
        }
    }

    // levou ele até a placa: "Não! Lá não!"
    LaunchedEffect(overCorner) { if (overCorner) say(CORNER_FEAR_LINES.random(brainRandom), 1_400) }

    // revolução: atira bolinhas de papel na sua direção (só visual)
    LaunchedEffect(revolting) {
        if (!revolting) { papers = emptyList(); return@LaunchedEffect }
        var last = 0L
        var nextThrow = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { nanos ->
                val dt = if (last == 0L) 1f / 60f else ((nanos - last) / 1e9f).coerceIn(0f, .05f)
                last = nanos
                val n = System.currentTimeMillis()
                if (n > nextThrow) {
                    nextThrow = n + 900
                    val a = currentArea
                    val from = pos.value + Offset(widthPx / 2, heightPx * .2f)
                    val tx = (a.l + a.r) / 2 + (brainRandom.nextFloat() - .5f) * (a.r - a.l) * .8f
                    papers = papers + Body(from.x, from.y, (tx - from.x) * 1.1f, -500f - brainRandom.nextFloat() * 300f)
                }
                val a = currentArea
                papers = papers.map { b -> b.copy(x = b.x + b.vx * dt, y = b.y + b.vy * dt, vy = b.vy + GRAVITY * .45f * dt) }.filter { it.y < a.b + 40f }
            }
        }
    }

    // telefone inclinado: depois de um instante ele escorrega para o lado de baixo
    LaunchedEffect(tiltNow, loaded) {
        if (tiltNow == Tilt.LEVEL) { tiltShown = Tilt.LEVEL; return@LaunchedEffect }
        if (!loaded) return@LaunchedEffect
        delay(if (tiltNow == Tilt.UPSIDE_DOWN) 700 else 1_100)
        if (held || game != null || mood == Mood.SLEEPING) return@LaunchedEffect
        tiltShown = tiltNow
        say((if (tiltNow == Tilt.UPSIDE_DOWN) UPSIDE_LINES else TILT_LINES).random(brainRandom), 1_800)
        if (tiltNow == Tilt.UPSIDE_DOWN) return@LaunchedEffect
        val a = currentArea
        val here = pos.value
        val x = if (tiltNow == Tilt.LEFT) a.l else a.r - widthPx
        if (pathClear(here.x to here.y, x to here.y, widthPx, heightPx, currentObstacles)) {
            pos.animateTo(Offset(x, here.y), tween(1_400))
        }
    }

    // bola: cai, quica e rola (a não ser que você esteja segurando)
    LaunchedEffect(ball != null) {
        if (ball == null) return@LaunchedEffect
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { nanos ->
                val dt = if (last == 0L) 1f / 60f else ((nanos - last) / 1e9f).coerceIn(0f, 1f / 20f)
                last = nanos
                val b = ball
                if (b != null && !ballHeld) {
                    val a = currentArea
                    val supports = currentObstacles.filter { it.t - ballPx >= a.t }
                    val (nb, _) = physicsStep(b, dt, a, supports, ballPx, ballPx)
                    ballSpin += nb.vx * dt * 2f
                    ball = nb
                }
            }
            if (ball == null) break
        }
    }

    // teste por ADB (só na versão de desenvolvimento)
    LaunchedEffect(Unit) {
        PetDebug.commands.collect { cmd ->
            when (cmd) {
                "pisar" -> currentObstacles.maxByOrNull { (it.r - it.l) * (it.b - it.t) }?.let { pos.snapTo(Offset(it.l + widthPx, it.t + 40f)) }
                "parar" -> {
                    debugHold = true
                    val a = currentArea
                    val center = Offset((a.l + a.r) / 2, (a.t + a.b) / 2)
                    val target = (0 until 40).mapNotNull { spot(null, 0f) }
                        .minByOrNull { (it + Offset(widthPx / 2, heightPx / 2) - center).getDistance() }
                    if (target != null) pos.snapTo(target)
                    delay(300)
                    val c = rootBounds.center
                    android.util.Log.d(PetDebug.TAG, "centro=${c.x.roundToInt()},${c.y.roundToInt()} tamanho=${rootBounds.width.roundToInt()}x${rootBounds.height.roundToInt()}")
                }
                "soltar" -> debugHold = false
                "triste" -> { drives = drives.copy(longing = 1f, boredom = 1f); activity = Activity.SAD }
                "bola" -> { drives = drives.copy(boredom = .8f, energy = .9f, longing = 0f); activity = Activity.PLAY }
                "fogos" -> { fireworksUntil = System.currentTimeMillis() + 2_400; say(CELEBRATE_LINES.random(brainRandom)) }
                "protesto" -> pendingProtest++
                "greve" -> { protestTimes = listOf(System.currentTimeMillis()); pendingProtest++ }
                "revolucao" -> { protestTimes = List(2) { System.currentTimeMillis() }; pendingProtest++ }
                "drama" -> { faintUntil = System.currentTimeMillis() + 2_800; say(DRAMA_LINES.random(brainRandom), 2_600) }
                "rancor" -> grudge = grudgeUntil(System.currentTimeMillis())
                "noite" -> debugNight = !debugNight
                "memoria" -> pendingGame = GameKind.MEMORY
                "voo" -> pendingGame = GameKind.FLY
                "empurra" -> { shoveDir = 1f; shoveUntil = System.currentTimeMillis() + 1_500; say(SHOVE_LINES.random(brainRandom), 1_600) }
                "cocegas" -> { tickleUntil = System.currentTimeMillis() + 1_600; say(TICKLE_LINES.random(brainRandom), 1_600) }
                "cabeca2" -> { pendingShow = Show.ON_HEAD; activity = Activity.SHOW }
                "buscar" -> { pendingShow = Show.FETCH; activity = Activity.SHOW }
                "susto" -> { pendingShow = Show.SCARE; activity = Activity.SHOW }
                "derruba" -> { towerBlocks = maxOf(towerBlocks, 3); pendingShow = Show.KNOCK; activity = Activity.SHOW }
                "soneca" -> { pendingShow = Show.NAP_TOGETHER; activity = Activity.SHOW }
                "visita" -> if (visitor == null) visitor = Visitor("Bit", androidx.compose.ui.graphics.Color(0xFF7FB7A4), Accessory.CAP, Buddy.CAT)
                "brincar" -> { pendingShow = Show.BUDDY; activity = Activity.SHOW }
                "clones" -> { debugBattle = true; pendingShow = Show.CLONES; activity = Activity.SHOW }
                "jetpack" -> { pendingShow = Show.JETPACK; activity = Activity.SHOW }
                "skate" -> { pendingShow = Show.SKATE; activity = Activity.SHOW }
                "patinete" -> { pendingShow = Show.SCOOTER; activity = Activity.SHOW }
                "cafe" -> { pendingShow = Show.COFFEE; activity = Activity.SHOW }
                "flexao" -> { pendingShow = Show.PUSHUPS; activity = Activity.SHOW }
                "regar" -> { pendingShow = Show.WATER; activity = Activity.SHOW }
                "blocos" -> { pendingShow = Show.BLOCKS; activity = Activity.SHOW }
                "magica" -> { pendingShow = Show.MAGIC; activity = Activity.SHOW }
                "grafite" -> { pendingShow = Show.GRAFFITI; activity = Activity.SHOW }
                "paraquedas" -> { pendingShow = Show.PARACHUTE; activity = Activity.SHOW }
                "caixa" -> { pendingShow = Show.BOX; activity = Activity.SHOW }
                "queda" -> { pendingShow = Show.DROP; activity = Activity.SHOW }
                "foto" -> pendingPhoto++
                "bugs" -> pendingGame = GameKind.BUGS
                "duelo" -> pendingGame = GameKind.DUEL
                "poleiro" -> activity = Activity.PERCH
                "comer" -> activity = Activity.EAT
                "barriga" -> belly = 1f
                "relogio" -> GlitchMischief.current.value = Mischief.CLOCK
                "icone" -> GlitchMischief.current.value = Mischief.ICON
                "print" -> GlitchSignals.screenshots.tryEmit(Unit)
                "data" -> debugSpecial = SpecialDay.entries.getOrNull((debugSpecial?.ordinal ?: -1) + 1)
                "chacoalha" -> { dizzyUntil = System.currentTimeMillis() + 2_500; say(SHAKE_LINES.random(brainRandom), 1_600) }
                "esquerda" -> tiltNow = Tilt.LEFT
                "direita" -> tiltNow = Tilt.RIGHT
                "cabeca" -> tiltNow = Tilt.UPSIDE_DOWN
                "nivel" -> tiltNow = Tilt.LEVEL
                "ak" -> { reaction = Reaction.MENACE; reactionUntil = System.currentTimeMillis() + 5_000 }
                "marcha" -> marchUntil = System.currentTimeMillis() + 8_000
                "menu" -> menuOpen = true
                "bola2" -> pendingGame = GameKind.PASS
                "esconde" -> pendingGame = GameKind.HIDE
                "pega" -> pendingGame = GameKind.TAG
                "jokenpo" -> pendingGame = GameKind.JOKENPO
                "bolhas" -> pendingGame = GameKind.BUBBLES
                "postit" -> { workUntil = 0L; game = null; drives = drives.copy(boredom = .8f, longing = .1f); activity = Activity.PRANK }
                "trabalho" -> { workUntil = System.currentTimeMillis() + 20_000; say(WORK_LINES.random(brainRandom), 2_600) }
                "encolher" -> pokeExpect = .9f
                "castigo" -> { punishment = Punishment.start(System.currentTimeMillis()); punishSign = null }
            }
        }
    }

    if (!placed) return

    // ---- rosto e pose ----
    val jkRevealing = jkGlitch != null && now < jkRevealUntil
    val showNow = show?.takeIf { it.active(now) && !held && !flying && !menacing }
    val showK = showNow?.progress(now) ?: 0f
    val showPoseNow = showNow?.let { showPose(it, showK, facingLeft, t) }
    val visitNow = visitScene?.takeIf { visitor != null }
    val visitK = visitNow?.let { ((now - visitAt).toFloat() / it.ms).coerceIn(0f, 1f) } ?: 0f
    val guestLeft = guestStand.x >= pos.value.x // ele fica à direita olhando para a esquerda, e vice-versa
    val visitPair = visitNow?.let { visitPoses(it, visitK, t, guestLeft, visitHandsNow) }
    val face = when {
        visitPair != null && !held && !flying -> visitPair.first.face
        shoved -> Face.DIZZY
        tickled -> Face.GIGGLE
        game == GameKind.FLY -> Face.GIGGLE
        showPoseNow != null -> showPoseNow.face
        talking != null -> Face.SCAN
        eating -> Face.GIGGLE
        tiltShown != Tilt.LEVEL -> Face.SURPRISED
        posing -> photoPose(photoPoseKind, facingLeft, t).face
        fainted -> Face.DIZZY
        jkRevealing -> when (jkGlitch) { Hand.ROCK -> Face.ROCK; Hand.PAPER -> Face.PAPER; else -> Face.SCISSORS }
        flinching || spinning -> Face.SURPRISED
        working -> Face.SCAN
        petting -> Face.BLISS
        celebrating -> Face.HEART
        held || flying -> Face.SURPRISED
        punished -> Face.ANGRY
        protesting -> Face.ANGRY
        dizzy -> Face.DIZZY
        menacing -> Face.ANGRY
        marching -> Face.SERIOUS
        reacting && reaction == Reaction.GIGGLE -> Face.GIGGLE
        reacting && reaction == Reaction.ANGRY -> Face.ANGRY
        reacting -> Face.SERIOUS
        calming == Reaction.ANGRY || calming == Reaction.FLEE -> Face.ANGRY
        calming == Reaction.SERIOUS -> Face.SERIOUS
        mood == Mood.SLEEPING -> Face.SLEEP
        mood == Mood.TIRED -> Face.TIRED
        !online && t % 12f < 3f -> Face.CONFUSED
        sad -> Face.SAD
        mood == Mood.DANCING -> Face.MUSIC
        mood == Mood.HAPPY -> Face.HEART
        scanning -> Face.SCAN
        else -> Face.NORMAL
    }
    val blinkCycle = t % 4f
    val blink = if (blinkCycle in 3.75f..3.9f) .1f else 1f
    val holdingLetter = now < letterUntil
    // olhos seguem o último toque na tela por uns segundos
    val touch = GlitchSignals.lastTouch
    val lookFrom = rootBounds.center
    val (lookX, lookY) = if (touch != null && now - touch.third < 2_500) {
        val dx = (touch.first - lookFrom.x) / 300f
        val dy = (touch.second - lookFrom.y) / 400f
        (if (facingLeft) -dx else dx).coerceIn(-1f, 1f) to dy.coerceIn(-1f, 1f)
    } else 0f to 0f
    val pose = when {
        visitPair != null && !held && !flying -> visitPair.first
        shoved -> RobotPose(face, shoveDir < 0f, armL = 70f, armR = -70f, tilt = 82f * shoveDir, lift = -6f, phase = t)
        tickled -> RobotPose(face, facingLeft, armL = 60f + 30f * sin(t * 26f), armR = -60f - 30f * sin(t * 26f), tilt = 7f * sin(t * 26f), lift = 4f * abs(sin(t * 18f)), phase = t)
        game == GameKind.FLY -> RobotPose(face, false, armL = 40f, armR = -40f, legL = 18f, legR = 8f, tilt = 8f, phase = t)
        showPoseNow != null -> showPoseNow
        tiltShown == Tilt.UPSIDE_DOWN -> RobotPose(face, facingLeft, armL = 150f, armR = -150f, rotation = 180f, phase = t)
        tiltShown != Tilt.LEVEL -> RobotPose(face, tiltShown == Tilt.LEFT, armL = 70f, armR = -70f, tilt = if (tiltShown == Tilt.LEFT) -12f else 12f, phase = t)
        posing -> photoPose(photoPoseKind, facingLeft, t)
        eating -> RobotPose(face, facingLeft, armL = -20f, armR = 20f, lift = 2f * abs(sin(t * 14f)), phase = t)
        // desmaio dramático: tomba de lado e fica estendido
        fainted && !held && !flying -> RobotPose(face, facingLeft, armL = 70f, armR = -70f, tilt = 88f, lift = -2f, phase = t)
        // greve: de costas, braços cruzados, parado com o cartaz
        striking && !held && !flying -> RobotPose(face, facingLeft, armL = 20f, armR = -20f, back = true, phase = t)
        spinning -> RobotPose(face, facingLeft, armL = 110f, armR = -110f, rotation = spinDeg, phase = t)
        flinching -> RobotPose(face, facingLeft, armL = -150f, armR = 150f, lift = -4f, phase = t)
        jkCounting -> RobotPose(face, facingLeft, armL = 10f, armR = -70f + 25f * sin(t * 22f), phase = t)
        working -> RobotPose(face, false, armL = 12f + 4f * sin(t * 22f), armR = -12f - 4f * sin(t * 22f + 1f), laptop = true, phase = t)
        // comemorando: pula com os braços para cima
        celebrating -> {
            val air = abs(sin(t * 6f))
            RobotPose(face, facingLeft, armL = 140f, armR = -140f, lift = 12f * air, phase = t)
        }
        // de castigo: de costas, braços cruzados para trás, batendo o pé
        punished -> RobotPose(face, facingLeft, armL = 20f, armR = -20f, back = true, legL = if (sin(t * 8f) > .6f) -10f else 0f, phase = t)
        // protesto: marcha com o cartaz para o alto
        protesting -> {
            val s2 = sin(t * 9f)
            RobotPose(face, facingLeft, legL = if (walking) 24f * s2 else 0f, legR = if (walking) -24f * s2 else 0f, armL = 165f, armR = -165f, lift = if (walking) 3f * abs(s2) else 0f, antennaOn = (t * 6).toInt() % 2 == 0, phase = t)
        }
        sad && !walking -> RobotPose(face, facingLeft, armL = 4f, armR = -4f, tilt = if (facingLeft) -5f else 5f, headTilt = 10f, phase = t)
        // carinho: inclina para o lado do dedo, todo mole
        petting -> RobotPose(face, facingLeft, armL = 12f, armR = -12f, tilt = 5f * sin(t * 6f), phase = t)
        // no colo: esperneia com os braços para cima
        held -> {
            val k = sin(t * 14f)
            RobotPose(face, facingLeft, legL = 18f * sin(t * 10f), legR = -18f * sin(t * 10f + 1f), armL = 120f + 20f * k, armR = -120f - 20f * k, phase = t)
        }
        // voando: inclina para onde vai, braços para cima
        flying -> RobotPose(face, flyVx < 0f, armL = 140f, armR = -140f, legL = 10f, legR = -10f, tilt = (flyVx / 60f).coerceIn(-25f, 25f), phase = t)
        dizzy -> RobotPose(face, facingLeft, armL = 15f, armR = -15f, tilt = 7f * sin(t * 3f), phase = t)
        // de frente, braços fechados no meio segurando a arma apontada para você
        menacing -> RobotPose(face, false, armL = -47f, armR = 47f, tilt = 1.5f * sin(t * 3f), antennaOn = (t * 6).toInt() % 2 == 0, phase = t, gun = GunHold.AIM)
        // marcha: joelho alto, passo duro, arma atravessada no peito segura pelas duas mãos
        marching -> {
            val s = sin(t * 9f)
            RobotPose(face, facingLeft, legL = 24f * s, legR = -24f * s, armL = -38f, armR = 22f, lift = 3.5f * abs(s), phase = t, gun = GunHold.MARCH)
        }
        holdingLetter && !held && !flying -> {
            val s = if (walking) sin(t * 12f) else 0f
            RobotPose(face, facingLeft, legL = 16f * s, legR = -16f * s, armL = -40f, armR = 40f, lift = 2f * abs(sin(t * 6f)), blink = blink, phase = t, letter = true)
        }
        reacting && reaction == Reaction.GIGGLE -> {
            val k = ((reactionUntil - now) / 900f).coerceIn(0f, 1f)
            RobotPose(face, facingLeft, armL = -30f, armR = 30f, lift = 10f * sin(k * Math.PI.toFloat()), blink = blink, phase = t)
        }
        face == Face.ANGRY -> {
            val stomp = sin(t * 18f)
            RobotPose(face, facingLeft, legL = if (stomp > 0) -14f else 0f, armL = 25f, armR = -25f, antennaOn = (t * 6).toInt() % 2 == 0, phase = t)
        }
        mood == Mood.DANCING && !reacting -> dancePose(danceMove, face, if (danceMove == DanceMove.MOONWALK) moonFacingLeft else facingLeft, t)
        sulking && !walking -> RobotPose(face, facingLeft, armL = 8f, armR = -8f, back = true, phase = t)
        activity == Activity.SEEK_ATTENTION && mood == Mood.NORMAL && !walking -> {
            val wave = sin(t * 9f)
            RobotPose(face, facingLeft, armL = 6f, armR = -150f + 22f * wave, lift = 2f * abs(wave), blink = blink, phase = t)
        }
        mood == Mood.HAPPY && !walking -> {
            val hop = sin(t * 5f).coerceAtLeast(0f)
            RobotPose(face, facingLeft, armL = -20f, armR = 20f, lift = 8f * hop, blink = blink, phase = t)
        }
        walking -> {
            val s = sin(t * 12f)
            RobotPose(face, facingLeft, legL = 16f * s, legR = -16f * s, armL = -16f * s, armR = 16f * s, lift = 2.5f * abs(s), blink = blink, phase = t)
        }
        perch == Perch.SIT && !walking -> {
            val s = sin(t * 3f)
            RobotPose(face, facingLeft, legL = -62f + 10f * s, legR = -62f - 10f * s, armL = 12f, armR = -12f, lift = -24f, blink = blink, phase = t)
        }
        perch == Perch.HANG && !walking -> {
            val s = sin(t * 2.5f)
            RobotPose(face, facingLeft, legL = 6f * s, legR = -6f * s, armL = 122f, armR = -122f, tilt = 3f * s, blink = blink, phase = t)
        }
        mood == Mood.TIRED -> RobotPose(face, facingLeft, armL = 8f, armR = -8f, tilt = if (facingLeft) -4f else 4f, phase = t)
        else -> RobotPose(face, facingLeft, blink = blink, antennaOn = !scanning || (t * 3).toInt() % 2 == 0, phase = t)
    }

    val scope = androidx.compose.runtime.rememberCoroutineScope()
    // área de toque maior que o desenho: ele é pequeno e está andando
    val pad = with(density) { TOUCH_PAD.toPx() }
    var placedAt by remember { mutableStateOf(Offset.Zero) }

    /** Ele conta, faz pose e tira o print: sai de primeira, sem balão na frente. */
    fun takePhoto() {
        menuOpen = false
        photoUntil = System.currentTimeMillis() + 7_000
        poseUntil = photoUntil
        photoPoseKind = pickPhotoPose(photoPoseKind, brainRandom)
        brainScope.launch {
            for (n in listOf("3", "2", "1")) { say(n, 900); delay(900) }
            speechUntil = 0L
            delay(350)
            if (!com.brendon978.menulauncher.gestures.LauncherAccessibility.screenshot()) {
                poseUntil = 0L
                say("Liga a acessibilidade do launcher pra eu tirar foto!", 3_000)
            } else {
                delay(1_200)
                poseUntil = 0L
                say(POSE_LINES.random(brainRandom), 2_400)
            }
        }
    }

    fun gameTap(p: Offset) {
        if (game == GameKind.FLY) { fly = (fly ?: return).let { s -> flyWorld?.let { w -> flyTap(s, w) } ?: s }; return }
        if (game == GameKind.DUEL) {
            if (duelGoAt > 0L) { if (duelTapped == 0L) duelTapped = System.currentTimeMillis() } else if (duelMessage == "Espera...") duelEarly = true
            return
        }
        val reach = with(density) { 34.dp.toPx() }
        bugs.firstOrNull { (Offset(it.x, it.y) - p).getDistance() < reach }?.let { hit ->
            bugs = bugs.filter { it.id != hit.id }
            pops = pops + (Offset(hit.x, hit.y) to System.currentTimeMillis())
            bugsYou++
        }
    }

    fun startGame(g: GameKind) {
        menuOpen = false
        game = g
        log { it.copy(games = it.games + 1) }
        val n = System.currentTimeMillis()
        when (g) {
            GameKind.PASS -> { say("Bora! Chuta pra mim!", 1_800); activity = Activity.PLAY }
            GameKind.HIDE -> {
                val candidates = currentObstacles.filter { (it.r - it.l) > widthPx * 1.4f && (it.b - it.t) > heightPx * .4f }
                val h = candidates.randomOrNull(brainRandom)
                if (h == null) { say("Não tem onde me esconder aqui...", 1_800); game = null; return }
                say("Conta até três!", 1_400)
                hideTries = 0
                scope.launch {
                    delay(900)
                    visible = 0f
                    hideSpot = h
                    hideAntennaX = h.l + 40f + brainRandom.nextFloat() * ((h.r - h.l) - 80f).coerceAtLeast(1f)
                    android.util.Log.d(PetDebug.TAG, "escondido em ${h.l.roundToInt()},${h.t.roundToInt()},${h.r.roundToInt()},${h.b.roundToInt()} (origem da camada ${rootBounds.left.roundToInt() - pos.value.x.roundToInt()},${rootBounds.top.roundToInt() - pos.value.y.roundToInt()})")
                }
            }
            GameKind.TAG -> { tagUntil = n + 25_000; say("Me pega se for capaz!", 1_800) }
            GameKind.JOKENPO -> { jkChoosing = true; jkGlitch = null; jkOutcome = null; say("Escolhe aí!", 1_600) }
            GameKind.BUGS -> { bugsUntil = n + 25_000; bugs = emptyList(); bugsYou = 0; bugsHim = 0; say("Invasão de bugs! Bora!", 1_600) }
            GameKind.DUEL -> say("Toca quando aparecer JÁ!", 1_800)
            GameKind.MEMORY -> say("Repete o que eu mostrar!", 1_600)
            GameKind.FLY -> say("Toca pra subir!", 1_600)
            GameKind.BUBBLES -> { bubblesUntil = n + 30_000; bubbles = emptyList(); say("Toca na tela pra soltar bolhas!", 2_200) }
        }
    }

    fun endHide(found: Boolean) {
        val h = hideSpot ?: return
        hideSpot = null
        game = null
        visible = 1f
        scope.launch { spotNear(Offset((h.l + h.r) / 2, h.b + heightPx / 2))?.let { pos.snapTo(it) } }
        say(if (found) HIDE_FOUND_LINES.random(brainRandom) else "Hehe, tava aqui!", 2_000)
        if (found) { reaction = Reaction.GIGGLE; reactionUntil = System.currentTimeMillis() + 900 }
    }

    fun playJokenpo(h: Hand) {
        jkCounting = true
        jkChoosing = false
        scope.launch {
            say("Jo...", 450); delay(450)
            say("Ken...", 450); delay(450)
            val gh = glitchHand(brainRandom)
            jkGlitch = gh
            val out = jokenpo(h, gh)
            jkOutcome = out
            jkCounting = false
            jkRevealUntil = System.currentTimeMillis() + 1_800
            say("Pô!", 800)
            delay(900)
            say(when (out) { Outcome.WIN -> JOKENPO_LOSE_LINES; Outcome.LOSE -> JOKENPO_WIN_LINES; Outcome.DRAW -> JOKENPO_DRAW_LINES }.random(brainRandom), 2_200)
            delay(1_000)
            if (out == Outcome.WIN) { reaction = Reaction.ANGRY; reactionUntil = System.currentTimeMillis() + 1_800 }
            if (out == Outcome.LOSE) { reaction = Reaction.GIGGLE; reactionUntil = System.currentTimeMillis() + 1_200 }
        }
    }

    LaunchedEffect(pendingGame) { pendingGame?.let { pendingGame = null; startGame(it) } }

    fun startProtest(at: Long) {
        protestTimes = recentProtests(protestTimes, at) + at
        protestStage = protestStage(protestTimes.size)
        when (protestStage) {
            ProtestStage.COMPLAINT -> { protestUntil = at + PROTEST_MS; protestSign = PROTEST_SIGNS.random(brainRandom) }
            ProtestStage.STRIKE -> { menuOpen = false; protestUntil = at + 60_000; protestSign = STRIKE_SIGNS.random(brainRandom); say(STRIKE_LINES.random(brainRandom), 2_000) }
            ProtestStage.REVOLUTION -> { protestUntil = at + 30_000; protestSign = REVOLUTION_SIGNS.random(brainRandom) }
        }
    }
    LaunchedEffect(pendingProtest) { if (pendingProtest > 0) startProtest(System.currentTimeMillis()) }
    LaunchedEffect(pendingPhoto) { if (pendingPhoto > 0) takePhoto() }

    fun onTap() {
        val tapNow = System.currentTimeMillis()
        // cutucou depois da travessura: ele arruma
        if (GlitchMischief.current.value != null) {
            GlitchMischief.current.value = null
            say(MISCHIEF_FIXED_LINES.random(brainRandom), 1_600)
            return
        }
        // de greve não brinca nem acha graça
        // (lê os estados na hora: o gesto guarda o onTap da primeira composição)
        if (tapNow < protestUntil && protestStage == ProtestStage.STRIKE) { say(STRIKE_LINES.random(brainRandom), 1_600); lastTapAt = 0L; return }
        // pega-pega: encostou, pegou
        if (game == GameKind.TAG) {
            game = null
            say(TAG_CAUGHT_LINES.random(brainRandom), 2_000)
            reaction = Reaction.GIGGLE; reactionUntil = tapNow + 900
            return
        }
        // toque duplo: menuzinho de brincadeiras
        if (game == null && tapNow - lastTapAt < 330) {
            lastTapAt = 0L
            reaction = null
            menuOpen = true
            return
        }
        lastTapAt = tapNow
        // de castigo, cutucada irrita; muitas seguidas e ele foge
        punishment?.takeIf { it.active(tapNow) }?.let { p ->
            val (np, fled) = p.poke(tapNow)
            punishment = np
            if (fled) {
                bond = (bond - .05f).coerceIn(0f, 1f); save()
                say("Fui! Castigo é o caramba!", 2_000)
                grudge = grudgeUntil(tapNow)
                scope.launch { fleeSignal++ }
            } else say(PUNISH_LINES.random(brainRandom), 1_400)
            return
        }
        val (next, r) = annoyance.tap(tapNow, bond)
        annoyance = next
        // com rancor, não acha graça: sem risadinha e sem ganhar vínculo
        if (r == Reaction.GIGGLE && holdsGrudge(grudge, tapNow)) {
            reaction = Reaction.SERIOUS
            reactionUntil = tapNow + 1_200
            say(GRUDGE_LINES.random(speechRandom), 1_600)
            return
        }
        val prevBond = bond
        bond = bondAfter(bond, r)
        save() // o vínculo é o que mais importa não perder
        if (r == Reaction.FLEE) grudge = grudgeUntil(tapNow)
        if (celebrates(prevBond, bond)) { fireworksUntil = tapNow + 2_400; say(CELEBRATE_LINES.random(brainRandom)) }
        if (r == Reaction.GIGGLE) {
            drives = onEvent(drives, GlitchEvent.GENTLE_TAP)
            // deu atenção a quem estava pedindo: missão cumprida
            if (activity == Activity.SEEK_ATTENTION) activity = Activity.WANDER
        }
        // cada vez que fica bravo conta para o protesto
        var spoke = false
        if (r == Reaction.ANGRY && reaction != Reaction.ANGRY) {
            val (m, protest) = protestMeter.angry(tapNow)
            protestMeter = m
            if (protest) { startProtest(tapNow); spoke = protestStage == ProtestStage.STRIKE }
            // às vezes, em vez de brigar, ele finge que morreu
            if (faints(bond, speechRandom)) {
                faintUntil = tapNow + 2_800
                say(DRAMA_LINES.random(speechRandom), 2_600)
                spoke = true
            }
        }
        pokeExpect = expectPoke(pokeExpect, poked = r != Reaction.GIGGLE)
        if (r != Reaction.GIGGLE) log { it.copy(poked = it.poked + 1) }
        if (r == Reaction.FLEE) log { it.copy(fled = it.fled + 1) }
        reaction = r
        reactionUntil = tapNow + if (r == Reaction.GIGGLE) 900 else 1_200
        if (!spoke) angerLine(r, speechRandom)?.let { say(it, 1_800) }
        if (r == Reaction.FLEE) scope.launch { fleeSignal++ }
    }

    // carinho perdoa: zera a irritação, mata tédio e carência e aproxima
    fun startPetting() {
        val wasMad = annoyance.levelAt(System.currentTimeMillis()) >= 2f
        petting = true
        annoyance = Annoyance()
        reaction = null
        marchUntil = 0L
        protestUntil = 0L
        faintUntil = 0L
        grudge = 0L
        punishment?.let { if (it.active(System.currentTimeMillis())) say("Desculpa... tô perdoado?", 1_800) }
        punishment = punishment?.forgiven()
        punishSign = null
        drives = onEvent(drives, GlitchEvent.GENTLE_TAP)
        pokeExpect = expectPoke(expectPoke(pokeExpect, false), false)
        log { it.copy(petted = it.petted + 1) }
        val prevBond = bond
        bond = bondAfterPetting(bond)
        if (celebrates(prevBond, bond)) fireworksUntil = System.currentTimeMillis() + 2_400
        save()
        say((if (wasMad) FORGIVE_LINES else PET_LINES).random(brainRandom))
        if (activity == Activity.SEEK_ATTENTION || activity == Activity.SULK) activity = Activity.WANDER
    }

    fun pickUp() {
        held = true
        // a placa do castigo aparece num canto livre, perto do topo à direita
        val a = currentArea
        val cw = with(density) { 96.dp.toPx() }
        val ch = with(density) { 48.dp.toPx() }
        corner = (0 until 30).mapNotNull { freeSpot(random, a, cw, ch, currentObstacles, null, 0f) }
            .minByOrNull { (it.first - (a.r - cw)) * (it.first - (a.r - cw)) + (it.second - a.t) * (it.second - a.t) }
            ?.let { Area(it.first, it.second, it.first + cw, it.second + ch) }
        say(PICKUP_LINES.random(brainRandom), 1_600)
    }

    // solta: voa com a velocidade do dedo, cai, quica e pousa (no chão ou em cima de algo); jogado forte fica tonto
    fun throwWith(v: Offset) {
        held = false
        if (v.getDistance() > 600f) log { it.copy(thrown = it.thrown + 1) }
        val c = corner
        corner = null
        overCorner = false
        val me = Area(pos.value.x, pos.value.y, pos.value.x + widthPx, pos.value.y + heightPx)
        if (c != null && c.intersects(me)) {
            // soltou em cima da placa: de castigo, de costas, ao lado dela
            punishment = Punishment.start(System.currentTimeMillis())
            punishSign = null
            corner = c
            scope.launch {
                val spot2 = spotNear(Offset(c.l + (c.r - c.l) / 2, c.b + heightPx / 2)) ?: pos.value
                pos.snapTo(spot2)
            }
            say(PUNISH_LINES.first(), 2_000)
            return
        }
        scope.launch {
            flying = true
            var b = Body(pos.value.x, pos.value.y, v.x.coerceIn(-4_000f, 4_000f), v.y.coerceIn(-4_000f, 4_000f))
            var impact = 0f
            var last = 0L
            var steps = 0
            android.util.Log.d(PetDebug.TAG, "arremesso v=${v.x.roundToInt()},${v.y.roundToInt()} de ${b.x.roundToInt()},${b.y.roundToInt()}")
            try {
                while (true) {
                    var settled = false
                    androidx.compose.runtime.withFrameNanos { nanos ->
                        val dt = if (last == 0L) 1f / 60f else ((nanos - last) / 1e9f).coerceIn(0f, 1f / 20f)
                        last = nanos
                        val a = currentArea
                        val supports = currentObstacles.filter { it.t - heightPx >= a.t }
                        val (nb, s2) = physicsStep(b, dt, a, supports, widthPx, heightPx)
                        if (b.vy > 0f && nb.vy <= 0f) impact = maxOf(impact, b.vy)
                        b = nb
                        settled = s2
                    }
                    flyVx = b.vx
                    pos.snapTo(Offset(b.x, b.y))
                    steps++
                    if (settled) break
                }
            } finally {
                flying = false
                android.util.Log.d(PetDebug.TAG, "pousou em ${b.x.roundToInt()},${b.y.roundToInt()} depois de $steps passos, impacto=${impact.roundToInt()}")
            }
            // caiu por cima de algo tocável (soltou ele já dentro da lista, por exemplo): sai dali num glitch
            val me = Area(pos.value.x, pos.value.y, pos.value.x + widthPx, pos.value.y + heightPx)
            if (currentObstacles.any { it.intersects(me) }) spot(null, 0f)?.let { glitchTo(it) }
            if (impact > 1_500f) {
                dizzyUntil = System.currentTimeMillis() + 2_500
                say(LAND_LINES.random(brainRandom))
                bond = (bond - .01f).coerceIn(0f, 1f)
            }
        }
    }
    if (battery.charging && visible > 0f && !hidden) {
        Canvas(Modifier.fillMaxSize()) {
            val from = Offset(pos.value.x + widthPx * .5f, pos.value.y + heightPx * .62f)
            val plugX = (from.x + widthPx * .8f).coerceAtMost(area.r - 6f)
            val to = Offset(plugX, floorUnder(plugX, from.y, area, obstacles))
            val cable = androidx.compose.ui.graphics.Path().apply {
                moveTo(from.x, from.y)
                cubicTo(from.x + widthPx * .9f, from.y, to.x - widthPx * .3f, to.y - heightPx * .6f, to.x, to.y - 14f)
            }
            drawPath(cable, color.copy(alpha = .8f), style = androidx.compose.ui.graphics.drawscope.Stroke(2.2f * lw, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            drawRect(color, Offset(to.x - 6f, to.y - 16f), androidx.compose.ui.geometry.Size(12f, 16f), style = androidx.compose.ui.graphics.drawscope.Stroke(2f * lw))
            // faiscazinha de energia correndo pelo cabo
            val k = (t % 1f)
            val spark = Offset(from.x + (to.x - from.x) * (1 - k), from.y + (to.y - from.y) * (1 - k) * (1 - k))
            drawCircle(color, 3f, spark)
        }
    }

    // jardim: plantinha e torrinha, em cima da lista
    val gardenGround = currentNamed["list"]?.let { Offset(it.r - widthPx * .9f, it.t - 4f) }
    if (gardenGround != null && ((!calmGarden && (plantWatered > 0 || towerBlocks > 0)) || show?.kind == Show.WATER || show?.kind == Show.BLOCKS)) {
        Canvas(Modifier.fillMaxSize()) {
            drawGarden(color, plantWatered, plantWilted(lastWaterDay, todayInt()), towerBlocks, gardenGround, heightPx / 170f * .8f)
        }
    }
    // grafite: fica na tela até você esfregar o dedo por cima
    graffiti?.let { g ->
        var rubbed by remember(g) { mutableFloatStateOf(0f) }
        Canvas(Modifier.fillMaxSize()) { drawGraffiti(color, g, graffitiAt, graffitiShown) }
        Box(
            Modifier
                .offset { IntOffset(graffitiAt.left.roundToInt(), graffitiAt.top.roundToInt()) }
                .size(with(density) { graffitiAt.width.toDp() }, with(density) { graffitiAt.height.toDp() })
                .pointerInput(g) {
                    awaitEachGesture {
                        awaitFirstDown().consume()
                        while (true) {
                            val ch = awaitPointerEvent().changes.first()
                            rubbed += ch.positionChange().getDistance()
                            ch.consume()
                            if (!ch.pressed) break
                            if (erased(rubbed, graffitiAt.width)) { graffiti = null; say("Ei, era arte!", 1_400); break }
                        }
                    }
                },
        )
    }

    // ---- por baixo do Glitch: rede de toques das brincadeiras, post-its, bolhas, antena escondida ----
    val catching = menuOpen || game == GameKind.HIDE || game == GameKind.TAG || game == GameKind.BUBBLES || game == GameKind.BUGS || game == GameKind.DUEL || game == GameKind.FLY
    if (catching) {
        TouchCatcher(
            onDown = { p ->
                when {
                    menuOpen -> menuOpen = false
                    game == GameKind.HIDE -> hideSpot?.let { h ->
                        when (guessHide(h, p.x, p.y)) {
                            HideGuess.FOUND -> endHide(true)
                            HideGuess.WARM -> { hint = p to HIDE_WARM_LINES.random(brainRandom); hintUntil = System.currentTimeMillis() + 1_200; hideTries++ }
                            HideGuess.COLD -> { hint = p to HIDE_COLD_LINES.random(brainRandom); hintUntil = System.currentTimeMillis() + 1_200; hideTries++ }
                        }
                        if (game == GameKind.HIDE && hideTries >= 5) endHide(false)
                    }
                    game == GameKind.TAG -> finger = p
                    game == GameKind.BUGS || game == GameKind.DUEL || game == GameKind.FLY -> gameTap(p)
                    game == GameKind.BUBBLES -> {
                        val r = with(density) { (14 + brainRandom.nextInt(10)).dp.toPx() }
                        bubbles = (bubbles + Bubble(++bubbleSeq, p.x, p.y, r, System.currentTimeMillis())).takeLast(12)
                    }
                }
            },
            onMove = { p -> if (game == GameKind.TAG) finger = p },
            onUp = { finger = null },
        )
    }

    notes.forEach { note ->
        key(note.id) {
            var drag by remember { mutableStateOf(Offset.Zero) }
            PostIt(
                note.text,
                color,
                Modifier
                    .offset { IntOffset((note.x + drag.x).roundToInt(), (note.y + drag.y).roundToInt()) }
                    .graphicsLayer { rotationZ = note.tilt }
                    .pointerInput(note.id) {
                        awaitEachGesture {
                            awaitFirstDown().consume()
                            while (true) {
                                val ch = awaitPointerEvent().changes.first()
                                // lê antes de consumir: depois de consumido o movimento vem zerado
                                val moved = ch.positionChange()
                                ch.consume()
                                if (!ch.pressed) break
                                drag += moved
                            }
                            // arrastou para longe: joga fora; pouquinho: só muda de lugar
                            if (drag.getDistance() > with(density) { 60.dp.toPx() }) {
                                notes = notes.filter { it.id != note.id }
                                say(POSTIT_REMOVED_LINES.random(brainRandom), 1_600)
                            }
                        }
                    },
            )
        }
    }

    if (bugs.isNotEmpty()) {
        Canvas(Modifier.fillMaxSize()) {
            val s = with(density) { 7.dp.toPx() }
            bugs.forEach { drawBug(color, Offset(it.x, it.y), s, t, lw) }
        }
    }
    fly?.let { s -> flyWorld?.let { w ->
        Canvas(Modifier.fillMaxSize()) {
            drawFlyWalls(color, s, w, lw)
            drawShowProps(color, ShowRun(Show.JETPACK, 0L), 0f, t, pos.value, widthPx, heightPx, Offset.Zero)
        }
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
            androidx.compose.material3.Text(
                "${s.score}" + if (flyBest > 0) "  ·  recorde $flyBest" else "",
                color = color,
                modifier = Modifier.padding(top = 56.dp).background(androidx.compose.ui.graphics.Color(0xCC0C0C0E), androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
    } }
    if (game == GameKind.MEMORY) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            MemoryPanel(color, memoryLit, memoryYourTurn, memorySeq.size, memoryBest, onPad = { pad ->
                memoryTapAt = System.currentTimeMillis()
                when (memoryTap(memorySeq, memoryPos, pad)) {
                    MemoryTap.CORRECT -> memoryPos++
                    MemoryTap.ROUND_DONE -> memoryYourTurn = false
                    MemoryTap.WRONG -> memoryLost = true
                }
            })
        }
    }
    if (game == GameKind.DUEL && duelMessage.isNotEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            DuelPanel(color, duelMessage, duelYou, duelHim, duelBest)
        }
    }
    if (bubbles.isNotEmpty() || pops.isNotEmpty()) {
        Canvas(Modifier.fillMaxSize()) {
            bubbles.forEach { drawBubble(color, Offset(it.x, it.y), it.r, lw) }
            pops.forEach { (c, at) -> drawPop(color, c, with(density) { 18.dp.toPx() }, ((now - at) / 400f).coerceIn(0f, 1f), lw) }
        }
    }

    hideSpot?.let { h ->
        val gap = with(density) { 8.dp.toPx() }
        val cw = with(density) { 60.dp.toPx() }
        val chh = with(density) { 40.dp.toPx() }
        Canvas(
            Modifier
                .offset { IntOffset((hideAntennaX - cw / 2).roundToInt(), (h.t + gap - chh).roundToInt()) }
                .size(with(density) { cw.toDp() }, with(density) { chh.toDp() }),
        ) { drawPeekingAntenna(color, Offset(size.width / 2, size.height), density.density / 2.6f * 1.4f, t, lw) }
    }

    // os efeitos longos leem daqui os valores de agora, não os do começo
    val asleepNow by rememberUpdatedState(asleep)
    val danceNow by rememberUpdatedState(mood == Mood.DANCING)
    val sadNow by rememberUpdatedState(activity == Activity.SAD && mood == Mood.NORMAL)
    val angryNow by rememberUpdatedState(menacing || marching || protesting)
    val showState by rememberUpdatedState(show)
    val gameNow by rememberUpdatedState(game)
    val gardenNow by rememberUpdatedState(gardenGround)
    // triste com ele do lado: agradece uma vez; bravo: o bichinho sai correndo
    LaunchedEffect(sad) {
        if (!sad || buddy == Buddy.NONE) { buddyConsoled = false; return@LaunchedEffect }
        delay(6_000)
        if (!buddyConsoled) { buddyConsoled = true; say(BUDDY_CONSOLE_LINES.random(brainRandom), 2_400); buddyHeartUntil = System.currentTimeMillis() + 1_800 }
    }
    LaunchedEffect(menacing) {
        if (menacing && buddy != Buddy.NONE) buddySpeak(BUDDY_VOICE[buddy]?.last() ?: "Bip?")
    }
    // bichinho: segue o Glitch e de vez em quando escapa
    LaunchedEffect(buddy, resumed) {
        if (buddy == Buddy.NONE || !resumed) return@LaunchedEffect
        var strayUntil = 0L
        var stray = Offset.Zero
        var nextStray = System.currentTimeMillis() + 20_000
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { nanos ->
                val dt = if (last == 0L) .016f else ((nanos - last) / 1e9f).coerceIn(0f, .05f)
                last = nanos
                val n = System.currentTimeMillis()
                if (n > nextStray) {
                    nextStray = n + brainRandom.nextLong(25_000, 60_000)
                    strayUntil = n + 3_000
                    val a = currentArea
                    stray = Offset(a.l + brainRandom.nextFloat() * (a.r - a.l), pos.value.y + heightPx * .5f)
                    if (!asleep && brainRandom.nextBoolean()) say(BUDDY_LINES.random(brainRandom), 1_500)
                }
                val side = if (facingLeft) -1f else 1f
                val run = showState?.takeIf { it.active(n) }
                val towerAt = gardenNow?.plus(Offset(with(density) { 12.dp.toPx() }, -heightPx * .15f)) ?: pos.value
                val a = currentArea
                val touch = GlitchSignals.lastTouch
                val bug = if (gameNow == GameKind.BUGS) bugs.minByOrNull { (Offset(it.x, it.y) - (if (buddyAt.isSpecified) buddyAt else pos.value)).getDistance() } else null
                if (bug != null && buddyAt.isSpecified && (Offset(bug.x, bug.y) - buddyAt).getDistance() < heightPx * .22f) {
                    // ajuda a caçar
                    bugs = bugs.filter { it.id != bug.id }
                    pops = pops + (Offset(bug.x, bug.y) to n)
                    bugsHim++
                }
                val goal = when {
                    run != null -> duoGoal(run, run.progress(n), pos.value, widthPx, heightPx, side, fetchLanding, towerAt, n)
                        ?: pos.value + Offset(widthPx * .5f + side * widthPx * .9f, heightPx * .12f)
                    bug != null -> Offset(bug.x, bug.y)
                    // ele bravo: foge para a borda de medo
                    angryNow -> Offset(if (pos.value.x > (a.l + a.r) / 2) a.l + 40f else a.r - 40f, pos.value.y + heightPx * .7f)
                    // ele triste: fica colado, consolando
                    sadNow -> pos.value + Offset(widthPx * .5f + side * widthPx * .6f, heightPx * .85f)
                    // acordado, vai atrás do seu dedo
                    !asleepNow && gameNow == null && touch != null && fingerAttracts(touch.third, n) -> Offset(touch.first, touch.second) - (rootBounds.topLeft - pos.value + Offset(pad, pad)) // toque vem da tela inteira; a camada dele começa abaixo da barra de status
                    asleepNow -> pos.value + Offset(widthPx * .5f + side * widthPx * .85f, heightPx * .94f)
                    danceNow && n >= strayUntil -> pos.value + Offset(widthPx * .5f + side * widthPx * (.95f + .25f * sin(n / 160f)), heightPx * (.5f + .18f * kotlin.math.abs(sin(n / 200f))))
                    n < strayUntil -> stray
                    buddy == Buddy.DRONE -> pos.value + Offset(widthPx * .5f + side * widthPx * .9f, heightPx * .12f + sin(n / 400f) * 8f)
                    else -> pos.value + Offset(widthPx * .5f - side * widthPx * .95f, heightPx * .94f)
                }
                buddyAt = if (buddyAt.isSpecified) buddyAt + (goal - buddyAt) * (dt * 3.5f).coerceAtMost(1f) else goal
            }
        }
    }
    if (buddy != Buddy.NONE && buddyAt.isSpecified && visible > 0f && !hidden) {
        val napping = asleep || show?.kind == Show.NAP_TOGETHER
        Canvas(Modifier.fillMaxSize()) {
            drawBuddy(color, buddy, buddyAt, heightPx / 170f, t, napping, lw)
            val u = heightPx / 170f
            val run = showNow
            if (run?.kind == Show.FETCH) drawCircle(color, 7f * u, fetchBall(showK, pos.value + Offset(widthPx * .8f, heightPx * .5f), fetchLanding, buddyAt), style = androidx.compose.ui.graphics.drawscope.Stroke(2f * u * lw))
            if (run?.kind == Show.BUDDY && buddySteals(run.variant)) drawRoundRect(color, buddyAt + Offset(10f * u, -3f * u), androidx.compose.ui.geometry.Size(16f * u, 6f * u))
        }
        // voz e coração do bichinho, em balãozinho próprio
        val heart = now < buddyHeartUntil
        val voice = buddyLine?.takeIf { now < buddyLineUntil }
        if (heart || voice != null) {
            androidx.compose.material3.Text(
                if (heart) "♥" else voice.orEmpty(),
                color = color,
                fontSize = 12.sp,
                modifier = Modifier
                    .offset { IntOffset((buddyAt.x - 24f).roundToInt(), (buddyAt.y - with(density) { 44.dp.toPx() }).roundToInt()) }
                    .background(androidx.compose.ui.graphics.Color(0xCC0C0C0E), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        // tocar no bichinho: só quando ele não está em cima de nada tocável
        val r = with(density) { 26.dp.toPx() }
        if (game == null && currentObstacles.none { it.intersects(Area(buddyAt.x - r, buddyAt.y - r, buddyAt.x + r, buddyAt.y + r)) }) {
            Box(
                Modifier
                    .offset { IntOffset((buddyAt.x - r).roundToInt(), (buddyAt.y - r).roundToInt()) }
                    .size(52.dp)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown().consume()
                            var rubbed = 0f
                            while (true) {
                                val ch = awaitPointerEvent().changes.first()
                                rubbed += ch.positionChange().getDistance()
                                ch.consume()
                                if (!ch.pressed) break
                            }
                            if (buddyPetted(rubbed, r)) {
                                // carinho nele: corações, e o Glitch às vezes fica com ciúme
                                buddyHeartUntil = System.currentTimeMillis() + 1_800
                                bond = (bond + .005f).coerceIn(0f, 1f)
                                if (brainRandom.nextInt(3) == 0) say(BUDDY_JEALOUS_LINES.random(brainRandom), 1_800)
                            } else {
                                buddySpeak(BUDDY_VOICE[buddy]?.random(brainRandom) ?: "Bip!")
                                if (brainRandom.nextInt(4) == 0) say(BUDDY_TAP_LINES.random(brainRandom), 1_500)
                            }
                        }
                    },
            )
        }
    }
    // o visitante e o que ele traz
    visitor?.let { v ->
        visitNow?.let { scene ->
            val at = guestPlace(scene, visitK, guestStand, guestEdge)
            val guestPose = (visitPair?.second ?: RobotPose()).copy(accessory = visibleAccessory(v.accessory, visitPair?.second?.face ?: Face.NORMAL))
            Canvas(Modifier.fillMaxSize()) {
                translate(at.x, at.y) { drawRobot(v.color, guestPose, lw, heightPx) }
                if (v.buddy != Buddy.NONE) drawBuddy(v.color, v.buddy, at + Offset(if (at.x > pos.value.x) widthPx * 1.05f else -widthPx * .1f, heightPx * .92f), heightPx / 170f, t, lw = lw)
                if (scene == VisitScene.BALL) drawCircle(color, heightPx * .05f, ballAt(visitK, pos.value + Offset(widthPx / 2, heightPx * .9f), at + Offset(widthPx / 2, heightPx * .9f), heightPx))
            }
            androidx.compose.material3.Text(
                v.name,
                color = v.color,
                fontSize = 11.sp,
                modifier = Modifier.offset { IntOffset(at.x.roundToInt(), (at.y + heightPx + 2f).roundToInt()) },
            )
            guestLine?.takeIf { now < guestLineUntil }?.let { line ->
                SpeechBubble(
                    text = line,
                    color = v.color,
                    modifier = Modifier.layout { m, c ->
                        val p = m.measure(c.copy(minWidth = 0, minHeight = 0))
                        layout(p.width, p.height) {
                            val x = (at.x + widthPx / 2 - p.width / 2).coerceIn(area.l, (area.r - p.width).coerceAtLeast(area.l))
                            // com o outro falando, sobe uma linha para os balões não se cobrirem
                            val above = if (speech != null && now < speechUntil) p.height + with(density) { 6.dp.toPx() } else 0f
                            p.place(x.roundToInt(), (at.y - p.height - above).coerceAtLeast(0f).roundToInt())
                        }
                    },
                )
            }
        }
    }
    showNow?.let { run ->
        Canvas(Modifier.fillMaxSize()) {
            if (run.kind == Show.CLONES) drawBattle(color, run, showK, t, pos.value, widthPx, heightPx, battleLeft, battleRight, battleEvil, lw)
            else drawShowProps(color, run, showK, t, pos.value, widthPx, heightPx, gardenGround ?: Offset.Zero)
        }
    }

    // placa do castigo (aparece com ele no colo e fica durante o castigo); vem antes dele para não cobri-lo
    corner?.let { c ->
        PunishCorner(
            color = color,
            highlighted = overCorner,
            modifier = Modifier.offset { IntOffset(c.l.roundToInt(), c.t.roundToInt()) },
        )
    }

    Box(
        contentAlignment = androidx.compose.ui.Alignment.Center,
        modifier = Modifier
            .offset { IntOffset((pos.value.x - pad).roundToInt(), (pos.value.y - pad).roundToInt()) }
            .size(with(density) { widthPx.toDp() } + TOUCH_PAD * 2, ROBOT_HEIGHT + TOUCH_PAD * 2)
            .alpha(visible)
            .onPlaced { placedAt = it.positionInParent() }
            .then(if (hidden) Modifier.size(0.dp) else Modifier)
            .onGloballyPositioned { rootBounds = it.boundsInRoot() }
            .pointerInput(Unit) {
                val slop = viewConfiguration.touchSlop
                val minRub = 12.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    // nos jogos de reflexo o toque vale ao encostar, não ao soltar
                    if (game == GameKind.DUEL || game == GameKind.BUGS || game == GameKind.FLY) {
                        gameTap(placedAt + down.position)
                        while (awaitPointerEvent().changes.any { c -> c.consume(); c.pressed }) Unit
                        return@awaitEachGesture
                    }
                    if (flinches(pokeExpect) && game == null) {
                        flinchUntil = System.currentTimeMillis() + 450
                        if (brainRandom.nextInt(3) == 0) say("Ai! Não!", 900)
                    }
                    val grab = down.position
                    val xs = mutableListOf(placedAt.x + down.position.x)
                    var travel = 0f
                    // soltou logo = toque; mexeu o dedo = carinho (quando vai e volta); parado ~0,65 s = pegar no colo
                    val early = withTimeoutOrNull(PICKUP_HOLD_MS) {
                        while (true) {
                            val ev = awaitPointerEvent()
                            // lê antes de consumir: depois de consumido o movimento vem zerado
                            val moved = ev.changes.first().positionChange().getDistance()
                            ev.changes.forEach { it.consume() }
                            if (ev.changes.count { it.pressed } >= 2) return@withTimeoutOrNull "spin"
                            val ch = ev.changes.first()
                            if (!ch.pressed) return@withTimeoutOrNull "up"
                            xs += placedAt.x + ch.position.x
                            travel += moved
                            if (countReversals(xs, minRub) >= 2) return@withTimeoutOrNull "rub"
                            if (travel >= slop) return@withTimeoutOrNull "move"
                        }
                        @Suppress("UNREACHABLE_CODE") ""
                    }
                    when {
                        early == "spin" -> {
                            // pião: gira com dois dedos; solto, continua girando e pode ficar tonto
                            spinning = true
                            var lastAngle: Float? = null
                            var lastT = 0L
                            var w = 0f // graus por ms
                            var total = 0f
                            while (true) {
                                val ev = awaitPointerEvent()
                                ev.changes.forEach { it.consume() }
                                val pressed = ev.changes.filter { it.pressed }
                                if (pressed.size < 2) break
                                val d = pressed[1].position - pressed[0].position
                                val ang = Math.toDegrees(kotlin.math.atan2(d.y, d.x).toDouble()).toFloat()
                                lastAngle?.let { la ->
                                    var delta = ang - la
                                    if (delta > 180f) delta -= 360f
                                    if (delta < -180f) delta += 360f
                                    spinDeg += delta
                                    total += kotlin.math.abs(delta)
                                    val dtMs = (pressed[0].uptimeMillis - lastT).coerceAtLeast(1)
                                    w = delta / dtMs
                                }
                                lastAngle = ang
                                lastT = pressed[0].uptimeMillis
                            }
                            val spinW = w
                            val spinTotal = total
                            scope.launch {
                                say("Wheee!", 1_000)
                                var ww = spinW
                                var spun = spinTotal
                                var last = 0L
                                while (kotlin.math.abs(ww) > .02f) {
                                    androidx.compose.runtime.withFrameNanos { nanos ->
                                        val dt = if (last == 0L) 16f else ((nanos - last) / 1e6f).coerceIn(0f, 50f)
                                        last = nanos
                                        spinDeg += ww * dt
                                        spun += kotlin.math.abs(ww * dt)
                                        ww *= .97f
                                    }
                                }
                                spinDeg = 0f
                                spinning = false
                                if (spun > 540f) { dizzyUntil = System.currentTimeMillis() + 2_500; say(LAND_LINES.random(brainRandom)) }
                            }
                            while (true) {
                                val ev = awaitPointerEvent()
                                ev.changes.forEach { it.consume() }
                                if (ev.changes.none { it.pressed }) break
                            }
                        }
                        early == "up" -> if (travel < slop) onTap()
                        early == "rub" || early == "move" -> {
                            // mexendo: vira carinho quando vai e volta
                            if (early == "rub") startPetting()
                            val began = down.uptimeMillis
                            var endX = xs.last()
                            var ended = began
                            while (true) {
                                val ch = awaitPointerEvent().changes.first()
                                ch.consume()
                                ended = ch.uptimeMillis
                                if (!ch.pressed) break
                                endX = placedAt.x + ch.position.x
                                xs += endX
                                if (!petting && countReversals(xs, minRub) >= 2) startPetting()
                                if (now >= tickleUntil && isTickle(countReversals(xs, minRub), ended - began)) {
                                    tickleUntil = System.currentTimeMillis() + 1_600
                                    say(TICKLE_LINES.random(brainRandom), 1_600)
                                    bond = (bond + .01f).coerceIn(0f, 1f)
                                }
                            }
                            petting = false
                            // peteleco: ele tropeça para o lado e cai
                            if (isFlick(endX - xs.first(), ended - began, with(density) { 56.dp.toPx() })) {
                                shoveDir = if (endX > xs.first()) 1f else -1f
                                shoveUntil = System.currentTimeMillis() + 1_500
                                say(SHOVE_LINES.random(brainRandom), 1_600)
                                val c = currentArea
                                val x = (pos.value.x + shoveDir * widthPx).coerceIn(c.l, c.r - widthPx)
                                if (pathClear(pos.value.x to pos.value.y, x to pos.value.y, widthPx, heightPx, currentObstacles)) {
                                    scope.launch { pos.animateTo(Offset(x, pos.value.y), tween(300)) }
                                }
                            }
                        }
                        else -> {
                            // segurou parado: pega no colo, arrasta e, ao soltar, joga
                            pickUp()
                            val tracker = VelocityTracker()
                            while (true) {
                                val ch = awaitPointerEvent().changes.first()
                                ch.consume()
                                val finger = placedAt + ch.position
                                tracker.addPosition(ch.uptimeMillis, finger)
                                if (!ch.pressed) break
                                val p = finger - grab + Offset(pad, pad)
                                val a = currentArea
                                overCorner = corner?.intersects(Area(p.x, p.y, p.x + widthPx, p.y + heightPx)) == true
                                scope.launch {
                                    pos.snapTo(Offset(p.x.coerceIn(a.l, a.r - widthPx), p.y.coerceIn(a.t, a.b - heightPx)))
                                }
                            }
                            val v = tracker.calculateVelocity()
                            throwWith(Offset(v.x, v.y))
                        }
                    }
                }
            },
    ) {
        val dressed = pose.copy(lookX = lookX, lookY = lookY, belly = belly, accessory = wornAccessory(face, hour, dnd, headset, special, accessory, weather))
        Canvas(Modifier.size(with(density) { widthPx.toDp() }, ROBOT_HEIGHT)) { drawRobot(color, dressed, lw) }
    }

    // bola: dá para pegar e jogar também
    ball?.let { b ->
        Box(
            Modifier
                .offset { IntOffset(b.x.roundToInt(), b.y.roundToInt()) }
                .size(with(density) { ballPx.toDp() })
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        ballHeld = true
                        lastBallTouch = System.currentTimeMillis()
                        if (game != GameKind.PASS) say("Ei, devolve a bola!", 1_500)
                        val tracker = VelocityTracker()
                        var cur = ball ?: return@awaitEachGesture
                        while (true) {
                            val ch = awaitPointerEvent().changes.first()
                            ch.consume()
                            val finger = Offset(cur.x, cur.y) + ch.position
                            tracker.addPosition(ch.uptimeMillis, finger)
                            if (!ch.pressed) break
                            val np = finger - Offset(ballPx / 2, ballPx / 2)
                            cur = cur.copy(x = np.x.coerceIn(currentArea.l, currentArea.r - ballPx), y = np.y.coerceIn(currentArea.t, currentArea.b - ballPx), vx = 0f, vy = 0f)
                            ball = cur
                        }
                        val v = tracker.calculateVelocity()
                        ball = cur.copy(vx = v.x.coerceIn(-4_000f, 4_000f), vy = v.y.coerceIn(-4_000f, 4_000f))
                        ballHeld = false
                    }
                },
        ) {
            Canvas(Modifier.size(with(density) { ballPx.toDp() })) { drawBall(color, ballSpin, lw) }
        }
    }

    // bolinhas de papel da revolução
    if (papers.isNotEmpty()) {
        Canvas(Modifier.fillMaxSize()) {
            papers.forEach { b ->
                val c = Offset(b.x, b.y)
                // papel amassado: contorno irregular com vincos, girando no ar
                val r = 8.dp.toPx()
                val turn = (b.x + b.y) * .012f
                fun at(deg: Float, k: Float): Offset {
                    val a = Math.toRadians(deg.toDouble()).toFloat() + turn
                    return c + Offset(kotlin.math.cos(a) * r * k, sin(a) * r * k)
                }
                val edge = listOf(1f, .72f, .98f, .68f, 1.05f, .76f, .92f, .7f, 1f)
                val ball = androidx.compose.ui.graphics.Path().apply {
                    edge.forEachIndexed { i, k ->
                        val o = at(i * 40f, k)
                        if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y)
                    }
                    close()
                }
                drawPath(ball, Color(0xE60C0C0E))
                drawPath(ball, color, style = androidx.compose.ui.graphics.drawscope.Stroke(1.6.dp.toPx() * lw))
                drawLine(color, at(200f, .7f), at(20f, .3f), 1.1.dp.toPx() * lw)
                drawLine(color, at(20f, .3f), at(95f, .75f), 1.1.dp.toPx() * lw)
                drawLine(color, at(20f, .3f), at(300f, .7f), 1.1.dp.toPx() * lw)
            }
        }
    }

    // fogos em cima dele
    if (celebrating) {
        val fw = with(density) { 280.dp.toPx() }
        val fh = with(density) { 240.dp.toPx() }
        val p = 1f - ((fireworksUntil - now) / 2_400f).coerceIn(0f, 1f)
        Canvas(
            Modifier
                .offset { IntOffset((pos.value.x + widthPx / 2 - fw / 2).roundToInt(), (pos.value.y - fh * .8f).roundToInt()) }
                .size(with(density) { fw.toDp() }, with(density) { fh.toDp() }),
        ) { drawFireworks(color, p, lw) }
    }

    // cartaz: protesto (sempre) e castigo (de vez em quando)
    val signText = if (protesting) protestSign else if (punished) punishSign else null
    val signShown = signText != null && visible > 0f
    if (signShown) {
        val cx = pos.value.x + widthPx / 2
        val top = pos.value.y + with(density) { 6.dp.toPx() }
        Placard(
            signText!!,
            color,
            Modifier.onSizeChanged { signHeight = it.height }.layout { m, c ->
                val pl = m.measure(c.copy(minWidth = 0, minHeight = 0))
                layout(pl.width, pl.height) {
                    val x = (cx - pl.width / 2).coerceIn(area.l, (area.r - pl.width).coerceAtLeast(area.l))
                    pl.place(x.roundToInt(), (top - pl.height).coerceAtLeast(0f).roundToInt())
                }
            },
        )
    }

    if (menuOpen) {
        val cx = pos.value.x + widthPx / 2
        val top = pos.value.y
        PlayMenu(
            color = color,
            onPick = { startGame(it) },
            onClose = { menuOpen = false },
            talkTo = aiPackage?.let { p -> runCatching { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(p, 0)).toString() }.getOrNull() },
            onTalk = { menuOpen = false; GlitchTalk.requests.tryEmit(Unit) },
            onPhoto = { takePhoto() },
            modifier = Modifier.layout { m, c ->
                val pl = m.measure(c.copy(minWidth = 0, minHeight = 0))
                layout(pl.width, pl.height) {
                    val x = (cx - pl.width / 2).coerceIn(area.l, (area.r - pl.width).coerceAtLeast(area.l))
                    pl.place(x.roundToInt(), menuTop(top, heightPx, pl.height.toFloat(), area).roundToInt())
                }
            },
        )
    }

    if (game == GameKind.JOKENPO && !jkCounting) {
        val cx = pos.value.x + widthPx / 2
        val below = pos.value.y + heightPx + with(density) { 10.dp.toPx() }
        JokenpoPanel(
            color = color,
            choosing = jkChoosing,
            onHand = { playJokenpo(it) },
            onAgain = { jkChoosing = true; jkGlitch = null; say("Agora vai!", 1_200) },
            onStop = { game = null; say("Valeu pelo jogo!", 1_600) },
            modifier = Modifier.layout { m, c ->
                val pl = m.measure(c.copy(minWidth = 0, minHeight = 0))
                layout(pl.width, pl.height) {
                    val x = (cx - pl.width / 2).coerceIn(area.l, (area.r - pl.width).coerceAtLeast(area.l))
                    val y = below.coerceAtMost(area.b - pl.height)
                    pl.place(x.roundToInt(), y.roundToInt())
                }
            },
        )
    }

    hint?.takeIf { now < hintUntil }?.let { (p, text) ->
        SpeechBubble(
            text = text,
            color = color,
            modifier = Modifier.layout { m, c ->
                val pl = m.measure(c.copy(minWidth = 0, minHeight = 0))
                layout(pl.width, pl.height) {
                    val x = (p.x - pl.width / 2).coerceIn(area.l, (area.r - pl.width).coerceAtLeast(area.l))
                    pl.place(x.roundToInt(), (p.y - pl.height - 20f).coerceAtLeast(0f).roundToInt())
                }
            },
        )
    }

    val line = speech
    if (line != null && now < speechUntil && visible > 0f) {
        val cx = pos.value.x + widthPx / 2
        // com cartaz na mão, o balão sai acima do cartaz
        val top = if (signShown) pos.value.y + with(density) { 6.dp.toPx() } - signHeight else pos.value.y - with(density) { 2.dp.toPx() }
        SpeechBubble(
            text = line,
            color = color,
            modifier = Modifier.layout { m, c ->
                val p = m.measure(c.copy(minWidth = 0, minHeight = 0))
                layout(p.width, p.height) {
                    // centralizado em cima da cabeça, sem sair da tela
                    val x = (cx - p.width / 2).coerceIn(area.l, (area.r - p.width).coerceAtLeast(area.l))
                    // sem espaço acima do cartaz: vai para baixo dele, para não cobrir
                    val y = if (signShown && top - p.height < 0f) pos.value.y + heightPx else (top - p.height).coerceAtLeast(0f)
                    p.place(x.roundToInt(), y.roundToInt())
                }
            },
        )
    }
}

/** Balãozinho com rabinho apontando para a cabeça dele; na cor do traço, fundo escuro para dar leitura. */
@Composable
private fun SpeechBubble(text: String, color: Color, modifier: Modifier = Modifier) {
    val border = LocalBorderWidth.current
    androidx.compose.foundation.layout.Column(modifier, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        androidx.compose.material3.Text(
            text,
            color = color,
            fontSize = 13.sp,
            maxLines = 4,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier
                .widthIn(max = 240.dp)
                .glitchBox(color, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 7.dp),
        )
        Canvas(Modifier.size(12.dp, 6.dp)) {
            val tail = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2, size.height); close()
            }
            drawPath(tail, BOX_FILL)
            drawLine(color.copy(alpha = .45f), Offset(0f, 0f), Offset(size.width / 2, size.height), border.toPx())
            drawLine(color.copy(alpha = .45f), Offset(size.width, 0f), Offset(size.width / 2, size.height), border.toPx())
        }
    }
}

/**
 * Uma pose por passo de dança, no tempo [t] (segundos).
 * Braços: ângulo positivo gira para a esquerda da tela. O esquerdo abre para fora com +, o direito com -.
 */
private fun dancePose(move: DanceMove, face: Face, facingLeft: Boolean, t: Float): RobotPose {
    val beat = sin(t * 7f)
    return when (move) {
        // o de sempre: balança de um lado pro outro
        DanceMove.SWAY -> RobotPose(face, facingLeft, legL = 8f * beat, legR = -8f * beat, armL = -35f + 30f * beat, armR = 35f + 30f * beat, tilt = 6f * beat, lift = 3f * abs(beat), phase = t)
        // robô clássico: poses travadas que trocam em "tique"
        DanceMove.ROBOT -> {
            val k = (t * 3f).toInt() % 4
            val (aL, aR, head) = when (k) { 0 -> Triple(90f, 0f, -12f); 1 -> Triple(0f, -90f, 12f); 2 -> Triple(90f, -90f, 0f); else -> Triple(0f, 0f, -12f) }
            RobotPose(face, facingLeft, armL = aL, armR = aR, headTilt = head, phase = t)
        }
        // pulinho: um tempinho no chão, sobe abrindo os braços em V (para fora, sem passar na cabeça)
        DanceMove.HOP -> {
            val p = (t * 1.6f) % 1f
            val air = if (p < .55f) sin(p / .55f * Math.PI.toFloat()) else 0f
            RobotPose(face, facingLeft, legL = 8f * air, legR = -8f * air, armL = 25f + 105f * air, armR = -25f - 105f * air, lift = 16f * air, phase = t)
        }
        // dois passos pra um lado e volta, estalando as garrinhas
        DanceMove.SIDESTEP -> {
            val side = sin(t * 2.5f)
            val snap = if ((t * 4).toInt() % 2 == 0) 20f else -5f
            RobotPose(face, side < 0, legL = 14f * beat, legR = -14f * beat, armL = -20f - snap, armR = 20f + snap, offsetX = 26f * side, lift = 2f * abs(beat), phase = t)
        }
        // moonwalk: o deslize é a posição de verdade (RobotPet); aqui só as pernas "andando" para a frente
        // e os braços soltos, como se fosse para o lado que ele olha
        DanceMove.MOONWALK -> {
            val step = sin(t * 8f)
            RobotPose(face, facingLeft, legL = 20f * step, legR = -20f * step, armL = -14f * step, armR = 14f * step, lift = 1.5f * abs(step), phase = t)
        }
        // giro: vira, fica um instante de costas e volta (frente 0.6s, vira 0.3s, costas 0.6s, vira 0.3s)
        DanceMove.SPIN -> {
            val p = (t % 1.8f) / 1.8f
            val (width, back) = when {
                p < .33f -> 1f to false
                p < .5f -> kotlin.math.cos((p - .33f) / .17f * Math.PI.toFloat()).let { kotlin.math.abs(it) to (it < 0f) }
                p < .83f -> 1f to true
                else -> kotlin.math.cos((p - .83f) / .17f * Math.PI.toFloat()).let { kotlin.math.abs(it) to (it > 0f) }
            }
            RobotPose(face, facingLeft, armL = -60f, armR = 60f, spin = width, back = back, lift = 3f * abs(beat), antennaOn = (t * 6).toInt() % 2 == 0, phase = t)
        }
        // headbang: só a cabeça vai e volta forte; equalizador a mil
        DanceMove.HEADBANG -> {
            val nod = sin(t * 13f)
            RobotPose(face, facingLeft, legL = 4f * nod, armL = -30f + 10f * nod, armR = 30f - 10f * nod, headTilt = 18f * nod, lift = 2f * abs(nod), phase = t * 2f)
        }
    }
}

private data class BatteryState(val level: Int?, val charging: Boolean)

@Composable
private fun rememberBattery(): BatteryState {
    val context = LocalContext.current
    var state by remember { mutableStateOf(BatteryState(null, false)) }
    DisposableEffect(context) {
        fun read(i: Intent?) {
            if (i == null) return
            val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            state = BatteryState(
                level = if (level >= 0 && scale > 0) level * 100 / scale else null,
                charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
            )
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) = read(i)
        }
        read(context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED)))
        onDispose { context.unregisterReceiver(receiver) }
    }
    return state
}
