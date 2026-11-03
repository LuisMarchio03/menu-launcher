package com.brendon978.menulauncher.clock

import com.brendon978.menulauncher.settings.DateStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.TimeZone

class DateTextTest {
    private val sp = TimeZone.getTimeZone("America/Sao_Paulo")
    /** 2026-11-03 22:15 em São Paulo (UTC-3) = 2026-11-04 01:15 UTC. */
    private val millis = 1793754900000L

    @Test fun dataLongaENumerica() {
        assertEquals("terça-feira, 3 de novembro", formatDate(DateStyle.LONG, millis, sp))
        assertEquals("03/11/2026", formatDate(DateStyle.NUMERIC, millis, sp))
    }

    @Test fun semData() {
        assertNull(formatDate(DateStyle.HIDDEN, millis, sp))
    }

    @Test fun horaNoFusoPedido() {
        assertEquals("22:15", formatTime(millis, sp, is24h = true))
        assertEquals("10:15", formatTime(millis, sp, is24h = false))
        assertEquals("01:15", formatTime(millis, TimeZone.getTimeZone("UTC"), is24h = true))
    }

    @Test fun nomeDaCidadeDoFuso() {
        assertEquals("New York", zoneCity("America/New_York"))
        assertEquals("Lisbon", zoneCity("Europe/Lisbon"))
        assertEquals("UTC", zoneCity("UTC"))
    }

    @Test fun nomeEmPortuguesDosFusosDaLista() {
        assertEquals("Lisboa", zoneLabel("Europe/Lisbon"))
        assertEquals("Nova York", zoneLabel("America/New_York"))
        assertEquals("Bogota", zoneLabel("America/Bogota"))
    }
}
