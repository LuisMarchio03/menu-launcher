package com.brendon978.menulauncher.settingsui

import android.app.WallpaperManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.clock.ClockFace
import com.brendon978.menulauncher.clock.clockFace
import com.brendon978.menulauncher.clock.formatDate
import com.brendon978.menulauncher.home.AppRow
import com.brendon978.menulauncher.home.ClockBody
import com.brendon978.menulauncher.home.family
import com.brendon978.menulauncher.home.rowLook
import com.brendon978.menulauncher.icons.IconRenderer
import com.brendon978.menulauncher.settings.ClockAlign
import com.brendon978.menulauncher.settings.ClockStyle
import com.brendon978.menulauncher.settings.LauncherSettings
import com.brendon978.menulauncher.settings.LookTheme
import com.brendon978.menulauncher.settings.LookThemes
import com.brendon978.menulauncher.settings.StrokeAuto
import com.brendon978.menulauncher.settings.applyTheme
import com.brendon978.menulauncher.settings.toTheme
import com.brendon978.menulauncher.settings.uses
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone

/**
 * Fundo das prévias: um degradê com as cores que o sistema informa do papel de parede.
 * O launcher não pode ler a imagem em si sem permissão de arquivos; sem cor informada, fica um cinza escuro.
 */
@Composable
internal fun rememberWallpaperBrush(): Brush {
    val context = LocalContext.current
    val colors by produceState<List<Color>?>(null) {
        value = withContext(Dispatchers.IO) {
            runCatching { WallpaperManager.getInstance(context).getWallpaperColors(WallpaperManager.FLAG_SYSTEM) }.getOrNull()
                ?.let { wc -> listOfNotNull(wc.primaryColor, wc.secondaryColor, wc.tertiaryColor).map { Color(it.toArgb()) } }
        }
    }
    val c = colors?.takeIf { it.isNotEmpty() } ?: listOf(Color(0xFF2A2A30), Color(0xFF121216))
    return remember(c) { Brush.linearGradient(if (c.size == 1) listOf(c[0], c[0].copy(alpha = .7f)) else c) }
}

/** A hora de agora, relida a cada minuto. */
@Composable
private fun rememberMinuteCalendar(): Calendar {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000 - System.currentTimeMillis() % 60_000)
            now = System.currentTimeMillis()
        }
    }
    return remember(now) { Calendar.getInstance(TimeZone.getDefault()).apply { timeInMillis = now } }
}

@Composable
private fun faceOf(cal: Calendar, settings: LauncherSettings): ClockFace {
    val is24h = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    return clockFace(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), 0, settings.hourFormat, is24h, showSeconds = false)
}

/** Um pedaço da tela inicial com os ajustes de agora, sobre o papel de parede: muda na hora, enquanto você mexe. */
@Composable
internal fun LivePreview(settings: LauncherSettings, apps: List<AppEntry>, renderer: IconRenderer, modifier: Modifier = Modifier) {
    val cal = rememberMinuteCalendar()
    val face = faceOf(cal, settings)
    val color = Color(settings.strokeColor)
    val horizontal = when (settings.clockAlign) {
        ClockAlign.START -> Alignment.Start
        ClockAlign.CENTER -> Alignment.CenterHorizontally
        ClockAlign.END -> Alignment.End
    }
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(shape)
            // sem fundo: os Ajustes deixam um vão aqui e o papel de parede de verdade aparece
            .border(1.dp, Color.White.copy(alpha = .18f), shape),
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = horizontal) {
            // relógio em 60%: cabe a data e uns apps embaixo, como na tela de verdade
            ClockBody(settings.clockStyle, face, cal, color, settings.clockScale * .6f, horizontal, TextAlign.Start)
            formatDate(settings.dateStyle, cal.timeInMillis, TimeZone.getDefault())?.let {
                Text(it, color = color.copy(alpha = .7f), fontSize = 11.sp * settings.textScale)
            }
            Spacer(Modifier.height(6.dp))
            val look = settings.rowLook.let { it.copy(height = it.height * .8f) }
            Column(Modifier.fillMaxWidth()) {
                apps.take(4).forEach { app -> AppRow(app, settings.iconOverrides[app.key], look, renderer, onClick = {}, onLongClick = {}) }
            }
        }
    }
}

