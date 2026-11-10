package com.brendon978.menulauncher.icons

import java.math.BigDecimal
import java.math.RoundingMode

private val STROKE_WIDTH = Regex("""(stroke-width\s*(?:=\s*"|:\s*))([0-9]*\.?[0-9]+)""")

/** Engrossa ou afina o traço de um SVG multiplicando cada stroke-width (atributo ou style). */
fun scaleStrokeWidths(svg: String, factor: Float): String {
    if (factor == 1f) return svg
    val f = BigDecimal(factor.toString())
    return STROKE_WIDTH.replace(svg) { m ->
        val scaled = BigDecimal(m.groupValues[2]).multiply(f).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros()
        m.groupValues[1] + scaled.toPlainString()
    }
}
