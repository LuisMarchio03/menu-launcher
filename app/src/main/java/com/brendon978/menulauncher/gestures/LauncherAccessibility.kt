package com.brendon978.menulauncher.gestures

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Serviço de acessibilidade mínimo, ligado pelo usuário em Ajustes do Android > Acessibilidade.
 * Só existe para as ações globais que um app comum não pode fazer: bloquear a tela e abrir as notificações.
 */
class LauncherAccessibility : AccessibilityService() {
    override fun onServiceConnected() {
        instance = this
        _isOn.value = true
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        disconnect()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        disconnect()
        super.onDestroy()
    }

    private fun disconnect() {
        instance = null
        _isOn.value = false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    companion object {
        @Volatile
        private var instance: LauncherAccessibility? = null

        private val _isOn = MutableStateFlow(false)
        val isOn: StateFlow<Boolean> = _isOn

        fun lock(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) == true

        /** Tira um print pelo sistema; false sem a acessibilidade ligada. */
        fun screenshot(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT) == true

        /** Pela acessibilidade; sem ela, a API escondida que os launchers usam (permissão EXPAND_STATUS_BAR). */
        @SuppressLint("WrongConstant")
        fun notifications(context: Context): Boolean =
            instance?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) == true || runCatching {
                val sb = context.getSystemService("statusbar")
                Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(sb)
            }.isSuccess
    }
}
