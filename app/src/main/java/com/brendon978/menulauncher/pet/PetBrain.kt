package com.brendon978.menulauncher.pet

import kotlin.math.max
import kotlin.random.Random

/** Humor de fundo, pelo estado do celular. A reação ao toque aparece por cima por um instante. */
enum class Mood { SLEEPING, TIRED, DANCING, HAPPY, NORMAL }

/** MENACE: muito bravo, aponta a AK de brinquedo (nunca atira). */
enum class Reaction { GIGGLE, SERIOUS, ANGRY, MENACE, FLEE }

const val SLEEP_FROM_HOUR = 23
const val SLEEP_UNTIL_HOUR = 7
const val TIRED_BATTERY = 15
const val HAPPY_MS = 5_000L

/** Prioridade da spec: dormindo > cansado > música > feliz > normal (o "bravo" vem do toque, por cima). */
fun isNightHour(hour: Int): Boolean = hour >= SLEEP_FROM_HOUR || hour < SLEEP_UNTIL_HOUR

/** O que ele usa dormindo: gorro e tapa-olho de noite, tapa-olho no não perturbe; acordado, o que você escolheu. */
fun sleepAccessory(face: Face, hour: Int, dnd: Boolean, chosen: Accessory): Accessory = when {
    face != Face.SLEEP -> chosen
    isNightHour(hour) -> Accessory.NIGHT_GEAR
    dnd -> Accessory.SLEEP_MASK
    else -> chosen
}

fun baseMood(hour: Int, battery: Int?, charging: Boolean, musicPlaying: Boolean, happyUntil: Long, now: Long): Mood = when {
    isNightHour(hour) -> Mood.SLEEPING
    battery != null && battery <= TIRED_BATTERY && !charging -> Mood.TIRED
    musicPlaying -> Mood.DANCING
    now < happyUntil -> Mood.HAPPY
    else -> Mood.NORMAL
}

/** Fica feliz só quando as notificações vão de "alguma" para zero. [prev] null = ainda não sabia quantas eram. */
fun happyUntil(prev: Int?, new: Int, now: Long, current: Long): Long =
    if (prev != null && prev > 0 && new == 0) now + HAPPY_MS else current

/**
 * Irritação: cada toque soma 1, e ela cai [DECAY_PER_S] por segundo.
 * Faixas: < 2 risadinha, < 3.5 sério, < 4.5 bravo, < 6 ameaça (aponta a arma), a partir de 6 foge (glitch e teleporte) e reaparece ainda apontando a arma, até se acalmar.
 */
data class Annoyance(val level: Float = 0f, val at: Long = 0L) {
    fun levelAt(now: Long): Float = max(0f, level - (now - at).coerceAtLeast(0) / 1000f * DECAY_PER_S)

    /** Cada toque soma 1 com vínculo médio; com vínculo alto soma menos (demora a ficar bravo), com baixo soma mais. */
    fun tap(now: Long, bond: Float = .5f): Pair<Annoyance, Reaction> {
        val l = levelAt(now) + (1.4f - .8f * bond.coerceIn(0f, 1f))
        val r = reactionFor(l)
        val next = if (r == Reaction.FLEE) Annoyance(AFTER_FLEE, now) else Annoyance(l, now)
        return next to r
    }

    /** Reação que corresponde ao nível atual, sem novo toque (para o rosto enquanto ele se acalma). */
    fun levelReaction(now: Long): Reaction = reactionFor(levelAt(now))

    private fun reactionFor(l: Float) = when {
        l < 2f -> Reaction.GIGGLE
        l < 3.5f -> Reaction.SERIOUS
        l < 4.5f -> Reaction.ANGRY
        l < 6f -> Reaction.MENACE
        else -> Reaction.FLEE
    }

    companion object {
        const val DECAY_PER_S = 0.5f
        /** Depois de fugir: ainda no nível "ameaça" (~2 s), depois bravo, sério e calmo. */
        const val AFTER_FLEE = 5.5f
    }
}

// ---- andar pela tela toda ----

/** Retângulo em px (esquerda, topo, direita, base). */
data class Area(val l: Float, val t: Float, val r: Float, val b: Float) {
    fun intersects(o: Area) = l < o.r && o.l < r && t < o.b && o.t < b
}

/** O robô (canto superior esquerdo em [p], tamanho [w]×[h]) não encosta em nenhum obstáculo ao longo do segmento. */
fun pathClear(from: Pair<Float, Float>, to: Pair<Float, Float>, w: Float, h: Float, obstacles: List<Area>): Boolean {
    val dx = to.first - from.first
    val dy = to.second - from.second
    val steps = (kotlin.math.hypot(dx, dy) / (minOf(w, h) / 3f)).toInt().coerceIn(1, 200)
    for (i in 0..steps) {
        val x = from.first + dx * i / steps
        val y = from.second + dy * i / steps
        val me = Area(x, y, x + w, y + h)
        if (obstacles.any { it.intersects(me) }) return false
    }
    return true
}

