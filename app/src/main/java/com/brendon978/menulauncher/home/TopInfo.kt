package com.brendon978.menulauncher.home

import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendon978.menulauncher.clock.formatDate
import com.brendon978.menulauncher.clock.ClockFace
import com.brendon978.menulauncher.clock.clockFace
import com.brendon978.menulauncher.clock.segmentsOf
import com.brendon978.menulauncher.clock.timeInWords
import com.brendon978.menulauncher.clock.weekdayShort
import com.brendon978.menulauncher.clock.zoneLabel
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import com.brendon978.menulauncher.settings.ClockAlign
import com.brendon978.menulauncher.settings.ClockStyle
import com.brendon978.menulauncher.settings.ClockTap
import com.brendon978.menulauncher.settings.LauncherSettings
import com.brendon978.menulauncher.pet.petObstacle
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private fun calendarOf(millis: Long, zone: TimeZone): Calendar = Calendar.getInstance(zone).apply { timeInMillis = millis }

/** Estilos que mostram dígitos (os que aceitam segundos). */
private val DIGITAL = setOf(ClockStyle.THIN, ClockStyle.STACKED, ClockStyle.SEGMENTS, ClockStyle.TERMINAL, ClockStyle.GIANT)

/**
 * Relógio e data no topo: só para olhar (o toque no relógio faz o que os ajustes mandam).
 * Estilo, tamanho, formato e alinhamento vêm dos ajustes; [align] só vale onde o topo tem a largura toda.
 */
@Composable
fun TopInfo(settings: LauncherSettings, color: Color, modifier: Modifier = Modifier, screenTime: String? = null, align: ClockAlign = ClockAlign.START) {
    val context = LocalContext.current
    val seconds = settings.clockSeconds && settings.clockStyle in DIGITAL
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    // com segundos acorda a cada segundo; sem, uma vez por minuto
    LaunchedEffect(seconds) {
        val step = if (seconds) 1_000L else 60_000L
        while (true) {
            now = System.currentTimeMillis()
            delay(step - now % step)
        }
    }
    val is24h = android.text.format.DateFormat.is24HourFormat(context)
    val zone = remember(now) { TimeZone.getDefault() } // o usuário pode trocar o fuso: relê a cada minuto
    val cal = remember(now, zone) { calendarOf(now, zone) }
    val face = remember(cal, is24h, settings.hourFormat, seconds) {
        clockFace(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), settings.hourFormat, is24h, seconds)
    }
    val date = remember(now / 60_000, zone, settings.dateStyle) { formatDate(settings.dateStyle, now, zone) }
    val second = remember(now / 60_000, is24h, settings.secondZone, settings.hourFormat) {
        settings.secondZone?.let { id ->
            val c = calendarOf(now, TimeZone.getTimeZone(id))
            "${zoneLabel(id)} ${clockFace(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), 0, settings.hourFormat, is24h, false).text}"
        }
    }
    // travessura do Glitch: entorta o relógio até você cutucar ele
    val clockTilt by androidx.compose.animation.core.animateFloatAsState(
        if (com.brendon978.menulauncher.pet.GlitchMischief.current.value == com.brendon978.menulauncher.pet.Mischief.CLOCK) -8f else 0f,
        label = "relogio",
    )
    val tapIntent = when (settings.clockTap) {
        ClockTap.ALARMS -> Intent(AlarmClock.ACTION_SHOW_ALARMS)
        ClockTap.CALENDAR -> Intent(Intent.ACTION_VIEW, CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build())
        ClockTap.NONE -> null
    }
    val tilt = Modifier.graphicsLayer { rotationZ = clockTilt }
    val onTap = if (tapIntent == null) tilt else tilt.clickable(remember { MutableInteractionSource() }, indication = null) {
        runCatching { context.startActivity(tapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
    val scale = settings.clockScale
    val textAlign = when (align) {
        ClockAlign.START -> TextAlign.Start
        ClockAlign.CENTER -> TextAlign.Center
        ClockAlign.END -> TextAlign.End
    }
    val horizontal = when (align) {
        ClockAlign.START -> Alignment.Start
        ClockAlign.CENTER -> Alignment.CenterHorizontally
        ClockAlign.END -> Alignment.End
    }
    Column(modifier, horizontalAlignment = horizontal) {
        ClockBody(settings.clockStyle, face, cal, color, scale, horizontal, textAlign, onTap)
        val small = 13.sp * settings.textScale
        if (date != null) Text(date, color = color.copy(alpha = 0.7f), fontSize = small)
        if (second != null) Text(second, color = color.copy(alpha = 0.7f), fontSize = small, modifier = Modifier.padding(top = 2.dp))
        screenTime?.let { Text(it, color = color.copy(alpha = 0.55f), fontSize = 12.sp, modifier = Modifier.petObstacle("tempo-tela")) }
        if (settings.showNextEvent) {
            // próximo compromisso, relido a cada minuto (fora da thread principal)
            val event by androidx.compose.runtime.produceState<String?>(null, now / 60_000) {
                value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { queryNextEvent(context) }
            }
            event?.let {
                Text(
                    it, color = color.copy(alpha = 0.85f), fontSize = small, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 200.dp),
                )
            }
        }
    }
}

/** Só o relógio, no estilo pedido; também desenha as miniaturas dos Ajustes. */
@Composable
fun ClockBody(
    style: ClockStyle,
    face: ClockFace,
    cal: Calendar,
    color: Color,
    scale: Float,
    horizontal: Alignment.Horizontal,
    textAlign: TextAlign,
    onTap: Modifier = Modifier,
) {
    when (style) {
        ClockStyle.THIN -> Text(face.text, color = color, fontSize = 52.sp * scale, fontWeight = FontWeight.ExtraLight, modifier = onTap)
        // Como o da tela de bloqueio da One UI: horas em cima, minutos embaixo, em negrito.
        ClockStyle.STACKED -> StackedClock(face, color, 64.sp * scale, FontWeight.Bold, horizontal, onTap)
        // O mesmo empilhado, grande e fino: ocupa a largura da tela.
        ClockStyle.GIANT -> StackedClock(face, color, 120.sp * scale, FontWeight.Thin, horizontal, onTap)
        ClockStyle.SEGMENTS -> SegmentClock(face, color, onTap.padding(vertical = 6.dp).height(64.dp * scale))
        ClockStyle.TERMINAL -> TerminalClock(face, color, 44.sp * scale, onTap)
        ClockStyle.ANALOG -> AnalogClock(cal, color, onTap.padding(vertical = 4.dp).size(96.dp * scale))
        ClockStyle.ANALOG_MINIMAL -> AnalogClock(cal, color, onTap.padding(vertical = 4.dp).size(96.dp * scale), minimal = true)
        ClockStyle.WORDS -> Text(
            remember(cal) { timeInWords(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)) },
            color = color,
            fontSize = 34.sp * scale,
            lineHeight = 38.sp * scale,
            fontWeight = FontWeight.ExtraLight,
            textAlign = textAlign,
            modifier = onTap.widthIn(max = 240.dp * scale),
        )
        ClockStyle.WEEKDAY -> WeekdayClock(cal, face, color, scale, onTap)
    }
}

