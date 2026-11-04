package com.brendon978.menulauncher.ai

import com.brendon978.menulauncher.settings.LauncherSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiPlugTest {
    @Test fun segurarOuveSoQuandoAIaAceitaComando() {
        assertEquals(HoldAction.LISTEN, holdAction(acceptsCommands = true))
        assertEquals(HoldAction.OPEN, holdAction(acceptsCommands = false))
    }

    @Test fun posicaoFicaDentroDaTela() {
        assertEquals(0f, clampFraction(-0.3f))
        assertEquals(1f, clampFraction(1.7f))
        assertEquals(0.4f, clampFraction(0.4f))
        assertEquals(0.5f, clampFraction(Float.NaN))
    }

    @Test fun iaDesinstaladaEDesconectada() {
        val s = LauncherSettings(aiPackage = "com.brendon.iris", aiActivity = "x.Main")
        val r = pruneAi(s, installedPackages = setOf("com.outro"))
        assertNull(r.aiPackage)
        assertNull(r.aiActivity)
    }

    @Test fun iaInstaladaContinua() {
        val s = LauncherSettings(aiPackage = "com.brendon.iris", aiActivity = "x.Main")
        assertEquals(s, pruneAi(s, setOf("com.brendon.iris")))
    }

    @Test fun padraoSemIaNaZonaDoPolegar() {
        val s = LauncherSettings()
        assertNull(s.aiPackage)
        assert(s.aiY >= 0.5f) { "símbolo deve nascer na metade de baixo" }
    }

    @Test fun posicaoNoMapaViraFracaoDaTela() {
        assertEquals(0.25f, positionFraction(50f, 200f))
        assertEquals(1f, positionFraction(260f, 200f))
        assertEquals(0f, positionFraction(-10f, 200f))
    }

    @Test fun mapaSemTamanhoFicaNoMeio() {
        assertEquals(0.5f, positionFraction(10f, 0f))
    }
}
