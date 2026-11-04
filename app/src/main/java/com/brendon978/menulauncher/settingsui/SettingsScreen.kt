package com.brendon978.menulauncher.settingsui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.apps.AppKey
import com.brendon978.menulauncher.home.LocalBorderWidth
import com.brendon978.menulauncher.settings.FavoritesOps
import com.brendon978.menulauncher.settings.activeFavorites
import com.brendon978.menulauncher.settings.editFavorites
import com.brendon978.menulauncher.settings.HomeMode
import com.brendon978.menulauncher.settings.LauncherSettings
import com.brendon978.menulauncher.settings.Backup
import kotlinx.coroutines.launch

/** Toque longo numa área vazia abre aqui. Tudo se ajusta no celular. */
@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    apps: Map<AppKey, AppEntry>,
    onChange: ((LauncherSettings) -> LauncherSettings) -> Unit,
    onSetDefaultLauncher: () -> Unit,
    aiSection: @Composable () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0E))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ajustes", color = Color.White, fontSize = 24.sp, modifier = Modifier.weight(1f))
            Text("Fechar", color = Color.White.copy(alpha = 0.7f), modifier = Modifier.clickable(onClick = onClose).padding(8.dp))
        }

        Section("Modo")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Uma mão", settings.mode == HomeMode.ONE_HAND) { onChange { it.copy(mode = HomeMode.ONE_HAND) } }
            Chip("Grade", settings.mode == HomeMode.GRID) { onChange { it.copy(mode = HomeMode.GRID) } }
        }

        SizeSection(settings, onChange)
        ClockSection(settings, onChange)

        Section("Cor do traço")
        ColorSourceChoice(settings, onChange)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            COLOR_CHOICES.forEach { c ->
                Box(
                    Modifier
                        .size(36.dp)
                        .background(Color(c), CircleShape)
                        .border(if (c == settings.strokeColor) LocalBorderWidth.current * 3 else LocalBorderWidth.current, Color.White.copy(alpha = if (c == settings.strokeColor) 1f else 0.3f), CircleShape)
                        .clickable { onChange { it.copy(strokeColor = c) } },
                )
            }
        }
        var hex by rememberSaveable { mutableStateOf("") }
        OutlinedTextField(
            value = hex,
            onValueChange = { v ->
                hex = v
                parseHexColor(v)?.let { c -> onChange { it.copy(strokeColor = c) } }
            },
            singleLine = true,
            placeholder = { Text("ou digite um hex, ex.: #A0C4FF") },
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        )

        IconLookSection(settings, onChange)
        FontSection(settings, onChange)
        BarSideSection(settings, onChange)
        GridSection(settings, onChange)
        GestureSection(settings, onChange)

        Section("Música")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mostrar o que está tocando", color = Color.White, modifier = Modifier.weight(1f))
            Switch(settings.showMusic, onCheckedChange = { v -> onChange { it.copy(showMusic = v) } })
        }

        Section("Tela inicial")
        ToggleRow("Linha de mais usados", "No modo uma mão, os apps que você mais abre, em cima dos favoritos.", settings.showMostUsed) { v -> onChange { it.copy(showMostUsed = v) } }
        ToggleRow("Atalhos rápidos", "Lanterna, Wi-Fi, internet e não perturbe embaixo da data.", settings.showQuickActions) { v -> onChange { it.copy(showQuickActions = v) } }
        NextEventToggle(settings.showNextEvent) { v -> onChange { it.copy(showNextEvent = v) } }
        ContactsToggle(settings.searchContacts) { v -> onChange { it.copy(searchContacts = v) } }

        Section("Favoritos")
        ToggleRow("Favoritos por horário", "Listas diferentes de dia, no trabalho (seg. a sex., 8h às 18h) e à noite (19h às 7h).", settings.timeProfiles) { v -> onChange { it.copy(timeProfiles = v) } }
        var editing by rememberSaveable { mutableStateOf(com.brendon978.menulauncher.settings.currentProfile()) }
        if (settings.timeProfiles) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                com.brendon978.menulauncher.settings.TimeProfile.entries.forEach { p -> Chip(p.label, editing == p) { editing = p } }
            }
        }
        val shownFavorites = settings.activeFavorites(editing)
        if (shownFavorites.isEmpty()) {
            Text("Nenhum. Segure um app na tela inicial para favoritar.", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
        }
        shownFavorites.forEachIndexed { i, key ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text(apps[key]?.label ?: key.packageName, color = Color.White, modifier = Modifier.weight(1f))
                SmallButton("↑", enabled = i > 0) { onChange { s -> s.editFavorites(editing) { FavoritesOps.move(it, i, i - 1) } } }
                SmallButton("↓", enabled = i < shownFavorites.lastIndex) { onChange { s -> s.editFavorites(editing) { FavoritesOps.move(it, i, i + 1) } } }
                SmallButton("✕", enabled = true) { onChange { s -> s.editFavorites(editing) { FavoritesOps.toggle(it, key) } } }
            }
        }
        if (settings.timeProfiles) {
            Text("Para adicionar a outro horário, segure um app enquanto estiver nele; aqui dá para ordenar e tirar.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        }

        AddFavorite(settings, apps, onChange)

        HiddenAppsSection(settings, apps, onChange)

        Section("Glitch")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Glitch na tela inicial", color = Color.White, modifier = Modifier.weight(1f))
            Switch(settings.showPet, onCheckedChange = { v -> onChange { it.copy(showPet = v) } })
        }
        if (settings.showPet) {
            Text("Acessório", color = Color.White.copy(alpha = .75f), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                com.brendon978.menulauncher.pet.Accessory.entries.filter { it.choosable }.forEach { a ->
                    Chip(a.label, settings.glitchAccessory == a.name) { onChange { it.copy(glitchAccessory = a.name) } }
                }
            }
            GlitchPanel()
            ToggleRow("Comentários", "Ele comenta o app de onde você voltou, de vez em quando.", settings.glitchComments) { v -> onChange { it.copy(glitchComments = v) } }
            ToggleRow("Travessuras", "Ele entorta o relógio e empurra um favorito; cutucar faz ele arrumar.", settings.glitchMischief) { v -> onChange { it.copy(glitchMischief = v) } }
            var birthdayText by rememberSaveable { mutableStateOf(settings.birthday) }
            OutlinedTextField(
                value = birthdayText,
                onValueChange = { v ->
                    birthdayText = v
                    if (v.isBlank() || com.brendon978.menulauncher.pet.parseBirthday(v) != null) onChange { it.copy(birthday = v.trim()) }
                },
                singleLine = true,
                label = { Text("Seu aniversário (dia/mês)") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            var cityText by rememberSaveable { mutableStateOf(settings.weatherCity) }
            OutlinedTextField(
                value = cityText,
                onValueChange = { v -> cityText = v; onChange { it.copy(weatherCity = v) } },
                singleLine = true,
                label = { Text("Sua cidade, para o clima (opcional)") },
                supportingText = { Text("Só o nome da cidade vai para o Open-Meteo. Vazio, ele não consulta nada.") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            GlitchReset()
        }

        Section("IA")
        aiSection()

        Section("Papel de parede")
        WallpaperSection()

        Section("Backup")
        BackupSection(settings, onChange)

        Section("Sistema")
        // Sem UpdateRow neste fork: o atualizador baixa o APK do Brendon, sem as alterações daqui.
        Text(
            "Definir como tela inicial padrão",
            color = Color.White,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onSetDefaultLauncher).padding(vertical = 12.dp),
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
internal fun Section(title: String) {
    Text(title, color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp, modifier = Modifier.padding(top = 24.dp, bottom = 10.dp))
}

@Composable
internal fun Chip(text: String, selected: Boolean, fontFamily: FontFamily? = null, onClick: () -> Unit) {
    Text(
        text,
        color = if (selected) Color.Black else Color.White,
        fontFamily = fontFamily,
        // A sombra da tela inicial borra o texto escuro no chip selecionado.
        style = LocalTextStyle.current.copy(shadow = null),
        modifier = Modifier
            .background(if (selected) Color.White else Color.Transparent, RoundedCornerShape(10.dp))
            .border(LocalBorderWidth.current, Color.White.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

@Composable
private fun SmallButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(44.dp).clickable(enabled = enabled, onClick = onClick),
    ) {
        Text(text, color = Color.White.copy(alpha = if (enabled) 0.9f else 0.25f))
    }
    Spacer(Modifier.width(2.dp))
}

/** O cérebro do Glitch ao vivo: o que ele está fazendo, as vontades e o vínculo com você. */
@Composable
private fun GlitchPanel() {
    val snap by com.brendon978.menulauncher.pet.GlitchLive.state.collectAsState()
    val s = snap
    if (s == null) {
        Text("Abra a tela inicial no modo uma mão para acordar o Glitch.", color = Color.White.copy(alpha = .6f), fontSize = 13.sp)
        return
    }
    Text(s.doing, color = Color.White, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
    Bar("Energia", s.drives.energy)
    Bar("Tédio", s.drives.boredom)
    Bar("Carência", s.drives.longing)
    Bar("Curiosidade", s.drives.curiosity)
    Bar("Irritação", s.irritation.coerceIn(0f, 1f))
    Bar("Vínculo com você", s.bond)
    if (s.diary.isNotBlank()) {
        Text("Diário de hoje", color = Color.White.copy(alpha = .55f), fontSize = 13.sp, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
        Text("“${s.diary}”", color = Color.White, fontSize = 14.sp)
    }
}

@Composable
private fun Bar(label: String, value: Float) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = Color.White.copy(alpha = .75f), fontSize = 13.sp, modifier = Modifier.width(130.dp))
        Box(Modifier.weight(1f).height(8.dp).background(Color.White.copy(alpha = .12f), RoundedCornerShape(4.dp))) {
            Box(Modifier.fillMaxWidth(value.coerceIn(0f, 1f)).height(8.dp).background(Color.White.copy(alpha = .85f), RoundedCornerShape(4.dp)))
        }
        Text("${(value * 100).toInt()}%", color = Color.White.copy(alpha = .6f), fontSize = 12.sp, modifier = Modifier.width(44.dp).padding(start = 8.dp))
    }
}

/** Procura versão nova no GitHub; instalar sempre passa pela confirmação do Android. */
@Composable
private fun UpdateRow() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var status by androidx.compose.runtime.remember { mutableStateOf("Versão ${com.brendon978.menulauncher.BuildConfig.VERSION_NAME}") }
    var busy by androidx.compose.runtime.remember { mutableStateOf(false) }
    Text(
        "Procurar atualização",
        color = Color.White,
        modifier = Modifier.fillMaxWidth().clickable(enabled = !busy) {
            busy = true
            scope.launch {
                val r = com.brendon978.menulauncher.update.Updater.latest()
                when {
                    r == null -> status = "Não consegui ver agora"
                    !com.brendon978.menulauncher.update.isNewer(r, com.brendon978.menulauncher.BuildConfig.VERSION_CODE) -> status = "Já está na versão mais nova"
                    !context.packageManager.canRequestPackageInstalls() -> {
                        status = "Libere instalar apps do menu-launcher e toque de novo"
                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, android.net.Uri.parse("package:" + context.packageName)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                    else -> {
                        status = "Baixando…"
                        runCatching { com.brendon978.menulauncher.update.Updater.install(context, com.brendon978.menulauncher.update.Updater.download(context, r)) }
                            .onSuccess { status = r.notes.ifBlank { "Pronto para instalar" } }
                            .onFailure { status = "O download falhou" }
                    }
                }
                busy = false
            }
        }.padding(top = 8.dp, bottom = 2.dp),
    )
    Text(status, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
}

/** Volta o Glitch ao zero: vontades, vínculo, diário, aprendizado, rancor e protestos. Acessório e ajustes ficam. */
@Composable
private fun GlitchReset() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var asking by androidx.compose.runtime.remember { mutableStateOf(false) }
    Text(
        "Zerar o Glitch",
        color = Color.White,
        modifier = Modifier.fillMaxWidth().clickable { asking = true }.padding(vertical = 12.dp),
    )
    if (asking) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { asking = false },
            title = { Text("Zerar o Glitch?") },
            text = { Text("Ele esquece tudo: vínculo, vontades, diário, o que aprendeu e o que você aprontou. Não dá para desfazer.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    asking = false
                    scope.launch {
                        val live = com.brendon978.menulauncher.pet.GlitchLive
                        live.importing = true // o Glitch da tela não pode salvar por cima
                        com.brendon978.menulauncher.pet.GlitchStore(context.applicationContext)
                            .save(com.brendon978.menulauncher.pet.GlitchMemory(lastSeen = System.currentTimeMillis()))
                        (context as? android.app.Activity)?.recreate()
                    }
                }) { Text("Zerar") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { asking = false }) { Text("Cancelar") } },
        )
    }
}

/** Exportar e importar ajustes (e a memória do Glitch) num arquivo de texto. */
@Composable
private fun BackupSection(settings: LauncherSettings, onChange: ((LauncherSettings) -> LauncherSettings) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val store = androidx.compose.runtime.remember { com.brendon978.menulauncher.pet.GlitchStore(context.applicationContext) }
    fun toast(m: String) = android.widget.Toast.makeText(context, m, android.widget.Toast.LENGTH_SHORT).show()
    val export = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = Backup.encode(settings, com.brendon978.menulauncher.pet.GlitchBackup.toExtras(store.load()))
            runCatching { context.contentResolver.openOutputStream(uri)!!.use { it.write(text.toByteArray()) } }
                .onSuccess { toast("Backup salvo") }.onFailure { toast("Não consegui salvar") }
        }
    }
    val import = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = runCatching { context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() } }.getOrNull()
            val restored = text?.let { Backup.decode(it) }
            if (restored == null) { toast("Esse arquivo não é um backup do launcher"); return@launch }
            com.brendon978.menulauncher.pet.GlitchBackup.fromExtras(Backup.extras(text, "glitch."))?.let { mem ->
                com.brendon978.menulauncher.pet.GlitchLive.importing = true
                store.save(mem)
            }
            // não traz a primeira abertura de volta
            onChange { restored.copy(firstRunDone = true) }
            toast("Backup restaurado")
            (context as? android.app.Activity)?.recreate()
        }
    }
    Text(
        "Exportar ajustes e o Glitch",
        color = Color.White,
        modifier = Modifier.fillMaxWidth().clickable { export.launch("menu-launcher-backup.txt") }.padding(vertical = 12.dp),
    )
    Text(
        "Importar de um arquivo",
        color = Color.White,
        modifier = Modifier.fillMaxWidth().clickable { import.launch(arrayOf("text/plain", "application/octet-stream", "*/*")) }.padding(vertical = 12.dp),
    )
    Text(
        "Favoritos, cores, nomes, apps escondidos, IA, acessório e a memória do Glitch (vínculo e vontades).",
        color = Color.White.copy(alpha = .5f),
        fontSize = 12.sp,
    )
}

