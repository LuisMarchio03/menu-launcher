package com.brendon978.menulauncher.pet

import androidx.compose.runtime.mutableStateOf
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

// ---- sente o celular ----

enum class Tilt { LEVEL, LEFT, RIGHT, UPSIDE_DOWN }

/** Acelerômetro parado: x > 0 é o lado esquerdo para baixo; y < 0 é de cabeça para baixo. */
fun tiltOf(x: Float, y: Float, z: Float): Tilt = when {
    y < -6f && abs(z) < 7f -> Tilt.UPSIDE_DOWN
    abs(x) > 6.5f && abs(z) < 7f -> if (x > 0f) Tilt.LEFT else Tilt.RIGHT
    else -> Tilt.LEVEL
}

/** Chacoalhar: três trancos fortes em um segundo, com pausa antes de contar de novo. */
data class ShakeMeter(val peaks: List<Long> = emptyList(), val quietUntil: Long = 0L) {
    fun sample(magnitude: Float, now: Long): Pair<ShakeMeter, Boolean> {
        if (now < quietUntil || magnitude < SHAKE_FORCE) return this to false
        val recent = (peaks.filter { now - it < 1_000L }.takeLast(4)).let { if (it.isNotEmpty() && now - it.last() < 90L) it else it + now }
        if (recent.size >= 3) return ShakeMeter(emptyList(), now + 6_000L) to true
        return ShakeMeter(recent, quietUntil) to false
    }

    private companion object { const val SHAKE_FORCE = 20f }
}

val SHAKE_LINES = listOf("Ei, ei, EI!", "Terremoto!", "Vou vomitar parafuso...", "Para de me sacudir, pô!")
val TILT_LINES = listOf("Tá torto!", "Segura aí!", "Aaaah, escorregando!")
val UPSIDE_LINES = listOf("Me vira!", "O mundo tá de cabeça pra baixo!", "Isso não tá certo...")

// ---- comenta o que você fez ----

private val COMMENT_APPS = listOf(
    "instagram" to listOf("Instagram de novo?", "Viu algo bom no Instagram?"),
    "tiktok" to listOf("Mais um videozinho, né?", "O TikTok te soltou?"),
    "zhiliaoapp.musically" to listOf("Mais um videozinho, né?", "O TikTok te soltou?"),
    "youtube" to listOf("Maratona de YouTube?", "Tinha gatinho no YouTube?"),
    "whatsapp" to listOf("Fofoca boa no zap?", "Respondeu todo mundo?"),
    "twitter" to listOf("Muita treta por lá?"),
    "spotify" to listOf("Coloca um som aí!"),
    "netflix" to listOf("E aí, spoiler?"),
    "camera" to listOf("Saiu bonito na foto?"),
    "chrome" to listOf("Achou o que procurava?"),
)

/** Comentário ao voltar de um app, ou null. Longo tempo fora ganha uma fala própria. */
fun returnComment(pkg: String, label: String, awayMs: Long, random: Random): String? {
    val minutes = awayMs / 60_000L
    if (minutes >= 60) return "${minutes / 60}h no $label, hein?"
    if (minutes >= 20) return "$minutes minutos no $label, hein?"
    return COMMENT_APPS.firstOrNull { pkg.contains(it.first, ignoreCase = true) }?.second?.random(random)
}

/** Sem encher: no máximo um a cada 10 min, e só em parte das vezes. */
fun commentNow(lastAt: Long, now: Long, random: Random): Boolean = now - lastAt >= 10 * 60_000L && random.nextFloat() < .4f

// ---- datas especiais ----

enum class SpecialDay(val hat: Accessory, val line: String, val party: Boolean) {
    NEW_YEAR(Accessory.PARTY_HAT, "Feliz ano novo!", true),
    HALLOWEEN(Accessory.WITCH_HAT, "Doces ou travessuras?", false),
    CHRISTMAS(Accessory.SANTA_HAT, "Feliz Natal!", true),
    BIRTHDAY(Accessory.PARTY_HAT, "Parabéns! Hoje é seu dia!", true),
}

