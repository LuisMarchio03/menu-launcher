package com.brendon978.menulauncher.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdaterTest {
    @Test fun leAVersaoPublicada() {
        val r = parseRelease("""{"versionCode": 7, "url": "https://x.y/a.apk", "notes": "Novidade"}""")!!
        assertEquals(7, r.versionCode)
        assertEquals("https://x.y/a.apk", r.url)
        assertEquals("Novidade", r.notes)
    }

    @Test fun recusaLinkSemHttps() {
        assertNull(parseRelease("""{"versionCode": 7, "url": "http://x.y/a.apk"}"""))
        assertNull(parseRelease("isso nao e json"))
    }

    @Test fun soAtualizaParaVersaoMaior() {
        val r = Release(3, "https://x", "")
        assertTrue(isNewer(r, 2))
        assertFalse(isNewer(r, 3))
        assertFalse(isNewer(r, 4))
    }
}
