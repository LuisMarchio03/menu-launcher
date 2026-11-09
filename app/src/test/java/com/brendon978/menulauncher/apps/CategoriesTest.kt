package com.brendon978.menulauncher.apps

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoriesTest {
    private fun app(pkg: String, label: String, cat: Int = -1) = AppEntry(AppKey(pkg, "$pkg.Main", 0), label, cat)

    @Test fun usaACategoriaQueOAppDeclara() {
        assertEquals(AppCategory.GAMES, categoryOf(0, "com.x.qualquer", null))
        assertEquals(AppCategory.SOCIAL, categoryOf(4, "com.x.qualquer", null))
        assertEquals(AppCategory.MEDIA, categoryOf(1, "com.x.qualquer", null))
        assertEquals(AppCategory.WORK, categoryOf(7, "com.x.qualquer", null))
    }

    @Test fun semCategoriaAdivinhaPeloPacote() {
        assertEquals(AppCategory.SOCIAL, categoryOf(-1, "com.whatsapp", null))
        assertEquals(AppCategory.WORK, categoryOf(-1, "com.google.android.gm", null))
        assertEquals(AppCategory.MEDIA, categoryOf(-1, "com.spotify.music", null))
        assertEquals(AppCategory.OTHER, categoryOf(-1, "com.desconhecido.app", null))
    }

    @Test fun nomeConhecidoVenceACategoriaDeclaradaErrada() {
        // o Gmail e o Chrome declaram "social" no aparelho
        assertEquals(AppCategory.WORK, categoryOf(4, "com.google.android.gm", null))
        assertEquals(AppCategory.WORK, categoryOf(4, "com.google.android.apps.tachyon", null))
        assertEquals(AppCategory.TOOLS, categoryOf(4, "com.android.chrome", null))
    }

    @Test fun aEscolhaDoUsuarioVence() {
        assertEquals(AppCategory.GAMES, categoryOf(4, "com.whatsapp", "GAMES"))
        // nome que não existe mais: volta ao automático
        assertEquals(AppCategory.SOCIAL, categoryOf(4, "com.whatsapp", "SUMIU"))
    }

    @Test fun agrupaNaOrdemDasCategoriasSemGrupoVazio() {
        val apps = listOf(app("com.spotify.music", "Spotify"), app("com.whatsapp", "WhatsApp"), app("com.instagram.android", "Instagram", 4))
        val groups = groupByCategory(apps, emptyMap())
        assertEquals(listOf(AppCategory.SOCIAL, AppCategory.MEDIA), groups.map { it.first })
        assertEquals(listOf("Instagram", "WhatsApp"), groups.first().second.map { it.label })
    }
}
