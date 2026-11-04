package com.brendon978.menulauncher.apps

/** Apps visíveis na tela inicial: com os nomes que você deu e sem os que você escondeu. */
fun organize(apps: List<AppEntry>, labels: Map<AppKey, String>, hidden: Set<AppKey>): List<AppEntry> =
    apps.filter { it.key !in hidden }.map { withLabel(it, labels) }

/** Só aplica o nome próprio (a busca usa isto: encontra também os escondidos). */
fun withLabel(app: AppEntry, labels: Map<AppKey, String>): AppEntry =
    labels[app.key]?.trim()?.takeIf { it.isNotEmpty() }?.let { app.copy(label = it) } ?: app
