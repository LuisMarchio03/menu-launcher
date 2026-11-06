package com.brendon978.menulauncher.pet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

// ---- interações ----

/** Peteleco: rápido e comprido o bastante para não confundir com carinho. */
fun isFlick(dx: Float, elapsedMs: Long, minDistance: Float): Boolean = elapsedMs in 1..280 && kotlin.math.abs(dx) >= minDistance

/** Cócegas: muito vai e vem em pouco tempo. */
fun isTickle(reversals: Int, elapsedMs: Long): Boolean = reversals >= 6 && elapsedMs <= 1_800

val SHOVE_LINES = listOf("Ei! Sem empurrar!", "Quem foi?!", "Meu parafuso!", "Vai empurrar a tua avó!")
val TICKLE_LINES = listOf("Para, para! Hahaha!", "Cócegas não vale!", "01001000! Hahaha!")
val BUDDY_TAP_LINES = listOf("Ele gostou!", "Não mima muito, hein.", "Tá, ele é fofo mesmo.")

// ---- memória ----

enum class MemoryTap { CORRECT, ROUND_DONE, WRONG }

fun memoryTap(sequence: List<Int>, position: Int, pad: Int): MemoryTap = when {
    sequence.getOrNull(position) != pad -> MemoryTap.WRONG
    position == sequence.lastIndex -> MemoryTap.ROUND_DONE
    else -> MemoryTap.CORRECT
}

/** Cada rodada acende um pouco mais rápido. */
fun memoryFlashMs(length: Int): Long = (560L - length * 28L).coerceAtLeast(230L)

@Composable
fun MemoryPanel(color: Color, lit: Int?, enabled: Boolean, score: Int, best: Int, onPad: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            if (enabled) "Sua vez!" else "Olha bem...",
            color = color,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.background(Color(0xCC0C0C0E), RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 6.dp),
        )
        for (row in 0..1) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (col in 0..1) {
                val pad = row * 2 + col
                Box(
                    Modifier
                        .size(82.dp)
                        .background(if (lit == pad) color else Color(0xCC0C0C0E), RoundedCornerShape(16.dp))
                        .border(2.dp, color, RoundedCornerShape(16.dp))
                        .clickable(remember { MutableInteractionSource() }, indication = null, enabled = enabled) { onPad(pad) },
                )
            }
        }
        Text(
            "Rodada $score" + if (best > 0) "  ·  recorde $best" else "",
            color = color.copy(alpha = .85f),
            fontSize = 14.sp,
            modifier = Modifier.background(Color(0xCC0C0C0E), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

// ---- voo ----

/** Um muro com abertura: o centro da abertura é [gapY]. */
data class FlyWall(val x: Float, val gapY: Float, val passed: Boolean = false)

data class FlyState(val y: Float, val vy: Float = 0f, val walls: List<FlyWall> = emptyList(), val score: Int = 0, val crashed: Boolean = false, val started: Boolean = true)

/** Medidas do voo, proporcionais ao tamanho dele. */
data class FlyWorld(val area: Area, val x: Float, val w: Float, val h: Float) {
    val gravity get() = h * 10f
    val thrust get() = -h * 3.6f
    val speed get() = h * 2.5f
    val gap get() = h * 2.7f
    val wall get() = h * .7f
    val spacing get() = h * 3.6f
}

fun flyTap(s: FlyState, world: FlyWorld): FlyState = s.copy(vy = world.thrust, started = true)

fun flyStep(s: FlyState, dt: Float, world: FlyWorld, random: Random): FlyState {
    if (!s.started) return s // parado no ar até o primeiro toque
    val a = world.area
    val rising = s.vy + world.gravity * dt
    val y = (s.y + rising * dt).coerceAtLeast(a.t) // o teto segura, não derruba
    val vy = if (y <= a.t && rising < 0f) 0f else rising
    var score = s.score
    var walls = s.walls.map { it.copy(x = it.x - world.speed * dt) }.filter { it.x + world.wall > a.l }
    walls = walls.map { w ->
        if (!w.passed && w.x + world.wall < world.x) { score++; w.copy(passed = true) } else w
    }
    if ((walls.lastOrNull()?.x ?: 0f) < a.r - world.spacing) {
        val margin = world.gap / 2 + world.h * .4f
        walls = walls + FlyWall(a.r, a.t + margin + random.nextFloat() * (a.b - a.t - margin * 2))
    }
    val top = y
    val bottom = y + world.h
    val hit = walls.any { w ->
        val inX = world.x + world.w * .8f > w.x && world.x + world.w * .2f < w.x + world.wall
        inX && (top + world.h * .08f < w.gapY - world.gap / 2 || bottom - world.h * .08f > w.gapY + world.gap / 2)
    }
    return FlyState(y.coerceIn(a.t, a.b - world.h), vy, walls, score, hit || bottom > a.b)
}

fun DrawScope.drawFlyWalls(color: Color, s: FlyState, world: FlyWorld, lw: Float = 1f) {
    val a = world.area
    val st = Stroke(2.4f * lw)
    val fill = Color(0xE60C0C0E)
    for (w in s.walls) {
        val top = w.gapY - world.gap / 2 - a.t
        val bottomY = w.gapY + world.gap / 2
        for ((o, sz) in listOf(Offset(w.x, a.t) to Size(world.wall, top), Offset(w.x, bottomY) to Size(world.wall, a.b - bottomY))) {
            drawRect(fill, o, sz)
            drawRect(color, o, sz, style = st)
        }
    }
}

// ---- falas espontâneas ----

private val HAPPY_IDLE = listOf("Tô de boa aqui.", "Você é legal, sabia?", "Essa tela é minha casa.", "Melhor humano.", "Hoje tô inspirado.")
private val GRUMPY_IDLE = listOf("Hmpf.", "Tô de olho.", "Lembro de cada cutucada.", "Nem vem.")
private val NEUTRAL_IDLE = listOf("Alguém viu meu parafuso?", "Beep. Boop. Brincadeira.", "Será que robô sonha?", "Tédio é um bug?", "Quanto tempo tá isso?")

fun idleLine(hour: Int, bond: Float, random: Random): String {
    val byHour = when (hour) {
        in 7..10 -> "Bora acordar esse dia."
        in 12..13 -> "Hora do almoço, né?"
        in 19..22 -> "Noite boa pra não fazer nada."
        else -> null
    }
    val pool = when {
        bond >= .7f -> HAPPY_IDLE
        bond <= .3f -> GRUMPY_IDLE
        else -> NEUTRAL_IDLE
    }
    return if (byHour != null && random.nextInt(4) == 0) byHour else pool.random(random)
}

/** Fala sozinho a cada 2 a 5 minutos: de vez em quando, sem encher. */
fun nextIdleGapMs(random: Random): Long = random.nextLong(120_000, 300_000)
