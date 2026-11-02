package com.example.enforcement

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.container

/** Foreground notification lets a verified parent END temporary access at any time. */
class GuardianService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_END_PARENT) container.settings.endParentUse()
        val parentActive = container.settings.current.isMaintenance(System.currentTimeMillis())
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.guardian_channel_name), NotificationManager.IMPORTANCE_LOW)
                .apply { description = getString(R.string.guardian_channel_description) }
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle(if (parentActive) "家長暫時使用中" else "DetoxWall 保護中")
            .setContentText(if (parentActive) "用完請按「結束家長模式」；最多 15 分鐘。" else "孩子遊戲計時與休息限制啟用中。")
            .setOngoing(true)
            .setContentIntent(open)

        if (parentActive) {
            val end = PendingIntent.getService(
                this, 1,
                Intent(this, GuardianService::class.java).setAction(ACTION_END_PARENT),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            builder.addAction(android.R.drawable.ic_lock_idle_lock, "結束家長模式", end)
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        return START_STICKY
    }

    companion object {
        const val ACTION_END_PARENT = "com.example.enforcement.END_PARENT_USE"
        private const val CHANNEL = "guardian"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, GuardianService::class.java)) }
        }
    }
}
