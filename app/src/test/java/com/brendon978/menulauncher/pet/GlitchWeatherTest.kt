package com.brendon978.menulauncher.pet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GlitchWeatherTest {
    @Test fun chuva() {
        assertEquals(Weather.RAIN, weatherOf(61, 25f, 14))
        assertEquals(Weather.RAIN, weatherOf(95, 30f, 14))
    }

    @Test fun frioVemDepoisDaChuva() {
        assertEquals(Weather.COLD, weatherOf(3, 12f, 14))
        assertEquals(Weather.RAIN, weatherOf(63, 12f, 14))
    }

    @Test fun solSoDeDiaEQuente() {
        assertEquals(Weather.SUN, weatherOf(0, 32f, 14))
        assertNull(weatherOf(0, 32f, 22))
        assertNull(weatherOf(0, 24f, 14))
        assertNull(weatherOf(3, 32f, 14))
    }

    @Test fun leNumeroDoJson() {
        val j = """{"results":[{"name":"Goiania","latitude":-16.68,"longitude":-49.25}]}"""
        assertEquals(-16.68f, jsonNumber(j, "latitude"))
        assertNull(jsonNumber(j, "elevation"))
    }

    @Test fun acessorioEscolhidoVenceOClima() {
        assertEquals(Accessory.CAP, wornAccessory(Face.NORMAL, 14, false, false, null, Accessory.CAP, Weather.RAIN))
        assertEquals(Accessory.UMBRELLA, wornAccessory(Face.NORMAL, 14, false, false, null, Accessory.NONE, Weather.RAIN))
        assertEquals(Accessory.SANTA_HAT, wornAccessory(Face.NORMAL, 14, false, false, SpecialDay.CHRISTMAS, Accessory.NONE, Weather.RAIN))
    }
}
