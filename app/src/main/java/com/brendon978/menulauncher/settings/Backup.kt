package com.brendon978.menulauncher.settings

import com.brendon978.menulauncher.apps.AppKey
import com.brendon978.menulauncher.wallpaper.WallpaperInterval
import java.io.StringReader
import java.io.StringWriter
import java.util.Properties

/**
 * Backup dos ajustes num arquivo de texto (formato .properties), para trocar de celular ou mandar pro amigo.
 * A memória do Glitch vai junto em chaves "glitch.*" (ver [encode] e GlitchBackup).
 */
object Backup {
    private const val HEADER = "menu-launcher-backup"
    private const val SEP = "\t"
    private const val LOOK_PREFIX = "look."

    fun encode(s: LauncherSettings, extra: Map<String, String> = emptyMap()): String {
        val p = Properties()
        p[HEADER] = "1"
        p["mode"] = s.mode.name
        p["favorites"] = s.favorites.joinToString("\n") { it.encode() }
        p["strokeColor"] = s.strokeColor.toString()
        p["strokeAuto"] = s.strokeAuto.name
        p["strokeDay"] = s.strokeDay.toString()
        p["strokeNight"] = s.strokeNight.toString()
        p["iconOverrides"] = s.iconOverrides.entries.joinToString("\n") { "${it.key.encode()}$SEP${it.value}" }
        p["showMusic"] = s.showMusic.toString()
        p["firstRunDone"] = s.firstRunDone.toString()
        s.aiPackage?.let { p["aiPackage"] = it }
        s.aiActivity?.let { p["aiActivity"] = it }
        p["aiX"] = s.aiX.toString()
        p["aiY"] = s.aiY.toString()
        p["showPet"] = s.showPet.toString()
        p["glitchAccessory"] = s.glitchAccessory
        p["customLabels"] = s.customLabels.entries.joinToString("\n") { "${it.key.encode()}$SEP${it.value}" }
        p["hiddenApps"] = s.hiddenApps.joinToString("\n") { it.encode() }
        p["folders"] = s.folders.entries.joinToString("\n") { (name, keys) -> name + SEP + keys.joinToString(",") { it.encode() } }
        p["doubleTapLock"] = s.doubleTapLock.toString()
        p["gestureDown"] = s.gestureDown.encode()
        p["gestureUp"] = s.gestureUp.encode()
        s.gestureDoubleTap?.let { p["gestureDoubleTap"] = it.encode() }
        p["showMostUsed"] = s.showMostUsed.toString()
        p["usage"] = s.usage.entries.joinToString("\n") { "${it.key.encode()}$SEP${it.value}" }
        p["swipeAppActions"] = s.swipeAppActions.toString()
        p["showQuickActions"] = s.showQuickActions.toString()
        p["showNote"] = s.showNote.toString()
        p["noteText"] = s.noteText
        p["showScreenTime"] = s.showScreenTime.toString()
        p["showContacts"] = s.showContacts.toString()
        p["contactShortcuts"] = ContactOps.encode(s.contactShortcuts)
        p["showNextEvent"] = s.showNextEvent.toString()
        p["timeProfiles"] = s.timeProfiles.toString()
        p["profileFavorites"] = s.profileFavorites.entries.joinToString("\n") { (name, keys) -> name + SEP + keys.joinToString(",") { it.encode() } }
        p["searchContacts"] = s.searchContacts.toString()
        p["glitchComments"] = s.glitchComments.toString()
        p["glitchMischief"] = s.glitchMischief.toString()
        p["birthday"] = s.birthday
        p["weatherCity"] = s.weatherCity
        p["wallpaperFolder"] = s.wallpaperFolder
        p["wallpaperInterval"] = s.wallpaperInterval.name
        p["wallpaperLastChange"] = s.wallpaperLastChange.toString()
        p["wallpaperLastImage"] = s.wallpaperLastImage
        p["glitchBuddy"] = s.glitchBuddy
        SettingsCodec.FIELDS.forEach { p[LOOK_PREFIX + it.name] = it.write(s) }
        extra.forEach { (k, v) -> p[k] = v }
        return StringWriter().also { p.store(it, "menu-launcher") }.toString()
    }

