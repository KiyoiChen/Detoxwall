package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.security.PinHasher
import com.example.security.PinManager
import com.example.security.PinResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PinManagerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var now = 1_000_000L
    private fun manager() = PinManager(context, PinHasher(iterations = 1_000)) { now }

    @Test fun `set and verify persists across instances`() = runTest {
        assertTrue(manager().setPin("2468"))
        assertEquals(PinResult.Success, manager().verify("2468"))
    }

    @Test fun `five wrong tries lock out, and the lock survives a new instance`() = runTest {
        manager().setPin("2468")
        repeat(4) { assertTrue(manager().verify("0000") is PinResult.Wrong) }
        assertTrue(manager().verify("0000") is PinResult.LockedOut)
        // Even the right PIN is refused while locked out.
        assertTrue(manager().verify("2468") is PinResult.LockedOut)
        now += 31_000
        assertEquals(PinResult.Success, manager().verify("2468"))
    }
}
