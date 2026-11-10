package com.brendon978.menulauncher.icons

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppFilterTest {
    private val xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <resources>
          <calendar component="ComponentInfo{com.android.calendar/com.android.calendar.AllInOneActivity}" prefix="themed_icon_calendar_" name="Calendar" />
          <item component="ComponentInfo{com.spotify.music/com.spotify.music.MainActivity}" drawable="spotify" name="Spotify" />
          <item name="Outro" drawable="outro" component="ComponentInfo{com.outro/com.outro.Main}" />
        </resources>
    """.trimIndent()

    private val filter = AppFilter.parse(xml.byteInputStream())

    @Test fun componenteExato() {
        assertEquals("spotify", filter.drawableFor("com.spotify.music", "com.spotify.music.MainActivity"))
    }

    @Test fun mesmoPacoteOutraActivity() {
        assertEquals("spotify", filter.drawableFor("com.spotify.music", "Outra"))
    }

    @Test fun ordemDosAtributosNaoImporta() {
        assertEquals("outro", filter.drawableFor("com.outro", "com.outro.Main"))
    }

    @Test fun pacoteUsaODesenhoMaisComum() {
        val f = AppFilter.parse("""
            <item component="ComponentInfo{com.spotify.music/com.spotify.music.MainActivityAlias}" drawable="metrolist" />
            <item component="ComponentInfo{com.spotify.music/a.B}" drawable="spotify" />
            <item component="ComponentInfo{com.spotify.music/c.D}" drawable="spotify" />
        """.byteInputStream())
        assertEquals("spotify", f.drawableFor("com.spotify.music", "Desconhecida"))
        assertEquals("metrolist", f.drawableFor("com.spotify.music", "com.spotify.music.MainActivityAlias"))
    }

    @Test fun pacoteAusente() {
        assertNull(filter.drawableFor("com.nao.existe", "X"))
    }

    @Test fun componenteRelativoCasaComONomeCompleto() {
        val f = AppFilter.parse("""
            <item component="ComponentInfo{com.insta/.activity.Main}" drawable="insta" name="Insta" />
            <item component="ComponentInfo{com.insta/com.insta.Outra}" drawable="outra" />
            <item component="ComponentInfo{com.insta/com.insta.Mais}" drawable="outra" />
        """.byteInputStream())
        assertEquals("insta", f.drawableFor("com.insta", "com.insta.activity.Main"))
    }

    @Test fun nomeDoAppSemAcentoNemCaixa() {
        assertEquals("spotify", filter.drawableForName("spotify"))
        assertEquals("spotify", filter.drawableForName(" SPOTIFY "))
        assertNull(filter.drawableForName("Nada"))
    }
}
