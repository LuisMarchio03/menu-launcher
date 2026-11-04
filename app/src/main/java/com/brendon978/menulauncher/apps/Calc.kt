package com.brendon978.menulauncher.apps

/**
 * Calculadora da busca: "12*7" dá "84". Só responde se o texto for uma conta de verdade
 * (com pelo menos uma operação); qualquer outra coisa dá null e a busca segue normal.
 */
fun calc(text: String): String? {
    val s = text.replace(" ", "").replace(',', '.').replace('x', '*').replace('×', '*').replace('÷', '/')
    if (s.isEmpty() || s.none { it in "+-*/%" } || s.any { it !in "0123456789.+-*/%()" }) return null
    val v = runCatching { Parser(s).parse() }.getOrNull() ?: return null
    if (v.isNaN() || v.isInfinite()) return null
    return format(v)
}

/** Até 6 casas, sem notação científica. */
private fun format(v: Double): String =
    java.math.BigDecimal(v).setScale(6, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString().replace('.', ',')

private class Parser(val s: String) {
    var i = 0

    fun parse(): Double {
        val v = expr()
        check(i == s.length)
        return v
    }

    private fun expr(): Double {
        var v = term()
        while (i < s.length && s[i] in "+-") {
            val op = s[i++]
            val r = term()
            v = if (op == '+') v + r else v - r
        }
        return v
    }

    private fun term(): Double {
        var v = factor()
        while (i < s.length && s[i] in "*/") {
            val op = s[i++]
            val r = factor()
            v = if (op == '*') v * r else v / r
        }
        return v
    }

    private fun factor(): Double {
        check(i < s.length)
        if (s[i] == '-') { i++; return -factor() }
        var v: Double
        if (s[i] == '(') {
            i++
            v = expr()
            check(i < s.length && s[i] == ')')
            i++
        } else {
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            check(i > start)
            v = s.substring(start, i).toDouble()
        }
        while (i < s.length && s[i] == '%') { i++; v /= 100 }
        return v
    }
}
