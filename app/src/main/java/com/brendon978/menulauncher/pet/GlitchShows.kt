package com.brendon978.menulauncher.pet

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Os números curtos que ele faz por conta própria, de vez em quando. */
enum class Show(val ms: Long) {
    CLONES(15_000), JETPACK(5_000), SKATE(6_000), SCOOTER(6_000),
    COFFEE(5_000), PUSHUPS(6_000), WATER(5_000), BLOCKS(5_000), MAGIC(7_000), GRAFFITI(6_000),
    PARACHUTE(2_600), DROP(1_600), BOX(2_400),
}

val ENTRANCES = listOf(Show.PARACHUTE, Show.DROP, Show.BOX)
val VEHICLES = listOf(Show.JETPACK, Show.SKATE, Show.SCOOTER)

/** Um show rodando. [variant] sorteia o golpe, o truque ou o desenho; [evil] diz se há clone do mal. */
data class ShowRun(val kind: Show, val at: Long, val variant: Int = 0, val evil: Boolean = false) {
    fun progress(now: Long) = ((now - at).toFloat() / kind.ms).coerceIn(0f, 1f)
    fun active(now: Long) = now - at < kind.ms
}

/** Sorteia sem repetir o último; o que depende de hora ou do dia fica de fora quando não cabe. */
fun pickShow(previous: Show?, hour: Int, canWater: Boolean, random: Random): Show {
    val options = Show.entries.filter { it !in ENTRANCES && it != previous }
        .filter { it != Show.COFFEE || hour in 7..11 }
        .filter { it != Show.WATER || canWater }
    return options.random(random)
}

/** Volta depois de pelo menos 1 minuto fora: às vezes chega de um jeito especial. */
fun entranceFor(awayMs: Long, random: Random): Show? =
    if (awayMs >= 60_000L && random.nextFloat() < .6f) ENTRANCES.random(random) else null

// ---- batalha ----

enum class BattlePhase { APPEAR, CONFUSED, TURN, ALLIES, FINISH, BOOM, VICTORY, GONE }

enum class Finisher(val line: String) {
    SWORD("Toma, falsificado!"), RAY("Pew pew!"), WAND("Abracadabra, some!"),
    AK("Agora ela atira!"), HAMMER("MARRETADA!"), ROCKET("Foguetão nele!"),
}

fun pickFinisher(variant: Int): Finisher = Finisher.entries[variant.mod(Finisher.entries.size)]

/** Só parte das vezes um clone vira do mal; nas outras, eles se olham e somem. */
fun battleHasEvil(random: Random): Boolean = random.nextFloat() < .7f

fun battlePhase(k: Float, evil: Boolean): BattlePhase = when {
    k < .12f -> BattlePhase.APPEAR
    !evil && k < .7f -> BattlePhase.CONFUSED
    !evil -> BattlePhase.GONE
    k < .28f -> BattlePhase.CONFUSED
    k < .4f -> BattlePhase.TURN
    k < .58f -> BattlePhase.ALLIES
    k < .78f -> BattlePhase.FINISH
    k < .88f -> BattlePhase.BOOM
    k < .97f -> BattlePhase.VICTORY
    else -> BattlePhase.GONE
}

val BATTLE_APPEAR_LINES = listOf("Ué, tem mais de mim?", "Bug na matrix!", "Quem são esses caras bonitos?")
val BATTLE_EVIL_LINES = listOf("Muahaha!", "Agora eu sou o Glitch!", "A tela é minha!")
val BATTLE_ALLY_LINES = listOf("Segura ele!", "Pra cima dele, galera!", "Glitches, atacar!")
val BATTLE_WIN_LINES = listOf("Bug corrigido.", "Valeu, galera!", "Ninguém copia esse corpinho.")
val BATTLE_NO_EVIL_LINES = listOf("Foi mal, galera, tchau.", "Control+Z.")

