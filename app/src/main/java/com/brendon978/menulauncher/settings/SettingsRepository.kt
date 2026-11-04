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
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.apps.AppKey
import com.brendon978.menulauncher.wallpaper.WallpaperInterval
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "launcher_settings")

class SettingsRepository(private val context: Context) {
    private object K {
        val mode = stringPreferencesKey("mode")
        val favorites = stringPreferencesKey("favorites")
        val strokeColor = intPreferencesKey("stroke_color")
        val strokeAuto = stringPreferencesKey("stroke_auto")
        val strokeDay = intPreferencesKey("stroke_day")
        val strokeNight = intPreferencesKey("stroke_night")
        val overrides = stringSetPreferencesKey("icon_overrides")
        val showMusic = booleanPreferencesKey("show_music")
        val firstRunDone = booleanPreferencesKey("first_run_done")
        val aiPackage = stringPreferencesKey("ai_package")
        val aiActivity = stringPreferencesKey("ai_activity")
        val aiX = floatPreferencesKey("ai_x")
        val aiY = floatPreferencesKey("ai_y")
        val showPet = booleanPreferencesKey("show_pet")
        val glitchAccessory = stringPreferencesKey("glitch_accessory")
        val labels = stringSetPreferencesKey("custom_labels")
        val hidden = stringSetPreferencesKey("hidden_apps")
        val oldHidden = stringPreferencesKey("look_hidden_apps")
        val folders = stringSetPreferencesKey("folders")
        val doubleTapLock = booleanPreferencesKey("double_tap_lock")
        val gestureDown = stringPreferencesKey("gesture_down")
        val gestureUp = stringPreferencesKey("gesture_up")
        val gestureDoubleTap = stringPreferencesKey("gesture_double_tap")
        val usage = stringSetPreferencesKey("usage")
        val showMostUsed = booleanPreferencesKey("show_most_used")
        val swipeAppActions = booleanPreferencesKey("swipe_app_actions")
        val showQuickActions = booleanPreferencesKey("show_quick_actions")
        val showNote = booleanPreferencesKey("show_note")
        val noteText = stringPreferencesKey("note_text")
        val showScreenTime = booleanPreferencesKey("show_screen_time")
        val showContacts = booleanPreferencesKey("show_contacts")
        val contactShortcuts = stringPreferencesKey("contact_shortcuts")
        val showNextEvent = booleanPreferencesKey("show_next_event")
        val timeProfiles = booleanPreferencesKey("time_profiles")
        val profileFavorites = stringSetPreferencesKey("profile_favorites")
        val searchContacts = booleanPreferencesKey("search_contacts")
        val glitchComments = booleanPreferencesKey("glitch_comments")
        val glitchMischief = booleanPreferencesKey("glitch_mischief")
        val birthday = stringPreferencesKey("birthday")
        val weatherCity = stringPreferencesKey("weather_city")
        val wallpaperFolder = stringPreferencesKey("wallpaper_folder")
        val wallpaperInterval = stringPreferencesKey("wallpaper_interval")
        val wallpaperLastChange = longPreferencesKey("wallpaper_last_change")
        val wallpaperLastImage = stringPreferencesKey("wallpaper_last_image")
        val glitchBuddy = stringPreferencesKey("glitch_buddy")
    }

