package com.brendon978.menulauncher.clock

private val HOURS = listOf("", "uma", "duas", "três", "quatro", "cinco", "seis", "sete", "oito", "nove", "dez", "onze")
private val UNITS = listOf(
    "", "um", "dois", "três", "quatro", "cinco", "seis", "sete", "oito", "nove",
    "dez", "onze", "doze", "treze", "catorze", "quinze", "dezesseis", "dezessete", "dezoito", "dezenove",
)
private val TENS = listOf("", "", "vinte", "trinta", "quarenta", "cinquenta")

/** Hora por extenso em pt-BR, no relógio de 12 horas: "dez e quinze", "meio-dia", "três em ponto". */
fun timeInWords(hour: Int, minute: Int): String {
    val h = when (hour % 24) {
        0 -> "meia-noite"
        12 -> "meio-dia"
        else -> HOURS[hour % 12]
    }
    return when {
        minute == 0 -> if (hour % 12 == 0) h else "$h em ponto"
        minute == 30 -> "$h e meia"
        else -> "$h e ${minuteWords(minute)}"
    }
}

private fun minuteWords(m: Int): String = when {
    m < 20 -> UNITS[m]
    m % 10 == 0 -> TENS[m / 10]
    else -> "${TENS[m / 10]} e ${UNITS[m % 10]}"
}
