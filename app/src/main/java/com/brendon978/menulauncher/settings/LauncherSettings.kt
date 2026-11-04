package com.brendon978.menulauncher.settings

import com.brendon978.menulauncher.apps.AppKey

enum class HomeMode { ONE_HAND, GRID }

/** Fino e grande (o original), negrito com horas sobre minutos, analógico de contorno, por extenso. */
enum class ClockStyle { THIN, STACKED, ANALOG, WORDS }

/** "ter, 3 de novembro" (o original), "terça-feira, 3 de novembro", "03/11/2026", sem data. */
enum class DateStyle { SHORT, LONG, NUMERIC, HIDDEN }

/** Cor do traço: a fixa escolhida nos ajustes (o original) ou tirada do papel de parede. */
enum class ColorSource { FIXED, WALLPAPER }

/** Fontes do sistema: a padrão da One UI, condensada, serifada e monoespaçada. */
enum class FontChoice { SYSTEM, CONDENSED, SERIF, MONO }

enum class BarSide { RIGHT, LEFT }

enum class GestureAction { NONE, NOTIFICATIONS, SEARCH, LOCK, SETTINGS }

data class LauncherSettings(
    val mode: HomeMode = HomeMode.ONE_HAND,
    val favorites: List<AppKey> = emptyList(),
    val strokeColor: Int = 0xFFFFFFFF.toInt(),
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
    val colorSource: ColorSource = ColorSource.FIXED,
    /** Multiplicador da espessura do traço dos ícones. */
    val strokeScale: Float = 1f,
    val textShadow: Boolean = false,
    val font: FontChoice = FontChoice.SYSTEM,
    val barSide: BarSide = BarSide.RIGHT,
    /** Colunas da grade; 0 = automático pela largura da tela. */
    val gridColumns: Int = 0,
    val gridLabels: Boolean = true,
    val swipeDown: GestureAction = GestureAction.NOTIFICATIONS,
    val swipeUp: GestureAction = GestureAction.SEARCH,
    val doubleTap: GestureAction = GestureAction.NONE,
    /** Nome do acessório do Glitch (enum Accessory). */
    val glitchAccessory: String = "NONE",
    /** Nomes que você deu aos apps ("Samsung Shop" -> "Shop"). */
    val customLabels: Map<AppKey, String> = emptyMap(),
    /** Apps escondidos da lista e da grade (a busca ainda acha). */
    val hiddenApps: Set<AppKey> = emptySet(),
    /** Pastas do modo grade: nome -> apps (na ordem em que entraram). */
    val folders: Map<String, List<AppKey>> = emptyMap(),
    /** Quanto você usa cada app (para a linha de mais usados). */
    val usage: Map<AppKey, Float> = emptyMap(),
    /** Linha com os apps mais usados em cima dos favoritos. */
    val showMostUsed: Boolean = false,
    /** Atalhos rápidos (lanterna, Wi-Fi, internet, não perturbe) embaixo da data. */
    val showQuickActions: Boolean = false,
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
)

/** O símbolo da IA fica ao lado da barra de letras: troca de lado junto, para não cair em cima da lista. */
fun LauncherSettings.withBarSide(to: BarSide): LauncherSettings =
    if (barSide == to) this else copy(barSide = to, aiX = 1f - aiX)

/**
 * Quanto engrossa o traço dos desenhos (Glitch, brinquedos, botões) para a espessura de borda escolhida.
 * Cresce menos que a borda: traço muito grosso fecha os olhos e as sanfonas do Glitch.
 */
fun lineScale(borderScale: Float): Float = 1f + (borderScale - 1f) * .6f
