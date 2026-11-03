package com.brendon978.menulauncher.aloy

import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.apps.AppKey
import com.brendon978.menulauncher.settings.HomeMode

data class AloyResult(val ok: Boolean, val erro: String? = null)

enum class MusicCmd { PREVIOUS, PLAY_PAUSE, NEXT }
enum class MusicOutcome { DONE, NO_ACCESS, NO_MEDIA }
enum class AloyScreen { SEARCH, SETTINGS }

interface AloySystem {
    fun notifications(): Boolean
    fun lock(): Boolean
    fun setMode(mode: HomeMode)
    fun music(cmd: MusicCmd): MusicOutcome
    /** false = o launcher não tem como mostrar a tela agora. */
    fun open(screen: AloyScreen): Boolean
}

val APP_COLUMNS: Array<String> = arrayOf("label", "package", "activity", "user_serial")

/** Linhas de `/apps`, nas colunas de [APP_COLUMNS]. App escondido nos ajustes fica de fora. */
fun appRows(apps: List<AppEntry>, hidden: Set<AppKey>): List<Array<Any>> =
    apps.filter { it.key !in hidden }.map { arrayOf(it.label, it.key.packageName, it.key.activityName, it.key.userSerial) }

/** Contrato com a ALOY: uma [acao] com [args] opcional vira uma chamada em [sys] e um resultado ok/erro. */
fun runAloy(acao: String, args: String?, sys: AloySystem): AloyResult = when (acao) {
    "notificacoes" -> okOr(sys.notifications(), "acessibilidade_desligada")
    "bloquear" -> okOr(sys.lock(), "acessibilidade_desligada")
    "modo" -> when (args) {
        "ONE_HAND" -> done { sys.setMode(HomeMode.ONE_HAND) }
        "GRID" -> done { sys.setMode(HomeMode.GRID) }
        else -> AloyResult(false, "args_invalidos")
    }
    "musica" -> when (args) {
        "anterior" -> musicResult(sys.music(MusicCmd.PREVIOUS))
        "play_pause" -> musicResult(sys.music(MusicCmd.PLAY_PAUSE))
        "proxima" -> musicResult(sys.music(MusicCmd.NEXT))
        else -> AloyResult(false, "args_invalidos")
    }
    "buscar" -> okOr(sys.open(AloyScreen.SEARCH), "tela_indisponivel")
    "ajustes" -> okOr(sys.open(AloyScreen.SETTINGS), "tela_indisponivel")
    else -> AloyResult(false, "acao_desconhecida")
}

private inline fun done(action: () -> Unit): AloyResult {
    action()
    return AloyResult(true)
}

private fun okOr(worked: Boolean, erro: String) = if (worked) AloyResult(true) else AloyResult(false, erro)

private fun musicResult(outcome: MusicOutcome) = when (outcome) {
    MusicOutcome.DONE -> AloyResult(true)
    MusicOutcome.NO_ACCESS -> AloyResult(false, "sem_acesso_notificacoes")
    MusicOutcome.NO_MEDIA -> AloyResult(false, "sem_midia")
}
