package com.brendon978.menulauncher.settings

import java.util.Locale

const val MAX_CONTACTS = 4

/** Atalho de contato: o endereço que o seletor do sistema devolveu e o nome daquele momento. */
data class ContactShortcut(val uri: String, val name: String) {
    fun encode(): String = uri + SEP + name.replace(Regex("\\s+"), " ")

    companion object {
        private const val SEP = '\t'

        fun decode(text: String): ContactShortcut? {
            val i = text.indexOf(SEP)
            return if (i <= 0) null else ContactShortcut(text.substring(0, i), text.substring(i + 1))
        }
    }
}

object ContactOps {
    /** Contato repetido não entra de novo; a lista para em [MAX_CONTACTS]. */
    fun add(list: List<ContactShortcut>, contact: ContactShortcut): List<ContactShortcut> =
        if (list.size >= MAX_CONTACTS || list.any { it.uri == contact.uri }) list else list + contact

    fun encode(list: List<ContactShortcut>): String = list.joinToString("\n") { it.encode() }

    fun decode(text: String?): List<ContactShortcut> =
        text.orEmpty().split("\n").mapNotNull { ContactShortcut.decode(it) }.distinctBy { it.uri }.take(MAX_CONTACTS)
}

/** Primeira letra do primeiro e do último nome: "João da Silva" vira "JS"; sem letra nem número, "?". */
fun contactInitials(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { w -> w.isNotEmpty() && Character.isLetterOrDigit(w.codePointAt(0)) }
    if (words.isEmpty()) return "?"
    val picked = if (words.size == 1) words else listOf(words.first(), words.last())
    return picked.joinToString("") { w -> String(Character.toChars(w.codePointAt(0))) }.uppercase(Locale.forLanguageTag("pt-BR"))
}
