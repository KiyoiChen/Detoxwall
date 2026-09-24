package com.example.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** PBKDF2-HMAC-SHA256 PIN hashing. Pure JVM so it can be unit-tested without Android. */
class PinHasher(
    val iterations: Int = DEFAULT_ITERATIONS,
    private val random: SecureRandom = SecureRandom(),
) {
    fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also(random::nextBytes)

    fun hash(pin: String, salt: ByteArray, iterations: Int = this.iterations): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_BITS)
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    /** Constant-time comparison so timing does not leak how many bytes matched. */
    fun verify(pin: String, salt: ByteArray, expected: ByteArray, iterations: Int): Boolean =
        MessageDigest.isEqual(hash(pin, salt, iterations), expected)

    companion object {
        const val ALGORITHM = "PBKDF2WithHmacSHA256"
        const val DEFAULT_ITERATIONS = 120_000
        const val SALT_BYTES = 16
        const val KEY_BITS = 256
        const val MIN_LENGTH = 4
        const val MAX_LENGTH = 8

        fun isValidPin(pin: String): Boolean =
            pin.length in MIN_LENGTH..MAX_LENGTH && pin.all { it in '0'..'9' }
    }
}

/** Exponential back-off after repeated wrong PINs: 5 free tries, then 30 s doubling up to 15 min. */
object LockoutPolicy {
    const val FREE_ATTEMPTS = 5
    const val BASE_LOCKOUT_MS = 30_000L
    const val MAX_LOCKOUT_MS = 15 * 60_000L

    fun lockoutDurationMs(failedAttempts: Int): Long {
        if (failedAttempts < FREE_ATTEMPTS) return 0L
        val exponent = (failedAttempts - FREE_ATTEMPTS).coerceAtMost(10)
        return (BASE_LOCKOUT_MS shl exponent).coerceAtMost(MAX_LOCKOUT_MS)
    }
}
