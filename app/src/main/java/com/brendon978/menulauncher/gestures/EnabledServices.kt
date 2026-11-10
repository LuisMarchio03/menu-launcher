package com.brendon978.menulauncher.gestures

/** A lista de Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES: "pacote/classe" separados por ':'. */
object EnabledServices {
    fun has(list: String?, me: String): Boolean = entries(list).any { same(it, me) }

    fun with(list: String?, me: String): String =
        if (has(list, me)) entries(list).joinToString(":") else (entries(list) + me).joinToString(":")

    fun without(list: String?, me: String): String = entries(list).filterNot { same(it, me) }.joinToString(":")

    private fun entries(list: String?) = list.orEmpty().split(':').filter { it.isNotBlank() }

    // O sistema às vezes grava a classe curta: "pacote/.Classe".
    private fun full(name: String): String {
        val pkg = name.substringBefore('/')
        val cls = name.substringAfter('/')
        return if (cls.startsWith(".")) "$pkg/$pkg$cls" else name
    }

    private fun same(a: String, b: String) = full(a) == full(b)
}
