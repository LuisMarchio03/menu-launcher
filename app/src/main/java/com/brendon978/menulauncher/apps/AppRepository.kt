package com.brendon978.menulauncher.apps

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppRepository(context: Context) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = reload()
        override fun onPackageAdded(packageName: String, user: UserHandle) = reload()
        override fun onPackageChanged(packageName: String, user: UserHandle) = reload()
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = reload()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = reload()
        override fun onPackagesSuspended(packageNames: Array<out String>, user: UserHandle) = reload()
        override fun onPackagesUnsuspended(packageNames: Array<out String>, user: UserHandle) = reload()
    }

    init {
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        reload()
    }

    fun reload() {
        _apps.value = userManager.userProfiles.flatMap { user ->
            val serial = userManager.getSerialNumberForUser(user)
            launcherApps.getActivityList(null, user).map { it.toEntry(serial) }
        }
    }

    private fun LauncherActivityInfo.toEntry(serial: Long) =
        AppEntry(AppKey(componentName.packageName, componentName.className, serial), label.toString(), applicationInfo.category)

    /** Desliga o aviso do sistema; chamar no onDestroy da Activity para ela não ficar presa na memória. */
    fun close() = launcherApps.unregisterCallback(callback)

    fun userOf(key: AppKey): UserHandle? = userManager.getUserForSerialNumber(key.userSerial)

    /** Info do sistema para a chave (ícone, flags); null se o app não existe mais. */
    fun activityInfo(key: AppKey): LauncherActivityInfo? {
        val user = userOf(key) ?: return null
        return launcherApps.getActivityList(key.packageName, user)
            .firstOrNull { it.componentName.className == key.activityName }
    }

    fun launch(key: AppKey, sourceBounds: Rect? = null) {
        val user = userOf(key) ?: return
        launcherApps.startMainActivity(ComponentName(key.packageName, key.activityName), user, sourceBounds, null)
    }
}