/** Rosto e braços do Glitch de verdade em cada fase da batalha. */
fun heroPose(phase: BattlePhase, finisher: Finisher, facingLeft: Boolean, t: Float): RobotPose = when (phase) {
    BattlePhase.APPEAR, BattlePhase.GONE -> RobotPose(Face.SURPRISED, facingLeft, phase = t)
    BattlePhase.CONFUSED -> RobotPose(Face.CONFUSED, facingLeft, armL = 60f, armR = -60f, headTilt = 10f * sin(t * 3f), phase = t)
    BattlePhase.TURN -> RobotPose(Face.SURPRISED, facingLeft, armL = 70f, armR = -70f, phase = t)
    BattlePhase.ALLIES -> RobotPose(Face.ANGRY, facingLeft, armL = 30f, armR = -118f, phase = t)
    BattlePhase.FINISH -> when (finisher) {
        Finisher.AK -> RobotPose(Face.ANGRY, facingLeft, armL = -38f, armR = 22f, gun = GunHold.MARCH, phase = t)
        Finisher.SWORD, Finisher.HAMMER -> RobotPose(Face.ANGRY, facingLeft, armL = 20f, armR = -112f + 60f * sin(t * 18f), phase = t)
        else -> RobotPose(Face.ANGRY, facingLeft, armL = 20f, armR = -90f, phase = t)
    }
    BattlePhase.BOOM -> RobotPose(Face.GIGGLE, facingLeft, armL = 20f, armR = -90f, phase = t)
    BattlePhase.VICTORY -> RobotPose(Face.BLISS, facingLeft, armL = 125f, armR = -125f, lift = 8f * abs(sin(t * 8f)), phase = t)
}

private val EVIL = Color(0xFFFF3B30)

private fun DrawScope.spark(c: Color, at: Offset, r: Float) {
    for (i in 0 until 4) {
        val a = i * PI.toFloat() / 2f + .4f
        drawLine(c, at, at + Offset(cos(a), sin(a)) * r, r * .2f, StrokeCap.Round)
    }
}

