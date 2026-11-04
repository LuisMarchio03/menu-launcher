package com.brendon978.menulauncher.icons

import com.brendon978.menulauncher.apps.AppKey
import org.junit.Assert.assertEquals
import org.junit.Test

class IconResolverTest {
    private val filter = AppFilter(mapOf("com.spotify.music/Main" to "spotify"), mapOf("com.spotify.music" to "spotify"))
    private val spotify = AppKey("com.spotify.music", "Main", 0)
    private val outro = AppKey("com.outro", "Main", 0)

    @Test fun trocaManualVenceTudo() {
        assertEquals(IconSource.Lawnicon("x"), resolveIcon(spotify, "Spotify", "x", filter) { true })
    }

    @Test fun lawniconsAntesDoMonocromatico() {
        assertEquals(IconSource.Lawnicon("spotify"), resolveIcon(spotify, "Spotify", null, filter) { true })
    }

    @Test fun monocromaticoQuandoForaDoPacote() {
        assertEquals(IconSource.Monochrome(outro), resolveIcon(outro, "Outro", null, filter) { true })
    }

    @Test fun inicialComoUltimoRecurso() {
        assertEquals(IconSource.Initial('S'), resolveIcon(outro, "Spotify", null, filter) { false })
        assertEquals(IconSource.Initial('9'), resolveIcon(outro, "9GAG", null, filter) { false })
        assertEquals(IconSource.Initial('I'), resolveIcon(outro, "ícone", null, filter) { false })
    }

    @Test fun iconeProprioAntesDoMonocromatico() {
        val iris = AppKey("com.brendon.iris", "Main", 0)
        assertEquals(IconSource.Own("iris"), resolveIcon(iris, "IRIS", null, filter, ownIcons = mapOf("com.brendon.iris" to "iris")) { true })
    }

    @Test fun lawniconsContinuaVencendoOProprio() {
        assertEquals(IconSource.Lawnicon("spotify"), resolveIcon(spotify, "Spotify", null, filter, ownIcons = mapOf("com.spotify.music" to "x")) { true })
    }
}
