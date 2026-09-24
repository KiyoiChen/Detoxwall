package com.example.usage

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telecom.TelecomManager
import android.util.LruCache
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class InstalledApp(val packageName: String, val label: String)

class InstalledApps(private val context: Context) {
    private val pm: PackageManager = context.packageManager
    private val labelCache = HashMap<String, String>()
    private val iconCache = LruCache<String, ImageBitmap>(120)

    @Volatile private var launchers: Set<String>? = null
    @Volatile private var imes: Set<String>? = null

    fun launchable(): List<InstalledApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != context.packageName }
            .map { InstalledApp(it, label(it)) }
            .sortedBy { it.label.lowercase() }
    }

    fun label(pkg: String): String = synchronized(labelCache) {
        labelCache.getOrPut(pkg) {
            runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }
                .getOrElse { pkg.substringAfterLast('.').replaceFirstChar(Char::uppercaseChar) }
        }
    }

    fun icon(pkg: String, sizePx: Int = 128): ImageBitmap? {
        iconCache.get(pkg)?.let { return it }
        return runCatching {
            pm.getApplicationIcon(pkg).toBitmap(sizePx, sizePx).asImageBitmap()
        }.getOrNull()?.also { iconCache.put(pkg, it) }
    }

    fun launcherPackages(): Set<String> = launchers ?: run {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .map { it.activityInfo.packageName }
            .toSet()
            .also { launchers = it }
    }

    fun inputMethodPackages(): Set<String> = imes ?: run {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.enabledInputMethodList.map { it.packageName }.toSet().also { imes = it }
    }

    fun defaultDialer(): String? = runCatching {
        (context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager).defaultDialerPackage
    }.getOrNull()

    fun invalidate() {
        launchers = null
        imes = null
    }
}
