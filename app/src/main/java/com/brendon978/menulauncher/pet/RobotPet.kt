package com.brendon978.menulauncher.pet

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
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** Altura do robozinho na tela. */
val ROBOT_HEIGHT = 72.dp
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
    area: Area,
    obstacles: List<Area>,
    accessory: Accessory = Accessory.NONE,
) {
    val density = LocalDensity.current
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
    val hour = remember(now / 60_000) { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
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
        val m = GlitchMemory(drives, bond, System.currentTimeMillis(), pokeExpect, diary, lastMorning, lastNightNag)
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
            activity = nextActivity(activity, drives, bond, musicNow, brainRandom)
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
    var game by remember { mutableStateOf<GameKind?>(null) }
    var pendingGame by remember { mutableStateOf<GameKind?>(null) }
    var pendingProtest by remember { mutableIntStateOf(0) }
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
    val stillMood = fainted || striking || menuOpen || game == GameKind.JOKENPO || hidden || working || spinning || flinching || punished || celebrating || debugHold || petting || held || flying || dizzy || menacing || (!marching && (mood == Mood.SLEEPING || mood == Mood.TIRED || mood == Mood.DANCING || sulking))
    val random = remember { Random(System.nanoTime()) }

    fun spot(from: Offset?, minDistance: Float): Offset? =
        freeSpot(random, currentArea, widthPx, heightPx, currentObstacles, from?.let { it.x to it.y }, minDistance)
            ?.let { Offset(it.first, it.second) }

    suspend fun glitchTo(target: Offset) {
        // pisca, some, reaparece no outro lugar. Se for cancelado no meio (a tela mudou), ele não pode ficar invisível.
        try {
            repeat(3) { visible = .3f; delay(50); visible = 1f; delay(60) }
            visible = 0f
            delay(220)
            pos.snapTo(target)
            visible = .5f; delay(60)
        } finally {
            visible = 1f
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

    suspend fun walkTo(target: Offset) {
        facingLeft = target.x < pos.value.x
        walking = true
        val speed = with(density) { 38.dp.toPx() }
        val ms = ((target - pos.value).getDistance() / speed * 1000).roundToInt().coerceAtLeast(1)
        try { pos.animateTo(target, tween(ms, easing = LinearEasing)) } finally { walking = false }
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
                    val clamped = Offset(want.x.coerceIn(a.l, a.r - widthPx), want.y.coerceIn(a.t, a.b - heightPx))
                    if ((clamped - pos.value).getDistance() < widthPx * .4f) {
                        spot(null, 0f)?.let { glitchTo(it) }
                        if (brainRandom.nextInt(2) == 0) say(TAG_LINES.random(brainRandom), 1_000)
                    } else {
                        facingLeft = clamped.x < pos.value.x
                        walking = true
                        val speed = with(density) { 260.dp.toPx() }
                        val ms = ((clamped - pos.value).getDistance() / speed * 1000).roundToInt().coerceIn(1, 500)
                        try { pos.animateTo(clamped, tween(ms, easing = LinearEasing)) } finally { walking = false }
                    }
                } else delay(80)
            }
            if (game == GameKind.TAG) { say("Lento demais! Ganhei!", 2_200); game = null }
            return@LaunchedEffect
        }
        if (game == GameKind.BUBBLES) {
            // bolhas: corre até a mais perto e estoura
            while (System.currentTimeMillis() < bubblesUntil) {
                val me = pos.value + Offset(widthPx / 2, heightPx / 2)
                val target = bubbles.minByOrNull { (Offset(it.x, it.y) - me).getDistance() }
                if (target == null) { delay(150); continue }
                val tc = Offset(target.x, target.y)
                if ((tc - me).getDistance() < with(density) { 44.dp.toPx() }) {
                    bubbles = bubbles.filter { it.id != target.id }
                    pops = pops + (tc to System.currentTimeMillis())
                    if (brainRandom.nextInt(3) == 0) say(BUBBLE_LINES.random(brainRandom), 900)
                    continue
                }
                val a = currentArea
                val dest = Offset((tc.x - widthPx / 2).coerceIn(a.l, a.r - widthPx), (tc.y - heightPx / 2).coerceIn(a.t, a.b - heightPx))
                facingLeft = dest.x < pos.value.x
                walking = true
                val speed = with(density) { 150.dp.toPx() }
                val ms = ((dest - pos.value).getDistance() / speed * 1000).roundToInt().coerceIn(1, 250)
                try { pos.animateTo(dest, tween(ms, easing = LinearEasing)) } finally { walking = false }
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
                val target = Offset(
                    (ballCenter.x + side * (widthPx * .55f) - widthPx / 2).coerceIn(currentArea.l, currentArea.r - widthPx),
                    (b.y + ballPx - heightPx).coerceIn(currentArea.t, currentArea.b - heightPx),
                )
                if ((target - pos.value).getDistance() > widthPx * .25f) {
                    facingLeft = target.x < pos.value.x
                    walking = true
                    val speed = with(density) { 90.dp.toPx() }
                    val ms = ((target - pos.value).getDistance() / speed * 1000).roundToInt().coerceIn(1, 700)
                    try { pos.animateTo(target, tween(ms, easing = LinearEasing)) } finally { walking = false }
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
            delay(3_000)
            scanning = false
            drives = drives.copy(curiosity = drives.curiosity - .4f).clamped()
            activity = decide(drives, bond, musicNow, brainRandom)
            return@LaunchedEffect
        }
        while (true) {
            val target = spot(pos.value, minDistance = widthPx * 1.5f)
            if (target == null) { delay(1_500); continue }
            facingLeft = target.x < pos.value.x
            walking = true
            val speed = with(density) { (if (marching || protesting) 48.dp else 38.dp).toPx() } // px por segundo
            val ms = ((target - pos.value).getDistance() / speed * 1000).roundToInt().coerceAtLeast(1)
            pos.animateTo(target, tween(ms, easing = LinearEasing))
            walking = false
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
        say(line, (4_500L + line.length * 55L).coerceAtMost(12_000L))
        aiSpeechUntil = speechUntil
    }

    // rotina: bom dia de manhã, "vai dormir" de madrugada
    LaunchedEffect(resumed, loaded) {
        if (!resumed || !loaded) return@LaunchedEffect
        delay(1_200)
        val n = System.currentTimeMillis()
        when (routineFor(Calendar.getInstance().get(Calendar.HOUR_OF_DAY), todayInt(n), lastMorning, lastNightNag, n)) {
            Routine.GOOD_MORNING -> { lastMorning = todayInt(n); say(GOOD_MORNING_LINES.random(brainRandom), 2_600); save() }
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

    // voltou de um app de trabalho depois de um tempo: ele "trabalhou junto"
    LaunchedEffect(resumed) {
        if (!resumed) return@LaunchedEffect
        GlitchSignals.lastLaunch?.let { (pkg, at) ->
            if (isWorkApp(pkg) && System.currentTimeMillis() - at > 2 * 60_000L) {
                workUntil = System.currentTimeMillis() + 20_000
                say(WORK_LINES.random(brainRandom), 2_600)
            }
        }
        GlitchSignals.lastLaunch = null
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
    val face = when {
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
        // desmaio dramático: tomba de lado e fica estendido
        fainted && !held && !flying -> RobotPose(face, facingLeft, armL = 70f, armR = -70f, tilt = 88f, lift = -2f, phase = t)
        // greve: de costas, braços cruzados, parado com o cartaz
        striking && !held && !flying -> RobotPose(face, facingLeft, armL = 20f, armR = -20f, back = true, phase = t)
        spinning -> RobotPose(face, facingLeft, armL = 110f, armR = -110f, rotation = spinDeg, phase = t)
        flinching -> RobotPose(face, facingLeft, armL = -150f, armR = 150f, lift = -4f, phase = t)
        jkCounting -> RobotPose(face, facingLeft, armL = 10f, armR = -70f + 25f * sin(t * 22f), phase = t)
        working -> RobotPose(face, facingLeft, armL = -58f + 6f * sin(t * 22f), armR = 58f - 6f * sin(t * 22f + 1f), laptop = true, phase = t)
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
        mood == Mood.TIRED -> RobotPose(face, facingLeft, armL = 8f, armR = -8f, tilt = if (facingLeft) -4f else 4f, phase = t)
        else -> RobotPose(face, facingLeft, blink = blink, antennaOn = !scanning || (t * 3).toInt() % 2 == 0, phase = t)
    }

    val scope = androidx.compose.runtime.rememberCoroutineScope()
    // área de toque maior que o desenho: ele é pequeno e está andando
    val pad = with(density) { TOUCH_PAD.toPx() }
    var placedAt by remember { mutableStateOf(Offset.Zero) }

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
            ProtestStage.STRIKE -> { protestUntil = at + 60_000; protestSign = STRIKE_SIGNS.random(brainRandom); say(STRIKE_LINES.random(brainRandom), 2_000) }
            ProtestStage.REVOLUTION -> { protestUntil = at + 30_000; protestSign = REVOLUTION_SIGNS.random(brainRandom) }
        }
    }
    LaunchedEffect(pendingProtest) { if (pendingProtest > 0) startProtest(System.currentTimeMillis()) }

    fun onTap() {
        val tapNow = System.currentTimeMillis()
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
        if (!spoke) angerLine(r, speechRandom)?.let { speech = it; speechUntil = tapNow + 1_800 }
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
            val to = Offset((from.x + widthPx * .8f).coerceAtMost(area.r - 6f), area.b)
            val cable = androidx.compose.ui.graphics.Path().apply {
                moveTo(from.x, from.y)
                cubicTo(from.x + widthPx * .9f, from.y, to.x - widthPx * .3f, to.y - heightPx * .6f, to.x, to.y - 14f)
            }
            drawPath(cable, color.copy(alpha = .8f), style = androidx.compose.ui.graphics.drawscope.Stroke(2.2f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            drawRect(color, Offset(to.x - 6f, to.y - 16f), androidx.compose.ui.geometry.Size(12f, 16f), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
            // faiscazinha de energia correndo pelo cabo
            val k = (t % 1f)
            val spark = Offset(from.x + (to.x - from.x) * (1 - k), from.y + (to.y - from.y) * (1 - k) * (1 - k))
            drawCircle(color, 3f, spark)
        }
    }

    // ---- por baixo do Glitch: rede de toques das brincadeiras, post-its, bolhas, antena escondida ----
    val catching = menuOpen || game == GameKind.HIDE || game == GameKind.TAG || game == GameKind.BUBBLES
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

    if (bubbles.isNotEmpty() || pops.isNotEmpty()) {
        Canvas(Modifier.fillMaxSize()) {
            bubbles.forEach { drawBubble(color, Offset(it.x, it.y), it.r) }
            pops.forEach { (c, at) -> drawPop(color, c, with(density) { 18.dp.toPx() }, ((now - at) / 400f).coerceIn(0f, 1f)) }
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
        ) { drawPeekingAntenna(color, Offset(size.width / 2, size.height), density.density / 2.6f * 1.4f, t) }
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
                            while (true) {
                                val ch = awaitPointerEvent().changes.first()
                                ch.consume()
                                if (!ch.pressed) break
                                xs += placedAt.x + ch.position.x
                                if (!petting && countReversals(xs, minRub) >= 2) startPetting()
                            }
                            petting = false
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
        val dressed = pose.copy(lookX = lookX, lookY = lookY, accessory = when {
            dnd && face == Face.SLEEP -> Accessory.SLEEP_MASK
            headset -> Accessory.HEADPHONES
            else -> accessory
        })
        Canvas(Modifier.size(with(density) { widthPx.toDp() }, ROBOT_HEIGHT)) { drawRobot(color, dressed) }
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
            Canvas(Modifier.size(with(density) { ballPx.toDp() })) { drawBall(color, ballSpin) }
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
                drawPath(ball, color, style = androidx.compose.ui.graphics.drawscope.Stroke(1.6.dp.toPx()))
                drawLine(color, at(200f, .7f), at(20f, .3f), 1.1.dp.toPx())
                drawLine(color, at(20f, .3f), at(95f, .75f), 1.1.dp.toPx())
                drawLine(color, at(20f, .3f), at(300f, .7f), 1.1.dp.toPx())
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
        ) { drawFireworks(color, p) }
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
            modifier = Modifier.layout { m, c ->
                val pl = m.measure(c.copy(minWidth = 0, minHeight = 0))
                layout(pl.width, pl.height) {
                    val x = (cx - pl.width / 2).coerceIn(area.l, (area.r - pl.width).coerceAtLeast(area.l))
                    val y = (top - pl.height - 8f).let { if (it < area.t) top + heightPx + 8f else it }
                    pl.place(x.roundToInt(), y.roundToInt())
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
    androidx.compose.foundation.layout.Column(modifier, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        androidx.compose.material3.Text(
            text,
            color = color,
            fontSize = 13.sp,
            maxLines = 4,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier
                .widthIn(max = 240.dp)
                .background(Color(0xE60C0C0E), RoundedCornerShape(12.dp))
                .border(1.dp, color.copy(alpha = .6f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        )
        Canvas(Modifier.size(12.dp, 6.dp)) {
            val tail = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2, size.height); close()
            }
            drawPath(tail, Color(0xE60C0C0E))
            drawLine(color.copy(alpha = .6f), Offset(0f, 0f), Offset(size.width / 2, size.height), 1.dp.toPx())
            drawLine(color.copy(alpha = .6f), Offset(size.width, 0f), Offset(size.width / 2, size.height), 1.dp.toPx())
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
