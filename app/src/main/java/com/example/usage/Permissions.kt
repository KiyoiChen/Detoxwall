package com.example.usage

import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

enum class PermissionKind(val title: String, val why: String, val essential: Boolean) {
    USAGE_ACCESS("Usage access", "Measures how long each app is used today.", true),
    ACCESSIBILITY("Accessibility service", "Detects the app on screen and shows the lock. Required for all blocking.", true),
    NOTIFICATIONS("Notifications", "Shows that protection is running.", false),
    BATTERY("Unrestricted battery", "Stops the system from putting protection to sleep.", false),
    DEVICE_ADMIN("Uninstall protection", "Device admin prevents DetoxWall from being uninstalled.", false),
}

data class PermissionStatus(val kind: PermissionKind, val granted: Boolean)

object Permissions {
    // Class names of components added in a later step; referenced by name so this compiles now.
    private const val ACCESSIBILITY_CLASS = "com.example.enforcement.DetoxAccessibilityService"
    private const val ADMIN_CLASS = "com.example.enforcement.DetoxDeviceAdminReceiver"


    fun all(context: Context): List<PermissionStatus> = PermissionKind.entries.map {
        PermissionStatus(it, isGranted(context, it))
    }

    fun isGranted(context: Context, kind: PermissionKind): Boolean = when (kind) {
        PermissionKind.USAGE_ACCESS -> hasUsageAccess(context)
        PermissionKind.ACCESSIBILITY -> isAccessibilityEnabled(context)
        PermissionKind.NOTIFICATIONS -> NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == 0)
        PermissionKind.BATTERY ->
            (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
                .isIgnoringBatteryOptimizations(context.packageName)
        PermissionKind.DEVICE_ADMIN -> isDeviceAdminActive(context)
    }

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun isAccessibilityEnabled(context: Context): Boolean {
        val expected = ComponentName(context.packageName, ACCESSIBILITY_CLASS)
        val enabled = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }

    fun adminComponent(context: Context) = ComponentName(context.packageName, ADMIN_CLASS)

    fun isDeviceAdminActive(context: Context): Boolean =
        (context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
            .isAdminActive(adminComponent(context))

    /** Intent that opens the right settings page, or null when a runtime request is needed. */
    @SuppressLint("BatteryLife")
    fun settingsIntent(context: Context, kind: PermissionKind): Intent? = when (kind) {
        PermissionKind.USAGE_ACCESS -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        PermissionKind.ACCESSIBILITY -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        PermissionKind.NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= 33) null else
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        PermissionKind.BATTERY -> Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}"),
        )
        PermissionKind.DEVICE_ADMIN -> Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent(context))
            .putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Keeps DetoxWall from being uninstalled while Child Mode protects this device.",
            )
    }

    /** Android 13+ blocks accessibility for sideloaded apps until "Allow restricted settings". */
    fun appDetailsIntent(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
}
