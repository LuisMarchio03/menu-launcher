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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import com.brendon978.menulauncher.aloy.AloyScreen
import com.brendon978.menulauncher.aloy.EXTRA_TELA
import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.apps.AppKey
import com.brendon978.menulauncher.apps.AppRepository
import com.brendon978.menulauncher.gestures.LauncherAccessibility
import com.brendon978.menulauncher.home.HomeScreen
import com.brendon978.menulauncher.home.ProvideLauncherText
import com.brendon978.menulauncher.home.smallTextSize
import com.brendon978.menulauncher.home.SCREEN_TIME_MIN_GAP
import com.brendon978.menulauncher.home.SearchScreen
import com.brendon978.menulauncher.home.hasUsageAccess
import com.brendon978.menulauncher.home.queryScreenTime
import com.brendon978.menulauncher.home.shouldRefreshScreenTime
import com.brendon978.menulauncher.icons.AppFilter
import com.brendon978.menulauncher.icons.IconRenderer
import com.brendon978.menulauncher.menu.AppActionsSheet
import com.brendon978.menulauncher.menu.RenameDialog
import com.brendon978.menulauncher.apps.organize
import com.brendon978.menulauncher.apps.withLabel
import com.brendon978.menulauncher.menu.IconPickerScreen
import com.brendon978.menulauncher.music.MusicCard
import com.brendon978.menulauncher.music.MusicControls
import com.brendon978.menulauncher.music.MusicRepository
import com.brendon978.menulauncher.onboarding.FirstRunScreen
import com.brendon978.menulauncher.settings.ColorSource
import com.brendon978.menulauncher.settings.FavoritesOps
import com.brendon978.menulauncher.settings.GestureAction
import com.brendon978.menulauncher.settings.GestureChoice
import com.brendon978.menulauncher.settings.activeFavorites
import com.brendon978.menulauncher.settings.currentProfile
import com.brendon978.menulauncher.settings.doubleTapChoice
import com.brendon978.menulauncher.settings.editFavorites
import com.brendon978.menulauncher.settings.HomeMode
import com.brendon978.menulauncher.settings.LauncherSettings
import com.brendon978.menulauncher.settings.SettingsRepository
import com.brendon978.menulauncher.settings.StrokeAuto
import com.brendon978.menulauncher.settings.usesTorchGesture
import com.brendon978.menulauncher.settingsui.AiPositionPicker
import com.brendon978.menulauncher.settingsui.SettingsScreen
import com.brendon978.menulauncher.wallpaper.WallpaperApplier
import com.brendon978.menulauncher.wallpaper.WallpaperColorSource
import com.brendon978.menulauncher.wallpaper.rotationDue
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val renaming = mutableStateOf<AppEntry?>(null)
    private val foldering = mutableStateOf<AppEntry?>(null)
    private val isDefaultHome = mutableStateOf(false)
    private val hasMusicAccess = mutableStateOf(false)
    private val hasUsageStats = mutableStateOf(false)
    private val screenTime = mutableStateOf<String?>(null)
    private var screenTimeQueried = 0L

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

        lifecycleScope.launch { settingsRepo.settings.collect { settingsSnapshot = it } }
        lifecycleScope.launch { com.brendon978.menulauncher.pet.GlitchTalk.requests.collect { talkThroughGlitch() } }
        watchScreenTime()
        setContent { MaterialTheme(colorScheme = LAUNCHER_COLORS) { Content() } }
        openRequestedScreen(intent)
    }

    /** A ALOY pede a busca ou os ajustes pelo extra [EXTRA_TELA]. */
    private fun openRequestedScreen(intent: Intent) {
        when (intent.getStringExtra(EXTRA_TELA)) {
            AloyScreen.SEARCH.name -> screen.value = Screen.Search
            AloyScreen.SETTINGS.name -> screen.value = Screen.Settings
        }
        intent.removeExtra(EXTRA_TELA) // vale uma vez: recriar a Activity não reabre a tela
    }

    @androidx.compose.runtime.Composable
    private fun Content() {
        run {
            val apps by appRepo.apps.collectAsState()
            val loaded by settingsRepo.settings.collectAsState(initial = null)
            val settings = loaded ?: LauncherSettings(firstRunDone = true)
            val music by musicRepo.state.collectAsState()
            // nomes próprios e apps escondidos (a busca ainda acha os escondidos)
            val visibleApps = remember(apps, settings.customLabels, settings.hiddenApps) { organize(apps, settings.customLabels, settings.hiddenApps) }
            val labeledApps = remember(apps, settings.customLabels) { apps.map { withLabel(it, settings.customLabels) } }
            val byKey = remember(labeledApps) { labeledApps.associateBy { it.key } }
            // A cor do papel de parede é a do WallpaperColorSource: também serve para papel claro.
            // A fonte da cor só existe (e só escuta o sistema) enquanto essa opção está ligada.
            val stroke = if (settings.strokeAuto == StrokeAuto.BY_WALLPAPER) {
                remember { wallpaperColorSource() }.color.collectAsState().value
            } else {
                com.brendon978.menulauncher.home.rememberStrokeColor(settings)
            }
            // O que a tela inicial desenha. Os ajustes gravam o original.
            val shown = remember(settings, stroke) { settings.copy(strokeColor = stroke) }
            val color = Color(stroke)
            // quem tinha a opção antiga de cor do papel de parede passa para a nova
            LaunchedEffect(settings.colorSource) {
                if (settings.colorSource == ColorSource.WALLPAPER) {
                    updateSettings { it.copy(colorSource = ColorSource.FIXED, strokeAuto = StrokeAuto.BY_WALLPAPER) }
                }
            }
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

            // voltou para a tela inicial por dentro do app (fechou os Ajustes, botão home)
            LaunchedEffect(screen.value) { rotateWallpaperIfDue() }

            val torch = if (settings.usesTorchGesture()) com.brendon978.menulauncher.home.rememberTorch() else null

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
                    homeOnTop = screen.value == Screen.Home,
                    apps = visibleApps,
                    settings = shown,
                    strokeColor = stroke,
                    renderer = renderer,
                    resetSignal = resetSignal.intValue,
                    onLaunch = {
                        launchCounted(it.key)
                    },
                    onAppLongPress = { sheetFor.value = it },
                    onEmptyLongPress = { screen.value = Screen.Settings },
                    onSearch = { screen.value = Screen.Search },
                    onSwipeDown = { runGesture(settings.gestureDown, torch) },
                    onSwipeUp = { runGesture(settings.gestureUp, torch) },
                    onEmptyDoubleTap = { runGesture(settings.doubleTapChoice(), torch) },
                    screenTime = screenTime.value,
                    onNoteChange = { text -> updateSettings { it.copy(noteText = text) } },
                    musicPlaying = music?.playing == true,
                    // Na grade o símbolo fica no meio dos ícones e parece parte do fundo: só no modo uma mão.
                    overlay = { if (settings.mode == HomeMode.ONE_HAND) AiOverlay(shown, apps, stroke) },
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
                    apps = labeledApps,
                    settings = shown,
                    strokeColor = stroke,
                    renderer = renderer,
                    onLaunch = {
                        launchCounted(it.key)
                        screen.value = Screen.Home
                    },
                    onAppLongPress = { sheetFor.value = it },
                )
                Screen.Settings -> SettingsScreen(
                    settings = settings,
                    apps = byKey,
                    onChange = ::updateSettings,
                    hasUsageAccess = hasUsageStats.value,
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
                            AiPositionPicker(settings, ::updateSettings)
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
                    isFavorite = entry.key in settings.activeFavorites(currentProfile()),
                    canUninstall = canUninstall(entry.key),
                    onToggleFavorite = {
                        val profile = currentProfile()
                        val adding = entry.key !in settings.activeFavorites(profile)
                        updateSettings { it.editFavorites(profile) { f -> FavoritesOps.toggle(f, entry.key) } }
                        toast(
                            when {
                                !adding -> "${entry.label} saiu dos favoritos"
                                settings.mode == HomeMode.GRID -> "${entry.label} nos favoritos (aparecem no modo uma mão)"
                                else -> "${entry.label} nos favoritos"
                            },
                        )
                    },
                    onChangeIcon = { screen.value = Screen.IconPicker(entry) },
                    onAppInfo = { openAppInfo(entry.key) },
                    onUninstall = { uninstall(entry.key) },
                    onDismiss = { sheetFor.value = null },
                    isHidden = entry.key in settings.hiddenApps,
                    folderName = settings.folders.entries.firstOrNull { entry.key in it.value }?.key,
                    onFolder = { foldering.value = entry },
                    onLeaveFolder = { updateSettings { it.copy(folders = com.brendon978.menulauncher.settings.FolderOps.remove(it.folders, entry.key)) } },
                    onRename = { renaming.value = entry },
                    onToggleHidden = {
                        updateSettings { FavoritesOps.setHidden(it, entry.key, hidden = entry.key !in it.hiddenApps) }
                    },
                )
            }
            foldering.value?.let { entry ->
                com.brendon978.menulauncher.menu.FolderDialog(
                    existing = settings.folders.keys.toList(),
                    onPick = { name -> updateSettings { it.copy(folders = com.brendon978.menulauncher.settings.FolderOps.put(it.folders, name, entry.key)) } },
                    onDismiss = { foldering.value = null },
                )
            }
            renaming.value?.let { entry ->
                val original = apps.firstOrNull { it.key == entry.key }?.label ?: entry.label
                RenameDialog(
                    original = original,
                    current = settings.customLabels[entry.key] ?: "",
                    onSave = { name ->
                        updateSettings { st ->
                            st.copy(customLabels = if (name.isBlank()) st.customLabels - entry.key else st.customLabels + (entry.key to name.trim()))
                        }
                    },
                    onDismiss = { renaming.value = null },
                )
            }
        }
    }

    private fun wallpaperColorSource(): WallpaperColorSource =
        wallpaperColors ?: WallpaperColorSource(this).also { wallpaperColors = it }

    /** Abre o app e registra: conta para "mais usados" e para o Glitch notar trabalho. */
    private fun launchCounted(key: com.brendon978.menulauncher.apps.AppKey) {
        com.brendon978.menulauncher.pet.GlitchSignals.lastLaunch = key.packageName to System.currentTimeMillis()
        appRepo.launch(key)
        updateSettings { st -> st.copy(usage = com.brendon978.menulauncher.settings.Usage.bump(st.usage, key)) }
    }

    private fun runGesture(choice: GestureChoice, torch: com.brendon978.menulauncher.home.Torch?) {
        when (choice.action) {
            GestureAction.NOTIFICATIONS -> if (!LauncherAccessibility.notifications(this)) {
                toast("Ligue a acessibilidade do menu-launcher para abrir as notificações")
            }
            GestureAction.SEARCH -> screen.value = Screen.Search
            GestureAction.SETTINGS -> screen.value = Screen.Settings
            GestureAction.LOCK -> lockScreen()
            GestureAction.CAMERA -> runCatching {
                startActivity(Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.onFailure { toast("Não achei o app de câmera") }
            GestureAction.TORCH -> torch?.toggle?.invoke()
            GestureAction.APP -> choice.app?.let { key ->
                if (appRepo.activityInfo(key) == null) toast("Esse app não está mais instalado")
                else runCatching { launchCounted(key) }
            }
            GestureAction.NONE -> Unit
        }
    }

    private fun lockScreen() {
        if (!LauncherAccessibility.lock()) {
            android.widget.Toast.makeText(this, "Ligue o menu-launcher em Acessibilidade para apagar a tela", android.widget.Toast.LENGTH_LONG).show()
            startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

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

    // print da tela inicial: o Glitch faz pose
    private val screenCapture = ScreenCaptureCallback {
        com.brendon978.menulauncher.pet.GlitchSignals.screenshots.tryEmit(Unit)
    }

    override fun onStart() {
        super.onStart()
        registerScreenCaptureCallback(mainExecutor, screenCapture)
    }

    override fun onStop() {
        unregisterScreenCaptureCallback(screenCapture)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        musicRepo.refresh()
        isDefaultHome.value = getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)
        hasMusicAccess.value = musicRepo.hasAccess()
        hasUsageStats.value = hasUsageAccess(this)
        rotateWallpaperIfDue()
    }

    /** Tempo de tela: só com a opção ligada e o launcher visível, no máximo uma consulta por minuto. */
    private fun watchScreenTime() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsRepo.settings.map { it.showScreenTime }.distinctUntilChanged().collectLatest { on ->
                    if (!on) {
                        screenTime.value = null
                        screenTimeQueried = 0
                        return@collectLatest
                    }
                    while (true) {
                        refreshScreenTime()
                        kotlinx.coroutines.delay(SCREEN_TIME_MIN_GAP)
                    }
                }
            }
        }
    }

    private suspend fun refreshScreenTime() {
        if (!hasUsageAccess(this)) {
            screenTime.value = null
            return
        }
        val now = System.currentTimeMillis()
        if (!shouldRefreshScreenTime(now, screenTimeQueried)) return
        screenTimeQueried = now
        val text = withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { queryScreenTime(applicationContext, now) }.getOrNull() }
        screenTime.value = text
    }

    private var rotating = false

    /** Papel de parede que muda: sem serviço em segundo plano, só confere ao voltar para a tela inicial. */
    private fun rotateWallpaperIfDue() {
        if (rotating || screen.value != Screen.Home) return
        rotating = true
        lifecycleScope.launch {
            try {
                val s = settingsRepo.settings.first()
                val now = System.currentTimeMillis()
                if (!rotationDue(s.wallpaperInterval, s.wallpaperFolder, s.wallpaperLastChange, now)) return@launch
                // marca antes: pasta com problema é tentada uma vez por intervalo, não a cada volta
                settingsRepo.update { it.copy(wallpaperLastChange = now) }
                val applied = runCatching {
                    WallpaperApplier(applicationContext).applyNextFromFolder(s.wallpaperFolder, s.wallpaperLastImage)
                }.getOrNull()
                if (applied != null) settingsRepo.update { it.copy(wallpaperLastImage = applied) }
            } finally {
                rotating = false
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun AiOverlay(settings: LauncherSettings, apps: List<AppEntry>, stroke: Int) {
        val pkg = settings.aiPackage ?: return
        val activity = settings.aiActivity ?: return
        val px = with(androidx.compose.ui.platform.LocalDensity.current) { 34.dp.roundToPx() }
        val symbol by androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, pkg, activity, stroke, settings.strokeScale, apps) {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                aiRepo.symbol(pkg, activity, px, stroke)?.asImageBitmap()
                    ?: apps.firstOrNull { it.key.packageName == pkg }?.let { renderer.iconFor(it, null, px, stroke, settings.strokeScale) }
            }
        }
        AiSymbol(
            symbol = symbol,
            color = Color(stroke),
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

    /** Falar com a IA pelo Glitch: ele mostra que ouviu e a resposta volta no balão. */
    private fun talkThroughGlitch() {
        val pkg = settingsSnapshot?.aiPackage ?: return
        val talk = com.brendon978.menulauncher.pet.GlitchTalk.state
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
            return
        }
        talk.value = "Ouvindo..."
        lifecycleScope.launch {
            kotlinx.coroutines.delay(15_000)
            if (talk.value == "Ouvindo...") { voice.cancel(); talk.value = null } // o microfone não entregou nada
        }
        voice.start { text ->
            if (text == null || !aiRepo.sendCommand(pkg, text)) {
                talk.value = null
                toast("Não entendi")
                return@start
            }
            talk.value = "Pensando..."
            lifecycleScope.launch {
                kotlinx.coroutines.delay(25_000)
                if (talk.value == "Pensando...") talk.value = null // a IA não respondeu
            }
        }
    }

    private var settingsSnapshot: LauncherSettings? = null

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
        openRequestedScreen(intent)
    }

    private companion object {
        const val REQUEST_HOME = 1
    }
}