/** Clones, golpe e explosão. [hero] e os outros são o canto superior esquerdo de cada robô, em px. */
fun DrawScope.drawBattle(
    color: Color, run: ShowRun, k: Float, t: Float, hero: Offset, width: Float, height: Float, left: Offset, right: Offset, evilAt: Offset, lw: Float = 1f,
) {
    val phase = battlePhase(k, run.evil)
    val finisher = pickFinisher(run.variant)
    val appear = if (phase == BattlePhase.APPEAR) (if ((t * 14f).toInt() % 2 == 0) .35f else .9f) else 1f
    fun clone(at: Offset, c: Color, pose: RobotPose) {
        translate(at.x, at.y) { drawRobot(c.copy(alpha = c.alpha * appear), pose, lw, height) }
    }
    val u = height / 170f
    if (phase == BattlePhase.GONE) return
    val punch = phase == BattlePhase.ALLIES
    val allyPose = when {
        phase == BattlePhase.VICTORY -> RobotPose(Face.BLISS, armL = 125f, armR = -125f, lift = 8f * abs(sin(t * 8f + 1f)), phase = t)
        punch -> RobotPose(Face.ANGRY, armL = 80f, armR = -80f + 40f * sin(t * 20f), lift = 3f * abs(sin(t * 16f)), phase = t)
        else -> RobotPose(Face.CONFUSED, armL = 60f, armR = -60f, headTilt = -10f * sin(t * 3f), phase = t)
    }
    clone(left, color, allyPose)
    clone(right, color, allyPose.copy(facingLeft = true))
    val turning = phase == BattlePhase.TURN || phase == BattlePhase.ALLIES || phase == BattlePhase.FINISH
    val alive = phase != BattlePhase.BOOM && phase != BattlePhase.VICTORY
    if (alive) {
        val shake = if (punch || phase == BattlePhase.FINISH) 4f * sin(t * 40f) else 0f
        val pose = if (turning) RobotPose(Face.ANGRY, armL = 40f, armR = -40f, tilt = shake, phase = t, offsetX = shake)
        else RobotPose(Face.CONFUSED, armL = 60f, armR = -60f, phase = t)
        clone(evilAt, if (turning && run.evil) EVIL else color, pose)
    }
    val target = evilAt + Offset(width / 2, height * .45f)
    val hand = hero + Offset(width * .85f, height * .55f)
    val dir = if (target.x >= hand.x) 1f else -1f
    if (phase == BattlePhase.FINISH) {
        val f = ((k - .58f) / .2f).coerceIn(0f, 1f)
        when (finisher) {
            Finisher.SWORD -> {
                val a = -80f + 150f * ((f * 3f) % 1f)
                rotate(a * dir, pivot = target) {
                    drawLine(color, target + Offset(0f, 8f * u), target - Offset(0f, 78f * u), 4f * u, StrokeCap.Round)
                    drawLine(color, target - Offset(14f * u, 0f), target + Offset(14f * u, 0f), 4f * u, StrokeCap.Round)
                }
            }
            Finisher.RAY -> {
                val c = Color.White
                drawLine(color, hand, target, 9f * u, StrokeCap.Round)
                drawLine(c, hand, target, 3.5f * u, StrokeCap.Round)
                spark(color, target, 18f * u)
            }
            Finisher.WAND -> {
                drawLine(color, hand, hand + Offset(30f * u * dir, -22f * u), 3f * u, StrokeCap.Round)
                for (i in 0 until 6) {
                    val p = ((f * 2f) + i / 6f) % 1f
                    spark(color, hand + (target - hand) * p + Offset(0f, sin(p * 9f) * 16f * u), 8f * u)
                }
            }
            Finisher.AK -> for (i in 0 until 7) {
                val p = ((f * 5f) + i / 7f) % 1f
                drawCircle(color, 3f * u, hand + (target - hand) * p)
                spark(color, target, 12f * u)
            }
            Finisher.HAMMER -> {
                // sobe em pé ao lado dele e desce na cabeça
                val swing = ((f * 2.5f) % 1f)
                val base = target + Offset(-dir * 100f * u, 10f * u)
                rotate(92f * dir * swing * swing, pivot = base) {
                    drawLine(color, base, base - Offset(0f, 90f * u), 5f * u, StrokeCap.Round)
                    drawRoundRect(color, base - Offset(26f * u, 108f * u), Size(52f * u, 26f * u))
                }
            }
            Finisher.ROCKET -> {
                val at = hand + (target - hand) * f
                drawRoundRect(color, at - Offset(16f * u, 6f * u), Size(32f * u, 12f * u))
                for (i in 1..3) drawCircle(color.copy(alpha = .6f), (5 - i) * u, at - Offset(16f * u * dir * i, 0f))
            }
        }
    }
    if (phase == BattlePhase.BOOM) {
        // o clone do mal vira pixels
        val f = ((k - .78f) / .1f).coerceIn(0f, 1f)
        val rnd = Random(run.at)
        for (i in 0 until 36) {
            val a = rnd.nextFloat() * 6.283f
            val d = (20f + rnd.nextFloat() * 110f) * u * f
            val at = target + Offset(cos(a) * d, sin(a) * d - 30f * u * f)
            val c = if (i % 3 == 0) color else EVIL
            drawRect(c.copy(alpha = 1f - f), at, Size(7f * u, 7f * u))
        }
    }
}

// ---- entradas e veículos ----

