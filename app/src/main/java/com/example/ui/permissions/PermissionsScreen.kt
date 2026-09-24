package com.example.ui.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.components.SectionCard
import com.example.ui.components.enterAnimation
import com.example.usage.PermissionKind
import com.example.usage.PermissionStatus
import com.example.usage.Permissions

/** Permissions the current build actually uses. The others come with later parts of the app. */
val AVAILABLE_PERMISSIONS = listOf(
    PermissionKind.USAGE_ACCESS, PermissionKind.ACCESSIBILITY, PermissionKind.NOTIFICATIONS, PermissionKind.BATTERY,
)

fun currentPermissions(context: Context): List<PermissionStatus> =
    AVAILABLE_PERMISSIONS.map { PermissionStatus(it, Permissions.isGranted(context, it)) }

@Composable
fun PermissionsScreen(
    statuses: List<PermissionStatus>,
    contentPadding: PaddingValues,
    onChanged: () -> Unit,
) {
    val context = LocalContext.current
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { onChanged() }

    fun open(kind: PermissionKind) {
        if (kind == PermissionKind.NOTIFICATIONS && Build.VERSION.SDK_INT >= 33) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        val intent = Permissions.settingsIntent(context, kind) ?: return
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { // Some phones lack the exact page; fall back to the app's details page.
                runCatching { context.startActivity(Permissions.appDetailsIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.enterAnimation()) {
                Text("Permissions", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Tap an item to open its settings page, then come back here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (Build.VERSION.SDK_INT >= 33 && statuses.any { it.kind == PermissionKind.ACCESSIBILITY && !it.granted }) {
            item {
                SectionCard(Modifier.enterAnimation(60), onClick = {
                    runCatching { context.startActivity(Permissions.appDetailsIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }) {
                    Text("Accessibility greyed out?", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Android restricts it for apps installed from a file. Tap here, open the ⋮ menu, " +
                            "choose \"Allow restricted settings\", then enable Accessibility again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        itemsIndexed(statuses, key = { _, s -> s.kind }) { index, status ->
            PermissionRow(status, Modifier.enterAnimation(80 + index * 60)) { open(status.kind) }
        }
    }
}

@Composable
private fun PermissionRow(status: PermissionStatus, modifier: Modifier, onClick: () -> Unit) {
    val tint by animateColorAsState(
        if (status.granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        label = "permTint",
    )
    SectionCard(modifier, onClick = if (status.granted) null else onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = tint)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(status.kind.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (status.granted) "Allowed" else status.kind.why,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!status.granted) Icon(Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}
