package com.brendon978.menulauncher.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Aplica os papéis de parede que o usuário escolhe. Nada aqui roda na thread principal. */
class WallpaperApplier(private val context: Context) {

    fun kindOf(uri: Uri): WallpaperKind = classify(context.contentResolver.getType(uri), displayName(uri))

    suspend fun applyStatic(uri: Uri, target: StaticTarget) = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)!!.use { input ->
            WallpaperManager.getInstance(context).setStream(input, null, true, target.flags())
        }
    }

    /** Copia para a pasta do serviço animado; grava num temporário e renomeia (o serviço nunca lê pela metade). */
    suspend fun prepareAnimated(uri: Uri, ext: String): File = withContext(Dispatchers.IO) {
        val dir = AnimatedFiles.dir(context).apply { mkdirs() }
        val tmp = File(dir, "incoming.tmp")
        context.contentResolver.openInputStream(uri)!!.use { input ->
            tmp.outputStream().use { input.copyTo(it) }
        }
        AnimatedFiles.all(context).forEach { it.delete() }
        val target = File(dir, "current.$ext")
        check(tmp.renameTo(target)) { "não consegui gravar $target" }
        target
    }

    /** Prévia do sistema do papel de parede animado; o usuário confirma lá. */
    fun livePreviewIntent(service: Class<*> = AnimatedWallpaperService::class.java): Intent =
        Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(context, service))

    /** Guarda a imagem de fundo do Glitch no bloqueio; o serviço lê quando a tela acende. */
    suspend fun prepareLockBackground(uri: Uri) = withContext(Dispatchers.IO) {
        val target = LockFiles.background(context)
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, "lock.tmp")
        context.contentResolver.openInputStream(uri)!!.use { input ->
            tmp.outputStream().use { input.copyTo(it) }
        }
        check(tmp.renameTo(target)) { "não consegui gravar $target" }
    }

    /**
     * Aplica na tela inicial (nunca no bloqueio) a próxima imagem da pasta e devolve o nome dela.
     * null = nada aplicado. Arquivo que não abre é pulado; pasta inacessível lança, quem chama decide.
     */
    suspend fun applyNextFromFolder(folder: String, last: String): String? = withContext(Dispatchers.IO) {
        val images = listImages(Uri.parse(folder))
        rotationOrder(images.keys.toList(), last).take(MAX_TRIES).firstOrNull { name ->
            val uri = images.getValue(name)
            isReadableImage(uri) && runCatching { applyStatic(uri, StaticTarget.HOME) }.isSuccess
        }
    }

    /** Imagens direto na pasta (sem entrar em subpastas): nome -> uri. */
    private fun listImages(tree: Uri): Map<String, Uri> {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val columns = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        val found = mutableMapOf<String, Uri>()
        context.contentResolver.query(children, columns, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val name = c.getString(1) ?: continue
                if (isRotationImage(c.getString(2), name)) found[name] = DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0))
            }
        }
        return found
    }

    /** Só lê o cabeçalho: não entrega arquivo corrompido para o sistema. */
    private fun isReadableImage(uri: Uri): Boolean = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
        bounds.outWidth > 0 && bounds.outHeight > 0
    }.getOrDefault(false)

    private fun displayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()
}

private const val MAX_TRIES = 5

/** Fundo do Glitch no bloqueio. */
object LockFiles {
    fun background(context: Context) = File(AnimatedFiles.dir(context), "lock_background")
}

/** Onde mora o arquivo do papel de parede animado (o serviço lê daqui). */
object AnimatedFiles {
    fun dir(context: Context) = File(context.filesDir, "wallpaper")
    fun all(context: Context) = listOf("mp4", "gif").map { File(dir(context), "current.$it") }
    fun current(context: Context): File? = all(context).firstOrNull { it.exists() }
}
