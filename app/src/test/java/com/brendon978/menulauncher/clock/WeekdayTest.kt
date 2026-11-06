package com.brendon978.menulauncher.clock

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class WeekdayTest {
    @Test fun tresLetrasEmPortugues() {
        assertEquals("DOM", weekdayShort(Calendar.SUNDAY))
        assertEquals("QUA", weekdayShort(Calendar.WEDNESDAY))
        assertEquals("QUI", weekdayShort(Calendar.THURSDAY))
        assertEquals("SÁB", weekdayShort(Calendar.SATURDAY))
    }
}
