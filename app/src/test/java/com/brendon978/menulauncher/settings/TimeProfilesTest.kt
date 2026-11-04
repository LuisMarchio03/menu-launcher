package com.brendon978.menulauncher.settings

import com.brendon978.menulauncher.apps.AppKey
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class TimeProfilesTest {
    private val a = AppKey("com.a", "A", 0)
    private val b = AppKey("com.b", "B", 0)

    @Test fun horarios() {
        assertEquals(TimeProfile.WORK, profileFor(Calendar.WEDNESDAY, 9))
        assertEquals(TimeProfile.DAY, profileFor(Calendar.WEDNESDAY, 18))
        assertEquals(TimeProfile.DAY, profileFor(Calendar.WEDNESDAY, 7))
        assertEquals(TimeProfile.DAY, profileFor(Calendar.SATURDAY, 9))
        assertEquals(TimeProfile.NIGHT, profileFor(Calendar.WEDNESDAY, 19))
        assertEquals(TimeProfile.NIGHT, profileFor(Calendar.SUNDAY, 3))
    }

    @Test fun listaVaziaUsaOsFavoritosDeSempre() {
        assertEquals(listOf(a), LauncherSettings(favorites = listOf(a)).favoritesFor(TimeProfile.WORK))
    }

    @Test fun editarPartiDoQueEleVe() {
        val s = LauncherSettings(favorites = listOf(a), timeProfiles = true)
        val r = s.editFavorites(TimeProfile.WORK) { it + b }
        assertEquals(listOf(a, b), r.favoritesFor(TimeProfile.WORK))
        assertEquals(listOf(a), r.favorites)
        assertEquals(listOf(a), r.favoritesFor(TimeProfile.NIGHT))
    }

    @Test fun desligadoEditaOsFavoritosDeSempre() {
        val r = LauncherSettings(favorites = listOf(a)).editFavorites(TimeProfile.WORK) { it + b }
        assertEquals(listOf(a, b), r.favorites)
        assertEquals(emptyMap<String, List<AppKey>>(), r.profileFavorites)
    }
}
