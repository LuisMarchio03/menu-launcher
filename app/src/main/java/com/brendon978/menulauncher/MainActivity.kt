package com.brendon978.menulauncher

import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.os.UserManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.brendon978.menulauncher.ai.AiChooser
import com.brendon978.menulauncher.ai.AiRepository
import com.brendon978.menulauncher.ai.AiSymbol
import com.brendon978.menulauncher.ai.HoldAction
import com.brendon978.menulauncher.ai.VoiceCapture
import com.brendon978.menulauncher.ai.clampFraction
import com.brendon978.menulauncher.ai.holdAction
import com.brendon978.menulauncher.ai.pruneAi
import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.apps.AppKey
import com.brendon978.menulauncher.apps.AppRepository
import com.brendon978.menulauncher.gestures.LauncherAccessibility
import com.brendon978.menulauncher.home.HomeScreen
import com.brendon978.menulauncher.home.ProvideLauncherText
import com.brendon978.menulauncher.home.smallTextSize
import com.brendon978.menulauncher.home.SearchScreen
import com.brendon978.menulauncher.icons.AppFilter
import com.brendon978.menulauncher.icons.IconRenderer
import com.brendon978.menulauncher.menu.AppActionsSheet
import com.brendon978.menulauncher.menu.IconPickerScreen
import com.brendon978.menulauncher.music.MusicCard
import com.brendon978.menulauncher.music.MusicControls
import com.brendon978.menulauncher.music.MusicRepository
import com.brendon978.menulauncher.onboarding.FirstRunScreen
import com.brendon978.menulauncher.settings.ColorSource
import com.brendon978.menulauncher.settings.FavoritesOps
import com.brendon978.menulauncher.settings.GestureAction
import com.brendon978.menulauncher.settings.HomeMode
import com.brendon978.menulauncher.settings.LauncherSettings
import com.brendon978.menulauncher.settings.SettingsRepository
import com.brendon978.menulauncher.settingsui.SettingsScreen
import com.brendon978.menulauncher.wallpaper.WallpaperColorSource
import kotlinx.coroutines.launch

/** Tema escuro neutro para as telas internas (ajustes, seletor): branco sobre quase preto. */
private val LAUNCHER_COLORS = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    surface = Color(0xFF0C0C0E),
    onSurface = Color.White,
    onSurfaceVariant = Color.White.copy(alpha = 0.6f),
    outline = Color.White.copy(alpha = 0.3f),
)

private sealed interface Screen {
    data object Home : Screen
    data object Settings : Screen
    data object Search : Screen
    data class IconPicker(val entry: AppEntry) : Screen
}

