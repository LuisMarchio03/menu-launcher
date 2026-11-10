package com.brendon978.menulauncher.clock

import com.brendon978.menulauncher.settings.HourFormat

/** As partes do relógio digital já formatadas; [seconds] e [suffix] (AM/PM) só quando pedidos. */
data class ClockFace(val hours: String, val minutes: String, val seconds: String?, val suffix: String?) {
    val text: String get() = listOfNotNull(hours, minutes, seconds).joinToString(":") + suffix?.let { " $it" }.orEmpty()
}

/** 24 horas com zero na frente ("09:05", como era); 12 horas sem ("9:05"), e meia-noite e meio-dia são 12. */
fun clockFace(hour: Int, minute: Int, second: Int, format: HourFormat, systemIs24h: Boolean, showSeconds: Boolean): ClockFace {
    val h24 = format == HourFormat.H24 || (format == HourFormat.SYSTEM && systemIs24h)
    val hours = if (h24) "%02d".format(hour) else (hour % 12).let { if (it == 0) 12 else it }.toString()
    return ClockFace(
        hours = hours,
        minutes = "%02d".format(minute),
        seconds = if (showSeconds) "%02d".format(second) else null,
        suffix = if (format == HourFormat.H12_AMPM) (if (hour < 12) "AM" else "PM") else null,
    )
}
