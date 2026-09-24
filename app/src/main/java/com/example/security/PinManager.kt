package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

sealed interface PinResult {
    data object Success : PinResult
    data class Wrong(val attemptsLeft: Int) : PinResult
    data class LockedOut(val remainingMs: Long) : PinResult
    data object NotSet : PinResult
}

/**
 * Stores the parent PIN locally (never leaves the device; backups are disabled in the manifest).
 * Failed attempts and the lockout deadline are persisted, so killing the app does not reset them.
 */
class PinManager(
    context: Context,
    private val hasher: PinHasher = PinHasher(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _isPinSet = MutableStateFlow(prefs.contains(KEY_HASH))
    val isPinSet: StateFlow<Boolean> = _isPinSet.asStateFlow()

    fun hasPin(): Boolean = prefs.contains(KEY_HASH)

    fun lockoutRemainingMs(): Long =
        (prefs.getLong(KEY_LOCKOUT_UNTIL, 0L) - clock()).coerceAtLeast(0L)

    suspend fun setPin(pin: String): Boolean = withContext(Dispatchers.Default) {
        if (!PinHasher.isValidPin(pin)) return@withContext false
        val salt = hasher.newSalt()
        val hash = hasher.hash(pin, salt)
        prefs.edit(commit = true) {
            putString(KEY_HASH, hash.b64())
            putString(KEY_SALT, salt.b64())
            putInt(KEY_ITERATIONS, hasher.iterations)
            putInt(KEY_FAILED, 0)
            putLong(KEY_LOCKOUT_UNTIL, 0L)
        }
        _isPinSet.value = true
        true
    }

    suspend fun verify(pin: String): PinResult = withContext(Dispatchers.Default) {
        val hash = prefs.getString(KEY_HASH, null)?.unb64() ?: return@withContext PinResult.NotSet
        val salt = prefs.getString(KEY_SALT, null)?.unb64() ?: return@withContext PinResult.NotSet
        val iterations = prefs.getInt(KEY_ITERATIONS, PinHasher.DEFAULT_ITERATIONS)

        val remaining = lockoutRemainingMs()
        if (remaining > 0) return@withContext PinResult.LockedOut(remaining)

        if (hasher.verify(pin, salt, hash, iterations)) {
            prefs.edit(commit = true) { putInt(KEY_FAILED, 0); putLong(KEY_LOCKOUT_UNTIL, 0L) }
            PinResult.Success
        } else {
            val failed = prefs.getInt(KEY_FAILED, 0) + 1
            val lockout = LockoutPolicy.lockoutDurationMs(failed)
            prefs.edit(commit = true) {
                putInt(KEY_FAILED, failed)
                if (lockout > 0) putLong(KEY_LOCKOUT_UNTIL, clock() + lockout)
            }
            if (lockout > 0) PinResult.LockedOut(lockout)
            else PinResult.Wrong(LockoutPolicy.FREE_ATTEMPTS - failed)
        }
    }

    suspend fun changePin(current: String, new: String): PinResult {
        val result = verify(current)
        if (result is PinResult.Success && !setPin(new)) return PinResult.Wrong(0)
        return result
    }

    private fun ByteArray.b64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.unb64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

    companion object {
        private const val PREFS = "detox_pin"
        private const val KEY_HASH = "hash"
        private const val KEY_SALT = "salt"
        private const val KEY_ITERATIONS = "iterations"
        private const val KEY_FAILED = "failed"
        private const val KEY_LOCKOUT_UNTIL = "lockout_until"
    }
}