/**
 * O dia da semana enorme e fino, com a hora pequena por cima: as letras abrem um vão
 * onde a hora passa (o vão é apagado, então o papel de parede aparece nele).
 */
@Composable
private fun WeekdayClock(cal: Calendar, face: ClockFace, color: Color, scale: Float, modifier: Modifier) {
    val big = 96.sp * scale
    Box(modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }, contentAlignment = Alignment.Center) {
        Text(weekdayShort(cal.get(Calendar.DAY_OF_WEEK)), color = color, fontSize = big, lineHeight = big, fontWeight = FontWeight.Thin, letterSpacing = 4.sp * scale)
        Text(
            face.text,
            color = color,
            fontSize = 15.sp * scale,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp * scale,
            // a sombra da tela inicial mancharia o vão
            style = LocalTextStyle.current.copy(shadow = null),
            modifier = Modifier
                .drawBehind { drawRect(Color.Black, topLeft = Offset(-6.dp.toPx(), 0f), size = Size(size.width + 12.dp.toPx(), size.height), blendMode = BlendMode.Clear) }
                .padding(horizontal = 2.dp),
        )
    }
}

/** Horas em cima, minutos (e segundos, menores) embaixo; AM/PM pequeno no fim. */
@Composable
private fun StackedClock(face: ClockFace, color: Color, size: TextUnit, weight: FontWeight, horizontal: Alignment.Horizontal, modifier: Modifier) {
    Column(modifier, horizontalAlignment = horizontal) {
        listOf(face.hours.padStart(2, '0'), face.minutes).forEach {
            Text(it, color = color, fontSize = size, lineHeight = size * 0.92f, fontWeight = weight)
        }
        val small = listOfNotNull(face.seconds, face.suffix).joinToString(" ")
        if (small.isNotEmpty()) Text(small, color = color.copy(alpha = .8f), fontSize = size * .3f, fontWeight = weight)
    }
}