/** "25/12" ou "5/3" vira dia e mês; qualquer outra coisa, null. */
fun parseBirthday(text: String): Pair<Int, Int>? {
    val p = text.trim().split("/", "-", ".")
    if (p.size != 2) return null
    val day = p[0].toIntOrNull() ?: return null
    val month = p[1].toIntOrNull() ?: return null
    return if (month in 1..12 && day in 1..31) day to month else null
}

fun specialDay(day: Int, month: Int, birthday: String): SpecialDay? = when {
    parseBirthday(birthday) == (day to month) -> SpecialDay.BIRTHDAY
    (month == 12 && day == 31) || (month == 1 && day == 1) -> SpecialDay.NEW_YEAR
    month == 12 && day in 20..25 -> SpecialDay.CHRISTMAS
    month == 10 && day == 31 -> SpecialDay.HALLOWEEN
    else -> null
}

/** O que ele veste: dormir vence tudo; depois o fone; depois o chapéu da data; depois o que você escolheu. */
fun wornAccessory(face: Face, hour: Int, dnd: Boolean, headset: Boolean, special: SpecialDay?, chosen: Accessory, weather: Weather? = null): Accessory {
    val awake = if (headset) Accessory.HEADPHONES else special?.hat ?: chosen.takeIf { it != Accessory.NONE } ?: weather?.wear ?: chosen
    return sleepAccessory(face, hour, dnd, awake)
}

// ---- mexe na tela ----

enum class Mischief { CLOCK, ICON }

/** A travessura ativa: a tela inicial lê daqui para entortar o relógio ou empurrar o favorito. */
object GlitchMischief {
    val current = mutableStateOf<Mischief?>(null)
}

val MISCHIEF_LINES = listOf("Hehe...", "Ops.", "Não fui eu.")
val MISCHIEF_FIXED_LINES = listOf("Tá, tá, arrumei.", "Foi sem querer!", "Tava melhor antes.")

// ---- print ----

val POSE_LINES = listOf("Saí bem?", "Pega meu lado bom!", "Manda pro grupo!", "Xiiis!")

/** Pose depois de um print comum: o aviso só chega com a foto já tirada. Para sair na foto, use "Tirar foto". */
const val POSE_MS = 3_000L

/** Maior das duas, para o aviso não encurtar outro que já está na tela. */
fun laterOf(a: Long, b: Long): Long = max(a, b)

// ---- caça aos bugs ----

data class Bug(val id: Int, val x: Float, val y: Float, val vx: Float, val vy: Float)

/** Anda em linha reta e rebate nas bordas: bug fugindo pela tela. */
fun moveBug(b: Bug, dt: Float, area: Area): Bug {
    val x = b.x + b.vx * dt
    val y = b.y + b.vy * dt
    val vx = if (x < area.l || x > area.r) -b.vx else b.vx
    val vy = if (y < area.t || y > area.b) -b.vy else b.vy
    return b.copy(x = x.coerceIn(area.l, area.r), y = y.coerceIn(area.t, area.b), vx = vx, vy = vy)
}

fun bugScoreLine(you: Int, him: Int): String = when {
    you > him -> "Você $you, eu $him. Tá, você venceu."
    him > you -> "Eu $him, você $you. Exterminador nato!"
    else -> "Empate: $you a $him."
}

// ---- duelo de reflexo ----

const val DUEL_ROUNDS = 5

/** Reflexo dele em ms: sem recorde, um adversário médio; com recorde, fica logo acima dele para a disputa ser de verdade. */
fun duelGlitchRange(best: Long): LongRange =
    if (best <= 0L) 290L..480L else (best - 10).coerceAtLeast(150L)..(best + 70).coerceAtMost(480L)

enum class DuelRound { YOU, HIM }

/** [earlyTap] é tocar antes do "JÁ!": ponto dele. Sem toque a tempo, também. */
fun duelRound(tapMs: Long?, glitchMs: Long, earlyTap: Boolean): DuelRound = when {
    earlyTap || tapMs == null -> DuelRound.HIM
    tapMs <= glitchMs -> DuelRound.YOU
    else -> DuelRound.HIM
}

