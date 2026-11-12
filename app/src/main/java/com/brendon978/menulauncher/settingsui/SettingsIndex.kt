package com.brendon978.menulauncher.settingsui

import java.text.Normalizer

/** Uma opção dos Ajustes que a busca acha: o nome que aparece e a página onde ela mora (nome do SettingsPage). */
data class SettingsHit(val label: String, val page: String)

/** Índice da busca dos Ajustes: cada opção com as palavras que levam a ela. */
object SettingsIndex {
    private val ENTRIES: List<Pair<SettingsHit, String>> = listOf(
        "Modo (uma mão ou grade)" to "LOOK" to "modo grade uma mao layout",
        "Tamanho do relógio, ícones e texto" to "LOOK" to "tamanho escala relogio icones texto linhas barra",
        "Estilo do relógio" to "LOOK" to "relogio hora estilo dia grande segmentos analogico extenso terminal",
        "Formato da hora e segundos" to "LOOK" to "relogio formato 24h 12h am pm segundos",
        "Data e segundo fuso" to "LOOK" to "data fuso horario relogio",
        "Acabamento (detalhes, títulos, respiro)" to "LOOK" to "acabamento detalhes titulos respiro espaco peso vidro sombra veu atalhos movimento animacao",
        "Cor do traço" to "LOOK" to "cor traco automatica papel de parede dia noite hex",
        "Ícones: espessura e sombra" to "LOOK" to "icones espessura traco borda sombra letra",
        "Forma dos ícones (placa, círculo, quadrado)" to "LOOK" to "icones forma placa circulo quadrado contorno aloy atlas fundo",
        "Fonte" to "LOOK" to "fonte letra manrope outfit serifada mono condensada",
        "Barra de letras" to "LOOK" to "barra letras alfabeto lado arco esconder rolar aparece sempre",
        "Grade: colunas e nomes" to "LOOK" to "grade colunas nomes",
        "Mais usados, atalhos e nota" to "HOME" to "mais usados atalhos lanterna wifi nota bloco tempo de tela compromisso agenda",
        "Contatos" to "HOME" to "contatos busca pessoas atalhos",
        "Música" to "HOME" to "musica tocando capa controles",
        "Gestos no fundo" to "HOME" to "gestos deslizar toque duplo bloquear",
        "Favoritos e horários" to "APPS" to "favoritos horario trabalho noite ordem",
        "Apps escondidos" to "APPS" to "esconder escondidos ocultos",
        "Glitch: acessório, bichinho e jardim" to "GLITCH" to "glitch acessorio bichinho drone gato jardim calmo comentarios travessuras aniversario clima cidade zerar",
        "Assistente de IA e símbolo" to "AI" to "ia assistente aloy simbolo posicao aro",
        "Papel de parede" to "WALLPAPER" to "papel de parede wallpaper trocar pasta rotacao",
        "Backup" to "SYSTEM" to "backup exportar importar arquivo",
        "Acessibilidade e tela padrão" to "SYSTEM" to "acessibilidade bloquear tela padrao inicial sistema",
    ).map { (labelPage, words) -> SettingsHit(labelPage.first, labelPage.second) to plain(labelPage.first + " " + words) }

    private fun plain(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

    /** Todas as palavras digitadas precisam aparecer (sem acento, sem maiúscula). */
    fun search(query: String): List<SettingsHit> {
        val words = plain(query).split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()
        return ENTRIES.filter { (_, text) -> words.all { it in text } }.map { it.first }
    }
}
