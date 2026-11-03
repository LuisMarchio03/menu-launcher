package com.brendon978.menulauncher.pet

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.sin

/** Rosto na telinha quadrada. */
/** SURPRISED: pego no colo/voando; DIZZY: tonto depois do arremesso; BLISS: derretendo de carinho. */
enum class Face { NORMAL, SLEEP, MUSIC, HEART, TIRED, SCAN, GIGGLE, SERIOUS, ANGRY, SURPRISED, DIZZY, BLISS }

/**
 * Pose de um quadro. Ângulos em graus; [phase] é um relógio em segundos para as animações do rosto.
 * [blink] 1 = olho aberto, ~0 = fechado.
 */
data class RobotPose(
    val face: Face = Face.NORMAL,
    val facingLeft: Boolean = false,
    val legL: Float = 0f,
    val legR: Float = 0f,
    val armL: Float = 0f,
    val armR: Float = 0f,
    val tilt: Float = 0f,
    val lift: Float = 0f,
    val blink: Float = 1f,
    val antennaOn: Boolean = true,
    val phase: Float = 0f,
    /** AK de brinquedo (nunca atira): apontando para você, ou atravessada no peito marchando. */
    val gun: GunHold = GunHold.NONE,
    /** Cabeça inclinando no pescoço (headbang, robô). */
    val headTilt: Float = 0f,
    /** Deslize do corpo, em unidades do desenho (moonwalk, passinho de lado). */
    val offsetX: Float = 0f,
    /** Largura do corpo: 1 normal, perto de 0 de perfil (giro). */
    val spin: Float = 1f,
    /** De costas: sem telinha nem LEDs, com grade de ventilação e tampa traseira. */
    val back: Boolean = false,
)

enum class GunHold { NONE, AIM, MARCH }

/** Proporção do desenho (viewBox do esboço: 120 × 170). */
const val ROBOT_ASPECT = 120f / 170f

