package com.brendon978.menulauncher.aloy

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.MatrixCursor
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Bundle
import androidx.core.app.NotificationManagerCompat
import com.brendon978.menulauncher.MainActivity
import com.brendon978.menulauncher.apps.AppRepository
import com.brendon978.menulauncher.apps.withLabel
import com.brendon978.menulauncher.gestures.LauncherAccessibility
import com.brendon978.menulauncher.music.MediaListener
import com.brendon978.menulauncher.settings.HomeMode
import com.brendon978.menulauncher.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Extra de MainActivity com o nome de um [AloyScreen]: abre essa tela. */
const val EXTRA_TELA = "tela"

private const val PERMISSION = "com.brendon978.menulauncher.permission.ALOY"

/**
 * Ponte com a ALOY (só no fork do Luis): `query /apps` lista os apps; `call(acao, args)` faz o que
 * só o launcher pode e devolve `ok` e, se falhou, `erro`. Contrato em [runAloy].
 */
class AloyProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        if (uri.path != "/apps") return null
        val context = requireContext()
        val repo = AppRepository(context)
        val apps = repo.apps.value
        repo.close()
        val settings = currentSettings(context)
        // Com os nomes que o usuário deu aos apps.
        val named = apps.map { withLabel(it, settings.customLabels) }
        return MatrixCursor(APP_COLUMNS).apply { appRows(named, settings.hiddenApps).forEach { addRow(it) } }
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        val context = requireContext()
        // O android:permission do manifesto vale para query, não para call: a checagem é aqui.
        if (context.checkCallingPermission(PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Requer $PERMISSION")
        }
        // Exceção não atravessa o binder: a ALOY sempre recebe ok/erro.
        val result = runCatching { runAloy(method, arg, AndroidAloySystem(context)) }
            .getOrElse { AloyResult(false, "falha_interna") }
        return Bundle().apply {
            putBoolean("ok", result.ok)
            result.erro?.let { putString("erro", it) }
        }
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}

private fun currentSettings(context: Context) = runBlocking { SettingsRepository(context).settings.first() }

private class AndroidAloySystem(private val context: Context) : AloySystem {
    override fun notifications(): Boolean = LauncherAccessibility.notifications(context)

    override fun lock(): Boolean = LauncherAccessibility.lock()

    override fun setMode(mode: HomeMode) {
        runBlocking { SettingsRepository(context).update { it.copy(mode = mode) } }
    }

    override fun music(cmd: MusicCmd): MusicOutcome {
        if (context.packageName !in NotificationManagerCompat.getEnabledListenerPackages(context)) return MusicOutcome.NO_ACCESS
        val manager = context.getSystemService(MediaSessionManager::class.java)
        // O acesso pode ser tirado entre a checagem acima e esta chamada.
        val sessions = runCatching { manager.getActiveSessions(ComponentName(context, MediaListener::class.java)) }
            .getOrElse { return MusicOutcome.NO_ACCESS }
        val session = sessions.firstOrNull { it.playing } ?: sessions.firstOrNull() ?: return MusicOutcome.NO_MEDIA
        val controls = session.transportControls
        when (cmd) {
            MusicCmd.PREVIOUS -> controls.skipToPrevious()
            MusicCmd.NEXT -> controls.skipToNext()
            MusicCmd.PLAY_PAUSE -> if (session.playing) controls.pause() else controls.play()
        }
        return MusicOutcome.DONE
    }

    override fun open(screen: AloyScreen): Boolean {
        // Só o launcher padrão abre tela em segundo plano sem o Android barrar em silêncio;
        // e na primeira abertura a tela pedida ficaria escondida atrás da configuração inicial.
        val isHome = context.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)
        if (!isHome || !currentSettings(context).firstRunDone) return false
        context.startActivity(
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_TELA, screen.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        return true
    }

    private val MediaController.playing get() = playbackState?.state == PlaybackState.STATE_PLAYING
}