class MainActivity : ComponentActivity() {
    private lateinit var appRepo: AppRepository
    private lateinit var settingsRepo: SettingsRepository
    private lateinit var renderer: IconRenderer
    private lateinit var musicRepo: MusicRepository
    private lateinit var aiRepo: AiRepository
    private lateinit var voice: VoiceCapture
    private var wallpaperColors: WallpaperColorSource? = null
    private val listening = mutableStateOf(false)
    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        toast(if (granted) "Microfone liberado. Segure o símbolo e fale." else "Sem o microfone, segurar só abre a IA.")
    }
    private val resetSignal = mutableIntStateOf(0)
    private val screen = mutableStateOf<Screen>(Screen.Home)
    private val sheetFor = mutableStateOf<AppEntry?>(null)
    private val isDefaultHome = mutableStateOf(false)
    private val hasMusicAccess = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appRepo = AppRepository(this)
        settingsRepo = SettingsRepository(this)
        musicRepo = MusicRepository(this)
        aiRepo = AiRepository(this)
        voice = VoiceCapture(this)
        if (BuildConfig.DEBUG) petDebug = com.brendon978.menulauncher.pet.PetDebug.register(this)
        renderer = IconRenderer(
            this,
            filterLoader = { assets.open("lawnicons/appfilter.xml").use { AppFilter.parse(it) } },
        ) { key -> appRepo.activityInfo(key)?.getIcon(0) }

        // Launcher: voltar só fecha telas internas; nunca sai da tela inicial.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                screen.value = Screen.Home
            }
        })

        setContent { MaterialTheme(colorScheme = LAUNCHER_COLORS) { Content() } }
    }

    @androidx.compose.runtime.Composable
    private fun Content() {
        run {
            val apps by appRepo.apps.collectAsState()
            val loaded by settingsRepo.settings.collectAsState(initial = null)
            val settings = loaded ?: LauncherSettings(firstRunDone = true)
            val music by musicRepo.state.collectAsState()
            val byKey = remember(apps) { apps.associateBy { it.key } }
            // O que a tela inicial desenha: com a cor do papel de parede, se escolhida. Os ajustes gravam o original.
            // A fonte da cor só existe (e só escuta o sistema) enquanto essa opção está ligada.
            val wallpaperColor = if (settings.colorSource == ColorSource.WALLPAPER) {
                remember { wallpaperColorSource() }.color.collectAsState().value
            } else {
                null
            }
            val shown = remember(settings, wallpaperColor) {
                if (wallpaperColor != null) settings.copy(strokeColor = wallpaperColor) else settings
            }
            val visibleApps = remember(apps, settings.hiddenApps) { apps.filter { it.key !in settings.hiddenApps } }
            val color = Color(shown.strokeColor)
            val current = music.takeIf { settings.showMusic }

            // App desinstalado sai dos favoritos e das trocas de ícone.
            LaunchedEffect(apps) {
                if (apps.isNotEmpty()) {
                    val installed = apps.map { it.key }.toSet()
                    settingsRepo.update { FavoritesOps.prune(it, installed) }
                    if (settings.aiPackage?.let { !aiRepo.isInstalled(it) } == true) {
                        settingsRepo.update { pruneAi(it, emptySet()) }
                    }
                }
            }

            if (loaded == null) return@run // evita piscar a primeira abertura antes de ler os ajustes

            if (!settings.firstRunDone) {
                FirstRunScreen(
                    isDefaultHome = isDefaultHome.value,
                    hasMusicAccess = hasMusicAccess.value,
                    onRequestHome = ::requestHomeRole,
                    onRequestMusic = {
                        startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    hasAi = settings.aiPackage != null,
                    aiChooser = {
                        val candidates = remember { aiRepo.discover() }
                        AiChooser(candidates, apps, settings.aiPackage) { pkg, act ->
                            updateSettings { it.copy(aiPackage = pkg, aiActivity = act) }
                        }
                    },
                    onDone = {
                        val defaults = settingsRepo.defaultFavorites(apps)
                        updateSettings { st ->
                            st.copy(firstRunDone = true, favorites = st.favorites.ifEmpty { defaults })
                        }
                    },
                )
                return@run
            }

            // Fonte e sombra escolhidas valem em todas as telas do launcher.
            ProvideLauncherText(shown) {
            // A tela inicial fica sempre por baixo: o Glitch continua vivo (e o painel dele, ao vivo)
            // enquanto a busca ou os Ajustes estão abertos por cima.
            Box(Modifier.fillMaxSize()) {
            HomeScreen(
                    apps = visibleApps,
                    settings = shown,
                    renderer = renderer,
                    resetSignal = resetSignal.intValue,
                    onLaunch = { appRepo.launch(it.key) },
                    onAppLongPress = { sheetFor.value = it },
                    onEmptyLongPress = { screen.value = Screen.Settings },
                    onSearch = { screen.value = Screen.Search },
                    musicPlaying = music?.playing == true,
                    // Na grade o símbolo fica no meio dos ícones e parece parte do fundo: só no modo uma mão.
                    overlay = { if (settings.mode == HomeMode.ONE_HAND) AiOverlay(shown, apps) },
                    onGesture = ::performGesture,
                    // Pedido do Brendon: controles logo abaixo do cartão, no topo (modo uma mão).
                    musicSlotTop = {
                        current?.let {
                            Column {
                                MusicCard(it, color, textSize = settings.smallTextSize)
                                MusicControls(it.playing, color, musicRepo::previous, musicRepo::playPause, musicRepo::next)
                            }
                        }
                    },
                )
            if (screen.value != Screen.Home) Box(
                Modifier
                    .fillMaxSize()
                    // toque na tela de cima não vaza para a tela inicial
                    .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
            ) {
            when (val s = screen.value) {
                Screen.Home -> Unit
                Screen.Search -> SearchScreen(
                    apps = visibleApps,
                    settings = shown,
                    renderer = renderer,
                    onLaunch = {
                        appRepo.launch(it.key)
                        screen.value = Screen.Home
                    },
                    onAppLongPress = { sheetFor.value = it },
                )
                Screen.Settings -> SettingsScreen(
                    settings = settings,
                    apps = byKey,
                    onChange = ::updateSettings,
                    onSetDefaultLauncher = ::requestHomeRole,
                    aiSection = {
                        val candidates = remember { aiRepo.discover() }
                        AiChooser(candidates, apps, settings.aiPackage) { pkg, act ->
                            updateSettings { it.copy(aiPackage = pkg, aiActivity = act) }
                        }
                        settings.aiPackage?.let { pkg ->
                            androidx.compose.material3.Text(
                                "Desconectar ${aiRepo.label(pkg)}",
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = androidx.compose.ui.Modifier
                                    .clickable { updateSettings { it.copy(aiPackage = null, aiActivity = null) } }
                                    .padding(vertical = 12.dp),
                            )
                        }
                    },
                    onClose = { screen.value = Screen.Home },
                )
                is Screen.IconPicker -> IconPickerScreen(
                    appLabel = s.entry.label,
                    renderer = renderer,
                    color = 0xFFFFFFFF.toInt(), // prévia sempre legível no fundo escuro
                    assets = assets,
                    onPick = { name ->
                        updateSettings { st ->
                            val o = if (name == null) st.iconOverrides - s.entry.key else st.iconOverrides + (s.entry.key to name)
                            st.copy(iconOverrides = o)
                        }
                        screen.value = Screen.Home
                    },
                )
            }
            }
            }
            }

            sheetFor.value?.let { entry ->
                AppActionsSheet(
                    label = entry.label,
                    isFavorite = entry.key in settings.favorites,
                    canUninstall = canUninstall(entry.key),
                    onToggleFavorite = { updateSettings { it.copy(favorites = FavoritesOps.toggle(it.favorites, entry.key)) } },
                    onChangeIcon = { screen.value = Screen.IconPicker(entry) },
                    onAppInfo = { openAppInfo(entry.key) },
                    onUninstall = { uninstall(entry.key) },
                    onHide = { updateSettings { FavoritesOps.setHidden(it, entry.key, hidden = true) } },
                    onDismiss = { sheetFor.value = null },
                )
            }
        }
    }

    private fun performGesture(action: GestureAction) {
        when (action) {
            GestureAction.NONE -> Unit
            GestureAction.SEARCH -> screen.value = Screen.Search
            GestureAction.SETTINGS -> screen.value = Screen.Settings
            GestureAction.NOTIFICATIONS -> if (!LauncherAccessibility.notifications(this)) {
                toast("Ligue a acessibilidade do menu-launcher para abrir as notificações")
            }
            GestureAction.LOCK -> if (!LauncherAccessibility.lock()) {
                toast("Para bloquear com o gesto, ligue a acessibilidade do menu-launcher")
                startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
    }

    private fun wallpaperColorSource(): WallpaperColorSource =
        wallpaperColors ?: WallpaperColorSource(this).also { wallpaperColors = it }

    private fun updateSettings(f: (LauncherSettings) -> LauncherSettings) {
        lifecycleScope.launch { settingsRepo.update(f) }
    }

    private fun isMainProfile(key: AppKey) =
        getSystemService(UserManager::class.java).getSerialNumberForUser(Process.myUserHandle()) == key.userSerial

    private fun canUninstall(key: AppKey): Boolean {
        val info = appRepo.activityInfo(key)?.applicationInfo ?: return false
        return isMainProfile(key) && info.flags and ApplicationInfo.FLAG_SYSTEM == 0
    }

    private fun openAppInfo(key: AppKey) {
        val user = appRepo.userOf(key) ?: return
        getSystemService(LauncherApps::class.java)
            .startAppDetailsActivity(android.content.ComponentName(key.packageName, key.activityName), user, null, null)
    }

    private fun uninstall(key: AppKey) {
        startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${key.packageName}")))
    }

    fun requestHomeRole() {
        val rm = getSystemService(RoleManager::class.java)
        if (rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
            @Suppress("DEPRECATION")
            startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME), REQUEST_HOME)
        } else {
            startActivity(Intent(android.provider.Settings.ACTION_HOME_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        musicRepo.refresh()
        isDefaultHome.value = getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)
        hasMusicAccess.value = musicRepo.hasAccess()
    }

    @androidx.compose.runtime.Composable
    private fun AiOverlay(settings: LauncherSettings, apps: List<AppEntry>) {
        val pkg = settings.aiPackage ?: return
        val activity = settings.aiActivity ?: return
        val px = with(androidx.compose.ui.platform.LocalDensity.current) { 34.dp.roundToPx() }
        val symbol by androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, pkg, activity, settings.strokeColor, settings.strokeScale, apps) {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                aiRepo.symbol(pkg, activity, px, settings.strokeColor)?.asImageBitmap()
                    ?: apps.firstOrNull { it.key.packageName == pkg }?.let { renderer.iconFor(it, null, px, settings.strokeColor, settings.strokeScale) }
            }
        }
        AiSymbol(
            symbol = symbol,
            color = Color(settings.strokeColor),
            xFraction = settings.aiX,
            yFraction = settings.aiY,
            listening = listening.value,
            onTap = { aiRepo.open(pkg, activity) },
            onHoldStart = { startListening(pkg, activity) },
            onHoldEnd = { if (listening.value) voice.stop() },
            onMoved = { x, y -> updateSettings { it.copy(aiX = clampFraction(x), aiY = clampFraction(y)) } },
        )
    }

    private fun startListening(pkg: String, activity: String) {
        if (holdAction(aiRepo.acceptsCommands(pkg)) == HoldAction.OPEN) {
            aiRepo.open(pkg, activity)
            return
        }
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
            return
        }
        listening.value = true
        window.decorView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        voice.start { text ->
            listening.value = false
            if (text == null) {
                toast("Não entendi")
            } else if (aiRepo.sendCommand(pkg, text)) {
                toast("${aiRepo.label(pkg)}: $text")
            } else {
                toast("${aiRepo.label(pkg)} não recebe comandos")
            }
        }
    }

    private fun toast(msg: String) = android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()

    private var petDebug: android.content.BroadcastReceiver? = null

    override fun onDestroy() {
        petDebug?.let { unregisterReceiver(it) }
        appRepo.close()
        musicRepo.close()
        voice.cancel()
        wallpaperColors?.close()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Botão home com o launcher já aberto: fecha menus e volta para os favoritos.
        sheetFor.value = null
        screen.value = Screen.Home
        resetSignal.intValue++
    }

    private companion object {
        const val REQUEST_HOME = 1
    }
}