    /** null se não for um backup do menu-launcher. */
    fun decode(text: String): LauncherSettings? = load(text)?.let { p ->
        val d = LauncherSettings()
        fun lines(k: String) = p.getProperty(k).orEmpty().split("\n").filter { it.isNotBlank() }
        fun pairs(k: String) = lines(k).mapNotNull { l ->
            val i = l.indexOf(SEP)
            if (i < 0) null else AppKey.decode(l.substring(0, i))?.let { it to l.substring(i + 1) }
        }.toMap()
        LauncherSettings(
            mode = runCatching { HomeMode.valueOf(p.getProperty("mode")) }.getOrDefault(d.mode),
            favorites = lines("favorites").mapNotNull { AppKey.decode(it) },
            strokeColor = p.getProperty("strokeColor")?.toIntOrNull() ?: d.strokeColor,
            strokeAuto = runCatching { StrokeAuto.valueOf(p.getProperty("strokeAuto")) }.getOrDefault(d.strokeAuto),
            strokeDay = p.getProperty("strokeDay")?.toIntOrNull() ?: d.strokeDay,
            strokeNight = p.getProperty("strokeNight")?.toIntOrNull() ?: d.strokeNight,
            iconOverrides = pairs("iconOverrides"),
            showMusic = p.getProperty("showMusic")?.toBooleanStrictOrNull() ?: d.showMusic,
            firstRunDone = p.getProperty("firstRunDone")?.toBooleanStrictOrNull() ?: d.firstRunDone,
            aiPackage = p.getProperty("aiPackage"),
            aiActivity = p.getProperty("aiActivity"),
            aiX = p.getProperty("aiX")?.toFloatOrNull() ?: d.aiX,
            aiY = p.getProperty("aiY")?.toFloatOrNull() ?: d.aiY,
            showPet = p.getProperty("showPet")?.toBooleanStrictOrNull() ?: d.showPet,
            glitchAccessory = p.getProperty("glitchAccessory") ?: d.glitchAccessory,
            customLabels = pairs("customLabels"),
            hiddenApps = lines("hiddenApps").mapNotNull { AppKey.decode(it) }.toSet(),
            folders = lines("folders").mapNotNull { l ->
                val i = l.indexOf(SEP)
                if (i <= 0) null else l.substring(0, i) to l.substring(i + 1).split(",").mapNotNull { AppKey.decode(it) }
            }.toMap(),
            doubleTapLock = p.getProperty("doubleTapLock")?.toBooleanStrictOrNull() ?: d.doubleTapLock,
            gestureDown = GestureChoice.decode(p.getProperty("gestureDown")) ?: d.gestureDown,
            gestureUp = GestureChoice.decode(p.getProperty("gestureUp")) ?: d.gestureUp,
            gestureDoubleTap = GestureChoice.decode(p.getProperty("gestureDoubleTap")),
            showMostUsed = p.getProperty("showMostUsed")?.toBooleanStrictOrNull() ?: d.showMostUsed,
            usage = pairs("usage").mapNotNull { (k, v) -> v.toFloatOrNull()?.let { k to it } }.toMap(),
            swipeAppActions = p.getProperty("swipeAppActions")?.toBooleanStrictOrNull() ?: d.swipeAppActions,
            showQuickActions = p.getProperty("showQuickActions")?.toBooleanStrictOrNull() ?: d.showQuickActions,
            showNote = p.getProperty("showNote")?.toBooleanStrictOrNull() ?: d.showNote,
            noteText = p.getProperty("noteText") ?: d.noteText,
            showScreenTime = p.getProperty("showScreenTime")?.toBooleanStrictOrNull() ?: d.showScreenTime,
            showContacts = p.getProperty("showContacts")?.toBooleanStrictOrNull() ?: d.showContacts,
            contactShortcuts = ContactOps.decode(p.getProperty("contactShortcuts")),
            showNextEvent = p.getProperty("showNextEvent")?.toBooleanStrictOrNull() ?: d.showNextEvent,
            timeProfiles = p.getProperty("timeProfiles")?.toBooleanStrictOrNull() ?: d.timeProfiles,
            profileFavorites = lines("profileFavorites").mapNotNull { l ->
                val i = l.indexOf(SEP)
                if (i <= 0) null else l.substring(0, i) to l.substring(i + 1).split(",").mapNotNull { AppKey.decode(it) }
            }.toMap(),
            searchContacts = p.getProperty("searchContacts")?.toBooleanStrictOrNull() ?: d.searchContacts,
            glitchComments = p.getProperty("glitchComments")?.toBooleanStrictOrNull() ?: d.glitchComments,
            glitchMischief = p.getProperty("glitchMischief")?.toBooleanStrictOrNull() ?: d.glitchMischief,
            birthday = p.getProperty("birthday") ?: d.birthday,
            weatherCity = p.getProperty("weatherCity") ?: d.weatherCity,
            wallpaperFolder = p.getProperty("wallpaperFolder") ?: d.wallpaperFolder,
            wallpaperInterval = runCatching { WallpaperInterval.valueOf(p.getProperty("wallpaperInterval")) }.getOrDefault(d.wallpaperInterval),
            wallpaperLastChange = p.getProperty("wallpaperLastChange")?.toLongOrNull() ?: d.wallpaperLastChange,
            wallpaperLastImage = p.getProperty("wallpaperLastImage") ?: d.wallpaperLastImage,
            glitchBuddy = p.getProperty("glitchBuddy") ?: d.glitchBuddy,
        ).let { base ->
            SettingsCodec.FIELDS.fold(base) { s, f -> p.getProperty(LOOK_PREFIX + f.name)?.let { f.read(s, it) } ?: s }
        }
    }

    /** As chaves extras (memória do Glitch), para quem souber ler. */
    fun extras(text: String, prefix: String): Map<String, String> =
        load(text)?.let { p -> p.stringPropertyNames().filter { it.startsWith(prefix) }.associateWith { p.getProperty(it) } }.orEmpty()

    private fun load(text: String): Properties? {
        val p = Properties()
        runCatching { p.load(StringReader(text)) }.getOrElse { return null }
        return if (p.getProperty(HEADER) == "1") p else null
    }
}
