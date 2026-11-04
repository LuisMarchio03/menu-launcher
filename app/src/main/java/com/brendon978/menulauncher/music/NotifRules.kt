package com.brendon978.menulauncher.music

/** O mínimo de uma notificação que importa para as bolinhas e para o Glitch. */
data class NotifInfo(val pkg: String, val clearable: Boolean, val ongoing: Boolean)

private fun NotifInfo.real(own: String) = clearable && !ongoing && pkg != own

/** Apps com notificação "de verdade" (sem música, downloads e outras fixas, e sem as do launcher). */
fun badgePackages(list: List<NotifInfo>, own: String): Set<String> = list.filter { it.real(own) }.map { it.pkg }.toSet()

fun realCount(list: List<NotifInfo>, own: String): Int = list.count { it.real(own) }

/** Apps que ganharam notificação agora (não tinham antes). */
fun newcomers(prev: Set<String>, now: Set<String>): Set<String> = now - prev
