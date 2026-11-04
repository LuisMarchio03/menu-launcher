package com.brendon978.menulauncher.music

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Libera a leitura das sessões de mídia e conta as notificações "de verdade" para o robozinho
 * (sem as persistentes, como música e downloads, e sem as do próprio launcher).
 */
class MediaListener : NotificationListenerService() {
    override fun onListenerConnected() = recount(announce = false) // o que já estava lá não é novidade
    override fun onListenerDisconnected() { _count.value = null; _badges.value = emptySet() }
    override fun onNotificationPosted(sbn: StatusBarNotification?) = recount()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = recount()

    private fun recount(announce: Boolean = true) {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        val infos = active.map { NotifInfo(it.packageName, it.isClearable, it.isOngoing) }
        _count.value = realCount(infos, packageName)
        val now = badgePackages(infos, packageName)
        if (announce) newcomers(_badges.value, now).forEach { _arrived.tryEmit(it) }
        _badges.value = now
    }

    companion object {
        private val _count = MutableStateFlow<Int?>(null)
        /** null = sem acesso às notificações (ou ainda não conectou). */
        val count: StateFlow<Int?> = _count.asStateFlow()

        private val _badges = MutableStateFlow<Set<String>>(emptySet())
        /** Pacotes com notificação "de verdade" agora (bolinha no ícone). */
        val badges: StateFlow<Set<String>> = _badges.asStateFlow()

        private val _arrived = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 8)
        /** Pacote que acabou de ganhar notificação (o Glitch traz a cartinha). */
        val arrived: kotlinx.coroutines.flow.SharedFlow<String> = _arrived
    }
}
