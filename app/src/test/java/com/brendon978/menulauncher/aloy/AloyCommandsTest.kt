package com.brendon978.menulauncher.aloy

import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.apps.AppKey
import com.brendon978.menulauncher.settings.HomeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class AloyCommandsTest {
    /** Sistema falso: guarda o que foi pedido e responde o que o teste mandar. */
    private class FakeSystem(
        val accessibility: Boolean = true,
        val music: MusicOutcome = MusicOutcome.DONE,
        val screens: Boolean = true,
    ) : AloySystem {
        val calls = mutableListOf<String>()
        override fun notifications(): Boolean { calls += "notificacoes"; return accessibility }
        override fun lock(): Boolean { calls += "bloquear"; return accessibility }
        override fun setMode(mode: HomeMode) { calls += "modo:$mode" }
        override fun music(cmd: MusicCmd): MusicOutcome { calls += "musica:$cmd"; return music }
        override fun open(screen: AloyScreen): Boolean { calls += "abrir:$screen"; return screens }
    }

    private val ok = AloyResult(true)
    private fun erro(e: String) = AloyResult(false, e)

    @Test fun notificacoesEBloquearDependemDaAcessibilidade() {
        val on = FakeSystem()
        assertEquals(ok, runAloy("notificacoes", null, on))
        assertEquals(ok, runAloy("bloquear", null, on))
        assertEquals(listOf("notificacoes", "bloquear"), on.calls)
        val off = FakeSystem(accessibility = false)
        assertEquals(erro("acessibilidade_desligada"), runAloy("notificacoes", null, off))
        assertEquals(erro("acessibilidade_desligada"), runAloy("bloquear", null, off))
    }

    @Test fun modoAceitaSoOsDoisValores() {
        val s = FakeSystem()
        assertEquals(ok, runAloy("modo", "GRID", s))
        assertEquals(ok, runAloy("modo", "ONE_HAND", s))
        assertEquals(listOf("modo:GRID", "modo:ONE_HAND"), s.calls)
        assertEquals(erro("args_invalidos"), runAloy("modo", "grade", s))
        assertEquals(erro("args_invalidos"), runAloy("modo", null, s))
        assertEquals(2, s.calls.size)
    }

    @Test fun musicaTraduzOsArgsEOsErros() {
        val s = FakeSystem()
        assertEquals(ok, runAloy("musica", "anterior", s))
        assertEquals(ok, runAloy("musica", "play_pause", s))
        assertEquals(ok, runAloy("musica", "proxima", s))
        assertEquals(listOf("musica:PREVIOUS", "musica:PLAY_PAUSE", "musica:NEXT"), s.calls)
        assertEquals(erro("args_invalidos"), runAloy("musica", "pular", s))
        assertEquals(erro("sem_acesso_notificacoes"), runAloy("musica", "proxima", FakeSystem(music = MusicOutcome.NO_ACCESS)))
        assertEquals(erro("sem_midia"), runAloy("musica", "proxima", FakeSystem(music = MusicOutcome.NO_MEDIA)))
    }

    @Test fun buscarEAjustesAbremATela() {
        val s = FakeSystem()
        assertEquals(ok, runAloy("buscar", null, s))
        assertEquals(ok, runAloy("ajustes", null, s))
        assertEquals(listOf("abrir:SEARCH", "abrir:SETTINGS"), s.calls)
    }

    @Test fun telaQueNaoPodeAbrirViraErro() {
        assertEquals(erro("tela_indisponivel"), runAloy("buscar", null, FakeSystem(screens = false)))
        assertEquals(erro("tela_indisponivel"), runAloy("ajustes", null, FakeSystem(screens = false)))
    }

    @Test fun acaoDesconhecida() {
        val s = FakeSystem()
        assertEquals(erro("acao_desconhecida"), runAloy("formatar", null, s))
        assertEquals(emptyList<String>(), s.calls)
    }

    @Test fun listaDeAppsTiraEscondidosENaoTemApelidos() {
        val a = AppEntry(AppKey("com.a", "com.a.Main", 0), "Alfa")
        val b = AppEntry(AppKey("com.b", "com.b.Main", 150), "Beta")
        assertEquals(listOf("label", "package", "activity", "user_serial"), APP_COLUMNS.toList())
        assertEquals(
            listOf(listOf<Any>("Beta", "com.b", "com.b.Main", 150L)),
            appRows(listOf(a, b), hidden = setOf(a.key)).map { it.toList() },
        )
    }
}
