package com.brendon978.menulauncher.pet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlitchRoutineTest {
    @Test fun bomDiaUmaVezPorDiaDeManha() {
        assertEquals(Routine.GOOD_MORNING, routineFor(hour = 8, today = 20261104, lastMorning = 20261103, lastNightNag = 0, now = 0))
        assertEquals(null, routineFor(hour = 8, today = 20261104, lastMorning = 20261104, lastNightNag = 0, now = 0))
        assertEquals(null, routineFor(hour = 14, today = 20261104, lastMorning = 20261103, lastNightNag = 0, now = 0))
    }

    @Test fun madrugadaMandaDormirNoMaximoACada30Min() {
        assertEquals(Routine.GO_TO_SLEEP, routineFor(hour = 2, today = 1, lastMorning = 1, lastNightNag = 0, now = 60 * 60_000L))
        assertEquals(null, routineFor(hour = 2, today = 1, lastMorning = 1, lastNightNag = 50 * 60_000L, now = 60 * 60_000L))
    }

    @Test fun diarioContaODia() {
        val d = Diary(day = 1, thrown = 7, petted = 2, poked = 12, games = 3, fled = 1)
        val text = diaryText(d)
        assertTrue(text.contains("7"))
        assertTrue(text.contains("2"))
        assertTrue(text.isNotBlank())
    }

    @Test fun diarioVazio() {
        assertTrue(diaryText(Diary(day = 1)).contains("Nada"))
    }

    @Test fun diarioViraODia() {
        val d = Diary(day = 1, petted = 5).forDay(2)
        assertEquals(0, d.petted)
        assertEquals(2, d.day)
        assertEquals(5, Diary(day = 3, petted = 5).forDay(3).petted)
    }
}