    val settings: Flow<LauncherSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun update(f: (LauncherSettings) -> LauncherSettings) {
        context.dataStore.edit { prefs ->
            val next = f(prefs.toSettings())
            prefs[K.mode] = next.mode.name
            prefs[K.favorites] = next.favorites.joinToString("\n") { it.encode() }
            prefs[K.strokeColor] = next.strokeColor
            prefs[K.strokeAuto] = next.strokeAuto.name
            prefs[K.strokeDay] = next.strokeDay
            prefs[K.strokeNight] = next.strokeNight
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
            prefs[K.glitchAccessory] = next.glitchAccessory
            prefs[K.labels] = next.customLabels.map { (k, v) -> "${k.encode()}$SEP$v" }.toSet()
            prefs[K.hidden] = next.hiddenApps.map { it.encode() }.toSet()
            prefs[K.folders] = next.folders.map { (name, keys) -> name + SEP + keys.joinToString("\n") { it.encode() } }.toSet()
            prefs[K.doubleTapLock] = next.doubleTapLock
            prefs[K.gestureDown] = next.gestureDown.encode()
            prefs[K.gestureUp] = next.gestureUp.encode()
            if (next.gestureDoubleTap != null) prefs[K.gestureDoubleTap] = next.gestureDoubleTap.encode() else prefs.remove(K.gestureDoubleTap)
            prefs[K.usage] = next.usage.map { (k, v) -> "${k.encode()}$SEP$v" }.toSet()
            prefs[K.showMostUsed] = next.showMostUsed
            prefs[K.swipeAppActions] = next.swipeAppActions
            prefs[K.showQuickActions] = next.showQuickActions
            prefs[K.showNote] = next.showNote
            prefs[K.noteText] = next.noteText
            prefs[K.showScreenTime] = next.showScreenTime
            prefs[K.showContacts] = next.showContacts
            prefs[K.contactShortcuts] = ContactOps.encode(next.contactShortcuts)
            prefs[K.showNextEvent] = next.showNextEvent
            prefs[K.timeProfiles] = next.timeProfiles
            prefs[K.profileFavorites] = next.profileFavorites.map { (name, keys) -> name + SEP + keys.joinToString("\n") { it.encode() } }.toSet()
            prefs[K.searchContacts] = next.searchContacts
            prefs[K.glitchComments] = next.glitchComments
            prefs[K.glitchMischief] = next.glitchMischief
            prefs[K.birthday] = next.birthday
            prefs[K.weatherCity] = next.weatherCity
            prefs[K.wallpaperFolder] = next.wallpaperFolder
            prefs[K.wallpaperInterval] = next.wallpaperInterval.name
            prefs[K.wallpaperLastChange] = next.wallpaperLastChange
            prefs[K.wallpaperLastImage] = next.wallpaperLastImage
            prefs[K.glitchBuddy] = next.glitchBuddy
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
            strokeAuto = this[K.strokeAuto]?.let { runCatching { StrokeAuto.valueOf(it) }.getOrNull() } ?: d.strokeAuto,
            strokeDay = this[K.strokeDay] ?: d.strokeDay,
            strokeNight = this[K.strokeNight] ?: d.strokeNight,
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
            glitchAccessory = this[K.glitchAccessory] ?: d.glitchAccessory,
            customLabels = this[K.labels]?.mapNotNull { entry ->
                val i = entry.indexOf(SEP)
                if (i < 0) null else AppKey.decode(entry.substring(0, i))?.let { it to entry.substring(i + 1) }
            }?.toMap() ?: d.customLabels,
            // Sem a chave nova: quem escondeu apps numa versão anterior do fork tem a lista em look_hidden_apps.
            hiddenApps = this[K.hidden]?.mapNotNull { AppKey.decode(it) }?.toSet()
                ?: this[K.oldHidden]?.split(',')?.mapNotNull { AppKey.decode(it) }?.toSet()
                ?: d.hiddenApps,
            folders = this[K.folders]?.mapNotNull { entry ->
                val i = entry.indexOf(SEP)
                if (i <= 0) null else entry.substring(0, i) to entry.substring(i + 1).split("\n").mapNotNull { AppKey.decode(it) }
            }?.toMap()?.toSortedMap() ?: d.folders,
            doubleTapLock = this[K.doubleTapLock] ?: d.doubleTapLock,
            gestureDown = GestureChoice.decode(this[K.gestureDown]) ?: d.gestureDown,
            gestureUp = GestureChoice.decode(this[K.gestureUp]) ?: d.gestureUp,
            gestureDoubleTap = GestureChoice.decode(this[K.gestureDoubleTap]),
            usage = this[K.usage]?.mapNotNull { entry ->
                val i = entry.indexOf(SEP)
                if (i < 0) null else AppKey.decode(entry.substring(0, i))?.let { k -> entry.substring(i + 1).toFloatOrNull()?.let { k to it } }
            }?.toMap() ?: d.usage,
            showMostUsed = this[K.showMostUsed] ?: d.showMostUsed,
            swipeAppActions = this[K.swipeAppActions] ?: d.swipeAppActions,
            showQuickActions = this[K.showQuickActions] ?: d.showQuickActions,
            showNote = this[K.showNote] ?: d.showNote,
            noteText = this[K.noteText] ?: d.noteText,
            showScreenTime = this[K.showScreenTime] ?: d.showScreenTime,
            showContacts = this[K.showContacts] ?: d.showContacts,
            contactShortcuts = ContactOps.decode(this[K.contactShortcuts]),
            showNextEvent = this[K.showNextEvent] ?: d.showNextEvent,
            timeProfiles = this[K.timeProfiles] ?: d.timeProfiles,
            profileFavorites = this[K.profileFavorites]?.mapNotNull { entry ->
                val i = entry.indexOf(SEP)
                if (i <= 0) null else entry.substring(0, i) to entry.substring(i + 1).split("\n").mapNotNull { AppKey.decode(it) }
            }?.toMap() ?: d.profileFavorites,
            searchContacts = this[K.searchContacts] ?: d.searchContacts,
            glitchComments = this[K.glitchComments] ?: d.glitchComments,
            glitchMischief = this[K.glitchMischief] ?: d.glitchMischief,
            birthday = this[K.birthday] ?: d.birthday,
            weatherCity = this[K.weatherCity] ?: d.weatherCity,
            wallpaperFolder = this[K.wallpaperFolder] ?: d.wallpaperFolder,
            wallpaperInterval = this[K.wallpaperInterval]?.let { runCatching { WallpaperInterval.valueOf(it) }.getOrNull() } ?: d.wallpaperInterval,
            wallpaperLastChange = this[K.wallpaperLastChange] ?: d.wallpaperLastChange,
            wallpaperLastImage = this[K.wallpaperLastImage] ?: d.wallpaperLastImage,
            glitchBuddy = this[K.glitchBuddy] ?: d.glitchBuddy,
        )
        // Personalização: uma chave de texto por campo, no mesmo formato do arquivo de exportação.
        return LOOK_KEYS.entries.fold(base) { s, (f, key) -> this[key]?.let { f.read(s, it) } ?: s }
    }

    private companion object {
        const val SEP = '\t'
        val LOOK_KEYS = SettingsCodec.FIELDS.associateWith { stringPreferencesKey("look_${it.name}") }
    }
}
