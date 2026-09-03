package com.example.security

import com.example.auth.ExternalMediaSessionGate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalMediaSessionGateTest {
    @Test
    fun activeCameraFlowDefersImmediateBackgroundLock() {
        var now = 1_000L
        val gate = ExternalMediaSessionGate(maxDurationMs = 300_000L, nowMs = { now })

        gate.begin()

        assertTrue(gate.consumeBackgroundLockDeferral())
        now += 1L
        assertFalse(gate.consumeBackgroundLockDeferral())
    }

    @Test
    fun completedOrCancelledCameraFlowStopsDeferringLock() {
        val gate = ExternalMediaSessionGate(maxDurationMs = 300_000L, nowMs = { 1_000L })
        gate.begin()

        gate.end()

        assertFalse(gate.consumeBackgroundLockDeferral())
    }

    @Test
    fun abandonedCameraFlowExpires() {
        var now = 1_000L
        val gate = ExternalMediaSessionGate(maxDurationMs = 300_000L, nowMs = { now })
        gate.begin()

        now += 300_000L

        assertFalse(gate.consumeBackgroundLockDeferral())
    }
}