/**
 * Um lugar livre dentro de [area] para o robô (devolve o canto superior esquerdo), sem encostar nos obstáculos.
 * Com [from], o caminho em linha reta até lá também precisa estar livre e ter ao menos [minDistance].
 * null quando não acha (tela cheia): ele fica onde está.
 */
fun freeSpot(
    random: Random,
    area: Area,
    w: Float,
    h: Float,
    obstacles: List<Area>,
    from: Pair<Float, Float>?,
    minDistance: Float,
): Pair<Float, Float>? {
    val maxX = area.r - w
    val maxY = area.b - h
    if (maxX < area.l || maxY < area.t) return null
    repeat(80) {
        val x = area.l + random.nextFloat() * (maxX - area.l)
        val y = area.t + random.nextFloat() * (maxY - area.t)
        val me = Area(x, y, x + w, y + h)
        if (obstacles.any { it.intersects(me) }) return@repeat
        if (from != null) {
            if (kotlin.math.hypot(x - from.first, y - from.second) < minDistance) return@repeat
            if (!pathClear(from, x to y, w, h, obstacles)) return@repeat
        }
        return x to y
    }
    return null
}

/** Folga até o obstáculo: encostado, o arredondamento já contaria como pisar nele. */
private const val FREE_MARGIN = 1f

/** O lugar livre mais perto de ([x], [y]) para o robô, ou null se os obstáculos não deixam nenhum. */
fun nearestFree(x: Float, y: Float, w: Float, h: Float, area: Area, obstacles: List<Area>): Pair<Float, Float>? {
    val xs = listOf(x) + obstacles.flatMap { listOf(it.l - w - FREE_MARGIN, it.r + FREE_MARGIN) }
    val ys = listOf(y) + obstacles.flatMap { listOf(it.t - h - FREE_MARGIN, it.b + FREE_MARGIN) }
    return xs.flatMap { cx -> ys.map { cy -> cx to cy } }
        .filter { (cx, cy) ->
            val me = Area(cx, cy, cx + w, cy + h)
            cx >= area.l && cy >= area.t && me.r <= area.r && me.b <= area.b && obstacles.none { it.intersects(me) }
        }
        .minByOrNull { (cx, cy) -> kotlin.math.hypot(cx - x, cy - y) }
}

/** Onde acaba o que desce a partir de ([x], [fromY]): no primeiro obstáculo abaixo ou no fim de [area]. */
fun floorUnder(x: Float, fromY: Float, area: Area, obstacles: List<Area>): Float =
    obstacles.filter { x >= it.l && x <= it.r && it.t >= fromY }.minOfOrNull { it.t } ?: area.b

/** Quanto tempo ele marcha com a arma depois de guardar a mira. */
const val MARCH_MS = 20_000L

/** Ao sair da ameaça (estava apontando e parou), marcha por [MARCH_MS]. */
fun marchUntil(wasMenacing: Boolean, menacing: Boolean, now: Long, current: Long): Long =
    if (wasMenacing && !menacing) now + MARCH_MS else current

/** Falas no balão, por nível de raiva. A risadinha não fala. */
val ANGER_LINES: Map<Reaction, List<String>> = mapOf(
    Reaction.SERIOUS to listOf("Ei.", "Hm.", "Que foi?", "Para.", "Pô...", "Sério isso?"),
    Reaction.ANGRY to listOf(
        "Para com isso!", "Tá me tirando?", "Me erra!", "Sai pra lá!", "Chega!",
        "Que saco, mano!", "Seu mané!", "Vai catar coquinho!", "Porra, para!", "Caralho, de novo?",
        "Vai te catar!", "Desgraça, me deixa!", "Que merda é essa?", "Seu chato do caramba!",
    ),
    Reaction.MENACE to listOf(
        "Mais um toque...", "Tá avisado!", "Vai encarar?", "Não testa.", "Mão pra cima!",
        "Encosta de novo, desgraçado!", "Tá achando que é bagunça, porra?", "Se folgar, já era!",
        "Perdeu, playboy!", "Vai pra pqp!", "Filho da mãe...",
    ),
    Reaction.FLEE to listOf("Pra mim chega!", "Fui!", "Me deixa!", "Aff, vou embora, porra!", "Que inferno!"),
)

/** Falas enquanto marcha com a arma. */
val MARCH_LINES: List<String> = listOf(
    "Esquerda, direita!", "Tô de olho.", "Um, dois! Um, dois!", "Patrulha.", "Hmpf.",
    "Folgado...", "Tô de olho em você, mané.", "Ninguém me respeita, porra.", "Encosta pra ver.",
)

fun angerLine(reaction: Reaction, random: Random): String? = ANGER_LINES[reaction]?.random(random)

/** Passos de dança; troca a cada [DANCE_MOVE_MS] em ordem aleatória. */
enum class DanceMove { SWAY, ROBOT, HOP, SIDESTEP, MOONWALK, SPIN, HEADBANG }

const val DANCE_MOVE_MS = 4_000L

/** Próximo passo, nunca o mesmo de agora. */
fun nextDanceMove(random: Random, current: DanceMove): DanceMove =
    DanceMove.entries.filter { it != current }.random(random)
