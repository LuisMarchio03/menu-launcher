package com.brendon978.menulauncher.icons

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class StrokeScaleTest {
    @Test fun multiplicaOAtributo() {
        assertEquals(
            """<path stroke-width="18" d="M0 0"/><circle stroke-width="15"/>""",
            scaleStrokeWidths("""<path stroke-width="12" d="M0 0"/><circle stroke-width="10"/>""", 1.5f),
        )
    }

    @Test fun decimalENoStyle() {
        assertEquals("""<path stroke-width="6.35" style="fill:none;stroke-width:8"/>""",
            scaleStrokeWidths("""<path stroke-width="3.175" style="fill:none;stroke-width:4"/>""", 2f))
    }

    @Test fun fatorUmNaoMexe() {
        val svg = """<path stroke-width="12"/>"""
        assertSame(svg, scaleStrokeWidths(svg, 1f))
    }
}
