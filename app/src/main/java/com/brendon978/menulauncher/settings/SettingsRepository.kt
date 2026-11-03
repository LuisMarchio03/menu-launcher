package com.brendon978.menulauncher.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.apps.AppKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "launcher_settings")

class SettingsRepository(private val context: Context) {
    private object K {
        val mode = stringPreferencesKey("mode")
        val favorites = stringPreferencesKey("favorites")
        val strokeColor = intPreferencesKey("stroke_color")
        val overrides = stringSetPreferencesKey("icon_overrides")
        val showMusic = booleanPreferencesKey("show_music")
        val firstRunDone = booleanPreferencesKey("first_run_done")
        val aiPackage = stringPreferencesKey("ai_package")
        val aiActivity = stringPreferencesKey("ai_activity")
        val aiX = floatPreferencesKey("ai_x")
        val aiY = floatPreferencesKey("ai_y")
        val showPet = booleanPreferencesKey("show_pet")
    }

    val settings: Flow<LauncherSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun update(f: (LauncherSettings) -> LauncherSettings) {
        context.dataStore.edit { prefs ->
            val next = f(prefs.toSettings())
            prefs[K.mode] = next.mode.name
            prefs[K.favorites] = next.favorites.joinToString("\n") { it.encode() }
            prefs[K.strokeColor] = next.strokeColor
            prefs[K.overrides] = next.iconOverrides.map { (k, v) -> "${k.encode()}$SEP$v" }.toSet()
            prefs[K.showMusic] = next.showMusic
            prefs[K.firstRunDone] = next.firstRunDone
            if (next.aiPackage != null && next.aiActivity != null) {
                prefs[K.aiPackage] = next.aiPackage
                prefs[K.aiActivity] = next.aiActivity
            } else {
                prefs.remove(K.aiPackage)
                prefs.remove(K.aiActivity)
            }
            prefs[K.aiX] = next.aiX
            prefs[K.aiY] = next.aiY
            prefs[K.showPet] = next.showPet
            for ((f, key) in LOOK_KEYS) prefs[key] = f.write(next)
        }
    }

    /** Telefone, Mensagens e Câmera padrão; o que não resolver fica de fora. */
    fun defaultFavorites(installed: List<AppEntry>): List<AppKey> {
        val pm = context.packageManager
        val intents = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING),
            Intent(MediaStore.ACTION_IMAGE_CAPTURE),
        )
        return intents.mapNotNull { intent ->
            val pkg = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
            // perfil principal (menor serial) quando o app também existe na Pasta Segura
            installed.filter { it.key.packageName == pkg }.minByOrNull { it.key.userSerial }?.key
        }.distinct()
    }

    private fun Preferences.toSettings(): LauncherSettings {
        val d = LauncherSettings()
        val base = LauncherSettings(
            mode = this[K.mode]?.let { runCatching { HomeMode.valueOf(it) }.getOrNull() } ?: d.mode,
            favorites = this[K.favorites]?.split("\n")?.mapNotNull { AppKey.decode(it) } ?: d.favorites,
            strokeColor = this[K.strokeColor] ?: d.strokeColor,
            iconOverrides = this[K.overrides]?.mapNotNull { entry ->
                val i = entry.indexOf(SEP)
                if (i < 0) null else AppKey.decode(entry.substring(0, i))?.let { it to entry.substring(i + 1) }
            }?.toMap() ?: d.iconOverrides,
            showMusic = this[K.showMusic] ?: d.showMusic,
            firstRunDone = this[K.firstRunDone] ?: d.firstRunDone,
            aiPackage = this[K.aiPackage],
            aiActivity = this[K.aiActivity],
            aiX = this[K.aiX] ?: d.aiX,
            aiY = this[K.aiY] ?: d.aiY,
            showPet = this[K.showPet] ?: d.showPet,
        )
        // Personalização: uma chave de texto por campo, no mesmo formato do arquivo de exportação.
        return LOOK_KEYS.entries.fold(base) { s, (f, key) -> this[key]?.let { f.read(s, it) } ?: s }
    }

    private companion object {
        const val SEP = '\t'
        val LOOK_KEYS = SettingsCodec.FIELDS.filterNot { it.legacy }.associateWith { stringPreferencesKey("look_${it.name}") }
    }
}
