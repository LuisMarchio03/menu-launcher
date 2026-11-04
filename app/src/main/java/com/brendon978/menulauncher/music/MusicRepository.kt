package com.brendon978.menulauncher.music

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** O que está tocando, via sessão de mídia. Sem acesso às notificações ou sem sessão: estado null. */
class MusicRepository(private val context: Context) {
    private val manager = context.getSystemService(MediaSessionManager::class.java)
    private val listener = ComponentName(context, MediaListener::class.java)
    private val handler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow<MusicState?>(null)
    val state: StateFlow<MusicState?> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var listening = false

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onSessionDestroyed() = refresh()
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { refresh() }

    fun hasAccess(): Boolean = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    /** Chamar ao voltar para a tela inicial: o acesso pode ter sido dado nesse meio-tempo. */
    fun refresh() {
        if (!hasAccess()) {
            stopListening()
            setController(null)
            return
        }
        val sessions = runCatching { manager.getActiveSessions(listener) }.getOrElse { emptyList() }
        if (!listening) {
            runCatching { manager.addOnActiveSessionsChangedListener(sessionsListener, listener, handler) }
                .onSuccess { listening = true }
        }
        val picked = pickSession(sessions.map { SessionInfo(it.packageName, it.isPlaying()) })
        setController(sessions.firstOrNull { it.packageName == picked?.packageName })
    }

    /** Desliga sessões e controller; chamar no onDestroy da Activity. */
    fun close() {
        handler.removeCallbacks(republish)
        stopListening()
        controller?.unregisterCallback(controllerCallback)
        controller = null
    }

    fun playPause() {
        val c = controller ?: return
        if (c.isPlaying()) c.transportControls.pause() else c.transportControls.play()
    }

    fun next() = controller?.transportControls?.skipToNext() ?: Unit
    fun previous() = controller?.transportControls?.skipToPrevious() ?: Unit

    private fun setController(c: MediaController?) {
        if (c?.sessionToken == controller?.sessionToken) {
            publish()
            return
        }
        controller?.unregisterCallback(controllerCallback)
        controller = c
        c?.registerCallback(controllerCallback, handler)
        publish()
    }

    private fun stopListening() {
        if (listening) manager.removeOnActiveSessionsChangedListener(sessionsListener)
        listening = false
    }

    private val republish = Runnable { publish() }

    private fun publish() {
        handler.removeCallbacks(republish)
        val c = controller
        val meta = c?.metadata
        val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
        val playing = c?.isPlaying() == true
        // lastPositionUpdateTime está no relógio elapsedRealtime: serve como "pausou em".
        val pausedSince = c?.playbackState?.lastPositionUpdateTime?.takeIf { !playing && it > 0 }
        val now = SystemClock.elapsedRealtime()
        if (c == null || title.isNullOrBlank() || !shouldShowMusic(playing, pausedSince, now)) {
            _state.value = null
            return
        }
        if (pausedSince != null) handler.postDelayed(republish, PAUSED_GRACE_MS - (now - pausedSince) + 1)
        val appLabel = runCatching {
            context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(c.packageName, 0)).toString()
        }.getOrDefault(c.packageName)
        _state.value = MusicState(
            title = title,
            artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST),
            appLabel = appLabel,
            art = meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ART),
            playing = playing,
            positionMs = c.playbackState?.position ?: -1,
            durationMs = meta.getLong(MediaMetadata.METADATA_KEY_DURATION),
            updatedAt = c.playbackState?.lastPositionUpdateTime ?: 0,
            speed = c.playbackState?.playbackSpeed?.takeIf { it > 0f } ?: 1f,
        )
    }

    private fun MediaController.isPlaying() = playbackState?.state == PlaybackState.STATE_PLAYING
}
