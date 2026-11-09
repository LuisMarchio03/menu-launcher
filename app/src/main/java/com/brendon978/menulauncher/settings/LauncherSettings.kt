package com.brendon978.menulauncher.settings

import com.brendon978.menulauncher.apps.AppKey
import com.brendon978.menulauncher.wallpaper.WallpaperInterval

enum class HomeMode { ONE_HAND, GRID }

/** Fino e grande (o original), negrito com horas sobre minutos, analógico de contorno, por extenso. */
enum class ClockStyle { THIN, STACKED, ANALOG, WORDS, SEGMENTS, TERMINAL, GIANT, ANALOG_MINIMAL, WEEKDAY }

/** Formato da hora: o do celular, 24 horas, 12 horas ou 12 horas com AM/PM. */
enum class HourFormat { SYSTEM, H24, H12, H12_AMPM }

/** Atalhos rápidos: só o desenho (mais leve) ou dentro de um círculo. */
enum class QuickStyle { PLAIN, CIRCLES }

/** Títulos "Favoritos", "Mais usados", letra: pequenos em maiúsculas, como eram, ou nenhum. */
enum class LabelStyle { CAPS, NORMAL, HIDDEN }

/** Nota vazia: um "+ nota" discreto ou o convite "Toque para anotar". */
enum class EmptyNote { PLUS, TEXT }

/** Peso do nome dos apps na lista. */
enum class AppWeight { LIGHT, NORMAL }

/** Cartão de música: o original (faixa fina, controles embaixo) ou com a capa grande e os controles dentro. */
enum class MusicStyle { CLASSIC, COVER }

/** Onde o relógio e a data ficam na largura da tela (modo uma mão). */
enum class ClockAlign { START, CENTER, END }

/** O que abre ao tocar no relógio. */
enum class ClockTap { ALARMS, CALENDAR, NONE }

/** "ter, 3 de novembro" (o original), "terça-feira, 3 de novembro", "03/11/2026", sem data. */
enum class DateStyle { SHORT, LONG, NUMERIC, HIDDEN }

/** Cor do traço: a fixa escolhida nos ajustes (o original) ou tirada do papel de parede. */
enum class ColorSource { FIXED, WALLPAPER }

/** Fontes do sistema: a padrão da One UI, condensada, serifada e monoespaçada. */
enum class FontChoice { SYSTEM, CONDENSED, SERIF, MONO }

enum class BarSide { RIGHT, LEFT }

