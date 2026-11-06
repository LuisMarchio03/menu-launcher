package com.brendon978.menulauncher.settings

import com.brendon978.menulauncher.apps.AppKey
import com.brendon978.menulauncher.wallpaper.WallpaperInterval
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupTest {
    private val a = AppKey("com.a", "A", 0)
    private val b = AppKey("com.b", "B", 10)

    @Test fun idaEVoltaGuardaTudo() {
        val s = LauncherSettings(
            mode = HomeMode.GRID, favorites = listOf(a, b), strokeColor = 0xFF7FB7A4.toInt(),
            iconOverrides = mapOf(a to "google_chrome"), showMusic = false, firstRunDone = true,
            aiPackage = "com.brendon.iris", aiActivity = "x.Main", aiX = .3f, aiY = .7f,
            showPet = false, glitchAccessory = "CAP", customLabels = mapOf(b to "Meu B"), hiddenApps = setOf(a),
            folders = mapOf("Jogos" to listOf(a, b)),
            showMostUsed = true, showQuickActions = true, showNextEvent = true,
        )
        assertEquals(s, Backup.decode(Backup.encode(s)))
    }

    @Test fun rodadaTresVaiNoBackup() {
        val s = LauncherSettings(
            wallpaperFolder = "content://x/tree/primary%3AFundos", wallpaperInterval = WallpaperInterval.SIX_HOURS,
            wallpaperLastChange = 1_800_000_000_000L, wallpaperLastImage = "praia.jpg",
            strokeAuto = StrokeAuto.BY_TIME, strokeDay = 0xFF7FB7A4.toInt(), strokeNight = 0xFFC9A27E.toInt(),
            gestureDown = GestureChoice(GestureAction.TORCH), gestureUp = GestureChoice(GestureAction.APP, b),
            gestureDoubleTap = GestureChoice(GestureAction.CAMERA),
            swipeAppActions = true, showNote = true, noteText = "pão = 2\nleite #1 ✓", showScreenTime = true,
            showContacts = true, contactShortcuts = listOf(ContactShortcut("content://com.android.contacts/contacts/lookup/ab/1", "Maria = Silva")),
        )
        assertEquals(s, Backup.decode(Backup.encode(s)))
    }

    @Test fun relogioVaiNoBackup() {
        val s = LauncherSettings(
            clockStyle = ClockStyle.SEGMENTS, clockSeconds = true, clockAlign = ClockAlign.CENTER,
            hourFormat = HourFormat.H12_AMPM, clockTap = ClockTap.CALENDAR,
        )
        assertEquals(s, Backup.decode(Backup.encode(s)))
    }

    @Test fun backupAntigoCaiNosPadroes() {
        assertEquals(LauncherSettings(), Backup.decode("menu-launcher-backup=1"))
    }

    @Test fun backupAntigoComDoisToquesContinuaApagando() {
        val s = Backup.decode("menu-launcher-backup=1\ndoubleTapLock=true")!!
        assertEquals(GestureAction.LOCK, s.doubleTapChoice().action)
    }

    @Test fun arquivoEstranhoNaoQuebra() {
        assertNull(Backup.decode("isso não é um backup"))
    }

    @Test fun nomesComCaracteresEspeciais() {
        val s = LauncherSettings(customLabels = mapOf(a to "Ação = é\\n#1 ✓"))
        assertEquals(s.customLabels, Backup.decode(Backup.encode(s))!!.customLabels)
    }

    @Test fun usoVaiNoBackup() {
        val s = LauncherSettings(usage = mapOf(a to 3.5f, b to .25f))
        assertEquals(s.usage, Backup.decode(Backup.encode(s))!!.usage)
    }

    @Test fun personalizacaoVaiNoBackup() {
        val s = LauncherSettings(
            clockScale = 1.3f, iconScale = 1.25f, textScale = 0.9f, rowScale = 1.1f, barScale = 1.25f,
            clockStyle = ClockStyle.WORDS, dateStyle = DateStyle.NUMERIC, secondZone = "Europe/Lisbon",
            colorSource = ColorSource.WALLPAPER, strokeScale = 2.5f, borderScale = 2.5f, textShadow = true, font = FontChoice.SERIF,
            gridColumns = 5, gridLabels = false,
        )
        assertEquals(s, Backup.decode(Backup.encode(s)))
    }

    @Test fun barraAEsquerdaNaoMexeNaPosicaoDaIa() {
        val s = LauncherSettings(barSide = BarSide.LEFT, aiX = 0.3f)
        assertEquals(s, Backup.decode(Backup.encode(s)))
    }

    @Test fun campoDePersonalizacaoInvalidoFicaNoPadrao() {
        val text = Backup.encode(LauncherSettings()).replace("look.grid_columns=0", "look.grid_columns=99")
        assertEquals(0, Backup.decode(text)!!.gridColumns)
    }
}
