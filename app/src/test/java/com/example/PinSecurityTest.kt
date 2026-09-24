package com.example

import com.example.security.LockoutPolicy
import com.example.security.PinHasher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinSecurityTest {
    private val hasher = PinHasher(iterations = 1_000) // fast for tests

    @Test fun `correct pin verifies`() {
        val salt = hasher.newSalt()
        val hash = hasher.hash("4821", salt)
        assertTrue(hasher.verify("4821", salt, hash, 1_000))
    }

    @Test fun `wrong pin fails`() {
        val salt = hasher.newSalt()
        val hash = hasher.hash("4821", salt)
        assertFalse(hasher.verify("4822", salt, hash, 1_000))
    }

    @Test fun `same pin with different salts gives different hashes`() {
        val a = hasher.hash("1111", hasher.newSalt())
        val b = hasher.hash("1111", hasher.newSalt())
        assertFalse(a.contentEquals(b))
    }

    @Test fun `pin validation`() {
        assertTrue(PinHasher.isValidPin("1234"))
        assertTrue(PinHasher.isValidPin("12345678"))
        assertFalse(PinHasher.isValidPin("123"))
        assertFalse(PinHasher.isValidPin("123456789"))
        assertFalse(PinHasher.isValidPin("12a4"))
    }

    @Test fun `lockout starts after five failures and doubles up to the cap`() {
        assertEquals(0L, LockoutPolicy.lockoutDurationMs(4))
        assertEquals(30_000L, LockoutPolicy.lockoutDurationMs(5))
        assertEquals(60_000L, LockoutPolicy.lockoutDurationMs(6))
        assertEquals(120_000L, LockoutPolicy.lockoutDurationMs(7))
        assertEquals(LockoutPolicy.MAX_LOCKOUT_MS, LockoutPolicy.lockoutDurationMs(50))
    }
}