data class LauncherSettings(
    val mode: HomeMode = HomeMode.ONE_HAND,
    val favorites: List<AppKey> = emptyList(),
    val strokeColor: Int = 0xFFFFFFFF.toInt(),
    /** Cor do traço automática; desligada vale [strokeColor]. */
    val strokeAuto: StrokeAuto = StrokeAuto.OFF,
    /** Cor de dia (7h às 19h) no modo por horário. */
    val strokeDay: Int = 0xFFFFFFFF.toInt(),
    /** Cor de noite no modo por horário. */
    val strokeNight: Int = 0xFFE6E1D6.toInt(),
    /** valor = nome do drawable do Lawnicons */
    val iconOverrides: Map<AppKey, String> = emptyMap(),
    val showMusic: Boolean = true,
    val firstRunDone: Boolean = false,
    /** IA conectada (null = nenhuma). aiActivity é a que abre no toque. */
    val aiPackage: String? = null,
    val aiActivity: String? = null,
    /** Posição do símbolo da IA, em fração da tela (zona do polegar, à esquerda da barra de letras). */
    val aiX: Float = 0.70f,
    val aiY: Float = 0.55f,
    /** Robozinho na tela inicial (modo uma mão). */
    val showPet: Boolean = true,
    /** Multiplicadores de tamanho (1 = o original). */
    val clockScale: Float = 1f,
    val iconScale: Float = 1f,
    val textScale: Float = 1f,
    val rowScale: Float = 1f,
    val barScale: Float = 1f,
    /** Multiplicador da espessura das bordas (cartão de música, busca, pastas, atalhos, símbolo da IA). */
    val borderScale: Float = 1f,
    val clockStyle: ClockStyle = ClockStyle.THIN,
    val dateStyle: DateStyle = DateStyle.SHORT,
    /** Segundo relógio, com o id do fuso (ex.: "Europe/Lisbon"); null = só o local. */
    val secondZone: String? = null,
    /** Segundos nos relógios digitais (o relógio passa a atualizar a cada segundo). */
    val clockSeconds: Boolean = false,
    val clockAlign: ClockAlign = ClockAlign.START,
    val hourFormat: HourFormat = HourFormat.SYSTEM,
    val clockTap: ClockTap = ClockTap.ALARMS,
    val colorSource: ColorSource = ColorSource.FIXED,
    /** Multiplicador da espessura do traço dos ícones. */
    val strokeScale: Float = 1f,
    val textShadow: Boolean = false,
    val font: FontChoice = FontChoice.SYSTEM,
    val barSide: BarSide = BarSide.RIGHT,
    /** Colunas da grade; 0 = automático pela largura da tela. */
    val gridColumns: Int = 0,
    val gridLabels: Boolean = true,
    /** Nome do acessório do Glitch (enum Accessory). */
    val glitchAccessory: String = "NONE",
    /** Nomes que você deu aos apps ("Samsung Shop" -> "Shop"). */
    val customLabels: Map<AppKey, String> = emptyMap(),
    /** Apps escondidos da lista e da grade (a busca ainda acha). */
    val hiddenApps: Set<AppKey> = emptySet(),
    /** Pastas do modo grade: nome -> apps (na ordem em que entraram). */
    val folders: Map<String, List<AppKey>> = emptyMap(),
    /** Tocar duas vezes no fundo apaga a tela (precisa do serviço de acessibilidade ligado). */
    val doubleTapLock: Boolean = false,
    /** Deslizar para baixo no fundo. */
    val gestureDown: GestureChoice = GestureChoice(GestureAction.NOTIFICATIONS),
    /** Deslizar para cima no fundo. */
    val gestureUp: GestureChoice = GestureChoice(GestureAction.SEARCH),
    /** Dois toques no fundo; null = ainda não escolhido (vale [doubleTapLock], ver doubleTapChoice). */
    val gestureDoubleTap: GestureChoice? = null,
    /** Quanto você usa cada app (para a linha de mais usados). */
    val usage: Map<AppKey, Float> = emptyMap(),
    /** Linha com os apps mais usados em cima dos favoritos. */
    val showMostUsed: Boolean = false,
    /** Deslizar um app da lista para o lado abre as mesmas ações do toque longo. */
    val swipeAppActions: Boolean = false,
    /** Atalhos rápidos (lanterna, Wi-Fi, internet, não perturbe) embaixo da data. */
    val showQuickActions: Boolean = false,
    /** Bloco de notas rápido na tela inicial. */
    val showNote: Boolean = false,
    /** O texto da nota. */
    val noteText: String = "",
    /** Tempo de tela de hoje embaixo da data (pede o acesso a uso de apps). */
    val showScreenTime: Boolean = false,
    /** Atalhos de contatos na tela inicial. */
    val showContacts: Boolean = false,
    /** Os contatos escolhidos, na ordem em que entraram. */
    val contactShortcuts: List<ContactShortcut> = emptyList(),
    /** Próximo compromisso da agenda embaixo da data (pede acesso à agenda). */
    val showNextEvent: Boolean = false,
    /** Favoritos diferentes de dia, no trabalho e à noite. */
    val timeProfiles: Boolean = false,
    /** Lista de cada horário (nome de TimeProfile -> apps); vazia usa [favorites]. */
    val profileFavorites: Map<String, List<AppKey>> = emptyMap(),
    /** Contatos nos resultados da busca (pede acesso aos contatos). */
    val searchContacts: Boolean = false,
    /** O Glitch comenta o app de onde você voltou. */
    val glitchComments: Boolean = true,
    /** O Glitch apronta com o relógio e os favoritos. */
    val glitchMischief: Boolean = true,
    /** Seu aniversário, "dia/mês" (vazio = sem festa). */
    val birthday: String = "",
    /** Cidade para o clima do Glitch (vazia = ele não consulta a internet). */
    val weatherCity: String = "",
    /** Pasta do papel de parede que muda (URI da árvore; vazia = nenhuma). */
    val wallpaperFolder: String = "",
    /** Intervalo da troca; OFF não mexe no papel de parede. */
    val wallpaperInterval: WallpaperInterval = WallpaperInterval.OFF,
    /** Quando a última troca foi tentada, em milissegundos. */
    val wallpaperLastChange: Long = 0,
    /** Nome da última imagem aplicada (a próxima vem depois dela). */
    val wallpaperLastImage: String = "",
    /** Bichinho que segue o Glitch (enum Buddy). */
    val glitchBuddy: String = "DRONE",
    /** Temas que você salvou nos Ajustes (os prontos ficam em [LookThemes.PRESETS]). */
    val customThemes: List<LookTheme> = emptyList(),
    val musicStyle: MusicStyle = MusicStyle.CLASSIC,
    // Acabamento: o que é secundário pesa menos que os apps e o relógio.
    val quickStyle: QuickStyle = QuickStyle.PLAIN,
    /** Opacidade dos detalhes (atalhos, títulos, barra de letras, nota vazia). */
    val detailAlpha: Float = 0.6f,
    val labelStyle: LabelStyle = LabelStyle.CAPS,
    val emptyNote: EmptyNote = EmptyNote.PLUS,
    /** Barra de letras em arco (o original) ou reta. */
    val barCurved: Boolean = false,
    val appWeight: AppWeight = AppWeight.LIGHT,
    /** Multiplica os espaços entre os blocos da tela inicial. */
    val spacing: Float = 1f,
)

/** O título de uma seção no estilo escolhido; null = sem título. */
fun sectionLabel(text: String, style: LabelStyle): String? = when (style) {
    LabelStyle.NORMAL -> text
    LabelStyle.CAPS -> text.uppercase()
    LabelStyle.HIDDEN -> null
}

/** O símbolo da IA fica ao lado da barra de letras: troca de lado junto, para não cair em cima da lista. */
fun LauncherSettings.withBarSide(to: BarSide): LauncherSettings =
    if (barSide == to) this else copy(barSide = to, aiX = 1f - aiX)

/**
 * Quanto engrossa o traço dos desenhos (Glitch, brinquedos, botões) para a espessura de borda escolhida.
 * Cresce menos que a borda: traço muito grosso fecha os olhos e as sanfonas do Glitch.
 */
fun lineScale(borderScale: Float): Float = 1f + (borderScale - 1f) * .6f
