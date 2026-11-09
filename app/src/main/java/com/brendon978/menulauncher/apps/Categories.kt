package com.brendon978.menulauncher.apps

/** As gavetas do modo por categoria, na ordem em que aparecem. */
enum class AppCategory(val label: String) {
    SOCIAL("Social"), WORK("Trabalho"), MEDIA("Mídia"), PHOTOS("Fotos"), GAMES("Jogos"),
    TRAVEL("Mapas e viagem"), NEWS("Notícias"), TOOLS("Ferramentas"), OTHER("Outros"),
}

/** Pedaços do nome do pacote que entregam a categoria, para app que não declara nenhuma. */
private val HINTS: List<Pair<AppCategory, List<String>>> = listOf(
    AppCategory.SOCIAL to listOf("whatsapp", "telegram", "instagram", "facebook", "twitter", "discord", "messag", "sms", "mms", "dialer", "contacts", "signal", "tiktok", "reddit", "linkedin", "threads"),
    AppCategory.WORK to listOf(".gm", "mail", "calendar", "docs", "drive", "office", "slack", "teams", "notion", "sheets", "keep", "zoom", "outlook", "todo", "tasks", "notes", "tachyon", "meet", "authenticator"),
    AppCategory.MEDIA to listOf("music", "spotify", "youtube", "netflix", "video", "player", "podcast", ".tv", "deezer", "primevideo", "disney", "hbo", "twitch"),
    AppCategory.PHOTOS to listOf("camera", "gallery", "photo", "lightroom", "snapseed"),
    AppCategory.TRAVEL to listOf("maps", "uber", "waze", "99app", "airbnb", "booking"),
    AppCategory.NEWS to listOf("news", "noticias", "globo"),
    AppCategory.GAMES to listOf("game", "games", "supercell", "roblox", "minecraft"),
    AppCategory.TOOLS to listOf("settings", "calculator", "clock", "files", "myfiles", "browser", "chrome", "firefox", "comet", "opera", "brave", "edge", "vpn", "bank", "nubank", "itau", "bradesco"),
)

/**
 * Categoria do app: a escolhida à mão; senão um palpite pelo pacote (muitos apps declaram a categoria
 * errada, como o Gmail e o Chrome que dizem "social"); senão a que ele declara ([androidCategory]).
 */
fun categoryOf(androidCategory: Int, packageName: String, override: String?): AppCategory {
    override?.let { name -> AppCategory.entries.firstOrNull { it.name == name }?.let { return it } }
    val pkg = packageName.lowercase()
    HINTS.firstOrNull { (_, words) -> words.any { it in pkg } }?.let { return it.first }
    return when (androidCategory) {
        0 -> AppCategory.GAMES // CATEGORY_GAME
        1, 2 -> AppCategory.MEDIA // AUDIO, VIDEO
        3 -> AppCategory.PHOTOS // IMAGE
        4 -> AppCategory.SOCIAL
        5 -> AppCategory.NEWS
        6 -> AppCategory.TRAVEL // MAPS
        7 -> AppCategory.WORK // PRODUCTIVITY
        8 -> AppCategory.TOOLS // ACCESSIBILITY
        else -> AppCategory.OTHER
    }
}

/** Apps agrupados na ordem de [AppCategory], cada grupo em ordem alfabética; grupo vazio fica de fora. */
fun groupByCategory(apps: List<AppEntry>, overrides: Map<AppKey, String>): List<Pair<AppCategory, List<AppEntry>>> =
    apps.groupBy { categoryOf(it.category, it.key.packageName, overrides[it.key]) }
        .toSortedMap(compareBy { it.ordinal })
        .map { (cat, list) -> cat to list.sortedBy { it.label.lowercase() } }
