package com.brendon978.menulauncher.pet

// ---- rotina: bom dia e "vai dormir" ----
enum class Routine { GOOD_MORNING, GO_TO_SLEEP }

/**
 * Ao abrir a tela inicial: bom dia uma vez por dia entre 6h e 11h; de madrugada (0h–5h), manda dormir,
 * no máximo a cada 30 min. [today] é a data no formato aaaammdd.
 */
fun routineFor(hour: Int, today: Int, lastMorning: Int, lastNightNag: Long, now: Long): Routine? = when {
    hour in 6..10 && lastMorning != today -> Routine.GOOD_MORNING
    hour in 0..4 && now - lastNightNag >= 30 * 60_000L -> Routine.GO_TO_SLEEP
    else -> null
}

val GOOD_MORNING_LINES = listOf("Bom dia!", "Bom dia, mané! Dormiu bem?", "Acordou, hein!", "Café já?")
val GO_TO_SLEEP_LINES = listOf("Vai dormir, mané!", "Isso são horas?!", "Larga o celular e dorme!", "Amanhã você reclama de sono...")

// ---- diário do dia ----
data class Diary(
    val day: Int,
    val thrown: Int = 0,
    val petted: Int = 0,
    val poked: Int = 0,
    val games: Int = 0,
    val fled: Int = 0,
) {
    /** Virou o dia: começa um diário novo. */
    fun forDay(today: Int) = if (today == day) this else Diary(today)
}

/** O diário na voz do Glitch. */
fun diaryText(d: Diary): String {
    val parts = buildList {
        if (d.petted > 0) add("me fez carinho ${d.petted} ${if (d.petted == 1) "vez" else "vezes"}")
        if (d.thrown > 0) add("me jogou ${d.thrown} ${if (d.thrown == 1) "vez" else "vezes"}")
        if (d.poked > 0) add("me cutucou ${d.poked} ${if (d.poked == 1) "vez" else "vezes"}")
        if (d.games > 0) add("brincou comigo ${d.games} ${if (d.games == 1) "vez" else "vezes"}")
        if (d.fled > 0) add("me fez fugir ${d.fled} ${if (d.fled == 1) "vez" else "vezes"}")
    }
    if (parts.isEmpty()) return "Nada de interessante hoje. Nem olhou pra mim."
    val list = if (parts.size == 1) parts[0] else parts.dropLast(1).joinToString(", ") + " e " + parts.last()
    val verdict = when {
        d.petted >= d.poked + d.thrown -> "Até que foi um bom dia."
        d.poked > 10 || d.fled > 0 -> "Tô de olho em você."
        else -> "Podia ser pior."
    }
    return "Hoje você $list. $verdict"
}
