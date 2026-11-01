package com.example

import com.example.data.PlaySessionPolicy
import com.example.data.PlaySessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PlaySessionPolicyTest {
    @Test fun supportedDurationsOnly() {
        assertEquals(300_000L, PlaySessionPolicy.deadline(0L, 5))
        assertEquals(1_800_000L, PlaySessionPolicy.deadline(0L, 30))
        assertThrows(IllegalArgumentException::class.java) { PlaySessionPolicy.deadline(0L, 999) }
    }

    @Test fun expiredSessionStaysExpired() {
        assertEquals(PlaySessionState.READY, PlaySessionPolicy.state(true, 0L, 100L))
        assertEquals(PlaySessionState.RUNNING, PlaySessionPolicy.state(true, 1000L, 999L))
        assertEquals(PlaySessionState.EXPIRED, PlaySessionPolicy.state(true, 1000L, 1000L))
        assertEquals(PlaySessionState.EXPIRED, PlaySessionPolicy.state(true, 1000L, 2000L))
    }

    @Test fun modeDoesNotStartWithoutParentEnablingIt() {
        assertEquals(PlaySessionState.DISABLED, PlaySessionPolicy.state(false, 1000L, 2000L))
    }
}
