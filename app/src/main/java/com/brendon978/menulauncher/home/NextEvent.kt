package com.brendon978.menulauncher.home

import android.content.Context
import android.provider.CalendarContract
import java.time.Instant
import java.time.ZoneOffset
import java.util.Calendar
import java.util.TimeZone

/**
 * Texto curto do próximo compromisso: "Agora · X", "16:00 · X", "Amanhã 09:30 · X", "Hoje · X" (dia inteiro).
 * null se já acabou. Só olha hoje e amanhã.
 */
fun formatNextEvent(title: String, begin: Long, end: Long, allDay: Boolean, now: Long, tz: TimeZone = TimeZone.getDefault()): String? {
    val name = title.trim().ifEmpty { "Compromisso" }
    val zone = tz.toZoneId()
    fun localDate(ms: Long) = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    val today = localDate(now)
    val tomorrow = today.plusDays(1)
    if (allDay) {
        // dia inteiro vem em meia-noite UTC, com fim exclusivo: compara as datas, não os instantes
        fun utcDate(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
        val first = utcDate(begin)
        return when {
            utcDate(end - 1) < today -> null
            first <= today -> "Hoje · $name"
            first == tomorrow -> "Amanhã · $name"
            else -> null
        }
    }
    if (end <= now) return null
    if (begin <= now) return "Agora · $name"
    val c = Calendar.getInstance(tz).apply { timeInMillis = begin }
    val hhmm = "%02d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
    return when (localDate(begin)) {
        today -> "$hhmm · $name"
        tomorrow -> "Amanhã $hhmm · $name"
        else -> null
    }
}

/** Próximo compromisso da agenda (hoje ou amanhã), já formatado; null sem permissão ou sem nada. */
fun queryNextEvent(context: Context, now: Long = System.currentTimeMillis()): String? = runCatching {
    val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
        android.content.ContentUris.appendId(it, now - 12 * 3_600_000L)
        android.content.ContentUris.appendId(it, now + 48 * 3_600_000L)
    }.build()
    val cols = arrayOf(CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.END, CalendarContract.Instances.ALL_DAY, CalendarContract.Instances.STATUS)
    val visible = "${CalendarContract.Instances.VISIBLE} = 1"
    context.contentResolver.query(uri, cols, visible, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
        // o de dia inteiro (feriado, aniversário) só aparece se não houver nada com horário
        var allDay: String? = null
        while (c.moveToNext()) {
            if (c.getInt(4) == CalendarContract.Instances.STATUS_CANCELED) continue
            val isAllDay = c.getInt(3) == 1
            val text = formatNextEvent(c.getString(0).orEmpty(), c.getLong(1), c.getLong(2), isAllDay, now) ?: continue
            if (!isAllDay) return@runCatching text
            if (allDay == null) allDay = text
        }
        allDay
    }
}.getOrNull()