/** Pose durante cada show; null significa que ele segue como estava. */
fun showPose(run: ShowRun, k: Float, facingLeft: Boolean, t: Float): RobotPose? {
    val s = sin(t * 12f)
    return when (run.kind) {
        Show.CLONES -> heroPose(battlePhase(k, run.evil), pickFinisher(run.variant), facingLeft, t)
        Show.JETPACK -> RobotPose(Face.GIGGLE, facingLeft, armL = 40f, armR = -40f, legL = 18f, legR = 8f, tilt = if (facingLeft) -10f else 10f, phase = t)
        Show.SKATE -> RobotPose(Face.BLISS, facingLeft, armL = 80f, armR = -80f, legL = 12f, legR = -12f, tilt = if (facingLeft) -6f else 6f, lift = 8f, phase = t)
        Show.SCOOTER -> RobotPose(Face.NORMAL, facingLeft, armL = -30f, armR = -30f, lift = 8f, phase = t)
        Show.PARACHUTE -> RobotPose(Face.SURPRISED, facingLeft, armL = 125f, armR = -125f, phase = t)
        Show.DROP -> if (k < .7f) RobotPose(Face.SURPRISED, facingLeft, armL = 110f, armR = -110f, phase = t)
        else RobotPose(Face.DIZZY, facingLeft, armL = 60f, armR = -60f, lift = -10f, phase = t)
        Show.BOX -> RobotPose(Face.GIGGLE, facingLeft, armL = 125f, armR = -125f, lift = 40f * sin((k * PI.toFloat()).coerceAtMost(3.1f)), phase = t)
        Show.COFFEE -> RobotPose(Face.BLISS, facingLeft, armL = 8f, armR = -96f + 28f * abs(sin(t * 1.2f)), phase = t)
        Show.PUSHUPS -> RobotPose(Face.SERIOUS, facingLeft, armL = 20f, armR = -20f, rotation = 82f, lift = -16f + 7f * sin(t * 6f), phase = t)
        Show.WATER -> RobotPose(Face.BLISS, facingLeft, armL = 8f, armR = -80f, phase = t)
        Show.BLOCKS -> RobotPose(Face.SERIOUS, facingLeft, armL = 90f + 12f * s, armR = -90f - 12f * s, phase = t)
        Show.MAGIC -> {
            val wave = if (k < .55f) 22f * sin(t * 10f) else 0f
            val fail = run.variant == 0
            RobotPose(if (fail && k > .7f) Face.SAD else Face.GIGGLE, facingLeft, armL = 18f, armR = -100f + wave, phase = t)
        }
        Show.GRAFFITI -> RobotPose(Face.SERIOUS, facingLeft, armL = 8f, armR = -96f + 6f * sin(t * 18f), phase = t)
    }
}

val SHOW_LINES = mapOf(
    Show.JETPACK to "Saindo da frente!", Show.SKATE to "Radical!", Show.SCOOTER to "Bi-bi!",
    Show.COFFEE to "Café primeiro, gente depois.", Show.PUSHUPS to "Um... dois... três...", Show.WATER to "Cresce, plantinha!",
    Show.BLOCKS to "Mais um andar!", Show.MAGIC to "Respeitável público...", Show.GRAFFITI to "Arte urbana.",
    Show.PARACHUTE to "Cheguei!", Show.BOX to "Surpresa!", Show.DROP to "Aaaaaah!",
)
val MAGIC_ITEMS = listOf("Um coelho!", "Um pombo!", "Uma meia?!", "Um pendrive!")
const val MAGIC_FAIL_LINE = "Era pra ser... deixa pra lá."

// ---- plantinha e torrinha ----

/** Regada uma vez por dia; cresce até 6 estágios. Três dias sem água e ela murcha, mas não morre. */
fun plantStage(watered: Int): Int = watered.coerceIn(0, 6)
fun plantWilted(lastWateredDay: Int, today: Int): Boolean = lastWateredDay != 0 && today - lastWateredDay >= 3
fun canWater(lastWateredDay: Int, today: Int): Boolean = lastWateredDay != today

/** A torre cai ao passar do limite. */
const val TOWER_MAX = 6
fun addBlock(blocks: Int): Pair<Int, Boolean> = if (blocks + 1 > TOWER_MAX) 0 to true else (blocks + 1) to false