/** Console: dígitos monoespaçados e um cursor piscando no fim. */
@Composable
private fun TerminalClock(face: ClockFace, color: Color, size: TextUnit, modifier: Modifier) {
    val blink by rememberInfiniteTransition(label = "cursor").animateFloat(
        1f, 0f, infiniteRepeatable(keyframes { durationMillis = 1_000; 1f at 499; 0f at 500 }), label = "cursor",
    )
    Text(
        buildAnnotatedString {
            append("> ")
            append(face.text)
            withStyle(SpanStyle(color = color.copy(alpha = color.alpha * blink))) { append("_") }
        },
        color = color,
        fontSize = size,
        fontFamily = FontFamily.Monospace,
        modifier = modifier,
    )
}

/** Dígitos de sete segmentos desenhados com o traço do launcher; a altura vem do [modifier]. */
@Composable
private fun SegmentClock(face: ClockFace, color: Color, modifier: Modifier) {
    val lw = LocalLineScale.current
    val text = listOfNotNull(face.hours, face.minutes, face.seconds).joinToString(":")
    Row(verticalAlignment = Alignment.Bottom) {
        // largura em alturas: dígito 0,55, dois-pontos 0,25, com folga de 0,12 entre os caracteres
        val widths = text.map { if (it == ':') .25f else .55f }
        Canvas(modifier.aspectRatio(widths.sum() + .12f * (text.length - 1))) {
            val h = size.height
            val stroke = h * .07f * lw
            var x = 0f
            text.forEachIndexed { i, ch ->
                val w = widths[i] * h
                if (ch == ':') {
                    for (y in listOf(.32f, .72f)) drawCircle(color, stroke * .8f, Offset(x + w / 2, h * y))
                } else {
                    drawSegments(segmentsOf(ch), color, Offset(x, 0f), w, h, stroke)
                }
                x += w + .12f * h
            }
        }
        face.suffix?.let { Text(it, color = color.copy(alpha = .8f), fontSize = 14.sp, modifier = Modifier.padding(start = 6.dp)) }
    }
}

private fun DrawScope.drawSegments(on: Int, color: Color, at: Offset, w: Float, h: Float, stroke: Float) {
    val m = stroke / 2
    val (l, r, t, mid, b) = listOf(at.x + m, at.x + w - m, at.y + m, at.y + h / 2, at.y + h - m)
    // a, b, c, d, e, f, g; os apagados ficam bem fracos, como no visor de verdade
    val segs = listOf(
        Offset(l, t) to Offset(r, t), Offset(r, t) to Offset(r, mid), Offset(r, mid) to Offset(r, b),
        Offset(l, b) to Offset(r, b), Offset(l, mid) to Offset(l, b), Offset(l, t) to Offset(l, mid), Offset(l, mid) to Offset(r, mid),
    )
    segs.forEachIndexed { i, (p, q) ->
        val lit = on and (1 shl i) != 0
        // encurta as pontas para os segmentos não se tocarem
        val d = (q - p) / (q - p).getDistance() * (stroke * .9f)
        drawLine(if (lit) color else color.copy(alpha = color.alpha * .08f), p + d, q - d, stroke, StrokeCap.Round)
    }
}

/** Mostrador só de contorno: aro, 12 marcas e dois ponteiros, na cor do traço. [minimal]: sem aro, só 4 marcas. */
@Composable
private fun AnalogClock(cal: Calendar, color: Color, modifier: Modifier, minimal: Boolean = false) {
    val minutes = cal.get(Calendar.MINUTE)
    val hours = cal.get(Calendar.HOUR) + minutes / 60f
    val lw = LocalLineScale.current
    Canvas(modifier) {
        val r = size.minDimension / 2
        val c = Offset(size.width / 2, size.height / 2)
        val stroke = r * 0.05f * lw
        // Ponto a [length] do centro na direção da fração do mostrador (0 = 12h, sentido horário).
        fun polar(fraction: Float, length: Float): Offset {
            val a = fraction * 2 * PI
            return Offset(c.x + sin(a).toFloat() * length, c.y - cos(a).toFloat() * length)
        }
        if (!minimal) drawCircle(color, radius = r - stroke, center = c, style = Stroke(stroke))
        for (i in 0 until 12) {
            val major = i % 3 == 0
            if (minimal && !major) continue
            drawLine(
                color.copy(alpha = if (major) 1f else 0.5f),
                polar(i / 12f, if (major) r * 0.72f else r * 0.79f),
                polar(i / 12f, r * 0.86f),
                stroke,
                StrokeCap.Round,
            )
        }
        drawLine(color, c, polar(hours / 12f, r * 0.45f), stroke * 1.6f, StrokeCap.Round)
        drawLine(color, c, polar(minutes / 60f, r * 0.66f), stroke, StrokeCap.Round)
        drawCircle(color, radius = stroke * 1.4f, center = c)
    }
}
