package com.brendon978.menulauncher.pet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepGearTest {
    @Test fun noiteVaiDas23As7() {
        assertTrue(isNightHour(23))
        assertTrue(isNightHour(0))
        assertTrue(isNightHour(6))
        assertFalse(isNightHour(7))
        assertFalse(isNightHour(22))
    }

    @Test fun deNoiteDormeDeGorroMesmoComOutroAcessorio() {
        assertEquals(Accessory.NIGHT_GEAR, sleepAccessory(Face.SLEEP, 23, false, Accessory.CAP))
        assertEquals(Accessory.NIGHT_GEAR, sleepAccessory(Face.SLEEP, 3, true, Accessory.HEADPHONES))
    }

    @Test fun naoPerturbeDeDiaSoTapaOlho() {
        assertEquals(Accessory.SLEEP_MASK, sleepAccessory(Face.SLEEP, 14, true, Accessory.NONE))
    }

    @Test fun cochiloDeDiaMantemOAcessorio() {
        assertEquals(Accessory.CAP, sleepAccessory(Face.SLEEP, 14, false, Accessory.CAP))
    }

    @Test fun acordadoDeNoiteNaoUsaGorro() {
        assertEquals(Accessory.CAP, sleepAccessory(Face.ANGRY, 23, false, Accessory.CAP))
    }

    @Test fun oculosSaemQuandoATelaMostraDesenho() {
        listOf(Face.HEART, Face.MUSIC, Face.ROCK, Face.PAPER, Face.SCISSORS, Face.CONFUSED).forEach {
            assertEquals(it.name, Accessory.NONE, visibleAccessory(Accessory.SUNGLASSES, it))
        }
    }

    @Test fun oculosFicamNosRostosComOlhos() {
        assertEquals(Accessory.SUNGLASSES, visibleAccessory(Accessory.SUNGLASSES, Face.NORMAL))
        assertEquals(Accessory.SUNGLASSES, visibleAccessory(Accessory.SUNGLASSES, Face.ANGRY))
    }

    @Test fun outrosAcessoriosNaoMudamComDesenho() {
        assertEquals(Accessory.CAP, visibleAccessory(Accessory.CAP, Face.HEART))
    }
}