fun DrawScope.drawGarden(color: Color, watered: Int, wilted: Boolean, blocks: Int, ground: Offset, u: Float) {
    val st = Stroke(1.8f * u, cap = StrokeCap.Round)
    // vaso
    val pot = Path().apply {
        moveTo(ground.x - 10f * u, ground.y - 13f * u); lineTo(ground.x + 10f * u, ground.y - 13f * u)
        lineTo(ground.x + 7f * u, ground.y); lineTo(ground.x - 7f * u, ground.y); close()
    }
    drawPath(pot, color, style = st)
    val stage = plantStage(watered)
    val h = (6f + stage * 7f) * u
    val top = ground - Offset(0f, 13f * u + h)
    val lean = if (wilted) 12f * u else 0f
    drawPath(Path().apply { moveTo(ground.x, ground.y - 13f * u); quadraticTo(ground.x, top.y, ground.x + lean, top.y + lean) }, color, style = st)
    for (i in 1..stage) {
        val y = ground.y - 13f * u - i * 7f * u + (if (wilted) 5f * u else 0f)
        val side = if (i % 2 == 0) 1f else -1f
        drawOval(color, Offset(ground.x + side * 8f * u - 5f * u, y - 2.5f * u), Size(10f * u, 5f * u), style = Stroke(1.4f * u))
    }
    if (stage >= 6 && !wilted) drawCircle(color, 4f * u, top)
    // torrinha ao lado
    val b = 11f * u
    for (i in 0 until blocks) {
        val x = ground.x + 22f * u + (if (i % 2 == 0) 0f else 2f * u)
        drawRoundRect(color, Offset(x, ground.y - b * (i + 1)), Size(b * 1.5f, b - 1f * u), style = Stroke(1.6f * u))
    }
}

