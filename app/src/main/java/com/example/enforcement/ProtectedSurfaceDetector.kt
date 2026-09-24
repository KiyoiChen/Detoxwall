package com.example.enforcement

/**
 * Recognises screens that could be used to disable DetoxWall: any Settings page that mentions the
 * app (app info → force stop / clear storage, accessibility toggle, device-admin deactivation,
 * battery) and the package installer's uninstall dialog.
 */
object ProtectedSurfaceDetector {

    val SETTINGS_PACKAGES = setOf(
        "com.android.settings",
        "com.samsung.android.settings",
        "com.samsung.accessibility",
        "com.samsung.android.lool", // Samsung device care
        "com.miui.securitycenter",
        "com.miui.securityadd",
        "com.coloros.safecenter",
        "com.oplus.safecenter",
        "com.huawei.systemmanager",
        "com.google.android.permissioncontroller",
        "com.android.permissioncontroller",
    )

    val INSTALLER_PACKAGES = setOf(
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.samsung.android.packageinstaller",
        "com.miui.packageinstaller",
    )

    fun isCandidate(pkg: String) = pkg in SETTINGS_PACKAGES || pkg in INSTALLER_PACKAGES

    fun detect(pkg: String, visibleTexts: List<String>, appLabel: String): SurfaceKind? {
        if (!isCandidate(pkg)) return null
        val mentionsUs = visibleTexts.any { it.contains(appLabel, ignoreCase = true) }
        if (!mentionsUs) return null
        return if (pkg in INSTALLER_PACKAGES) SurfaceKind.UNINSTALL else SurfaceKind.SETTINGS
    }
}
