package com.brendon978.menulauncher.settings

import java.net.URLDecoder
import java.net.URLEncoder

/** Um jeito pronto da tela inicial: relógio, cor, fonte, sombra, data e alinhamento juntos. */
data class LookTheme(
    val name: String,
    val clockStyle: ClockStyle,
    val strokeColor: Int,
    val strokeAuto: StrokeAuto,
    val font: FontChoice,
    val textShadow: Boolean,
    val dateStyle: DateStyle,
    val clockAlign: ClockAlign,
)

/** Troca só o que o tema define; tamanhos, favoritos e o resto ficam. */
fun LauncherSettings.applyTheme(t: LookTheme): LauncherSettings = copy(
    clockStyle = t.clockStyle,
    strokeColor = t.strokeColor,
    strokeAuto = t.strokeAuto,
    font = t.font,
    textShadow = t.textShadow,
    dateStyle = t.dateStyle,
    clockAlign = t.clockAlign,
)

fun LauncherSettings.toTheme(name: String): LookTheme =
    LookTheme(name, clockStyle, strokeColor, strokeAuto, font, textShadow, dateStyle, clockAlign)

/** A tela está exatamente como o tema deixa (marca o cartão dele nos Ajustes). */
fun LauncherSettings.uses(t: LookTheme): Boolean = toTheme(t.name) == t

object LookThemes {
    private const val WHITE = 0xFFFFFFFF.toInt()

    val PRESETS: List<LookTheme> = listOf(
        LookTheme("Minimal", ClockStyle.THIN, WHITE, StrokeAuto.OFF, FontChoice.SYSTEM, false, DateStyle.SHORT, ClockAlign.START),
        LookTheme("Dia grande", ClockStyle.WEEKDAY, WHITE, StrokeAuto.OFF, FontChoice.SYSTEM, false, DateStyle.LONG, ClockAlign.CENTER),
        LookTheme("Papel de parede", ClockStyle.THIN, WHITE, StrokeAuto.BY_WALLPAPER, FontChoice.CONDENSED, true, DateStyle.SHORT, ClockAlign.START),
        LookTheme("Clássico", ClockStyle.STACKED, 0xFFE6E1D6.toInt(), StrokeAuto.OFF, FontChoice.SERIF, false, DateStyle.LONG, ClockAlign.START),
        LookTheme("Neon", ClockStyle.SEGMENTS, 0xFFA0C4FF.toInt(), StrokeAuto.OFF, FontChoice.CONDENSED, true, DateStyle.SHORT, ClockAlign.CENTER),
        LookTheme("Poema", ClockStyle.WORDS, 0xFFFFD6A5.toInt(), StrokeAuto.OFF, FontChoice.SERIF, false, DateStyle.LONG, ClockAlign.START),
        LookTheme("Terminal", ClockStyle.TERMINAL, 0xFF7DFF9B.toInt(), StrokeAuto.OFF, FontChoice.MONO, false, DateStyle.NUMERIC, ClockAlign.START),
    )

    /** Um tema por linha, campos separados por tab; o nome vai codificado para não quebrar a linha. */
    fun encode(themes: List<LookTheme>): String = themes.joinToString("\n") { t ->
        listOf(
            URLEncoder.encode(t.name, "UTF-8"), t.clockStyle.name, t.strokeColor.toString(), t.strokeAuto.name,
            t.font.name, t.textShadow.toString(), t.dateStyle.name, t.clockAlign.name,
        ).joinToString("\t")
    }

    /** Linha que não se lê (enum que sumiu, campo faltando) fica de fora; as outras voltam. */
    fun decode(text: String): List<LookTheme> = text.lines().mapNotNull { line ->
        val f = line.split('\t')
        if (f.size != 8) return@mapNotNull null
        runCatching {
            LookTheme(
                URLDecoder.decode(f[0], "UTF-8"), ClockStyle.valueOf(f[1]), f[2].toInt(), StrokeAuto.valueOf(f[3]),
                FontChoice.valueOf(f[4]), f[5].toBooleanStrict(), DateStyle.valueOf(f[6]), ClockAlign.valueOf(f[7]),
            )
        }.getOrNull()
    }
}
