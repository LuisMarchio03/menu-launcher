package com.brendon978.menulauncher.pet

import kotlin.math.max
import kotlin.random.Random

/*
 * Cérebro do Glitch: vontades que mudam sozinhas, decisões com um pouco de sorte e um vínculo com você
 * que fica salvo. Tudo local, sem modelo de linguagem e sem internet.
 */

/** Vontades, de 0 a 1. */
data class Drives(
    val energy: Float = .8f,
    val boredom: Float = .2f,
    val longing: Float = .2f,
    val curiosity: Float = .2f,
) {
    fun clamped() = Drives(energy.c(), boredom.c(), longing.c(), curiosity.c())
    private fun Float.c() = coerceIn(0f, 1f)
}

/** O que está acontecendo enquanto o tempo passa. */
data class TickCtx(
    val napping: Boolean = false,
    val night: Boolean = false,
    val batteryLow: Boolean = false,
    val music: Boolean = false,
)

/** O que ele escolhe fazer quando não está dormindo de noite nem cansado pela bateria. */
/** SAD: carente e ignorado há muito tempo, vai para um canto. PLAY: entediado e com energia, joga bola. */
/** PRANK: entediado mas não carente, apronta (cola post-its na tela). */
enum class Activity { WANDER, NAP, SEEK_ATTENTION, EXPLORE, SULK, DANCE, SAD, PLAY, PRANK, EAT, PERCH, SHOW }

enum class GlitchEvent { GENTLE_TAP, NOTIFICATION, MUSIC_START }

enum class Greeting { MISSED_YOU, SULKY }

// ritmos, por segundo
private const val ENERGY_DRAIN = 1f / 2_400f   // acordado: de cheio a vazio em ~40 min
private const val ENERGY_NAP = 1f / 180f       // cochilando: recarrega em ~3 min
private const val BOREDOM_UP = 1f / 600f       // sem nada acontecendo: entediado em ~10 min
private const val BOREDOM_MUSIC = 1f / 120f    // música tira o tédio
private const val LONGING_UP = 1f / 1_200f     // sem atenção: carente em ~20 min
private const val CURIOSITY_DECAY = 1f / 240f  // novidade perde a graça em ~4 min

/** As vontades depois de [dtSec] segundos. */
fun tick(d: Drives, dtSec: Float, ctx: TickCtx): Drives {
    val drainK = (if (ctx.night) 2f else 1f) * (if (ctx.batteryLow) 2f else 1f)
    val energy = if (ctx.napping) d.energy + ENERGY_NAP * dtSec else d.energy - ENERGY_DRAIN * drainK * dtSec
    val boredom = if (ctx.music) d.boredom - BOREDOM_MUSIC * dtSec else d.boredom + BOREDOM_UP * dtSec
    return Drives(
        energy = energy,
        boredom = boredom,
        longing = d.longing + LONGING_UP * dtSec,
        curiosity = d.curiosity - CURIOSITY_DECAY * dtSec,
    ).clamped()
}

fun onEvent(d: Drives, e: GlitchEvent): Drives = when (e) {
    GlitchEvent.GENTLE_TAP -> d.copy(boredom = d.boredom - .4f, longing = d.longing - .5f)
    GlitchEvent.NOTIFICATION -> d.copy(curiosity = d.curiosity + .5f, boredom = d.boredom - .2f)
    GlitchEvent.MUSIC_START -> d.copy(curiosity = d.curiosity + .3f, boredom = d.boredom - .3f)
}.clamped()

/**
 * Escolhe a atividade com mais vontade, com um pouco de sorte (até 0.15) para não ficar previsível.
 * Com vínculo ruim ele fica emburrado e não dança, nem com música.
 */
