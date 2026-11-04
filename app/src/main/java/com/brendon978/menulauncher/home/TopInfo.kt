package com.brendon978.menulauncher.home

import android.content.Intent
import android.provider.AlarmClock
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendon978.menulauncher.clock.formatDate
import com.brendon978.menulauncher.clock.formatTime
import com.brendon978.menulauncher.clock.timeInWords
import com.brendon978.menulauncher.clock.zoneLabel
import com.brendon978.menulauncher.settings.ClockStyle
import com.brendon978.menulauncher.settings.LauncherSettings
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private fun calendarOf(millis: Long, zone: TimeZone): Calendar = Calendar.getInstance(zone).apply { timeInMillis = millis }

/** Relógio e data no topo: só para olhar (tocar no relógio abre os alarmes). Estilo e tamanho vêm dos ajustes. */
@Composable
fun TopInfo(settings: LauncherSettings, color: Color, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000 - now % 60_000)
        }
    }
    val is24h = android.text.format.DateFormat.is24HourFormat(context)
    val zone = remember(now) { TimeZone.getDefault() } // o usuário pode trocar o fuso: relê a cada minuto
    val cal = remember(now, zone) { calendarOf(now, zone) }
    val date = remember(now, zone, settings.dateStyle) { formatDate(settings.dateStyle, now, zone) }
    val second = remember(now, is24h, settings.secondZone) {
        settings.secondZone?.let { "${zoneLabel(it)} ${formatTime(now, TimeZone.getTimeZone(it), is24h)}" }
    }
    val openAlarms = Modifier.clickable(remember { MutableInteractionSource() }, indication = null) {
        runCatching {
            context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
    val scale = settings.clockScale
    Column(modifier) {
        when (settings.clockStyle) {
            ClockStyle.THIN -> Text(
                remember(now, zone, is24h) { formatTime(now, zone, is24h) },
                color = color,
                fontSize = 52.sp * scale,
                fontWeight = FontWeight.ExtraLight,
                modifier = openAlarms,
            )
            // Como o da tela de bloqueio da One UI: horas em cima, minutos embaixo, em negrito.
            ClockStyle.STACKED -> Column(openAlarms) {
                val h = cal.get(if (is24h) Calendar.HOUR_OF_DAY else Calendar.HOUR).let { if (!is24h && it == 0) 12 else it }
                val size = 64.sp * scale
                listOf(h, cal.get(Calendar.MINUTE)).forEach {
                    Text("%02d".format(it), color = color, fontSize = size, lineHeight = size * 0.92f, fontWeight = FontWeight.Bold)
                }
            }
            ClockStyle.ANALOG -> AnalogClock(cal, color, openAlarms.padding(vertical = 4.dp).size(96.dp * scale))
            ClockStyle.WORDS -> Text(
                remember(cal) { timeInWords(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)) },
                color = color,
                fontSize = 34.sp * scale,
                lineHeight = 38.sp * scale,
                fontWeight = FontWeight.ExtraLight,
                modifier = openAlarms.widthIn(max = 240.dp * scale),
            )
        }
        val small = 13.sp * settings.textScale
        if (date != null) Text(date, color = color.copy(alpha = 0.7f), fontSize = small)
        if (second != null) Text(second, color = color.copy(alpha = 0.7f), fontSize = small, modifier = Modifier.padding(top = 2.dp))
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

/** Mostrador só de contorno: aro, 12 marcas e dois ponteiros, na cor do traço. */
@Composable
private fun AnalogClock(cal: Calendar, color: Color, modifier: Modifier) {
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
        drawCircle(color, radius = r - stroke, center = c, style = Stroke(stroke))
        for (i in 0 until 12) {
            val major = i % 3 == 0
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
