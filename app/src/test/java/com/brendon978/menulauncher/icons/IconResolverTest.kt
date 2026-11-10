package com.brendon978.menulauncher.icons

import com.brendon978.menulauncher.apps.AppKey
import org.junit.Assert.assertEquals
import org.junit.Test

class IconResolverTest {
    private val filter = AppFilter(mapOf("com.spotify.music/Main" to "spotify"), mapOf("com.spotify.music" to "spotify"), mapOf("instagram" to "instagram"))
    private val spotify = AppKey("com.spotify.music", "Main", 0)
    private val outro = AppKey("com.outro", "Main", 0)

    @Test fun trocaManualVenceTudo() {
        assertEquals(IconSource.Lawnicon("x"), resolveIcon(spotify, "Spotify", "x", filter) { true })
    }

    @Test fun lawniconsAntesDoMonocromatico() {
        assertEquals(IconSource.Lawnicon("spotify"), resolveIcon(spotify, "Spotify", null, filter) { true })
    }

    @Test fun contornoDoIconeDoSistemaQuandoForaDoPacote() {
        assertEquals(IconSource.Traced(outro), resolveIcon(outro, "Outro", null, filter) { true })
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

    // WebAPK: o Chrome sorteia o pacote a cada instalação, então vale o nome do app.
    private fun webapk(hash: String) = AppKey("org.chromium.webapk.$hash", "org.chromium.webapk.shell_apk.h2o.H2OMainActivity", 0)

    @Test fun webapkCasaODesenhoProprioPeloNome() {
        val own = mapOf("atlas" to "atlas")
        assertEquals(IconSource.Own("atlas"), resolveIcon(webapk("a1b2"), "Atlas", null, filter, ownByName = own) { false })
        assertEquals(IconSource.Own("atlas"), resolveIcon(webapk("ff00"), "Atlas Acadêmico", null, filter, ownByName = mapOf("atlasacademico" to "atlas")) { false })
    }

    @Test fun webapkCasaOLawniconsPeloNome() {
        assertEquals(IconSource.Lawnicon("instagram"), resolveIcon(webapk("c3"), "Instagram", null, filter, ownByName = emptyMap()) { false })
    }

    @Test fun nomeSoValeParaWebapk() {
        assertEquals(IconSource.Initial('A'), resolveIcon(outro, "Atlas", null, filter, ownByName = mapOf("atlas" to "atlas")) { false })
        assertEquals(IconSource.Initial('I'), resolveIcon(outro, "Instagram", null, filter) { false })
    }

    @Test fun desenhosDoLuisPeloPacoteEPeloNome() {
        assertEquals(IconSource.Own("aloy"), resolveIcon(AppKey("com.luismarchio.aloy", "Main", 0), "ALOY", null, filter) { true })
        listOf("Atlas" to "atlas", "Macronaut" to "macronaut", "Quire" to "quire", "RecipeBox" to "recipebox", "Lista Compras" to "lista_compras", "Lista de Compras" to "lista_compras")
            .forEach { (label, icon) -> assertEquals(label, IconSource.Own(icon), resolveIcon(webapk("9"), label, null, filter) { true }) }
    }
}