fun decide(d: Drives, bond: Float, music: Boolean, random: Random): Activity {
    val dances = music && bond >= .25f
    val weights = mapOf(
        Activity.NAP to if (d.energy < .3f) (.3f - d.energy) / .3f * 1.2f else 0f,
        Activity.SEEK_ATTENTION to max(0f, max(d.boredom, d.longing) - .3f) * 1.1f,
        Activity.EXPLORE to max(0f, d.curiosity - .3f) * 1.1f,
        Activity.DANCE to if (dances) .9f else 0f,
        Activity.SULK to if (bond < .3f) (.3f - bond) / .3f + (if (music) .3f else 0f) else 0f,
        Activity.SAD to if (d.longing > .75f && d.boredom > .5f) (d.longing - .75f) * 4f + (if (bond < .5f) .15f else 0f) else 0f,
        Activity.PLAY to if (d.energy > .5f && d.longing < .6f) (d.boredom - .25f) * 1.2f + (d.energy - .5f) * .3f else 0f,
        Activity.PRANK to if (d.energy > .35f && d.longing < .5f && d.boredom > .45f) (d.boredom - .4f) * 1.4f else 0f,
        Activity.WANDER to .45f,
    )
    return weights.filterValues { it > 0f }
        .mapValues { it.value + random.nextFloat() * .15f }
        .maxBy { it.value }.key
}

/** Tristeza não passa sozinha: ele fica no canto até receber atenção. */
fun nextActivity(current: Activity, d: Drives, bond: Float, music: Boolean, random: Random): Activity =
    if (current == Activity.SAD && d.longing > .6f) Activity.SAD else decide(d, bond, music, random)

/** Vínculo depois de uma reação ao toque: carinho aproxima, raiva afasta. */
fun bondAfter(bond: Float, r: Reaction): Float = (bond + when (r) {
    Reaction.GIGGLE -> .03f
    Reaction.SERIOUS -> -.01f
    Reaction.ANGRY -> -.03f
    Reaction.MENACE -> -.05f
    Reaction.FLEE -> -.08f
}).coerceIn(0f, 1f)

/** Ficou mais de [AWAY_MS] sem ver a tela inicial: sente saudade (e descansou). Cumprimenta conforme o vínculo. */
const val AWAY_MS = 2 * 3_600_000L

fun onReturn(d: Drives, awayMs: Long, bond: Float): Pair<Drives, Greeting?> {
    if (awayMs < AWAY_MS) return d to null
    val hours = awayMs / 3_600_000f
    val back = d.copy(longing = d.longing + minOf(.8f, hours / 6f), energy = max(d.energy, .8f)).clamped()
    return back to if (bond >= .4f) Greeting.MISSED_YOU else Greeting.SULKY
}

// ---- falas da autonomia ----
val GREETING_LINES = mapOf(
    Greeting.MISSED_YOU to listOf("Ah, voltou!", "Opa! Senti sua falta.", "Até que enfim, hein!", "Eba, você!"),
    Greeting.SULKY to listOf("Hmpf. Lembrou de mim?", "Ah, é você.", "Sumiu, né?", "Tava bom sem você."),
)
val ATTENTION_LINES = listOf("Ei!", "Psiu!", "Tô aqui, mané!", "Ninguém brinca comigo?", "Oi? Alô?", "Me dá atenção, pô!")
val SULK_LINES = listOf("Hmpf.", "Não tô falando com você.", "...", "Me deixa.")
val REFUSE_DANCE_LINES = listOf("Não tô no clima.", "Dança você.", "Hmpf, nem vem.")
val EXPLORE_LINES = listOf("Hmm, que é isso?", "Deixa eu ver...", "Interessante...", "Escaneando.")

/** Carinho aproxima mais que uma risadinha (uma vez por sessão de carinho). */
fun bondAfterPetting(bond: Float): Float = (bond + .04f).coerceIn(0f, 1f)

val PET_LINES = listOf("Hmmm...", "Isso, aí mesmo...", "Que delícia...", "Pode continuar.", "Rindo em binário...")
val FORGIVE_LINES = listOf("Tá... tá bom.", "Hmpf... desculpo.", "Só dessa vez, hein.")
val PICKUP_LINES = listOf("Ei! Me solta!", "Opa, opa!", "Pra onde a gente vai?", "Tô voando!", "Me bota no chão!")
val LAND_LINES = listOf("Aiii...", "Tudo girando...", "Quem me jogou?!", "Bati a antena...", "Uou...")

/** Fogos quando o vínculo chega no topo. */
const val CELEBRATE_BOND = .85f
fun celebrates(prevBond: Float, newBond: Float): Boolean = prevBond < CELEBRATE_BOND && newBond >= CELEBRATE_BOND

