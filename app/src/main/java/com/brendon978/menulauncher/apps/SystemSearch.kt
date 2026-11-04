package com.brendon978.menulauncher.apps

import android.provider.Settings
import java.text.Normalizer

/** Atalho para uma tela dos ajustes do Android. */
data class SystemTarget(val label: String, val action: String, val words: List<String>)

val SYSTEM_TARGETS = listOf(
    SystemTarget("Wi-Fi", Settings.ACTION_WIFI_SETTINGS, listOf("wifi", "internet", "rede")),
    SystemTarget("Bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS, listOf("bluetooth", "fone")),
    SystemTarget("Bateria", Settings.ACTION_BATTERY_SAVER_SETTINGS, listOf("bateria", "economia", "energia")),
    SystemTarget("Tela", Settings.ACTION_DISPLAY_SETTINGS, listOf("tela", "brilho", "display")),
    SystemTarget("Som", Settings.ACTION_SOUND_SETTINGS, listOf("som", "volume", "toque")),
    SystemTarget("Aplicativos", Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS, listOf("aplicativos", "apps")),
    SystemTarget("Notificações", Settings.ACTION_ALL_APPS_NOTIFICATION_SETTINGS, listOf("notificacoes", "avisos")),
    SystemTarget("Armazenamento", Settings.ACTION_INTERNAL_STORAGE_SETTINGS, listOf("armazenamento", "memoria", "espaco")),
    SystemTarget("Segurança", Settings.ACTION_SECURITY_SETTINGS, listOf("seguranca", "senha", "digital", "biometria")),
    SystemTarget("Localização", Settings.ACTION_LOCATION_SOURCE_SETTINGS, listOf("localizacao", "gps")),
    SystemTarget("Data e hora", Settings.ACTION_DATE_SETTINGS, listOf("data", "hora", "fuso")),
    SystemTarget("Modo avião", Settings.ACTION_AIRPLANE_MODE_SETTINGS, listOf("aviao")),
    SystemTarget("Ajustes do Android", Settings.ACTION_SETTINGS, listOf("ajustes", "configuracoes", "android")),
)

private fun plain(s: String) = Normalizer.normalize(s.trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

/** Precisa de ao menos 2 letras, e casa com o começo de uma palavra-chave. */
fun searchSystem(query: String, targets: List<SystemTarget> = SYSTEM_TARGETS): List<SystemTarget> {
    val q = plain(query)
    if (q.length < 2) return emptyList()
    return targets.filter { t -> t.words.any { it.startsWith(q) } || plain(t.label).startsWith(q) }.take(3)
}
