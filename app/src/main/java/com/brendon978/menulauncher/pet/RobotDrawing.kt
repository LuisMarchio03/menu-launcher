package com.brendon978.menulauncher.pet

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
enum class Face { NORMAL, SLEEP, MUSIC, HEART, TIRED, SCAN, GIGGLE, SERIOUS, ANGRY, SURPRISED, DIZZY, BLISS, SAD, ROCK, PAPER, SCISSORS, CONFUSED }

/** Rostos que são um desenho na telinha, não olhos: nada pode ficar por cima. */
fun Face.isPicture(): Boolean = this in setOf(Face.HEART, Face.MUSIC, Face.ROCK, Face.PAPER, Face.SCISSORS, Face.CONFUSED)

/** Os óculos ficam em cima da tela e têm a mesma cor do rosto: com desenho, eles saem até os olhos voltarem. */
fun visibleAccessory(a: Accessory, face: Face): Accessory =
    if (a == Accessory.SUNGLASSES && face.isPicture()) Accessory.NONE else a

/** Acessórios do Glitch (tudo liberado, escolhe nos Ajustes). */
enum class Accessory(val label: String, val choosable: Boolean = true) {
    NONE("Nenhum"), CAP("Boné"), SUNGLASSES("Óculos escuros"), HEADPHONES("Fone"), HELMET("Capacete"), SCARF("Cachecol"), BOWTIE("Gravatinha"),
    /** Tapa-olho de dormir: ele põe sozinho no não perturbe. */
    SLEEP_MASK("Tapa-olho", choosable = false),
    /** Gorrinho e tapa-olho: ele põe sozinho para dormir de noite. */
    NIGHT_GEAR("Gorro de dormir", choosable = false),
    SANTA_HAT("Gorro de Natal", choosable = false),
    WITCH_HAT("Chapéu de bruxa", choosable = false),
    PARTY_HAT("Chapéu de festa", choosable = false),
    UMBRELLA("Guarda-chuva", choosable = false),
}

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
    /** Segurando um notebookzinho aberto, digitando. */
    val laptop: Boolean = false,
    /** Giro do corpo inteiro (pião), em graus. */
    val rotation: Float = 0f,
    val accessory: Accessory = Accessory.NONE,
    /** Segurando uma cartinha (chegou notificação). */
    val letter: Boolean = false,
    /** Barriga cheia de notificações: 0 normal, 1 estufado. */
    val belly: Float = 0f,
    /** Para onde os olhos olham (-1 a 1 em cada eixo): seguem o seu dedo pela tela. */
    val lookX: Float = 0f,
    val lookY: Float = 0f,
)

enum class GunHold { NONE, AIM, MARCH }

/** Proporção do desenho (viewBox do esboço: 120 × 170). */
const val ROBOT_ASPECT = 120f / 170f

/** Desenha o robozinho do esboço (variante "corpo") em contorno, numa cor só, ocupando o DrawScope inteiro. */
/** Corpo preenchido de escuro: o Glitch lê bem em qualquer papel de parede. */
private val BODY = Color(0xFF14171E)
private val SCREEN = Color(0xFF050608)
private fun fillFor(c: Color, base: Color) = base.copy(alpha = c.alpha)

/** LED aceso: ponto cheio com halo suave. */
private fun DrawScope.led(c: Color, r: Float, at: Offset) {
    drawCircle(c.copy(alpha = c.alpha * .22f), r * 2.4f, at)
    drawCircle(c, r, at)
}

