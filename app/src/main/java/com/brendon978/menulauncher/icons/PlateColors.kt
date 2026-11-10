package com.brendon978.menulauncher.icons

import com.brendon978.menulauncher.wallpaper.fromHsl
import com.brendon978.menulauncher.wallpaper.luminance
import com.brendon978.menulauncher.wallpaper.toHsl
import kotlin.math.max
import kotlin.math.min

/** Cores do ícone em placa: fundo, símbolo e o aro fino que separa a placa de papel de parede escuro. */
data class PlateColors(val fill: Int, val symbol: Int, val ring: Int)

/** Contraste mínimo do símbolo sobre a placa (WCAG AA). */
private const val MIN_CONTRAST = 4.5

/**
 * Placa no padrão da ALOY e do Atlas (fundo bem escuro, símbolo de traço por cima), derivada do destaque
 * do launcher, que já segue a cor escolhida, o horário e o papel de parede: a placa leva o matiz dele.
 * [light] vai de 0 (quase preto) a 1 (escuro mais aberto). O símbolo fica na cor do destaque; destaque
 * escuro demais (papel claro) clareia no mesmo matiz até ler sobre a placa.
 */
fun plateColors(accent: Int, light: Float): PlateColors {
    val (h, s, l0) = toHsl(accent)
    val fill = fromHsl(h, min(s, 0.5), 0.04 + 0.20 * light.coerceIn(0f, 1f))
    var symbol = accent or (0xFF shl 24)
    var l = l0
    while (contrast(symbol, fill) < MIN_CONTRAST && l < 1.0) {
        l = min(1.0, l + 0.02)
        symbol = fromHsl(h, s, l)
    }
    return PlateColors(fill, symbol, (0x48 shl 24) or (symbol and 0xFFFFFF))
}

private fun contrast(a: Int, b: Int): Double {
    val la = luminance(a)
    val lb = luminance(b)
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
}
