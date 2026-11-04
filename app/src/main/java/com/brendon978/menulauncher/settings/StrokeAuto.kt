package com.brendon978.menulauncher.settings

import kotlin.math.pow

/** De onde vem a cor do traço: do ajuste fixo, do horário ou do papel de parede. */
enum class StrokeAuto(val label: String) {
    OFF("Desligada"),
    BY_TIME("Por horário"),
    BY_WALLPAPER("Pelo papel de parede"),
}

/** Dia das 7h às 19h; o resto é noite. */
fun isDayHour(hour: Int): Boolean = hour in 7 until 19

/**
 * A cor que vale agora, a única que os lugares que desenham devem usar.
 * [wallpaperColor] null (papel de parede animado que não informa cor) cai na cor fixa.
 */
fun effectiveStroke(s: LauncherSettings, hour: Int, wallpaperColor: Int?): Int = when (s.strokeAuto) {
    StrokeAuto.OFF -> s.strokeColor
    StrokeAuto.BY_TIME -> if (isDayHour(hour)) s.strokeDay else s.strokeNight
    StrokeAuto.BY_WALLPAPER -> wallpaperColor?.let(::legibleOnDark) ?: s.strokeColor
}

/** O fundo das telas do launcher, quase preto. */
private const val DARK_BACKGROUND = 0xFF0C0C0E.toInt()
private const val MIN_CONTRAST = 7.0

/** Clareia misturando com branco, só o necessário para ler sobre o fundo escuro; a cor segue reconhecível. */
fun legibleOnDark(argb: Int): Int {
    val opaque = argb or 0xFF000000.toInt()
    if (contrastOnDark(opaque) >= MIN_CONTRAST) return opaque
    return (1..20).asSequence().map { mixWithWhite(opaque, it / 20.0) }.first { contrastOnDark(it) >= MIN_CONTRAST }
}

/** Contraste (1 a 21) da cor sobre o fundo escuro, pela fórmula do WCAG. */
fun contrastOnDark(argb: Int): Double = (luminance(argb) + .05) / (luminance(DARK_BACKGROUND) + .05)

private fun mixWithWhite(argb: Int, t: Double): Int {
    fun mix(shift: Int) = ((argb shr shift and 0xFF) * (1 - t) + 255 * t).toInt() shl shift
    return 0xFF000000.toInt() or mix(16) or mix(8) or mix(0)
}

private fun luminance(argb: Int): Double {
    fun channel(shift: Int): Double {
        val v = (argb shr shift and 0xFF) / 255.0
        return if (v <= .03928) v / 12.92 else ((v + .055) / 1.055).pow(2.4)
    }
    return .2126 * channel(16) + .7152 * channel(8) + .0722 * channel(0)
}
