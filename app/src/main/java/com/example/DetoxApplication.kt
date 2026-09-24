package com.example

import android.app.Application
import com.example.auth.ClerkAuthManager

class DetoxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Clerk authentication with stored or default publishable key
        ClerkAuthManager.initialize(this)
    }
}
