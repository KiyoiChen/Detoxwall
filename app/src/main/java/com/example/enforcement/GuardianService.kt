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

/** Sticky foreground service that keeps the app process alive so enforcement isn't killed. */
class GuardianService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.guardian_channel_name), NotificationManager.IMPORTANCE_LOW)
                .apply { description = getString(R.string.guardian_channel_description) }
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("DetoxWall protection is on")
            .setContentText("Limits and schedules are being enforced.")
            .setOngoing(true)
            .setContentIntent(open)
            .build()
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        return START_STICKY
    }

    companion object {
        private const val CHANNEL = "guardian"
        private const val NOTIFICATION_ID = 1

        /** Safe to call repeatedly; ignored if Android refuses a background start. */
        fun start(context: Context) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, GuardianService::class.java)) }
        }
    }
}
