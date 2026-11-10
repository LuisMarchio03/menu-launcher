package com.brendon978.menulauncher.wallpaper

import android.app.WallpaperColors
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.io.File
import kotlin.math.max

/**
 * Papel de parede animado: toca filesDir/wallpaper/current.mp4 (sem som) ou current.gif.
 * Fica parado quando invisível (tela apagada ou coberta). Sem arquivo: preto.
 * Ao ficar visível, recarrega se o arquivo mudou (o usuário escolheu outro nos Ajustes).
 * As cores do papel (para a opção "cor do papel de parede") vêm do primeiro quadro.
 */
class AnimatedWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = AnimatedEngine()

    private inner class AnimatedEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var loaded: File? = null
        private var loadedStamp = 0L
        private var player: MediaPlayer? = null
        private var gif: AnimatedImageDrawable? = null
        private var visible = false
        private var surfaceReady = false
        @Volatile private var colors: WallpaperColors? = null
        /** Só a leitura do arquivo mais recente grava as cores (trocas seguidas não se atropelam). */
        @Volatile private var colorsGen = 0

        private val gifFrame = object : Runnable {
            override fun run() {
                if (!visible) return
                drawGif()
                handler.postDelayed(this, FRAME_MS)
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            surfaceReady = true
            reloadIfChanged(force = true)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            when {
                gif != null -> drawGif()
                player == null -> drawStill()
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            surfaceReady = false
            releaseAll()
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            if (!surfaceReady) return
            if (isVisible) {
                reloadIfChanged(force = false)
                player?.start()
                if (gif != null) {
                    gif?.start()
                    handler.post(gifFrame)
                }
            } else {
                player?.takeIf { it.isPlaying }?.pause()
                gif?.stop()
                handler.removeCallbacks(gifFrame)
            }
        }

        override fun onDestroy() {
            releaseAll()
            super.onDestroy()
        }

        override fun onComputeColors(): WallpaperColors? = colors

        /** Lê o primeiro quadro fora da thread principal e avisa o sistema (e o launcher) das cores novas. */
        private fun updateColors(file: File?) {
            val gen = ++colorsGen
            Thread {
                val frame = runCatching {
                    when (file?.extension) {
                        "mp4" -> MediaMetadataRetriever().use { r ->
                            r.setDataSource(file.absolutePath)
                            r.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, COLOR_FRAME_PX, COLOR_FRAME_PX)
                        }
                        "gif" -> ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { d, info, _ ->
                            d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                            val scale = COLOR_FRAME_PX / maxOf(info.size.width, info.size.height).toFloat()
                            if (scale < 1f) d.setTargetSize((info.size.width * scale).toInt().coerceAtLeast(1), (info.size.height * scale).toInt().coerceAtLeast(1))
                        }
                        else -> null
                    }
                }.getOrNull()
                val computed = frame?.let { runCatching { WallpaperColors.fromBitmap(it) }.getOrNull() }
                handler.post {
                    if (gen != colorsGen) return@post
                    colors = computed
                    notifyColorsChanged()
                }
            }.start()
        }

        private fun reloadIfChanged(force: Boolean) {
            val file = AnimatedFiles.current(this@AnimatedWallpaperService)
            val stamp = file?.lastModified() ?: 0L
            if (!force && file == loaded && stamp == loadedStamp) return
            releaseAll()
            loaded = file
            loadedStamp = stamp
            updateColors(file)
            when (file?.extension) {
                "mp4" -> startVideo(file)
                "gif" -> startGif(file)
                else -> drawStill()
            }
        }

        private fun startVideo(file: File) {
            runCatching {
                player = MediaPlayer().apply {
                    setSurface(surfaceHolder.surface)
                    setDataSource(file.absolutePath)
                    isLooping = true
                    setVolume(0f, 0f)
                    setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                    setOnPreparedListener { if (visible) it.start() }
                    prepareAsync()
                }
            }.onFailure {
                player = null
                drawStill()
            }
        }

        private fun startGif(file: File) {
            val d = runCatching { ImageDecoder.decodeDrawable(ImageDecoder.createSource(file)) }.getOrNull()
            gif = d as? AnimatedImageDrawable
            if (gif == null) {
                drawStill()
                return
            }
            gif?.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
            if (visible) {
                gif?.start()
                handler.post(gifFrame)
            } else {
                drawGif()
            }
        }

        private fun drawGif() {
            val d = gif ?: return
            withCanvas { canvas ->
                canvas.drawColor(Color.BLACK)
                // Corte central: cobre a tela toda mantendo a proporção.
                val w = d.intrinsicWidth.coerceAtLeast(1)
                val h = d.intrinsicHeight.coerceAtLeast(1)
                val scale = max(canvas.width / w.toFloat(), canvas.height / h.toFloat())
                val dw = (w * scale).toInt()
                val dh = (h * scale).toInt()
                val left = (canvas.width - dw) / 2
                val top = (canvas.height - dh) / 2
                d.setBounds(left, top, left + dw, top + dh)
                d.draw(canvas)
            }
        }

        private fun drawStill() = withCanvas { it.drawColor(Color.BLACK) }

        private inline fun withCanvas(block: (Canvas) -> Unit) {
            if (!surfaceReady) return
            val holder = surfaceHolder
            val canvas = runCatching { holder.lockCanvas() }.getOrNull() ?: return
            try {
                block(canvas)
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }

        private fun releaseAll() {
            handler.removeCallbacks(gifFrame)
            gif?.stop()
            gif = null
            player?.release()
            player = null
        }
    }

    private companion object {
        const val FRAME_MS = 33L
        const val COLOR_FRAME_PX = 256
    }
}
