package com.brendon978.menulauncher.pet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Só na versão de desenvolvimento: comandos por ADB para testar o Glitch na tela de verdade.
 *   adb shell am broadcast -a com.brendon978.menulauncher.DEBUG_GLITCH --es acao parar   (fica parado no meio e loga onde está)
 *   adb shell am broadcast -a com.brendon978.menulauncher.DEBUG_GLITCH --es acao soltar  (volta a viver)
 *   adb shell am broadcast -a com.brendon978.menulauncher.DEBUG_GLITCH --es acao pisar   (joga ele em cima do maior item)
 */
object PetDebug {
    const val ACTION = "com.brendon978.menulauncher.DEBUG_GLITCH"
    const val TAG = "GlitchDebug"
    private val _commands = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val commands: SharedFlow<String> = _commands

    fun register(context: Context): BroadcastReceiver {
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                val acao = i.getStringExtra("acao") ?: return
                Log.d(TAG, "comando: $acao")
                _commands.tryEmit(acao)
            }
        }
        context.registerReceiver(r, IntentFilter(ACTION), Context.RECEIVER_EXPORTED)
        return r
    }
}