fun DrawScope.drawRobot(color: Color, pose: RobotPose, lw: Float) {
    val u = size.height / 170f
    val line = Stroke(width = 2.2f * lw, cap = StrokeCap.Round, join = StrokeJoin.Round)
    scale(u, u, pivot = Offset.Zero) {
        // de perfil no giro ele não some de vez: no mínimo 8% de largura
        val spin = if (kotlin.math.abs(pose.spin) < .08f) (if (pose.spin < 0f) -.08f else .08f) else pose.spin
        val sx = (if (pose.facingLeft) -1f else 1f) * spin
        rotate(pose.rotation, pivot = Offset(60f, 100f)) {
        scale(sx, 1f, pivot = Offset(60f, 0f)) {
            translate(left = pose.offsetX, top = -pose.lift) {
                rotate(pose.tilt, pivot = Offset(60f, 153f)) {
                    // pernas sanfonadas com pé em cúpula (giram no quadril)
                    leg(52f, pose.legL, color, line, lw)
                    leg(68f, pose.legR, color, line, lw)
                    // corpo
                    val fat = 10f * pose.belly // a barriga cresce para os dois lados
                    drawRoundRect(fillFor(color, BODY), Offset(44f - fat, 80f), Size(32f + fat * 2, 44f), CornerRadius(8f + fat))
                    drawRoundRect(color, Offset(44f - fat, 80f), Size(32f + fat * 2, 44f), CornerRadius(8f + fat), style = line)
                    if (pose.back) {
                        // tampa traseira com 4 parafusos
                        drawRoundRect(color.copy(alpha = color.alpha * .7f), Offset(49f, 88f), Size(22f, 26f), CornerRadius(3f), style = Stroke(1.4f * lw))
                        for ((sx, sy) in listOf(51.5f to 90.5f, 68.5f to 90.5f, 51.5f to 111.5f, 68.5f to 111.5f)) drawCircle(color, 1f, Offset(sx, sy))
                    } else if (pose.gun == GunHold.NONE) { // armado, o peito fica livre para a arma aparecer
                        // painel do peito com LEDs acesos e respiros
                        drawRoundRect(fillFor(color, SCREEN), Offset(49f, 86f), Size(22f, 14f), CornerRadius(3f))
                        drawRoundRect(color, Offset(49f, 86f), Size(22f, 14f), CornerRadius(3f), style = Stroke(1.4f * lw))
                        led(color, 1.9f, Offset(55f, 93f))
                        led(color.copy(alpha = color.alpha * .55f), 1.9f, Offset(63f, 93f))
                        val vent = color.copy(alpha = color.alpha * .6f)
                        drawLine(vent, Offset(51f, 108f), Offset(69f, 108f), 1.6f * lw, StrokeCap.Round)
                        drawLine(vent, Offset(51f, 114f), Offset(69f, 114f), 1.6f * lw, StrokeCap.Round)
                    }
                    when (pose.gun) {
                        GunHold.AIM -> ak47(color, lw)
                        GunHold.MARCH -> rotate(-42f, pivot = Offset(60f, 101f)) { ak47Side(color, lw) }
                        GunHold.NONE -> Unit
                    }
                    if (pose.laptop) laptop(color, pose.phase, lw)
                    if (pose.letter) envelope(color, lw)
                    bodyAccessory(color, pose.accessory, pose.phase, lw)
                    // braços sanfonados (por cima da arma: as garrinhas seguram ela) (iguais às pernas) com mão de 3 garrinhas; abertos 10° para fora
                    arm(40f, 10f + pose.armL, color, line, lw)
                    arm(80f, -10f + pose.armR, color, line, lw)
                    // cabeça (no esboço: translate(9 2) scale(.85) sobre o desenho da cabeça)
                    rotate(pose.headTilt, pivot = Offset(60f, 80f)) {
                        // cabeça maior: nesse tamanho é o rosto que dá personalidade
                        translate(0f, -8f) {
                            head(color, pose, line, lw)
                            headAccessory(color, visibleAccessory(pose.accessory, pose.face), pose.back, lw)
                            if (pose.face == Face.SLEEP && !pose.back) zzz(color, pose.phase, lw)
                        }
                    }
                }
            }
        }
    }
    }
}

private fun DrawScope.leg(x: Float, angle: Float, color: Color, line: Stroke, lw: Float) {
    rotate(angle, pivot = Offset(x, 124f)) {
        drawLine(color, Offset(x - 4, 124f), Offset(x - 4, 146f), 2.2f * lw, StrokeCap.Round)
        drawLine(color, Offset(x + 4, 124f), Offset(x + 4, 146f), 2.2f * lw, StrokeCap.Round)
        val ring = color.copy(alpha = color.alpha * .8f)
        var y = 129f
        while (y <= 145f) {
            drawLine(ring, Offset(x - 4, y), Offset(x + 4, y), 1.2f * lw, StrokeCap.Round)
            y += 4f
        }
        val foot = Path().apply {
            moveTo(x - 8, 153f)
            quadraticTo(x - 8, 145f, x, 145f)
            quadraticTo(x + 8, 145f, x + 8, 153f)
            close()
        }
        drawPath(foot, fillFor(color, BODY))
        drawPath(foot, color, style = line)
    }
}

