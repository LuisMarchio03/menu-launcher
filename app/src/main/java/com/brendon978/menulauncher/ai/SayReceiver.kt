package com.brendon978.menulauncher.ai

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Contrato "falar pelo Glitch" (docs/plugue-ia.md): uma IA manda um texto e o Glitch fala num balão.
 *   action  = com.brendon978.menulauncher.action.SAY
 *   extras  = android.intent.extra.TEXT (o que falar), com.brendon978.menulauncher.extra.FROM (quem fala, ex.: "IRIS")
 * Se a tela inicial não estiver aberta, a fala fica guardada e sai quando você voltar.
 */
class SayReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val text = clean(intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty())
        if (text.isEmpty()) return
        val from = intent.getStringExtra(AiContract.EXTRA_FROM)?.trim()?.take(24)?.ifEmpty { null }
        _pending.value = Speech(from, text, System.currentTimeMillis())
    }

    companion object {
        private val _pending = MutableStateFlow<Speech?>(null)
        /** Última fala recebida e ainda não dita. Quem fala zera com [consumed]. */
        val pending: StateFlow<Speech?> = _pending.asStateFlow()
        fun consumed(s: Speech) { _pending.compareAndSet(s, null) }
    }
}

data class Speech(val from: String?, val text: String, val at: Long)

/** Tira marcações de resposta ([OK], links) e corta textão: o balão é curto. */
internal fun clean(text: String): String {
    val t = text.replace(Regex("\\[(OK|ERRO|AVISO|DIR|ARQ)\\]\\s*"), "")
        .replace(Regex("https?://\\S+"), "")
        .replace(Regex("\\s+"), " ").trim()
    return if (t.length <= 140) t else t.take(137).trimEnd() + "..."
}