/** Desenha o robozinho do esboço (variante "corpo") em contorno, numa cor só, ocupando o DrawScope inteiro. */
fun DrawScope.drawRobot(color: Color, pose: RobotPose) {
    val u = size.height / 170f
    val line = Stroke(width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    scale(u, u, pivot = Offset.Zero) {
        // de perfil no giro ele não some de vez: no mínimo 8% de largura
        val spin = if (kotlin.math.abs(pose.spin) < .08f) (if (pose.spin < 0f) -.08f else .08f) else pose.spin
        val sx = (if (pose.facingLeft) -1f else 1f) * spin
        scale(sx, 1f, pivot = Offset(60f, 0f)) {
            translate(left = pose.offsetX, top = -pose.lift) {
                rotate(pose.tilt, pivot = Offset(60f, 153f)) {
                    // pernas sanfonadas com pé em cúpula (giram no quadril)
                    leg(52f, pose.legL, color, line)
                    leg(68f, pose.legR, color, line)
                    // corpo
                    drawRoundRect(color, Offset(44f, 80f), Size(32f, 44f), CornerRadius(8f), style = line)
                    if (pose.back) {
                        // tampa traseira com 4 parafusos
                        drawRoundRect(color.copy(alpha = color.alpha * .7f), Offset(49f, 88f), Size(22f, 26f), CornerRadius(3f), style = Stroke(1.4f))
                        for ((sx, sy) in listOf(51.5f to 90.5f, 68.5f to 90.5f, 51.5f to 111.5f, 68.5f to 111.5f)) drawCircle(color, 1f, Offset(sx, sy))
                    } else {
                        drawCircle(color, 1.6f, Offset(53f, 94f))
                        drawCircle(color.copy(alpha = color.alpha * .5f), 1.6f, Offset(59f, 94f))
                        drawLine(color.copy(alpha = color.alpha * .5f), Offset(52f, 106f), Offset(68f, 106f), 2.2f, StrokeCap.Round)
                    }
                    when (pose.gun) {
                        GunHold.AIM -> ak47(color)
                        GunHold.MARCH -> rotate(-42f, pivot = Offset(60f, 101f)) { ak47Side(color) }
                        GunHold.NONE -> Unit
                    }
                    // braços sanfonados (por cima da arma: as garrinhas seguram ela) (iguais às pernas) com mão de 3 garrinhas; abertos 10° para fora
                    arm(40f, 10f + pose.armL, color, line)
                    arm(80f, -10f + pose.armR, color, line)
                    // cabeça (no esboço: translate(9 2) scale(.85) sobre o desenho da cabeça)
                    rotate(pose.headTilt, pivot = Offset(60f, 80f)) {
                        translate(9f, 2f) {
                            scale(.85f, .85f, pivot = Offset.Zero) { head(color, pose, line) }
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.leg(x: Float, angle: Float, color: Color, line: Stroke) {
    rotate(angle, pivot = Offset(x, 124f)) {
        drawLine(color, Offset(x - 4, 124f), Offset(x - 4, 146f), 2.2f, StrokeCap.Round)
        drawLine(color, Offset(x + 4, 124f), Offset(x + 4, 146f), 2.2f, StrokeCap.Round)
        val ring = color.copy(alpha = color.alpha * .8f)
        var y = 129f
        while (y <= 145f) {
            drawLine(ring, Offset(x - 4, y), Offset(x + 4, y), 1.2f, StrokeCap.Round)
            y += 4f
        }
        val foot = Path().apply {
            moveTo(x - 8, 153f)
            quadraticTo(x - 8, 145f, x, 145f)
            quadraticTo(x + 8, 145f, x + 8, 153f)
            close()
        }
        drawPath(foot, color, style = line)
    }
}

/**
 * AK-47 de brinquedo vista de frente, com o cano apontado para quem olha a tela (para você), segurada
 * no meio do peito pelas duas mãos. Em contorno, como o resto. Ele nunca atira.
 */
private fun DrawScope.ak47(c: Color) {
    val st = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    // guarda-mão (o "corpo" da arma visto de frente)
    drawRoundRect(c, Offset(53f, 97f), Size(14f, 12f), CornerRadius(3f), style = st)
    // massa de mira em cima do cano
    drawLine(c, Offset(60f, 92.5f), Offset(60f, 97f), 1.8f, StrokeCap.Round)
    drawLine(c, Offset(58f, 92.5f), Offset(62f, 92.5f), 1.6f, StrokeCap.Round)
    // boca do cano olhando para você: anel + furo
    drawCircle(c, 3.6f, Offset(60f, 102.5f), style = Stroke(1.8f))
    drawCircle(c, 1.3f, Offset(60f, 102.5f))
    // carregador curvo descendo à frente
    drawPath(Path().apply { moveTo(57f, 109f); quadraticTo(56f, 116f, 58f, 122f); lineTo(63f, 121f); quadraticTo(62f, 115f, 63f, 109f) }, c, style = st)
}

/** AK de lado (para marchar com ela atravessada no peito), centrada em (60, 101). */
private fun DrawScope.ak47Side(c: Color) {
    val st = Stroke(1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    // coronha
    drawPath(Path().apply { moveTo(40f, 99f); lineTo(30f, 100.5f); lineTo(29f, 106f); lineTo(40f, 103f); close() }, c, style = st)
    // caixa da culatra
    drawRoundRect(c, Offset(40f, 97.5f), Size(22f, 6f), CornerRadius(1.2f), style = st)
    // empunhadura e carregador curvo
    drawPath(Path().apply { moveTo(46f, 103.5f); lineTo(44.5f, 109f); lineTo(48f, 109f); lineTo(49f, 103.5f) }, c, style = st)
    drawPath(Path().apply { moveTo(53f, 103.5f); quadraticTo(53f, 110f, 56f, 114f); lineTo(59.5f, 112.5f); quadraticTo(57.5f, 108f, 57.5f, 103.5f) }, c, style = st)
    // guarda-mão, tubo de gás e cano
    drawRoundRect(c, Offset(62f, 98f), Size(11f, 4.5f), CornerRadius(1f), style = st)
    drawLine(c, Offset(62f, 96.5f), Offset(76f, 96.5f), 1.2f, StrokeCap.Round)
    drawLine(c, Offset(73f, 100.2f), Offset(90f, 100.2f), 1.8f, StrokeCap.Round)
    drawLine(c, Offset(86f, 100.2f), Offset(86f, 96.8f), 1.4f, StrokeCap.Round)
}

/** Braço de tubo com anéis, girando no ombro (topo), e mão de três garrinhas. */
private fun DrawScope.arm(x: Float, angle: Float, color: Color, line: Stroke) {
    rotate(angle, pivot = Offset(x, 90f)) {
        drawLine(color, Offset(x - 3, 90f), Offset(x - 3, 112f), 2f, StrokeCap.Round)
        drawLine(color, Offset(x + 3, 90f), Offset(x + 3, 112f), 2f, StrokeCap.Round)
        val ring = color.copy(alpha = color.alpha * .8f)
        var y = 94f
        while (y <= 110f) {
            drawLine(ring, Offset(x - 3, y), Offset(x + 3, y), 1.1f, StrokeCap.Round)
            y += 4f
        }
        // palma + 3 garrinhas abertas
        drawRoundRect(color, Offset(x - 4.5f, 112f), Size(9f, 5f), CornerRadius(2f), style = Stroke(1.8f))
        val claw = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(Path().apply { moveTo(x - 3.5f, 117f); lineTo(x - 5.5f, 121.5f) }, color, style = claw)
        drawPath(Path().apply { moveTo(x, 117f); lineTo(x, 122.5f) }, color, style = claw)
        drawPath(Path().apply { moveTo(x + 3.5f, 117f); lineTo(x + 5.5f, 121.5f) }, color, style = claw)
    }
}

private fun DrawScope.head(color: Color, pose: RobotPose, line: Stroke) {
    drawLine(color, Offset(60f, 26f), Offset(60f, 16f), 2.2f, StrokeCap.Round)
    drawCircle(if (pose.antennaOn) color else color.copy(alpha = color.alpha * .2f), 3f, Offset(60f, 13f), style = line)
    drawRoundRect(color, Offset(30f, 26f), Size(60f, 62f), CornerRadius(14f), style = line)
    drawLine(color, Offset(30f, 52f), Offset(25f, 52f), 2.2f, StrokeCap.Round)
    drawLine(color, Offset(90f, 52f), Offset(95f, 52f), 2.2f, StrokeCap.Round)
    if (pose.back) {
        // nuca: grade de ventilação e dois parafusos
        for (y in listOf(46f, 52f, 58f, 64f)) drawLine(color, Offset(44f, y), Offset(76f, y), 1.8f, StrokeCap.Round)
        drawCircle(color, 1.4f, Offset(40f, 78f)); drawCircle(color, 1.4f, Offset(80f, 78f))
        return
    }
    drawRoundRect(color, Offset(38f, 36f), Size(44f, 44f), CornerRadius(4f), style = line)
    // o rosto não sai da telinha
    clipRect(39f, 37f, 81f, 79f) { face(color, pose) }
    if (pose.face == Face.SLEEP) zzz(color, pose.phase)
}

private fun DrawScope.face(c: Color, pose: RobotPose) {
    val t = pose.phase
    fun px(x: Float, y: Float, w: Float, h: Float, a: Float = 1f) =
        drawRoundRect(c.copy(alpha = c.alpha * a), Offset(x, y), Size(w, h), CornerRadius(1.2f))
    fun ln(x1: Float, y1: Float, x2: Float, y2: Float, w: Float = 2f) =
        drawLine(c, Offset(x1, y1), Offset(x2, y2), w, StrokeCap.Round)

    when (pose.face) {
        Face.NORMAL -> {
            val h = 9f * pose.blink.coerceIn(.1f, 1f)
            px(47f, 56.5f - h / 2, 8f, h); px(65f, 56.5f - h / 2, 8f, h)
            px(56f, 69f, 8f, 2f, .7f)
        }
        Face.SLEEP -> { px(45f, 57f, 10f, 2.6f); px(65f, 57f, 10f, 2.6f) }
        Face.MUSIC -> listOf(44f, 51f, 58f, 65f, 72f).forEachIndexed { i, x ->
            val k = .25f + .75f * ((sin(t * 9f + i * 1.7f) + 1f) / 2f)
            val h = 26f * k
            px(x, 74f - h, 4.5f, h)
        }
        Face.HEART -> {
            drawCircle(c, 5.6f, Offset(54.5f, 56.5f)); drawCircle(c, 5.6f, Offset(65.5f, 56.5f))
            drawPath(Path().apply { moveTo(49.1f, 58f); lineTo(70.9f, 58f); lineTo(60f, 70f); close() }, c)
        }
        Face.TIRED -> {
            px(46f, 58f, 8f, 5f); px(66f, 58f, 8f, 5f)
            ln(44f, 54f, 56f, 55.5f, 1.6f); ln(76f, 54f, 64f, 55.5f, 1.6f)
            drawRect(c, Offset(55f, 70f), Size(10f, 5f), style = Stroke(1.4f))
            px(56.5f, 71.5f, 2f, 2f)
        }
        Face.SCAN -> {
            px(47f, 53f, 7f, 7f); px(66f, 53f, 7f, 7f)
            val y = 57f + 14f * sin(t * 3.5f)
            px(40f, y, 40f, 1.6f, .8f)
        }
        Face.GIGGLE -> {
            drawPath(Path().apply { moveTo(46f, 60f); lineTo(50.5f, 54f); lineTo(55f, 60f) }, c, style = Stroke(2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(Path().apply { moveTo(65f, 60f); lineTo(69.5f, 54f); lineTo(74f, 60f) }, c, style = Stroke(2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawArc(c, 0f, 180f, false, Offset(55f, 64f), Size(10f, 8f), style = Stroke(2f, cap = StrokeCap.Round))
        }
        Face.SURPRISED -> {
            drawCircle(c, 4.2f, Offset(51f, 56f), style = Stroke(2f)); drawCircle(c, 1.4f, Offset(51f, 56f))
            drawCircle(c, 4.2f, Offset(69f, 56f), style = Stroke(2f)); drawCircle(c, 1.4f, Offset(69f, 56f))
            drawCircle(c, 2.6f, Offset(60f, 70f), style = Stroke(1.8f))
        }
        Face.DIZZY -> {
            // espirais girando nos olhos
            for (cx in listOf(51f, 69f)) {
                for (k in 0 until 3) {
                    val r = 1.5f + k * 1.6f
                    drawArc(c, t * 400f + k * 120f, 220f, false, Offset(cx - r, 56f - r), Size(r * 2, r * 2), style = Stroke(1.4f, cap = StrokeCap.Round))
                }
            }
            drawPath(Path().apply { moveTo(52f, 70f); quadraticTo(55f, 67f, 58f, 70f); quadraticTo(61f, 73f, 64f, 70f); quadraticTo(66f, 68f, 68f, 70f) }, c, style = Stroke(1.6f, cap = StrokeCap.Round))
        }
        Face.BLISS -> {
            // olhos fechados de felicidade, bochechas e sorrisão
            drawArc(c, 180f, 180f, false, Offset(46f, 53f), Size(10f, 8f), style = Stroke(2.2f, cap = StrokeCap.Round))
            drawArc(c, 180f, 180f, false, Offset(64f, 53f), Size(10f, 8f), style = Stroke(2.2f, cap = StrokeCap.Round))
            drawLine(c.copy(alpha = c.alpha * .6f), Offset(43f, 64f), Offset(47f, 62f), 1.4f, StrokeCap.Round)
            drawLine(c.copy(alpha = c.alpha * .6f), Offset(73f, 62f), Offset(77f, 64f), 1.4f, StrokeCap.Round)
            drawArc(c, 0f, 180f, false, Offset(54f, 64f), Size(12f, 8f), style = Stroke(2f, cap = StrokeCap.Round))
        }
        Face.SERIOUS -> { px(46f, 56f, 9f, 3f); px(65f, 56f, 9f, 3f); ln(55f, 70f, 65f, 70f) }
        Face.ANGRY -> {
            px(47f, 56f, 8f, 6f); px(65f, 56f, 8f, 6f)
            ln(45f, 50f, 56f, 54f, 2.4f); ln(75f, 50f, 64f, 54f, 2.4f)
            drawPath(Path().apply { moveTo(53f, 72f); lineTo(56.5f, 69f); lineTo(60f, 72f); lineTo(63.5f, 69f); lineTo(67f, 72f) }, c, style = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Três "z" subindo da cabeça (únicos traços fora da telinha). */
private fun DrawScope.zzz(c: Color, t: Float) {
    for (i in 0 until 3) {
        val p = ((t / 2.4f) + i / 3f) % 1f
        val a = when { p < .3f -> p / .3f; else -> 1f - (p - .3f) / .7f }.coerceIn(0f, 1f)
        val s = 4f + i * 1.2f
        val x = 84f + p * 16f + i * 2f
        val y = 30f - p * 24f
        drawPath(
            Path().apply { moveTo(x, y); lineTo(x + s, y); lineTo(x, y + s); lineTo(x + s, y + s) },
            c.copy(alpha = c.alpha * a * .9f),
            style = Stroke(1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