/**
 * AK-47 de brinquedo vista de frente, com o cano apontado para quem olha a tela (para você), segurada
 * no meio do peito pelas duas mãos. Em contorno, como o resto. Ele nunca atira.
 */
private fun DrawScope.ak47(c: Color, lw: Float) {
    val st = Stroke(1.8f * lw, cap = StrokeCap.Round, join = StrokeJoin.Round)
    // guarda-mão (o "corpo" da arma visto de frente)
    drawRoundRect(c, Offset(53f, 97f), Size(14f, 12f), CornerRadius(3f), style = st)
    // massa de mira em cima do cano
    drawLine(c, Offset(60f, 92.5f), Offset(60f, 97f), 1.8f * lw, StrokeCap.Round)
    drawLine(c, Offset(58f, 92.5f), Offset(62f, 92.5f), 1.6f * lw, StrokeCap.Round)
    // boca do cano olhando para você: anel + furo
    drawCircle(c, 3.6f, Offset(60f, 102.5f), style = Stroke(1.8f * lw))
    drawCircle(c, 1.3f, Offset(60f, 102.5f))
    // carregador curvo descendo à frente
    drawPath(Path().apply { moveTo(57f, 109f); quadraticTo(56f, 116f, 58f, 122f); lineTo(63f, 121f); quadraticTo(62f, 115f, 63f, 109f) }, c, style = st)
}

/** AK de lado (para marchar com ela atravessada no peito), centrada em (60, 101). */
private fun DrawScope.ak47Side(c: Color, lw: Float) {
    val st = Stroke(1.6f * lw, cap = StrokeCap.Round, join = StrokeJoin.Round)
    // coronha
    drawPath(Path().apply { moveTo(40f, 99f); lineTo(30f, 100.5f); lineTo(29f, 106f); lineTo(40f, 103f); close() }, c, style = st)
    // caixa da culatra
    drawRoundRect(c, Offset(40f, 97.5f), Size(22f, 6f), CornerRadius(1.2f), style = st)
    // empunhadura e carregador curvo
    drawPath(Path().apply { moveTo(46f, 103.5f); lineTo(44.5f, 109f); lineTo(48f, 109f); lineTo(49f, 103.5f) }, c, style = st)
    drawPath(Path().apply { moveTo(53f, 103.5f); quadraticTo(53f, 110f, 56f, 114f); lineTo(59.5f, 112.5f); quadraticTo(57.5f, 108f, 57.5f, 103.5f) }, c, style = st)
    // guarda-mão, tubo de gás e cano
    drawRoundRect(c, Offset(62f, 98f), Size(11f, 4.5f), CornerRadius(1f), style = st)
    drawLine(c, Offset(62f, 96.5f), Offset(76f, 96.5f), 1.2f * lw, StrokeCap.Round)
    drawLine(c, Offset(73f, 100.2f), Offset(90f, 100.2f), 1.8f * lw, StrokeCap.Round)
    drawLine(c, Offset(86f, 100.2f), Offset(86f, 96.8f), 1.4f * lw, StrokeCap.Round)
}

