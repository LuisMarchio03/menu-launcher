package com.brendon978.menulauncher.pet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendon978.menulauncher.home.LocalBorderWidth
import com.brendon978.menulauncher.home.LocalLineScale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Bola de tênis em contorno: círculo e as duas costuras, girando conforme rola. */
fun DrawScope.drawBall(color: Color, spinDeg: Float, lw: Float) {
    val r = size.minDimension / 2 - 1.5f * lw
    val c = Offset(size.width / 2, size.height / 2)
    drawCircle(color, r, c, style = Stroke(2f * lw))
    rotate(spinDeg, c) {
        drawArc(color, -60f, 120f, false, Offset(c.x - r * 1.6f, c.y - r), Size(r * 2f, r * 2f), style = Stroke(1.5f * lw))
        drawArc(color, 120f, 120f, false, Offset(c.x - r * .4f, c.y - r), Size(r * 2f, r * 2f), style = Stroke(1.5f * lw))
    }
}

/**
 * Fogos de artifício em contorno: três explosões em sequência, raios que crescem e somem.
 * [p] vai de 0 a 1 ao longo da comemoração.
 */
fun DrawScope.drawFireworks(color: Color, p: Float, lw: Float) {
    val bursts = listOf(Offset(.3f, .35f) to 0f, Offset(.7f, .25f) to .2f, Offset(.5f, .55f) to .4f)
    for ((pos, delay) in bursts) {
        val k = ((p - delay) / .55f).coerceIn(0f, 1f)
        if (k <= 0f || k >= 1f) continue
        val center = Offset(size.width * pos.x, size.height * pos.y)
        val radius = size.minDimension * .42f * k
        val a = (1f - k) * color.alpha
        for (i in 0 until 14) {
            val ang = i / 14f * 2f * PI.toFloat()
            val dir = Offset(cos(ang), sin(ang))
            val start = center + dir * (radius * .45f)
            val end = center + dir * radius
            drawLine(color.copy(alpha = a), start, end, 3f * lw, StrokeCap.Round)
        }
        drawCircle(color.copy(alpha = a), 2.5f * (1f - k) + 1f, center)
    }
}

/** Cartaz levantado num pauzinho (protesto e castigo). */
@Composable
fun Placard(text: String, color: Color, modifier: Modifier = Modifier) {
    val lw = LocalLineScale.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp,
            modifier = Modifier
                .width(84.dp)
                .background(Color(0xE60C0C0E), RoundedCornerShape(3.dp))
                .border(LocalBorderWidth.current * 1.6f, color, RoundedCornerShape(3.dp))
                .padding(horizontal = 4.dp, vertical = 5.dp),
        )
        Canvas(Modifier.size(4.dp, 18.dp)) { drawLine(color, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), 2.dp.toPx() * lw, StrokeCap.Round) }
    }
}

/** Placa do cantinho do castigo: aparece quando você pega o Glitch no colo. Solte ele em cima dela. */
@Composable
fun PunishCorner(color: Color, highlighted: Boolean, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "CANTINHO\nDO CASTIGO",
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp,
            modifier = Modifier
                .width(96.dp)
                .background(Color(0xCC0C0C0E), RoundedCornerShape(6.dp))
                // acesa só no contorno: preenchida na cor do traço, o Glitch sumiria em cima dela
                .border(LocalBorderWidth.current * (if (highlighted) 4f else 1.6f), color, RoundedCornerShape(6.dp))
                .padding(vertical = 8.dp),
        )
    }
}
