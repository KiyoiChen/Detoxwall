package com.example

import android.content.Context
import com.example.data.DetoxDatabase
import com.example.data.DetoxRepository
import com.example.data.SettingsStore
import com.example.security.PinManager
import com.example.usage.InstalledApps
import com.example.usage.UsageReader

/** Manual dependency container shared by the UI and the services (same process). */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val settings = SettingsStore(appContext)
    val pinManager = PinManager(appContext)
    val repository = DetoxRepository(DetoxDatabase.create(appContext))
    val installedApps = InstalledApps(appContext)
    val usageReader = UsageReader(appContext, installedApps)
}