/** Notebookzinho aberto na frente do peito, com linhas de código piscando na tela. */
private fun DrawScope.laptop(c: Color, t: Float, lw: Float) {
    val st = Stroke(2f * lw, cap = StrokeCap.Round, join = StrokeJoin.Round)
    // tela grande e acesa, tampando o corpo: nesse tamanho é o que faz parecer um computador
    drawRoundRect(fillFor(c, SCREEN), Offset(34f, 86f), Size(52f, 33f), CornerRadius(3f))
    drawRoundRect(
        Brush.radialGradient(listOf(c.copy(alpha = c.alpha * .3f), Color.Transparent), Offset(60f, 102f), 30f),
        Offset(34f, 86f), Size(52f, 33f), CornerRadius(3f),
    )
    drawRoundRect(c, Offset(34f, 86f), Size(52f, 33f), CornerRadius(3f), style = st)
    // código sendo digitado, linha por linha, com cursor piscando
    val lines = listOf(6f to 22f, 11f to 28f, 11f to 16f, 6f to 24f)
    val shown = (t * 2.5f).toInt() % 6
    lines.take(shown.coerceAtMost(4)).forEachIndexed { i, (x, w) ->
        drawLine(c, Offset(34f + x, 92f + i * 6f), Offset(34f + x + w, 92f + i * 6f), 2f * lw, StrokeCap.Round)
    }
    if ((t * 4f).toInt() % 2 == 0) {
        val row = shown.coerceAtMost(3)
        drawLine(c, Offset(36f + lines[row].first, 90f + row * 6f), Offset(36f + lines[row].first, 94f + row * 6f), 1.6f * lw)
    }
    // base com teclado
    val base = Path().apply { moveTo(34f, 119f); lineTo(86f, 119f); lineTo(95f, 128f); lineTo(25f, 128f); close() }
    drawPath(base, fillFor(c, BODY))
    drawPath(base, c, style = st)
    val key = c.copy(alpha = c.alpha * .65f)
    drawLine(key, Offset(38f, 122f), Offset(82f, 122f), 1.4f * lw, StrokeCap.Round)
    drawLine(key, Offset(35f, 125f), Offset(85f, 125f), 1.4f * lw, StrokeCap.Round)
}
private fun DrawScope.headAccessory(c: Color, a: Accessory, back: Boolean, lw: Float) {
    val st = Stroke(2f * lw, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (a) {
        Accessory.CAP -> {
            drawPath(Path().apply { moveTo(29f, 34f); quadraticTo(30f, 14f, 60f, 14f); quadraticTo(90f, 14f, 91f, 34f); close() }, c, style = st)
            drawLine(c, Offset(88f, 34f), Offset(108f, 37f), 3f * lw, StrokeCap.Round) // aba
            drawCircle(c, 2f, Offset(60f, 14f))
        }
        Accessory.HELMET -> {
            drawPath(Path().apply { moveTo(24f, 36f); quadraticTo(26f, 10f, 60f, 10f); quadraticTo(94f, 10f, 96f, 36f); close() }, c, style = st)
            drawLine(c, Offset(20f, 36f), Offset(100f, 36f), 3f * lw, StrokeCap.Round)
            drawLine(c, Offset(60f, 10f), Offset(60f, 36f), 1.6f * lw, StrokeCap.Round)
        }
        Accessory.HEADPHONES -> {
            drawArc(c, 180f, 180f, false, Offset(24f, 10f), androidx.compose.ui.geometry.Size(72f, 80f), style = st)
            drawRoundRect(c, Offset(18f, 42f), androidx.compose.ui.geometry.Size(12f, 22f), CornerRadius(4f))
            drawRoundRect(c, Offset(90f, 42f), androidx.compose.ui.geometry.Size(12f, 22f), CornerRadius(4f))
        }
        Accessory.UMBRELLA -> {
            drawLine(c, Offset(60f, -22f), Offset(60f, 26f), 2.2f * lw, StrokeCap.Round)
            val top = Path().apply {
                moveTo(16f, 4f)
                cubicTo(22f, -30f, 98f, -30f, 104f, 4f)
                quadraticTo(91f, -5f, 79f, 4f)
                quadraticTo(69f, -5f, 60f, 4f)
                quadraticTo(51f, -5f, 41f, 4f)
                quadraticTo(29f, -5f, 16f, 4f)
                close()
            }
            drawPath(top, fillFor(c, BODY))
            drawPath(top, c, style = st)
        }
        Accessory.SANTA_HAT -> {
            val hat = Path().apply { moveTo(30f, 32f); cubicTo(36f, 8f, 66f, 0f, 90f, 14f); quadraticTo(100f, 20f, 102f, 34f); quadraticTo(90f, 24f, 88f, 32f); close() }
            drawPath(hat, fillFor(c, BODY))
            drawPath(hat, c, style = st)
            drawCircle(c, 5f, Offset(102f, 38f))
            drawRoundRect(c, Offset(25f, 28f), Size(70f, 10f), CornerRadius(5f))
        }
        Accessory.WITCH_HAT -> {
            val hat = Path().apply { moveTo(40f, 30f); lineTo(58f, -14f); lineTo(64f, -8f); lineTo(80f, 30f); close() }
            drawPath(hat, fillFor(c, BODY))
            drawPath(hat, c, style = st)
            drawLine(c, Offset(18f, 31f), Offset(102f, 31f), 3.4f * lw, StrokeCap.Round)
            drawRoundRect(c, Offset(53f, 22f), Size(14f, 8f), CornerRadius(1f), style = Stroke(1.6f * lw))
        }
        Accessory.PARTY_HAT -> {
            val hat = Path().apply { moveTo(40f, 30f); lineTo(60f, -8f); lineTo(80f, 30f); close() }
            drawPath(hat, fillFor(c, BODY))
            drawPath(hat, c, style = st)
            drawLine(c, Offset(50f, 18f), Offset(68f, 22f), 1.8f * lw, StrokeCap.Round)
            drawLine(c, Offset(44f, 26f), Offset(74f, 30f), 1.8f * lw, StrokeCap.Round)
            drawCircle(c, 3.6f, Offset(60f, -10f))
        }
        Accessory.NIGHT_GEAR -> {
            headAccessory(c, Accessory.SLEEP_MASK, back, lw)
            // gorrinho caído para o lado, com pompom (a cor do traço é a única cor dele)
            val cap = Path().apply {
                moveTo(28f, 34f)
                cubicTo(30f, 6f, 60f, -6f, 74f, 8f)
                quadraticTo(84f, 18f, 92f, 34f)
                close()
            }
            drawPath(cap, fillFor(c, BODY))
            drawPath(cap, c, style = st)
            val tail = Path().apply {
                moveTo(44f, 12f)
                cubicTo(26f, 8f, 12f, 16f, 10f, 34f)
                quadraticTo(22f, 24f, 34f, 26f)
            }
            drawPath(tail, fillFor(c, BODY))
            drawPath(tail, c, style = st)
            drawCircle(c, 5.5f, Offset(10f, 38f))
            // barra dobrada
            drawRoundRect(fillFor(c, BODY), Offset(24f, 30f), Size(72f, 11f), CornerRadius(5.5f))
            drawRoundRect(c, Offset(24f, 30f), Size(72f, 11f), CornerRadius(5.5f), style = st)
        }
        Accessory.SLEEP_MASK -> if (!back) {
            // faixa em volta da cabeça e a máscara cobrindo a telinha, com olhos fechados desenhados
            drawLine(c, Offset(30f, 56f), Offset(90f, 56f), 3f * lw, StrokeCap.Round)
            drawRoundRect(Color(0xFF0C0C0E), Offset(41f, 47f), Size(38f, 18f), CornerRadius(9f))
            drawRoundRect(c, Offset(41f, 47f), Size(38f, 18f), CornerRadius(9f), style = st)
            drawArc(c, 0f, 180f, false, Offset(46f, 52f), Size(10f, 6f), style = Stroke(1.6f * lw, cap = StrokeCap.Round))
            drawArc(c, 0f, 180f, false, Offset(64f, 52f), Size(10f, 6f), style = Stroke(1.6f * lw, cap = StrokeCap.Round))
        } else drawLine(c, Offset(30f, 56f), Offset(90f, 56f), 3f * lw, StrokeCap.Round)
        Accessory.SUNGLASSES -> if (!back) {
            drawRoundRect(c, Offset(43f, 50f), androidx.compose.ui.geometry.Size(15f, 10f), CornerRadius(3f))
            drawRoundRect(c, Offset(62f, 50f), androidx.compose.ui.geometry.Size(15f, 10f), CornerRadius(3f))
            drawLine(c, Offset(58f, 53f), Offset(62f, 53f), 1.6f * lw, StrokeCap.Round)
            drawLine(c, Offset(38f, 52f), Offset(43f, 53f), 1.6f * lw, StrokeCap.Round)
            drawLine(c, Offset(77f, 53f), Offset(82f, 52f), 1.6f * lw, StrokeCap.Round)
        }
        else -> Unit
    }
}

/** Acessórios do corpo (cachecol balançando, gravatinha). */
private fun DrawScope.bodyAccessory(c: Color, a: Accessory, t: Float, lw: Float) {
    val st = Stroke(1.8f * lw, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (a) {
        Accessory.SCARF -> {
            drawRoundRect(c, Offset(42f, 79f), androidx.compose.ui.geometry.Size(36f, 6f), CornerRadius(3f), style = st)
            val sway = sin(t * 4f) * 3f
            drawPath(Path().apply { moveTo(68f, 85f); lineTo(70f + sway, 101f); lineTo(75f + sway, 100f); lineTo(73f, 85f) }, c, style = st)
        }
        Accessory.BOWTIE -> {
            drawPath(Path().apply { moveTo(60f, 83f); lineTo(51f, 78f); lineTo(51f, 88f); close() }, c)
            drawPath(Path().apply { moveTo(60f, 83f); lineTo(69f, 78f); lineTo(69f, 88f); close() }, c)
            drawCircle(c, 2f, Offset(60f, 83f))
        }
        else -> Unit
    }
}

/** Cartinha na frente do peito: envelope com a aba em V e um coraçãozinho de selo. */
private fun DrawScope.envelope(c: Color, lw: Float) {
    val st = Stroke(1.8f * lw, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawRoundRect(Color(0xFF0C0C0E), Offset(44f, 96f), Size(32f, 20f), CornerRadius(2f))
    drawRoundRect(c, Offset(44f, 96f), Size(32f, 20f), CornerRadius(2f), style = st)
    drawPath(Path().apply { moveTo(44f, 97f); lineTo(60f, 108f); lineTo(76f, 97f) }, c, style = st)
    drawCircle(c, 1.6f, Offset(70f, 111f))
}

/** Braço de tubo com anéis, girando no ombro (topo), e mão de três garrinhas. */
private fun DrawScope.arm(x: Float, angle: Float, color: Color, line: Stroke, lw: Float) {
    rotate(angle, pivot = Offset(x, 90f)) {
        drawLine(color, Offset(x - 3, 90f), Offset(x - 3, 112f), 2f * lw, StrokeCap.Round)
        drawLine(color, Offset(x + 3, 90f), Offset(x + 3, 112f), 2f * lw, StrokeCap.Round)
        val ring = color.copy(alpha = color.alpha * .8f)
        var y = 94f
        while (y <= 110f) {
            drawLine(ring, Offset(x - 3, y), Offset(x + 3, y), 1.1f * lw, StrokeCap.Round)
            y += 4f
        }
        // palma + 3 garrinhas abertas
        drawRoundRect(color, Offset(x - 4.5f, 112f), Size(9f, 5f), CornerRadius(2f), style = Stroke(1.8f * lw))
        val claw = Stroke(1.8f * lw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(Path().apply { moveTo(x - 3.5f, 117f); lineTo(x - 5.5f, 121.5f) }, color, style = claw)
        drawPath(Path().apply { moveTo(x, 117f); lineTo(x, 122.5f) }, color, style = claw)
        drawPath(Path().apply { moveTo(x + 3.5f, 117f); lineTo(x + 5.5f, 121.5f) }, color, style = claw)
    }
}

private fun DrawScope.head(color: Color, pose: RobotPose, line: Stroke, lw: Float) {
    drawLine(color, Offset(60f, 26f), Offset(60f, 16f), 2.2f * lw, StrokeCap.Round)
    if (pose.antennaOn) led(color, 3f, Offset(60f, 13f)) else drawCircle(color.copy(alpha = color.alpha * .35f), 3f, Offset(60f, 13f), style = Stroke(1.6f * lw))
    for (x in listOf(24f, 90f)) {
        drawRoundRect(fillFor(color, BODY), Offset(x, 46f), Size(6f, 20f), CornerRadius(3f))
        drawRoundRect(color, Offset(x, 46f), Size(6f, 20f), CornerRadius(3f), style = line)
    }
    drawRoundRect(fillFor(color, BODY), Offset(30f, 26f), Size(60f, 62f), CornerRadius(14f))
    drawRoundRect(color, Offset(30f, 26f), Size(60f, 62f), CornerRadius(14f), style = line)
    if (pose.back) {
        // nuca: grade de ventilação e dois parafusos
        for (y in listOf(46f, 52f, 58f, 64f)) drawLine(color, Offset(44f, y), Offset(76f, y), 1.8f * lw, StrokeCap.Round)
        drawCircle(color, 1.4f, Offset(40f, 78f)); drawCircle(color, 1.4f, Offset(80f, 78f))
        return
    }
    // telinha preta, levemente iluminada pelo próprio rosto
    drawRoundRect(fillFor(color, SCREEN), Offset(38f, 36f), Size(44f, 44f), CornerRadius(7f))
    drawRoundRect(
        Brush.radialGradient(listOf(color.copy(alpha = color.alpha * .26f), Color.Transparent), Offset(60f, 58f), 30f),
        Offset(38f, 36f), Size(44f, 44f), CornerRadius(7f),
    )
    drawRoundRect(color, Offset(38f, 36f), Size(44f, 44f), CornerRadius(7f), style = Stroke(1.8f * lw))
    // o rosto não sai da telinha
    clipRect(39f, 37f, 81f, 79f) { face(color, pose, lw) }
    // reflexo no canto da tela
    drawArc(color.copy(alpha = color.alpha * .45f), 180f, 90f, false, Offset(42f, 40f), Size(14f, 14f), style = Stroke(1.5f * lw, cap = StrokeCap.Round))
}

private fun DrawScope.face(c: Color, pose: RobotPose, lw: Float) {
    val t = pose.phase
    fun px(x: Float, y: Float, w: Float, h: Float, a: Float = 1f) =
        drawRoundRect(c.copy(alpha = c.alpha * a), Offset(x, y), Size(w, h), CornerRadius(1.2f))
    fun ln(x1: Float, y1: Float, x2: Float, y2: Float, w: Float = 2f) =
        drawLine(c, Offset(x1, y1), Offset(x2, y2), w * lw, StrokeCap.Round)

    when (pose.face) {
        Face.NORMAL -> {
            val h = 9f * pose.blink.coerceIn(.1f, 1f)
            val dx = pose.lookX * 3.5f
            val dy = pose.lookY * 3f
            px(47f + dx, 56.5f - h / 2 + dy, 8f, h); px(65f + dx, 56.5f - h / 2 + dy, 8f, h)
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
            drawRect(c, Offset(55f, 70f), Size(10f, 5f), style = Stroke(1.4f * lw))
            px(56.5f, 71.5f, 2f, 2f)
        }
        Face.SCAN -> {
            px(47f, 53f, 7f, 7f); px(66f, 53f, 7f, 7f)
            val y = 57f + 14f * sin(t * 3.5f)
            px(40f, y, 40f, 1.6f, .8f)
        }
        Face.GIGGLE -> {
            drawPath(Path().apply { moveTo(46f, 60f); lineTo(50.5f, 54f); lineTo(55f, 60f) }, c, style = Stroke(2.4f * lw, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(Path().apply { moveTo(65f, 60f); lineTo(69.5f, 54f); lineTo(74f, 60f) }, c, style = Stroke(2.4f * lw, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawArc(c, 0f, 180f, false, Offset(55f, 64f), Size(10f, 8f), style = Stroke(2f * lw, cap = StrokeCap.Round))
        }
        Face.SURPRISED -> {
            drawCircle(c, 4.2f, Offset(51f, 56f), style = Stroke(2f * lw)); drawCircle(c, 1.4f, Offset(51f, 56f))
            drawCircle(c, 4.2f, Offset(69f, 56f), style = Stroke(2f * lw)); drawCircle(c, 1.4f, Offset(69f, 56f))
            drawCircle(c, 2.6f, Offset(60f, 70f), style = Stroke(1.8f * lw))
        }
        Face.DIZZY -> {
            // espirais girando nos olhos
            for (cx in listOf(51f, 69f)) {
                for (k in 0 until 3) {
                    val r = 1.5f + k * 1.6f
                    drawArc(c, t * 400f + k * 120f, 220f, false, Offset(cx - r, 56f - r), Size(r * 2, r * 2), style = Stroke(1.4f * lw, cap = StrokeCap.Round))
                }
            }
            drawPath(Path().apply { moveTo(52f, 70f); quadraticTo(55f, 67f, 58f, 70f); quadraticTo(61f, 73f, 64f, 70f); quadraticTo(66f, 68f, 68f, 70f) }, c, style = Stroke(1.6f * lw, cap = StrokeCap.Round))
        }
        Face.BLISS -> {
            // olhos fechados de felicidade, bochechas e sorrisão
            drawArc(c, 180f, 180f, false, Offset(46f, 53f), Size(10f, 8f), style = Stroke(2.2f * lw, cap = StrokeCap.Round))
            drawArc(c, 180f, 180f, false, Offset(64f, 53f), Size(10f, 8f), style = Stroke(2.2f * lw, cap = StrokeCap.Round))
            drawLine(c.copy(alpha = c.alpha * .6f), Offset(43f, 64f), Offset(47f, 62f), 1.4f * lw, StrokeCap.Round)
            drawLine(c.copy(alpha = c.alpha * .6f), Offset(73f, 62f), Offset(77f, 64f), 1.4f * lw, StrokeCap.Round)
            drawArc(c, 0f, 180f, false, Offset(54f, 64f), Size(12f, 8f), style = Stroke(2f * lw, cap = StrokeCap.Round))
        }
        Face.SAD -> {
            // olhos caídos, boca para baixo e uma lágrima escorrendo
            px(47f, 56f, 8f, 6f); px(65f, 56f, 8f, 6f)
            ln(46f, 53f, 56f, 51f, 1.6f); ln(74f, 53f, 64f, 51f, 1.6f)
            drawArc(c, 180f, 180f, false, Offset(54f, 68f), Size(12f, 7f), style = Stroke(2f * lw, cap = StrokeCap.Round))
            val drop = (t * .8f) % 1f
            drawCircle(c.copy(alpha = c.alpha * (1f - drop)), 1.3f, Offset(50f, 63f + drop * 12f))
        }
        Face.ROCK -> drawRoundRect(c, Offset(48f, 47f), Size(24f, 22f), CornerRadius(9f))
        Face.PAPER -> {
            drawRoundRect(c, Offset(47f, 44f), Size(26f, 30f), CornerRadius(2f), style = Stroke(2f * lw))
            for (y in listOf(51f, 57f, 63f)) ln(51f, y, 69f, y, 1.4f)
        }
        Face.SCISSORS -> {
            ln(50f, 46f, 66f, 66f, 2.2f); ln(70f, 46f, 54f, 66f, 2.2f)
            drawCircle(c, 3.2f, Offset(52f, 69f), style = Stroke(1.8f * lw)); drawCircle(c, 3.2f, Offset(68f, 69f), style = Stroke(1.8f * lw))
        }
        Face.CONFUSED -> {
            // um "?" grande na telinha
            drawArc(c, 200f, 230f, false, Offset(52f, 45f), Size(16f, 14f), style = Stroke(2.4f * lw, cap = StrokeCap.Round))
            ln(62f, 58f, 60f, 64f, 2.4f)
            drawCircle(c, 1.6f, Offset(60f, 70f))
        }
        Face.SERIOUS -> { px(46f, 56f, 9f, 3f); px(65f, 56f, 9f, 3f); ln(55f, 70f, 65f, 70f) }
        Face.ANGRY -> {
            px(47f, 56f, 8f, 6f); px(65f, 56f, 8f, 6f)
            ln(45f, 50f, 56f, 54f, 2.4f); ln(75f, 50f, 64f, 54f, 2.4f)
            drawPath(Path().apply { moveTo(53f, 72f); lineTo(56.5f, 69f); lineTo(60f, 72f); lineTo(63.5f, 69f); lineTo(67f, 72f) }, c, style = Stroke(1.8f * lw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Três "z" subindo da cabeça (únicos traços fora da telinha). */
private fun DrawScope.zzz(c: Color, t: Float, lw: Float) {
    // sobe bem acima da cabeça, balançando e crescendo; é a única coisa que sai do corpo dele
    for (i in 0 until 4) {
        val p = ((t / 4.2f) + i / 4f) % 1f
        val a = when { p < .15f -> p / .15f; p > .7f -> (1f - p) / .3f; else -> 1f }.coerceIn(0f, 1f)
        val s = 6f + p * 13f
        val x = 90f + p * 26f + sin(p * 9f) * 5f
        val y = 34f - p * 88f
        drawPath(
            Path().apply { moveTo(x, y); lineTo(x + s, y); lineTo(x, y + s); lineTo(x + s, y + s) },
            c.copy(alpha = c.alpha * a),
            style = Stroke((1.8f + p * 1.2f) * lw, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
