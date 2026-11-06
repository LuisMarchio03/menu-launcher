package com.brendon978.menulauncher.clock

private val WEEKDAYS = listOf("DOM", "SEG", "TER", "QUA", "QUI", "SEX", "SÁB")

/** Dia da semana em três letras maiúsculas, pelo [java.util.Calendar.DAY_OF_WEEK] (1 = domingo). */
fun weekdayShort(dayOfWeek: Int): String = WEEKDAYS[(dayOfWeek - 1).mod(7)]
