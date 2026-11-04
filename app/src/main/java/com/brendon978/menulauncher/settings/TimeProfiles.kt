package com.brendon978.menulauncher.settings

import com.brendon978.menulauncher.apps.AppKey
import java.util.Calendar

/** Favoritos por horário: trabalho de segunda a sexta, das 8h às 18h; noite, das 19h às 7h. */
enum class TimeProfile(val label: String) { DAY("Dia"), WORK("Trabalho"), NIGHT("Noite") }

fun profileFor(dayOfWeek: Int, hour: Int): TimeProfile = when {
    hour >= 19 || hour < 7 -> TimeProfile.NIGHT
    dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY && hour in 8 until 18 -> TimeProfile.WORK
    else -> TimeProfile.DAY
}

/** A lista de um horário; enquanto ela estiver vazia, valem os favoritos de sempre. */
fun LauncherSettings.favoritesFor(profile: TimeProfile): List<AppKey> =
    profileFavorites[profile.name]?.takeIf { it.isNotEmpty() } ?: favorites

/** Muda a lista de um horário partindo do que ele mostra hoje; desligado, muda os favoritos de sempre. */
fun LauncherSettings.editFavorites(profile: TimeProfile, f: (List<AppKey>) -> List<AppKey>): LauncherSettings =
    if (!timeProfiles) copy(favorites = f(favorites))
    else copy(profileFavorites = profileFavorites + (profile.name to f(favoritesFor(profile))))

/** Os favoritos que valem agora: o horário só conta com a opção ligada. */
fun LauncherSettings.activeFavorites(profile: TimeProfile): List<AppKey> = if (timeProfiles) favoritesFor(profile) else favorites

fun currentProfile(): TimeProfile = Calendar.getInstance().let { profileFor(it.get(Calendar.DAY_OF_WEEK), it.get(Calendar.HOUR_OF_DAY)) }