/** Adereços de cada show, desenhados em volta dele. [at] é o canto superior esquerdo do robô. */
fun DrawScope.drawShowProps(color: Color, run: ShowRun, k: Float, t: Float, at: Offset, width: Float, height: Float, garden: Offset) {
    val u = height / 170f
    val st = Stroke(2.2f * u, cap = StrokeCap.Round)
    val feet = at + Offset(width / 2, height * .95f)
    val hand = at + Offset(width * .86f, height * .5f)
    when (run.kind) {
        Show.JETPACK -> {
            drawRoundRect(color, at + Offset(width * .1f, height * .48f), Size(22f * u, 42f * u), style = st)
            for (i in 0 until 3) {
                val y = at.y + height * .78f + i * 13f * u + sin(t * 30f + i) * 5f * u
                drawCircle(color.copy(alpha = 1f - i * .3f), (7 - i * 2) * u, Offset(at.x + width * .17f, y))
            }
        }
        Show.SKATE -> {
            drawLine(color, feet - Offset(44f * u, 0f), feet + Offset(44f * u, 0f), 4f * u, StrokeCap.Round)
            drawCircle(color, 6f * u, feet + Offset(-28f * u, 8f * u), style = st)
            drawCircle(color, 6f * u, feet + Offset(28f * u, 8f * u), style = st)
        }
        Show.SCOOTER -> {
            drawLine(color, feet - Offset(36f * u, 0f), feet + Offset(30f * u, 0f), 4f * u, StrokeCap.Round)
            drawLine(color, feet + Offset(30f * u, 0f), feet + Offset(30f * u, -86f * u), 3f * u, StrokeCap.Round)
            drawLine(color, feet + Offset(20f * u, -86f * u), feet + Offset(42f * u, -86f * u), 3f * u, StrokeCap.Round)
            drawCircle(color, 7f * u, feet + Offset(-34f * u, 8f * u), style = st)
            drawCircle(color, 7f * u, feet + Offset(30f * u, 8f * u), style = st)
        }
        Show.PARACHUTE -> {
            val top = at + Offset(width / 2, -56f * u)
            drawArc(color, 180f, 180f, true, top - Offset(70f * u, 46f * u), Size(140f * u, 92f * u), style = st)
            for (x in listOf(-68f, -24f, 24f, 68f)) drawLine(color, top + Offset(x * u, 0f), at + Offset(width / 2, height * .25f), 1.4f * u)
        }
        Show.BOX -> {
            val w = 96f * u
            val x = feet.x - w / 2
            drawRoundRect(color, Offset(x, feet.y - 70f * u), Size(w, 70f * u), style = st)
            drawLine(color, Offset(x, feet.y - 70f * u), Offset(x - 26f * u, feet.y - 92f * u), 2.2f * u, StrokeCap.Round)
            drawLine(color, Offset(x + w, feet.y - 70f * u), Offset(x + w + 26f * u, feet.y - 92f * u), 2.2f * u, StrokeCap.Round)
        }
        Show.COFFEE -> {
            drawRoundRect(color, hand - Offset(0f, 12f * u), Size(18f * u, 18f * u), style = st)
            for (i in 0 until 2) {
                val p = ((t * .7f) + i * .5f) % 1f
                drawCircle(color.copy(alpha = 1f - p), 2.4f * u, hand + Offset((5f + i * 8f) * u, (-16f - p * 28f) * u))
            }
        }
        Show.WATER -> {
            drawRoundRect(color, hand - Offset(0f, 10f * u), Size(26f * u, 20f * u), style = st)
            drawLine(color, hand + Offset(26f * u, -2f * u), hand + Offset(40f * u, -14f * u), 2.4f * u, StrokeCap.Round)
            for (i in 0 until 5) {
                val p = ((t * 1.4f) + i / 5f) % 1f
                val from = hand + Offset(40f * u, -14f * u)
                drawCircle(color.copy(alpha = 1f - p * .6f), 1.8f * u, from + (garden - from) * p)
            }
        }
        Show.BLOCKS -> {
            val y = at.y + height * .4f - 14f * u * abs(sin(t * 3f))
            drawRoundRect(color, Offset(at.x + width / 2 - 10f * u, y - 16f * u), Size(20f * u, 14f * u), style = st)
        }
        Show.MAGIC -> {
            val base = at + Offset(width * 1.3f, height * .96f)
            drawRoundRect(color, base - Offset(18f * u, 26f * u), Size(36f * u, 26f * u), style = st)
            drawLine(color, base - Offset(28f * u, 26f * u), base + Offset(28f * u, -26f * u), 2.6f * u, StrokeCap.Round)
            drawLine(color, hand, hand + Offset(26f * u, -14f * u), 3f * u, StrokeCap.Round)
            if (k in .5f..1f) {
                val fail = run.variant == 0
                val rise = ((k - .5f) / .3f).coerceIn(0f, 1f)
                val p = base - Offset(0f, 30f * u + rise * 50f * u)
                if (fail) drawCircle(color.copy(alpha = 1f - rise), 26f * u * rise, p, style = st)
                else {
                    drawOval(color, p - Offset(12f * u, 9f * u), Size(24f * u, 18f * u), style = st)
                    drawLine(color, p + Offset(-6f * u, -9f * u), p + Offset(-8f * u, -22f * u), 2f * u, StrokeCap.Round)
                    drawLine(color, p + Offset(6f * u, -9f * u), p + Offset(8f * u, -22f * u), 2f * u, StrokeCap.Round)
                    spark(color, p + Offset(24f * u, -16f * u), 8f * u)
                }
            }
        }
        Show.GRAFFITI -> drawRoundRect(color, hand - Offset(4f * u, 14f * u), Size(10f * u, 24f * u), style = st)
        else -> Unit
    }
}

// ---- grafite ----

enum class Graffiti { HEART, SMILE, BOLT, GLITCH }

