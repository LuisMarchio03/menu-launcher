package com.brendon978.menulauncher.settings

import com.brendon978.menulauncher.apps.AppKey

/** O que um gesto no fundo da tela inicial pode fazer. */
enum class GestureAction(val label: String) {
    NOTIFICATIONS("Cortina"),
    SEARCH("Busca"),
    SETTINGS("Ajustes"),
    LOCK("Apagar a tela"),
    CAMERA("Câmera"),
    TORCH("Lanterna"),
    APP("Abrir um app"),
    NONE("Nada"),
}

/** A ação de um gesto; [app] só vale para [GestureAction.APP]. */
data class GestureChoice(val action: GestureAction, val app: AppKey? = null) {
    fun encode(): String = if (action == GestureAction.APP && app != null) "${action.name}|${app.encode()}" else action.name

    companion object {
        /** null para texto desconhecido ou "abrir um app" sem o app: quem lê cai no padrão. */
        fun decode(text: String?): GestureChoice? {
            if (text == null) return null
            val action = runCatching { GestureAction.valueOf(text.substringBefore('|')) }.getOrNull() ?: return null
            if (action != GestureAction.APP) return GestureChoice(action)
            return AppKey.decode(text.substringAfter('|', ""))?.let { GestureChoice(action, it) }
        }
    }
}

/** Dois toques: enquanto ninguém escolheu nos Gestos, vale o interruptor antigo (ajustes e backups de antes). */
fun LauncherSettings.doubleTapChoice(): GestureChoice =
    gestureDoubleTap ?: GestureChoice(if (doubleTapLock) GestureAction.LOCK else GestureAction.NONE)

/** Escolhe a ação dos dois toques e mantém o interruptor antigo coerente. */
fun LauncherSettings.withDoubleTap(choice: GestureChoice): LauncherSettings =
    copy(gestureDoubleTap = choice, doubleTapLock = choice.action == GestureAction.LOCK)

/** Com "Nada" o toque simples nem espera pelo segundo toque. */
fun LauncherSettings.listensDoubleTap(): Boolean = doubleTapChoice().action != GestureAction.NONE

/** A lanterna só é vigiada quando algum gesto usa. */
fun LauncherSettings.usesTorchGesture(): Boolean =
    listOf(gestureDown, gestureUp, doubleTapChoice()).any { it.action == GestureAction.TORCH }
