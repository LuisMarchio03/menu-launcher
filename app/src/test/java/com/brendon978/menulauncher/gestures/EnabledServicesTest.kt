package com.brendon978.menulauncher.gestures

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnabledServicesTest {
    private val me = "com.brendon978.menulauncher/com.brendon978.menulauncher.gestures.LauncherAccessibility"
    private val other = "com.samsung.accessibility/com.samsung.accessibility.Service"

    @Test fun ligaNaListaVazia() {
        assertEquals(me, EnabledServices.with(null, me))
        assertEquals(me, EnabledServices.with("", me))
    }

    @Test fun ligaSemTirarOsOutrosNemDuplicar() {
        assertEquals("$other:$me", EnabledServices.with(other, me))
        assertEquals("$other:$me", EnabledServices.with("$other:$me", me))
    }

    @Test fun reconheceAFormaCurtaDoNome() {
        val short = "com.brendon978.menulauncher/.gestures.LauncherAccessibility"
        assertTrue(EnabledServices.has("$other:$short", me))
        assertEquals(other, EnabledServices.without("$other:$short", me))
    }

    @Test fun desligaSoOProprio() {
        assertEquals(other, EnabledServices.without("$me:$other", me))
        assertEquals("", EnabledServices.without(me, me))
        assertFalse(EnabledServices.has(other, me))
        assertFalse(EnabledServices.has(null, me))
    }
}
