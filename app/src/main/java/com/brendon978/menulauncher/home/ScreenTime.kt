package com.brendon978.menulauncher.home

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import java.util.Calendar

/** Consulta ao sistema no máximo uma vez por minuto. */
const val SCREEN_TIME_MIN_GAP = 60_000L

enum class ScreenEventKind { RESUMED, PAUSED, SCREEN_OFF }

/** [activity] identifica a tela do app; vazio em SCREEN_OFF. */
data class ScreenEvent(val time: Long, val kind: ScreenEventKind, val activity: String = "")

fun shouldRefreshScreenTime(now: Long, lastQuery: Long): Boolean = now < lastQuery || now - lastQuery >= SCREEN_TIME_MIN_GAP

/**
 * Tempo entre [start] e [end] com algum app em primeiro plano. Dois apps na tela dividida contam uma vez só.
 * Um app que aparece primeiro sendo pausado já estava aberto antes de [start] (virou a meia-noite nele).
 */
fun screenMillis(events: List<ScreenEvent>, start: Long, end: Long): Long {
    val sorted = events.sortedBy { it.time }
    val open = sorted.filter { it.kind != ScreenEventKind.SCREEN_OFF }
        .groupBy { it.activity }.values
        .filter { it.first().kind == ScreenEventKind.PAUSED }
        .mapTo(mutableSetOf()) { it.first().activity }
    var since = start
    var total = 0L
    for (e in sorted) {
        val wasOpen = open.isNotEmpty()
        when (e.kind) {
            ScreenEventKind.RESUMED -> open += e.activity
            ScreenEventKind.PAUSED -> open -= e.activity
            ScreenEventKind.SCREEN_OFF -> open.clear()
        }
        if (wasOpen && open.isEmpty()) total += e.time - since
        if (!wasOpen && open.isNotEmpty()) since = e.time
    }
    if (open.isNotEmpty()) total += end - since
    return total
}

/** "3h12 de tela hoje", "3h05 de tela hoje" ou "45min de tela hoje". */
fun formatScreenTime(millis: Long): String {
    val minutes = (millis.coerceAtLeast(0) / 60_000).toInt()
    val time = if (minutes >= 60) "%dh%02d".format(minutes / 60, minutes % 60) else "${minutes}min"
    return "$time de tela hoje"
}

/** Acesso a uso de apps: o usuário libera na tela do sistema. */
fun hasUsageAccess(context: Context): Boolean {
    val mode = context.getSystemService(AppOpsManager::class.java)
        .unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    return mode == AppOpsManager.MODE_ALLOWED ||
        (mode == AppOpsManager.MODE_DEFAULT && context.checkSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED)
}

/** O texto de hoje, sem contar o próprio launcher. Bloqueia: chamar fora da thread principal. */
fun queryScreenTime(context: Context, now: Long): String {
    val start = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val events = context.getSystemService(UsageStatsManager::class.java).queryEvents(start, now)
    val found = mutableListOf<ScreenEvent>()
    val e = UsageEvents.Event()
    while (events.getNextEvent(e)) {
        val activity = "${e.packageName}/${e.className}"
        when {
            e.eventType == UsageEvents.Event.SCREEN_NON_INTERACTIVE -> found += ScreenEvent(e.timeStamp, ScreenEventKind.SCREEN_OFF)
            e.packageName == context.packageName -> Unit
            e.eventType == UsageEvents.Event.ACTIVITY_RESUMED -> found += ScreenEvent(e.timeStamp, ScreenEventKind.RESUMED, activity)
            e.eventType == UsageEvents.Event.ACTIVITY_PAUSED -> found += ScreenEvent(e.timeStamp, ScreenEventKind.PAUSED, activity)
        }
    }
    return formatScreenTime(screenMillis(found, start, now))
}
