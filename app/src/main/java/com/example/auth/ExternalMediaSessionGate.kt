package com.example.auth

/**
 * Defers the immediate background lock only while a media Activity explicitly launched by
 * CartAdmin is returning a result. The normal inactivity timeout remains authoritative.
 */
class ExternalMediaSessionGate(
    private val maxDurationMs: Long,
    private val nowMs: () -> Long = { System.nanoTime() / 1_000_000L }
) {
    private var activeUntilMs: Long = 0L
    private var backgroundStopArmed: Boolean = false

    @Synchronized
    fun begin() {
        activeUntilMs = nowMs() + maxDurationMs
        backgroundStopArmed = true
    }

    @Synchronized
    fun end() {
        activeUntilMs = 0L
        backgroundStopArmed = false
    }

    /** A launch may suppress one ON_STOP only; later backgrounding always locks the app. */
    @Synchronized
    fun consumeBackgroundLockDeferral(): Boolean {
        val activeUntil = activeUntilMs
        if (!backgroundStopArmed || activeUntil <= 0L || nowMs() >= activeUntil) {
            end()
            return false
        }
        backgroundStopArmed = false
        return true
    }
}
