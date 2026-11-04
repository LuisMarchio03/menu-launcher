package com.brendon978.menulauncher.ai

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import androidx.core.graphics.createBitmap

/** Uma IA que pode ser conectada: o app e a Activity que abre no toque. */
data class AiCandidate(val packageName: String, val activityName: String, val label: String)

/** Fala com as IAs pelo contrato de AiContract (ver docs/plugue-ia.md). */
class AiRepository(private val context: Context) {
    private val pm = context.packageManager

    /** IAs que declaram a Activity ASSISTANT do contrato. */
    fun discover(): List<AiCandidate> =
        pm.queryIntentActivities(
            Intent(AiContract.ACTION_ASSISTANT).addCategory(Intent.CATEGORY_DEFAULT),
            PackageManager.MATCH_DEFAULT_ONLY,
        ).map {
            AiCandidate(it.activityInfo.packageName, it.activityInfo.name, it.loadLabel(pm).toString())
        }.filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }

    fun label(pkg: String): String =
        runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)

    fun isInstalled(pkg: String): Boolean = runCatching { pm.getPackageInfo(pkg, 0) }.isSuccess

    /** A IA recebe comando (por receiver, sem abrir, ou por Activity, abrindo)? Sem nenhum, segurar só abre a IA. */
    fun acceptsCommands(pkg: String): Boolean = commandReceiver(pkg) != null || commandActivity(pkg) != null

    private fun commandReceiver(pkg: String) =
        pm.queryBroadcastReceivers(Intent(AiContract.ACTION_COMMAND).setPackage(pkg), 0).firstOrNull()?.activityInfo

    private fun commandActivity(pkg: String) =
        pm.queryIntentActivities(
            Intent(AiContract.ACTION_COMMAND).addCategory(Intent.CATEGORY_DEFAULT).setPackage(pkg),
            PackageManager.MATCH_DEFAULT_ONLY,
        ).firstOrNull()?.activityInfo

    /** Símbolo declarado no meta-data, pintado numa cor só; null se a IA não declarou. */
    fun symbol(pkg: String, activity: String, sizePx: Int, color: Int): Bitmap? = runCatching {
        val info = pm.getActivityInfo(ComponentName(pkg, activity), PackageManager.GET_META_DATA)
        val res = info.metaData?.getInt(AiContract.META_SYMBOL, 0)?.takeIf { it != 0 } ?: return null
        val drawable = pm.getResourcesForApplication(pkg).getDrawable(res, null)
        createBitmap(sizePx, sizePx).also { bmp ->
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, sizePx, sizePx)
            drawable.draw(canvas)
            canvas.drawColor(color, PorterDuff.Mode.SRC_IN)
        }
    }.getOrNull()

    fun open(pkg: String, activity: String) {
        runCatching {
            context.startActivity(
                Intent().setComponent(ComponentName(pkg, activity))
                    .setAction(AiContract.ACTION_ASSISTANT)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure {
            // "outro app" (sem a Activity do contrato) ou Activity renomeada: abre o app pela entrada normal.
            pm.getLaunchIntentForPackage(pkg)?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }

    /**
     * Entrega o comando. Receiver (sem abrir o app) tem preferência; senão abre a Activity de comando com o texto.
     * Precisa ser chamado com o launcher em primeiro plano (o Android só deixa abrir tela a partir dele).
     */
    fun sendCommand(pkg: String, text: String): Boolean {
        commandReceiver(pkg)?.let { r ->
            // quem responde RESULT_OK resolveu sem abrir nada; quem não responde e tem Activity abre por ela
            val fallback = commandActivity(pkg)
            context.sendOrderedBroadcast(
                Intent(AiContract.ACTION_COMMAND)
                    .setComponent(ComponentName(r.packageName, r.name))
                    .putExtra(Intent.EXTRA_TEXT, text)
                    .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES or Intent.FLAG_RECEIVER_FOREGROUND),
                null,
                // a IA confere que quem manda é o launcher antes de obedecer em silêncio
                android.app.BroadcastOptions.makeBasic().setShareIdentityEnabled(true).toBundle(),
                object : android.content.BroadcastReceiver() {
                    override fun onReceive(c: Context, i: Intent) {
                        if (resultCode != android.app.Activity.RESULT_OK && fallback != null) openCommand(fallback.packageName, fallback.name, text)
                    }
                },
                null,
                android.app.Activity.RESULT_CANCELED,
                null,
                null,
            )
            return true
        }
        commandActivity(pkg)?.let { a -> return openCommand(a.packageName, a.name, text) }
        return false
    }

    private fun openCommand(pkg: String, activity: String, text: String): Boolean = runCatching {
        context.startActivity(
            Intent(AiContract.ACTION_COMMAND)
                .setComponent(ComponentName(pkg, activity))
                .putExtra(Intent.EXTRA_TEXT, text)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.isSuccess
}
