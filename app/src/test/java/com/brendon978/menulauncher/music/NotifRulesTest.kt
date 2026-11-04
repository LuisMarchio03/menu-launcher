package com.brendon978.menulauncher.music

import org.junit.Assert.assertEquals
import org.junit.Test

class NotifRulesTest {
    private val own = "com.brendon978.menulauncher"

    @Test fun soContaNotificacaoDeVerdade() {
        val list = listOf(
            NotifInfo("com.whatsapp", clearable = true, ongoing = false),
            NotifInfo("com.spotify.music", clearable = false, ongoing = true), // música tocando
            NotifInfo(own, clearable = true, ongoing = false),                 // do próprio launcher
            NotifInfo("com.whatsapp", clearable = true, ongoing = false),
            NotifInfo("com.discord", clearable = true, ongoing = false),
        )
        assertEquals(setOf("com.whatsapp", "com.discord"), badgePackages(list, own))
        assertEquals(3, realCount(list, own))
    }

    @Test fun novidadeEOQueApareceuAgora() {
        assertEquals(setOf("com.discord"), newcomers(prev = setOf("com.whatsapp"), now = setOf("com.whatsapp", "com.discord")))
        assertEquals(emptySet<String>(), newcomers(prev = setOf("com.whatsapp"), now = emptySet()))
    }
}
