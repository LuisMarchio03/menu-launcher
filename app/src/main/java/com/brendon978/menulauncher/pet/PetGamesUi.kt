package com.brendon978.menulauncher.pet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendon978.menulauncher.home.LocalBorderWidth
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Brincadeiras que você começa tocando duas vezes no Glitch. */
enum class GameKind(val label: String) {
    PASS("Bola"), HIDE("Esconde-esconde"), TAG("Pega-pega"), JOKENPO("Jokenpô"), BUBBLES("Bolhas"),
}

@Composable
private fun Pill(text: String, color: Color, onClick: () -> Unit) {
    Text(
        text,
        color = color,
        fontSize = 14.sp,
        modifier = Modifier
            .background(Color(0xE60C0C0E), RoundedCornerShape(16.dp))
            .border(LocalBorderWidth.current, color.copy(alpha = .7f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

/** Menuzinho de brincadeiras (aparece em cima do Glitch). */
@Composable
fun PlayMenu(color: Color, onPick: (GameKind) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Bora brincar de quê?",
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.background(Color(0xE60C0C0E), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
        )
        GameKind.entries.forEach { g -> Pill(g.label, color) { onPick(g) } }
        Pill("Nada não", color.copy(alpha = .7f), onClose)
    }
}

/** Painel do jokenpô: escolher a mão, ou jogar de novo / parar. */
@Composable
fun JokenpoPanel(color: Color, choosing: Boolean, onHand: (Hand) -> Unit, onAgain: () -> Unit, onStop: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (choosing) {
            Pill("Pedra", color) { onHand(Hand.ROCK) }
            Pill("Papel", color) { onHand(Hand.PAPER) }
            Pill("Tesoura", color) { onHand(Hand.SCISSORS) }
        } else {
            Pill("De novo", color, onAgain)
            Pill("Chega", color.copy(alpha = .7f), onStop)
        }
    }
}

/** Post-it que ele cola na tela: quadradinho com a ponta dobrada e o recado. */
@Composable
fun PostIt(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .width(110.dp)
            .background(Color(0xEE15151A), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomStart = 3.dp, bottomEnd = 14.dp))
            .border(LocalBorderWidth.current * 1.4f, color, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomStart = 3.dp, bottomEnd = 14.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Text(text, color = color, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 14.sp, modifier = Modifier.align(Alignment.Center))
    }
}

/** Bolha de sabão em contorno, com um brilhinho. */
fun DrawScope.drawBubble(color: Color, center: Offset, r: Float, lw: Float) {
    drawCircle(color.copy(alpha = color.alpha * .85f), r, center, style = Stroke(1.6f * lw))
    drawArc(color.copy(alpha = color.alpha * .6f), 200f, 60f, false, Offset(center.x - r * .6f, center.y - r * .6f), androidx.compose.ui.geometry.Size(r * 1.2f, r * 1.2f), style = Stroke(1.4f * lw, cap = StrokeCap.Round))
}

/** Estouro: raiozinhos saindo de onde a bolha estava. [k] de 0 a 1. */
fun DrawScope.drawPop(color: Color, center: Offset, r: Float, k: Float, lw: Float) {
    val a = (1f - k) * color.alpha
    for (i in 0 until 8) {
        val ang = i / 8f * 2f * PI.toFloat()
        val d = Offset(cos(ang), sin(ang))
        drawLine(color.copy(alpha = a), center + d * (r * (.6f + k)), center + d * (r * (1f + k * 1.4f)), 1.8f * lw, StrokeCap.Round)
    }
}

/** Antena espiando por cima do esconderijo. */
fun DrawScope.drawPeekingAntenna(color: Color, base: Offset, scale: Float, t: Float, lw: Float) {
    val wobble = sin(t * 3f) * 2f * scale
    drawLine(color, base, base + Offset(wobble, -14f * scale), 2f * scale * lw, StrokeCap.Round)
    drawCircle(color, 3.2f * scale, base + Offset(wobble, -17f * scale), style = Stroke(1.8f * scale * lw))
    // pontinha da cabeça
    drawPath(Path().apply {
        moveTo(base.x - 18f * scale, base.y + 2f); quadraticTo(base.x - 18f * scale, base.y - 4f * scale, base.x - 10f * scale, base.y - 4f * scale)
        lineTo(base.x + 10f * scale, base.y - 4f * scale); quadraticTo(base.x + 18f * scale, base.y - 4f * scale, base.x + 18f * scale, base.y + 2f)
    }, color, style = Stroke(2f * scale * lw, cap = StrokeCap.Round))
}

/** Pega o toque na tela inteira durante algumas brincadeiras (fica por baixo do Glitch). */
@Composable
fun TouchCatcher(onDown: (Offset) -> Unit, onMove: (Offset) -> Unit, onUp: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .pointerInputCatcher(onDown, onMove, onUp),
    )
}
