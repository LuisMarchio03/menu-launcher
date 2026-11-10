package com.brendon978.menulauncher.apps

/** [category]: o ApplicationInfo.category que o app declara (-1 = nenhuma). */
data class AppEntry(val key: AppKey, val label: String, val category: Int = -1)