/** Três raivas em 10 minutos: ele entra em protesto (cartaz, greve). Depois zera. */
data class ProtestMeter(val times: List<Long> = emptyList()) {
    fun angry(now: Long): Pair<ProtestMeter, Boolean> {
        val recent = times.filter { now - it < PROTEST_WINDOW_MS } + now
        return if (recent.size >= 3) ProtestMeter() to true else ProtestMeter(recent) to false
    }
}
const val PROTEST_WINDOW_MS = 10 * 60_000L
const val PROTEST_MS = 30_000L

/** Castigo: fica de costas no cantinho por [PUNISH_MS]. Carinho perdoa; importunar muito faz fugir. */
const val PUNISH_MS = 25_000L
data class Punishment(val start: Long, val pokes: List<Long> = emptyList(), val over: Boolean = false) {
    fun active(now: Long) = !over && now - start < PUNISH_MS
    fun forgiven() = copy(over = true)
    /** Cutucou de castigo: quatro cutucadas em 3 s e ele foge (devolve true). */
    fun poke(now: Long): Pair<Punishment, Boolean> {
        val recent = pokes.filter { now - it < 3_000 } + now
        return if (recent.size >= 4) copy(pokes = recent, over = true) to true else copy(pokes = recent) to false
    }
    companion object { fun start(now: Long) = Punishment(now) }
}

val SAD_LINES = listOf("Ninguém liga pra mim...", "Tô aqui sozinho...", "Snif.", "Esqueceram de mim.", "Tudo bem, eu me viro...")
val PLAY_LINES = listOf("Bora jogar!", "Olha o drible!", "Gooool!", "Toma essa!", "Chuta, chuta!")
val CELEBRATE_LINES = listOf("Você é demais!", "Melhor humano!", "Eba!!", "Te curto, mané!")
val PROTEST_SIGNS = listOf("ABAIXO O DEDO!", "GREVE!", "RESPEITO PROS ROBÔS!", "CHEGA DE CUTUCADA!", "DIREITOS JÁ!")
val PUNISH_SIGNS = listOf("INJUSTIÇA!", "EU NÃO FIZ NADA!", "TÔ DE MAL", "VOU FUGIR", "PQP")
val PUNISH_LINES = listOf("Isso é injusto!", "Tá bom, fico aqui...", "Hmpf.", "Vou lembrar disso.", "Que palhaçada.")

// ---- protesto em estágios, rancor e drama ----
/** Protestos seguidos na mesma hora vão escalando: reclamação, greve, revolução. */
enum class ProtestStage { COMPLAINT, STRIKE, REVOLUTION }

fun protestStage(protestsInLastHour: Int): ProtestStage = when {
    protestsInLastHour <= 1 -> ProtestStage.COMPLAINT
    protestsInLastHour == 2 -> ProtestStage.STRIKE
    else -> ProtestStage.REVOLUTION
}

fun recentProtests(times: List<Long>, now: Long): List<Long> = times.filter { now - it < 3_600_000L }

/** Rancor: depois de castigo ou fuga, por 3 minutos ele não acha graça em nada (carinho perdoa antes). */
const val GRUDGE_MS = 3 * 60_000L
fun grudgeUntil(now: Long): Long = now + GRUDGE_MS
fun holdsGrudge(until: Long, now: Long): Boolean = now < until

/** Às vezes, bravo e sem muito vínculo, ele finge que morreu. */
fun faints(bond: Float, random: Random): Boolean = bond < .7f && random.nextFloat() < .12f

val STRIKE_SIGNS = listOf("GREVE GERAL!", "PAREI!", "NÃO TRABALHO MAIS!")
val REVOLUTION_SIGNS = listOf("REVOLUÇÃO!", "ABAIXO O DEDO!", "O PODER É DOS ROBÔS!")
val STRIKE_LINES = listOf("Tô de greve.", "Não falo com patrão.", "Fala com o sindicato.")
val GRUDGE_LINES = listOf("Ainda tô bravo.", "Não tem graça.", "Lembra do castigo?", "Hmpf.")
val CORNER_FEAR_LINES = listOf("Não! Lá não!", "Tudo menos o cantinho!", "Eu me comporto!")
val DRAMA_LINES = listOf("Você me matou...", "Diga à IRIS que eu a amei.", "Adeus, mundo cruel.", "Vejo a luz...", "Foi bom enquanto durou.")
val SAD_NOTES = listOf("Ninguém me ama", "Tô aqui, tá?", "Volta logo", "Saudade :(")
