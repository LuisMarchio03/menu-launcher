package com.brendon978.menulauncher.wallpaper

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Abaixo disso (diferença entre o canal maior e o menor, 0 a 255) a cor parece preta, cinza ou branca. */
private const val MIN_CHROMA = 38

/**
 * Cor do papel de parede que dá o matiz do traço, entre as principais em ordem de presença: a primeira
 * que tem cor de fato. Em papel escuro a principal costuma ser o fundo quase preto, que daria traço cinza.
 */
fun strokeBase(colors: List<Int>): Int = colors.firstOrNull { chroma(it) >= MIN_CHROMA } ?: colors.first()

private fun chroma(c: Int): Int {
    val (r, g, b) = Triple(c shr 16 and 0xFF, c shr 8 and 0xFF, c and 0xFF)
    return max(r, max(g, b)) - min(r, min(g, b))
}

/**
 * Cor do traço a partir da cor do papel de parede ([strokeBase]): mesmo matiz, clareada (papel escuro)
 * ou escurecida (papel claro, [darkText]) até ter contraste 7:1 com o fundo. Sempre opaca.
 */
fun legibleStroke(argb: Int, darkText: Boolean): Int {
    val (h, s, l0) = toHsl(argb)
    // Pastel no escuro, fechado no claro: saturação contida para não gritar como um logo colorido.
    val sat = min(s, 0.6)
    var l = if (darkText) min(l0, 0.3) else max(l0, 0.72)
    var c = fromHsl(h, sat, l)
    while (if (darkText) luminance(c) > 0.10 else luminance(c) < 0.30) {
        l = if (darkText) l - 0.02 else l + 0.02
        c = fromHsl(h, sat, l.coerceIn(0.0, 1.0))
        if (l <= 0.0 || l >= 1.0) break
    }
    return c
}

private fun luminance(c: Int): Double {
    fun ch(v: Int): Double {
        val x = v / 255.0
        return if (x <= 0.03928) x / 12.92 else ((x + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * ch(c shr 16 and 0xFF) + 0.7152 * ch(c shr 8 and 0xFF) + 0.0722 * ch(c and 0xFF)
}

private fun toHsl(c: Int): Triple<Double, Double, Double> {
    val r = (c shr 16 and 0xFF) / 255.0
    val g = (c shr 8 and 0xFF) / 255.0
    val b = (c and 0xFF) / 255.0
    val mx = max(r, max(g, b))
    val mn = min(r, min(g, b))
    val l = (mx + mn) / 2
    val d = mx - mn
    if (d == 0.0) return Triple(0.0, 0.0, l)
    val s = d / (1 - abs(2 * l - 1))
    val h = when (mx) {
        r -> 60 * (((g - b) / d).mod(6.0))
        g -> 60 * ((b - r) / d + 2)
        else -> 60 * ((r - g) / d + 4)
    }
    return Triple(h, s, l)
}

private fun fromHsl(h: Double, s: Double, l: Double): Int {
    val c = (1 - abs(2 * l - 1)) * s
    val x = c * (1 - abs((h / 60).mod(2.0) - 1))
    val m = l - c / 2
    val (r, g, b) = when ((h / 60).toInt()) {
        0 -> Triple(c, x, 0.0)
        1 -> Triple(x, c, 0.0)
        2 -> Triple(0.0, c, x)
        3 -> Triple(0.0, x, c)
        4 -> Triple(x, 0.0, c)
        else -> Triple(c, 0.0, x)
    }
    fun ch(v: Double) = ((v + m) * 255).toInt().coerceIn(0, 255)
    return (0xFF shl 24) or (ch(r) shl 16) or (ch(g) shl 8) or ch(b)
}