/** Melhor tempo guardado; 0 significa que ainda não há recorde. */
fun bestTime(best: Long, tapMs: Long): Long = if (best == 0L || tapMs < best) tapMs else best

fun duelEndLine(you: Int, him: Int): String = if (you > him) "$you a $him. Dedo rápido, hein?" else "$him a $you pra mim. Treina mais!"

// ---- comer notificações ----

/** Bolinhas que ele já comeu voltam depois disso: comer nunca some com aviso de verdade. */
const val EATEN_MS = 60_000L

/** Quantas "refeições" cabem na barriga. */
fun bellyAfterMeal(belly: Float): Float = (belly + .34f).coerceAtMost(1f)

/** A barriga desce sozinha em cerca de 3 minutos; andando, em cerca de 1. */
fun bellyAfterTime(belly: Float, dtSec: Float, walking: Boolean = false): Float =
    (belly - dtSec * (if (walking) 3f else 1f) / 180f).coerceAtLeast(0f)

val EAT_LINES = listOf("Nham!", "Delícia de notificação.", "Essa tava crocante.", "Burp.")
val STUFFED_LINES = listOf("Tô cheio...", "Chega de notificação!")

// ---- sentar nas coisas ----

/** Sentado em cima da lista, com as pernas balançando, ou pendurado embaixo do relógio. */
enum class Perch { SIT, HANG }

// ---- menu ----

/** Acima dele se couber; senão embaixo; e nunca fora da tela, mesmo que precise cobrir ele. */
fun menuTop(robotTop: Float, robotHeight: Float, menuHeight: Float, area: Area): Float {
    val above = robotTop - menuHeight - 8f
    val wanted = if (above >= area.t) above else robotTop + robotHeight + 8f
    return wanted.coerceIn(area.t, maxOf(area.t, area.b - menuHeight))
}

// ---- poses de foto ----

/** Sorteada a cada foto. Nos braços, positivo abre o esquerdo e negativo abre o direito. */
enum class PhotoPose { VICTORY, FLEX, ONE_ARM, DAB, JUMP, HEART, COOL, SHRUG, ROCK, TOUGH }

fun pickPhotoPose(previous: PhotoPose?, random: Random): PhotoPose = PhotoPose.entries.filter { it != previous }.random(random)

fun photoPose(kind: PhotoPose, facingLeft: Boolean, t: Float): RobotPose = when (kind) {
    PhotoPose.VICTORY -> RobotPose(Face.BLISS, facingLeft, armL = 125f, armR = -125f, lift = 3f, phase = t)
    PhotoPose.FLEX -> RobotPose(Face.SERIOUS, facingLeft, armL = 88f, armR = -88f, phase = t)
    PhotoPose.ONE_ARM -> RobotPose(Face.GIGGLE, facingLeft, armL = 8f, armR = -128f, tilt = 3f, phase = t)
    PhotoPose.DAB -> RobotPose(Face.SERIOUS, facingLeft, armL = 125f, armR = 48f, tilt = -5f, headTilt = 14f, phase = t)
    PhotoPose.JUMP -> RobotPose(Face.GIGGLE, facingLeft, legL = 22f, legR = -22f, armL = 122f, armR = -122f, lift = 12f, phase = t)
    PhotoPose.HEART -> RobotPose(Face.HEART, facingLeft, armL = 62f, armR = -62f, phase = t)
    PhotoPose.COOL -> RobotPose(Face.NORMAL, facingLeft, armL = 14f, armR = -14f, tilt = 7f, headTilt = -9f, phase = t)
    PhotoPose.SHRUG -> RobotPose(Face.CONFUSED, facingLeft, armL = 72f, armR = -72f, lift = 2f, phase = t)
    PhotoPose.ROCK -> RobotPose(Face.MUSIC, facingLeft, legL = 12f, armL = 20f, armR = -128f, tilt = -5f, phase = t)
    PhotoPose.TOUGH -> RobotPose(Face.ANGRY, facingLeft, legL = 10f, legR = -10f, armL = 26f, armR = -26f, phase = t)
}

