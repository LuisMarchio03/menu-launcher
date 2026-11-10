package com.brendon978.menulauncher.home

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class AlphabetBarSizeTest {
    @Test fun tamanhoOriginalFicaNaMetadeDaTela() {
        assertEquals(400.dp, barMaxHeight(800.dp, 1f))
    }

    @Test fun maiorPassaDaMetade() {
        assertEquals(500.dp, barMaxHeight(800.dp, 1.25f))
    }

    @Test fun nuncaPassaDoLimiteParaNaoCobrirORelogio() {
        assertEquals(600.dp, barMaxHeight(800.dp, 2f))
    }
}