/** Temas prontos e os seus numa fileira; tocar aplica, segurar um seu apaga, o último salva o atual. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ThemesRow(settings: LauncherSettings, liveStroke: Int, onChange: ((LauncherSettings) -> LauncherSettings) -> Unit) {
    val brush = rememberWallpaperBrush()
    var naming by rememberSaveable { mutableStateOf(false) }
    var removing by remember { mutableStateOf<LookTheme?>(null) }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        (LookThemes.PRESETS + settings.customThemes).forEach { t ->
            val mine = t in settings.customThemes
            ThemeCard(
                t, brush, selected = settings.uses(t),
                // o tema que segue o papel de parede mostra a cor que ele daria agora
                color = if (t.strokeAuto == StrokeAuto.BY_WALLPAPER) liveStroke else t.strokeColor,
                modifier = Modifier.combinedClickable(
                    onClick = { onChange { it.applyTheme(t) } },
                    onLongClick = { if (mine) removing = t },
                ),
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 96.dp, height = 132.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = .25f), RoundedCornerShape(16.dp))
                .clickable { naming = true },
        ) {
            Text("+\nSalvar\no atual", color = Color.White.copy(alpha = .75f), fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
    if (naming) {
        var name by remember { mutableStateOf("Meu tema ${settings.customThemes.size + 1}") }
        AlertDialog(
            onDismissRequest = { naming = false },
            title = { Text("Salvar como tema") },
            text = { OutlinedTextField(name, { name = it.take(24) }, singleLine = true, label = { Text("Nome") }) },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    naming = false
                    onChange { s -> s.copy(customThemes = s.customThemes.filter { it.name != name.trim() } + s.toTheme(name.trim())) }
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { naming = false }) { Text("Cancelar") } },
        )
    }
    removing?.let { t ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("Apagar \"${t.name}\"?") },
            text = { Text("Só o tema salvo some; a tela fica como está.") },
            confirmButton = {
                TextButton(onClick = {
                    removing = null
                    onChange { s -> s.copy(customThemes = s.customThemes - t) }
                }) { Text("Apagar") }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun ThemeCard(t: LookTheme, brush: Brush, selected: Boolean, color: Int, modifier: Modifier) {
    val shape = RoundedCornerShape(16.dp)
    val cal = rememberMinuteCalendar()
    val face = faceOf(cal, LauncherSettings())
    Column(
        modifier
            .size(width = 96.dp, height = 132.dp)
            .clip(shape)
            .background(brush, shape)
            .border(if (selected) 2.dp else 1.dp, Color.White.copy(alpha = if (selected) 1f else .15f), shape)
            .padding(8.dp),
    ) {
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.dp)), contentAlignment = Alignment.CenterStart) {
            ProvideTextStyle(LocalTextStyle.current.copy(fontFamily = t.font.family)) {
                ClockBody(t.clockStyle, face, cal, Color(color), .32f, Alignment.Start, TextAlign.Start)
            }
        }
        Text(
            t.name, color = Color(color), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            fontFamily = t.font.family, style = LocalTextStyle.current.copy(shadow = null),
        )
    }
}

/** Os estilos de relógio em miniatura, com a cor e a fonte de agora. */
@Composable
internal fun ClockStylePicker(settings: LauncherSettings, onPick: (ClockStyle) -> Unit) {
    val cal = rememberMinuteCalendar()
    val face = faceOf(cal, settings)
    val color = Color(settings.strokeColor)
    CLOCK_STYLES.chunked(3).forEach { line ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            line.forEach { (label, style) ->
                val on = settings.clockStyle == style
                val shape = RoundedCornerShape(14.dp)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .height(92.dp)
                        .clip(shape)
                        .background(Color(0xFF0F0F13), shape)
                        .border(if (on) 2.dp else 1.dp, Color.White.copy(alpha = if (on) 1f else .14f), shape)
                        .clickable { onPick(style) }
                        .padding(6.dp),
                ) {
                    Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        ClockBody(style, face, cal, color, .3f, Alignment.CenterHorizontally, TextAlign.Center)
                    }
                    Text(label, color = Color.White.copy(alpha = if (on) 1f else .6f), fontSize = 10.sp, maxLines = 1, style = LocalTextStyle.current.copy(shadow = null))
                }
            }
            // completa a última linha para as miniaturas não esticarem
            repeat(3 - line.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

private val CLOCK_STYLES = listOf(
    "Fino" to ClockStyle.THIN,
    "Empilhado" to ClockStyle.STACKED,
    "Gigante" to ClockStyle.GIANT,
    "Dia grande" to ClockStyle.WEEKDAY,
    "Segmentos" to ClockStyle.SEGMENTS,
    "Terminal" to ClockStyle.TERMINAL,
    "Por extenso" to ClockStyle.WORDS,
    "Analógico" to ClockStyle.ANALOG,
    "Analógico mínimo" to ClockStyle.ANALOG_MINIMAL,
)

/** Uma categoria dos Ajustes na grade da tela principal. */
@Composable
internal fun CategoryTile(title: String, hint: String, glyph: String, accent: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .height(104.dp)
            .clip(shape)
            .background(Color(0xFF17171C), shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Box(Modifier.size(32.dp).background(accent.copy(alpha = .2f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Text(glyph, color = accent, fontSize = 16.sp, style = LocalTextStyle.current.copy(shadow = null))
        }
        Spacer(Modifier.weight(1f))
        Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(hint, color = Color.White.copy(alpha = .55f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Rótulo pequeno em maiúsculas entre os blocos da tela principal. */
@Composable
internal fun GroupLabel(text: String) {
    Text(
        text.uppercase(), color = Color.White.copy(alpha = .45f), fontSize = 11.sp, letterSpacing = 1.4.sp,
        modifier = Modifier.padding(start = 2.dp, top = 18.dp, bottom = 8.dp),
    )
}

/** Uma opção grande com desenho, para escolhas de dois ou três valores (modo, música). */
@Composable
internal fun PictureChoice(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, picture: @Composable () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(shape)
            .background(Color(0xFF0F0F13), shape)
            .border(if (selected) 2.dp else 1.dp, Color.White.copy(alpha = if (selected) 1f else .14f), shape)
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) { picture() }
        Text(label, color = Color.White.copy(alpha = if (selected) 1f else .65f), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

/** Traços cinza no lugar do texto, para os desenhos das opções. */
@Composable
internal fun Bars(vararg widths: Int, height: Int = 6) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        widths.forEach { w -> Box(Modifier.width(w.dp).height(height.dp).background(Color.White.copy(alpha = .5f), RoundedCornerShape(3.dp))) }
    }
}