/** Desenho em pontos de 0 a 1; cada lista é um traço. */
fun graffitiStrokes(g: Graffiti): List<List<Offset>> = when (g) {
    Graffiti.HEART -> listOf(listOf(Offset(.5f, .9f), Offset(.1f, .45f), Offset(.15f, .2f), Offset(.35f, .15f), Offset(.5f, .35f), Offset(.65f, .15f), Offset(.85f, .2f), Offset(.9f, .45f), Offset(.5f, .9f)))
    Graffiti.SMILE -> listOf(
        listOf(Offset(.15f, .15f), Offset(.85f, .15f), Offset(.85f, .85f), Offset(.15f, .85f), Offset(.15f, .15f)),
        listOf(Offset(.35f, .35f), Offset(.35f, .45f)), listOf(Offset(.65f, .35f), Offset(.65f, .45f)),
        listOf(Offset(.3f, .62f), Offset(.5f, .72f), Offset(.7f, .62f)),
    )
    Graffiti.BOLT -> listOf(listOf(Offset(.6f, .05f), Offset(.25f, .55f), Offset(.5f, .55f), Offset(.4f, .95f), Offset(.78f, .4f), Offset(.52f, .4f), Offset(.6f, .05f)))
    Graffiti.GLITCH -> listOf(
        listOf(Offset(.8f, .2f), Offset(.2f, .2f), Offset(.2f, .8f), Offset(.8f, .8f), Offset(.8f, .5f), Offset(.5f, .5f)),
    )
}

fun DrawScope.drawGraffiti(color: Color, g: Graffiti, area: androidx.compose.ui.geometry.Rect, shown: Float) {
    val strokes = graffitiStrokes(g)
    val total = strokes.sumOf { it.size - 1 }.coerceAtLeast(1)
    var budget = shown * total
    for (s in strokes) {
        for (i in 0 until s.size - 1) {
            if (budget <= 0f) return
            val f = budget.coerceAtMost(1f)
            val a = area.topLeft + Offset(s[i].x * area.width, s[i].y * area.height)
            val b = area.topLeft + Offset(s[i + 1].x * area.width, s[i + 1].y * area.height)
            drawLine(color.copy(alpha = color.alpha * .9f), a, a + (b - a) * f, 3f, StrokeCap.Round)
            budget -= 1f
        }
    }
}

/** Passar o dedo por este tanto (em px) apaga o desenho. */
fun erased(rubbed: Float, areaWidth: Float): Boolean = rubbed > areaWidth * 1.5f

// ---- bichinho ----

enum class Buddy(val label: String) { NONE("Nenhum"), DRONE("Mini drone"), CAT("Gato-robô") }

fun DrawScope.drawBuddy(color: Color, kind: Buddy, at: Offset, u: Float, t: Float) {
    val st = Stroke(1.8f * u, cap = StrokeCap.Round)
    when (kind) {
        Buddy.DRONE -> {
            drawRoundRect(color, at - Offset(12f * u, 7f * u), Size(24f * u, 14f * u), style = st)
            drawCircle(color, 2f * u, at)
            val spin = 11f * u * abs(cos(t * 30f))
            for (s in listOf(-1f, 1f)) drawLine(color, at + Offset(s * 15f * u - spin, -11f * u), at + Offset(s * 15f * u + spin, -11f * u), 2f * u, StrokeCap.Round)
        }
        Buddy.CAT -> {
            drawRoundRect(color, at - Offset(13f * u, 8f * u), Size(24f * u, 15f * u), style = st)
            drawCircle(color, 7f * u, at + Offset(14f * u, -8f * u), style = st)
            drawLine(color, at + Offset(9f * u, -14f * u), at + Offset(9f * u, -19f * u), 1.8f * u, StrokeCap.Round)
            drawLine(color, at + Offset(19f * u, -14f * u), at + Offset(19f * u, -19f * u), 1.8f * u, StrokeCap.Round)
            drawLine(color, at + Offset(-13f * u, -2f * u), at + Offset(-23f * u, -12f * u + sin(t * 4f) * 4f * u), 1.8f * u, StrokeCap.Round)
            for (x in listOf(-8f, 6f)) drawLine(color, at + Offset(x * u, 7f * u), at + Offset(x * u + sin(t * 8f + x) * 2f * u, 13f * u), 1.8f * u, StrokeCap.Round)
        }
        Buddy.NONE -> Unit
    }
}

val BUDDY_LINES = listOf("Ei, volta aqui!", "Quem te ensinou isso?", "Junto, junto!")
