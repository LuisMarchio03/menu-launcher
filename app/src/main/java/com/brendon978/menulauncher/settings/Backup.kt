package com.brendon978.menulauncher.settings

import com.brendon978.menulauncher.apps.AppKey
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
        p["showMostUsed"] = s.showMostUsed.toString()
        p["usage"] = s.usage.entries.joinToString("\n") { "${it.key.encode()}$SEP${it.value}" }
        p["showQuickActions"] = s.showQuickActions.toString()
        p["showNextEvent"] = s.showNextEvent.toString()
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
            showMostUsed = p.getProperty("showMostUsed")?.toBooleanStrictOrNull() ?: d.showMostUsed,
            usage = pairs("usage").mapNotNull { (k, v) -> v.toFloatOrNull()?.let { k to it } }.toMap(),
            showQuickActions = p.getProperty("showQuickActions")?.toBooleanStrictOrNull() ?: d.showQuickActions,
            showNextEvent = p.getProperty("showNextEvent")?.toBooleanStrictOrNull() ?: d.showNextEvent,
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
