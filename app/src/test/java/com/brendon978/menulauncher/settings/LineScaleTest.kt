package com.brendon978.menulauncher.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class LineScaleTest {
    @Test fun espessuraPadraoNaoMudaODesenho() = assertEquals(1f, lineScale(1f), 0.001f)

    @Test fun noMaximoODesenhoEngrossaMenosQueABorda() = assertEquals(2.2f, lineScale(3f), 0.001f)

    @Test fun noMinimoODesenhoAfina() = assertEquals(0.7f, lineScale(0.5f), 0.001f)
}