@Composable
private fun ToggleRow(title: String, hint: String, value: Boolean, onToggle: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White)
            Text(hint, color = Color.White.copy(alpha = .5f), fontSize = 12.sp)
        }
        Switch(value, onCheckedChange = onToggle)
    }
}

/** Ligar o próximo compromisso pede acesso à agenda na hora. */
@Composable
private fun ContactsToggle(value: Boolean, onToggle: (Boolean) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val ask = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { granted ->
        onToggle(granted)
    }
    ToggleRow("Contatos na busca", "Encontra pessoas ao buscar (pede acesso aos contatos).", value) { v ->
        if (!v) onToggle(false)
        else if (context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED) onToggle(true)
        else ask.launch(android.Manifest.permission.READ_CONTACTS)
    }
}

@Composable
private fun NextEventToggle(value: Boolean, onToggle: (Boolean) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val ask = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { granted ->
        onToggle(granted)
        if (!granted) android.widget.Toast.makeText(context, "Sem acesso à agenda não dá pra mostrar o compromisso", android.widget.Toast.LENGTH_SHORT).show()
    }
    ToggleRow("Próximo compromisso", "Da sua agenda, embaixo da data (pede acesso à agenda).", value) { v ->
        if (!v) onToggle(false)
        else if (context.checkSelfPermission(android.Manifest.permission.READ_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED) onToggle(true)
        else ask.launch(android.Manifest.permission.READ_CALENDAR)
    }
}
