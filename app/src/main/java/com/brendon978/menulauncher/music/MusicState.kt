package com.brendon978.menulauncher.music

import android.graphics.Bitmap

data class MusicState(
    val title: String,
    val artist: String?,
    val appLabel: String,
    val art: Bitmap?,
    val playing: Boolean,
    /** Para o contorno de progresso: posição e duração em ms, e quando a posição foi lida (elapsedRealtime). */
    val positionMs: Long = -1,
    val durationMs: Long = 0,
    val updatedAt: Long = 0,
    val speed: Float = 1f,
)

data class SessionInfo(val packageName: String, val playing: Boolean)

/** Prefere a sessão que está tocando; empate = a primeira (a mais recente, na ordem do MediaSessionManager). */
fun pickSession(sessions: List<SessionInfo>): SessionInfo? =
    sessions.firstOrNull { it.playing } ?: sessions.firstOrNull()

const val PAUSED_GRACE_MS = 10 * 60_000L

/**
 * Tocando: aparece. Pausada: continua por [PAUSED_GRACE_MS] para dar para retomar pela tela inicial, depois some.
 * Sem saber quando pausou: não aparece.
 */
fun shouldShowMusic(playing: Boolean, pausedSince: Long?, now: Long): Boolean =
    playing || (pausedSince != null && now - pausedSince < PAUSED_GRACE_MS)

/**
 * Quanto da música já tocou (0 a 1), andando sozinho entre as atualizações do player.
 * [updatedAt] e [now] no relógio elapsedRealtime. null quando o player não informa duração ou posição.
 */
fun songProgress(positionMs: Long, durationMs: Long, updatedAt: Long, now: Long, speed: Float, playing: Boolean): Float? {
    if (durationMs <= 0 || positionMs < 0) return null
    val pos = if (playing) positionMs + ((now - updatedAt) * speed).toLong() else positionMs
    return (pos.toFloat() / durationMs).coerceIn(0f, 1f)
}
