package com.brendon978.menulauncher.gestures

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Serviço de acessibilidade mínimo, só para as ações globais que um app comum não pode fazer:
 * bloquear a tela e tirar print (notificações têm caminho sem ele).
 *
 * Sob demanda: com WRITE_SECURE_SETTINGS (dada uma vez por adb) o launcher liga o serviço só no
 * instante da ação e desliga logo depois. Apps de banco recusam acessibilidade ligada de app fora
 * da Play Store; assim ela nunca fica ligada enquanto o banco está aberto. Sem a permissão, o
 * usuário liga em Ajustes do Android > Acessibilidade e o serviço fica ligado o tempo todo.
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

        /** Uma ação por vez: duas ao mesmo tempo (gesto e ALOY) não podem desligar o serviço da outra. */
        private val busy = Mutex()

        private const val CONNECT_TIMEOUT_MS = 3_000L

        /** Tempo para o sistema terminar a ação antes de desligar o serviço. */
        private const val LINGER_MS = 600L

        /** Pode ligar e desligar o próprio serviço: `adb shell pm grant <pacote> android.permission.WRITE_SECURE_SETTINGS`. */
        fun onDemand(context: Context): Boolean =
            context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

        suspend fun lock(context: Context): Boolean = global(context, GLOBAL_ACTION_LOCK_SCREEN)

        /** Tira um print pelo sistema; false se não deu para ter a acessibilidade. */
        suspend fun screenshot(context: Context): Boolean = global(context, GLOBAL_ACTION_TAKE_SCREENSHOT)

        /** Pela acessibilidade se ligada; senão, a API escondida que os launchers usam (permissão EXPAND_STATUS_BAR). */
        @SuppressLint("WrongConstant")
        fun notifications(context: Context): Boolean =
            instance?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) == true || runCatching {
                val sb = context.getSystemService("statusbar")
                Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(sb)
            }.isSuccess

        /**
         * No modo sob demanda, tira o serviço da lista se ficou ligado (ligado à mão antes da permissão,
         * ou o processo morreu no meio de uma ação). Chamado quando o launcher volta para a frente.
         */
        fun offIfOnDemand(context: Context) {
            if (!onDemand(context) || !busy.tryLock()) return
            try {
                setEnabled(context, false)
            } finally {
                busy.unlock()
            }
        }

        private suspend fun global(context: Context, action: Int): Boolean {
            if (!onDemand(context)) return instance?.performGlobalAction(action) == true
            return busy.withLock {
                val app = context.applicationContext
                try {
                    if (instance == null) {
                        setEnabled(app, true)
                        withTimeoutOrNull(CONNECT_TIMEOUT_MS) { isOn.first { it } }
                    }
                    val done = instance?.performGlobalAction(action) == true
                    if (done) delay(LINGER_MS)
                    done
                } finally {
                    setEnabled(app, false)
                }
            }
        }

        // A permissão pode ser tirada a qualquer momento: falha vira "não deu", não queda do app.
        private fun setEnabled(context: Context, on: Boolean) = runCatching {
            val me = ComponentName(context, LauncherAccessibility::class.java).flattenToString()
            val resolver = context.contentResolver
            val list = Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            if (EnabledServices.has(list, me) == on) return@runCatching
            val next = if (on) EnabledServices.with(list, me) else EnabledServices.without(list, me)
            Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, next)
            // Como a tela de Acessibilidade faz: chave geral ligada só com algum serviço na lista.
            Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, if (next.isEmpty()) 0 else 1)
        }
    }
}
