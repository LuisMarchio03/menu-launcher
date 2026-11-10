package com.brendon978.menulauncher.clock

import com.brendon978.menulauncher.settings.DateStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val PT_BR = Locale.forLanguageTag("pt-BR")

private fun pattern(p: String, millis: Long, zone: TimeZone): String =
    SimpleDateFormat(p, PT_BR).apply { timeZone = zone }.format(Date(millis))

/** Linha da data embaixo do relógio; null = não mostrar. */
fun formatDate(style: DateStyle, millis: Long, zone: TimeZone): String? = when (style) {
    DateStyle.SHORT -> pattern("EEE, d 'de' MMMM", millis, zone)
    DateStyle.LONG -> pattern("EEEE, d 'de' MMMM", millis, zone)
    DateStyle.NUMERIC -> pattern("dd/MM/yyyy", millis, zone)
    DateStyle.HIDDEN -> null
}

fun formatTime(millis: Long, zone: TimeZone, is24h: Boolean): String =
    pattern(if (is24h) "HH:mm" else "h:mm", millis, zone)

/** "America/New_York" -> "New York": o último trecho do id, sem sublinhado. */
fun zoneCity(id: String): String = id.substringAfterLast('/').replace('_', ' ')

/** Fusos oferecidos nos ajustes, com o nome em português. */
val ZONE_CHOICES: List<Pair<String, String>> = listOf(
    "Europe/Lisbon" to "Lisboa",
    "Europe/London" to "Londres",
    "Europe/Paris" to "Paris",
    "America/New_York" to "Nova York",
    "America/Los_Angeles" to "Los Angeles",
    "Asia/Tokyo" to "Tóquio",
    "UTC" to "UTC",
)

/** Nome do fuso para mostrar embaixo do relógio: o da lista em português, senão a cidade do id. */
fun zoneLabel(id: String): String = ZONE_CHOICES.firstOrNull { it.first == id }?.second ?: zoneCity(id)
