package com.brendon978.menulauncher.pet

import kotlin.math.hypot
import kotlin.math.max
import kotlin.random.Random

// ---- jokenpô ----
enum class Hand { ROCK, PAPER, SCISSORS }

/** Resultado do ponto de vista de quem joga com o Glitch. */
enum class Outcome { WIN, LOSE, DRAW }

fun jokenpo(you: Hand, glitch: Hand): Outcome = when {
    you == glitch -> Outcome.DRAW
    (you == Hand.ROCK && glitch == Hand.SCISSORS) ||
        (you == Hand.PAPER && glitch == Hand.ROCK) ||
        (you == Hand.SCISSORS && glitch == Hand.PAPER) -> Outcome.WIN
    else -> Outcome.LOSE
}

fun glitchHand(random: Random): Hand = Hand.entries.random(random)

val JOKENPO_LOSE_LINES = listOf("Revanche! Agora!", "Roubou!", "Sorte de principiante.", "De novo, porra!", "Isso não valeu!")
val JOKENPO_WIN_LINES = listOf("Hehe, otário!", "Chupa!", "Sou imbatível!", "Fácil demais.", "Mais uma?")
val JOKENPO_DRAW_LINES = listOf("Empate!", "Pensamos igual...", "De novo!")

// ---- esconde-esconde ----
enum class HideGuess { FOUND, WARM, COLD }

/** Tocou dentro do esconderijo: achou. Perto (até 250 px): quente. Longe: frio. */
fun guessHide(hide: Area, x: Float, y: Float): HideGuess {
    val dx = max(max(hide.l - x, 0f), x - hide.r)
    val dy = max(max(hide.t - y, 0f), y - hide.b)
    val d = hypot(dx, dy)
    return when {
        d == 0f -> HideGuess.FOUND
        d < 250f -> HideGuess.WARM
        else -> HideGuess.COLD
    }
}

val HIDE_FOUND_LINES = listOf("Ah, me achou!", "Droga!", "Como você sabia?!", "Tá, tá, você ganhou.")
val HIDE_WARM_LINES = listOf("Tá quente...", "Quase!", "Hmm, perto...")
val HIDE_COLD_LINES = listOf("Tá frio!", "Nem perto, mané!", "Errou feio!", "Hehe, não tô aí.")

// ---- pega-pega ----
val TAG_LINES = listOf("Não me pega!", "Lerdo!", "Tenta de novo!", "Wheee!")
val TAG_CAUGHT_LINES = listOf("Ai, me pegou!", "Tá com você!", "Não valeu, escorreguei!")

// ---- bolhas ----
val BUBBLE_LINES = listOf("Pop!", "Mais uma!", "Hehe, estourei!", "Bolha!")

// ---- aprender a se encolher (condicionamento simples) ----
/** Expectativa de levar cutucada: sobe com cada cutucada e desce com carinho/risada (taxa 0,25). */
fun expectPoke(e: Float, poked: Boolean): Float = (e + .25f * ((if (poked) 1f else 0f) - e)).coerceIn(0f, 1f)

/** Com expectativa alta ele se encolhe quando você encosta. */
fun flinches(e: Float): Boolean = e > .6f

// ---- travessuras ----
val POSTIT_LINES = listOf(
    "Vai beber água!", "Me liga", "Tô de olho 👀", "Larga esse celular", "Você é legal (às vezes)",
    "Hoje tem pizza?", "Lembra de mim", "Glitch esteve aqui", "Sorria :)", "Vai dormir, mané",
    "Não esquece do carregador", "Bora jogar?",
)
val POSTIT_REMOVED_LINES = listOf("Ei, era um recado!", "Tá, nem queria mesmo.", "Hmpf, joga fora então.")

// ---- trabalhar junto ----
/** Apps que contam como "trabalho" (por pedaço do nome do pacote). */
val WORK_APP_HINTS = listOf("github", "notes", "obsidian", "office", "docs", "word", "outlook", "webapk", "linkedin", "chatgpt", "promedico")
fun isWorkApp(pkg: String): Boolean = WORK_APP_HINTS.any { it in pkg.lowercase() }
val WORK_LINES = listOf("Trabalhou bastante, hein?", "Eu também tava trabalhando.", "Deploy feito?", "Commitou?", "Fechei 3 tickets enquanto isso.")

// ---- estado das brincadeiras na tela ----
data class Bubble(val id: Int, val x: Float, val y: Float, val r: Float, val born: Long)
data class PostItNote(val id: Int, val text: String, val x: Float, val y: Float, val tilt: Float)

/** Último app aberto pelo launcher (para o "trabalhar junto"). */
object GlitchSignals {
    @Volatile var lastLaunch: Pair<String, Long>? = null
    /** Último toque na tela inicial (coordenadas da janela), para os olhos dele seguirem. */
    @Volatile var lastTouch: Triple<Float, Float, Long>? = null
}
