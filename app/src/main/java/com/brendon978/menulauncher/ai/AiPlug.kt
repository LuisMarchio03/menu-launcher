package com.brendon978.menulauncher.ai

import com.brendon978.menulauncher.settings.LauncherSettings

/** Nomes do contrato com as IAs (documentado em docs/plugue-ia.md). Não mudar sem avisar a IRIS e a Eloy. */
object AiContract {
    const val ACTION_ASSISTANT = "com.brendon978.menulauncher.action.ASSISTANT"
    const val ACTION_COMMAND = "com.brendon978.menulauncher.action.COMMAND"
    const val META_SYMBOL = "com.brendon978.menulauncher.symbol"
    const val ACTION_SAY = "com.brendon978.menulauncher.action.SAY"
    const val EXTRA_FROM = "com.brendon978.menulauncher.extra.FROM"
}

enum class HoldAction { LISTEN, OPEN }

/** Segurar ouve só quando a IA tem o receiver de comando; senão faz o mesmo que tocar. */
fun holdAction(acceptsCommands: Boolean): HoldAction = if (acceptsCommands) HoldAction.LISTEN else HoldAction.OPEN

/** Posição do símbolo em fração da tela, sempre dentro dela. */
fun clampFraction(v: Float): Float = if (v.isNaN()) 0.5f else v.coerceIn(0f, 1f)

/** IA desinstalada: desconecta. */
fun pruneAi(s: LauncherSettings, installedPackages: Set<String>): LauncherSettings =
    if (s.aiPackage == null || s.aiPackage in installedPackages) s
    else s.copy(aiPackage = null, aiActivity = null)
