package com.example.util

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom

class SecurityManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("detoxwall_secure_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN_HASH = "parent_pin_hash"
        private const val KEY_PIN_SALT = "parent_pin_salt"
        private const val KEY_CHILD_MODE_ENABLED = "child_mode_enabled"
        private const val KEY_ACTIVE_PROFILE_ID = "active_child_profile_id"
        private const val KEY_SCREEN_BLANK_LIMIT_MIN = "screen_blank_limit_min"
        private const val KEY_SCREEN_BLANK_ENABLED = "screen_blank_enabled"
        private const val KEY_CHILD_ACCUMULATED_MIN = "child_accumulated_min"
        private const val KEY_SIREN_ALARM_ENABLED = "siren_alarm_enabled"
        private const val KEY_SIREN_VOLUME_PERCENT = "siren_volume_percent"
    }

    fun hasPinSet(): Boolean {
        return prefs.getString(KEY_PIN_HASH, null) != null
    }

    fun setParentPin(pin: String): Boolean {
        if (pin.length < 4) return false
        val salt = generateSalt()
        val hash = hashPinWithSalt(pin, salt)
        prefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, salt)
            .apply()
        return true
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val storedSalt = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val candidateHash = hashPinWithSalt(pin, storedSalt)
        return storedHash == candidateHash
    }

    fun isChildModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_CHILD_MODE_ENABLED, false)
    }

    fun setChildModeEnabled(enabled: Boolean, profileId: String = "child_default") {
        prefs.edit()
            .putBoolean(KEY_CHILD_MODE_ENABLED, enabled)
            .putString(KEY_ACTIVE_PROFILE_ID, profileId)
            .apply()
    }

    fun getActiveProfileId(): String {
        return prefs.getString(KEY_ACTIVE_PROFILE_ID, "child_default") ?: "child_default"
    }

    // Screen Blank (Blackout) Settings: 5 min to 5 hours (300 min)
    fun getScreenBlankLimitMinutes(): Int {
        return prefs.getInt(KEY_SCREEN_BLANK_LIMIT_MIN, 30).coerceIn(5, 300)
    }

    fun setScreenBlankLimitMinutes(minutes: Int) {
        val coerced = minutes.coerceIn(5, 300)
        prefs.edit().putInt(KEY_SCREEN_BLANK_LIMIT_MIN, coerced).apply()
    }

    fun isScreenBlankEnabled(): Boolean {
        return prefs.getBoolean(KEY_SCREEN_BLANK_ENABLED, true)
    }

    fun setScreenBlankEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SCREEN_BLANK_ENABLED, enabled).apply()
    }

    fun getChildAccumulatedMinutes(): Int {
        return prefs.getInt(KEY_CHILD_ACCUMULATED_MIN, 0)
    }

    fun setChildAccumulatedMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_CHILD_ACCUMULATED_MIN, minutes.coerceAtLeast(0)).apply()
    }

    fun resetChildAccumulatedMinutes() {
        prefs.edit().putInt(KEY_CHILD_ACCUMULATED_MIN, 0).apply()
    }

    fun isSirenAlarmEnabled(): Boolean {
        return prefs.getBoolean(KEY_SIREN_ALARM_ENABLED, true)
    }

    fun setSirenAlarmEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SIREN_ALARM_ENABLED, enabled).apply()
    }

    fun getSirenVolumePercent(): Int {
        return prefs.getInt(KEY_SIREN_VOLUME_PERCENT, 80).coerceIn(10, 100)
    }

    fun setSirenVolumePercent(percent: Int) {
        val clamped = percent.coerceIn(10, 100)
        prefs.edit().putInt(KEY_SIREN_VOLUME_PERCENT, clamped).apply()
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        return saltBytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPinWithSalt(pin: String, salt: String): String {
        val input = "$salt:$pin:DetoxWall_Secure_Salt_2026"
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
