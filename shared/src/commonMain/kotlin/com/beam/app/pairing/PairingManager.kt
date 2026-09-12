package com.beam.app.pairing

import kotlin.concurrent.Volatile
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private val PIN_TTL = 5.minutes

/**
 * One-time 6-digit PIN, shown on this device and typed into the other to establish trust.
 *
 * Written from the UI (Compose/AWT thread) and read from the transport server's request-handling
 * threads — without @Volatile the JVM is free to let a server thread cache a stale read of these
 * fields indefinitely (no synchronization ever forces a re-read), so every /pair check would keep
 * comparing against whatever PIN was active the first time that thread happened to read it.
 */
class PairingManager {
    @Volatile private var activePin: String? = null
    @Volatile private var expiresAt: TimeMark = TimeSource.Monotonic.markNow()

    fun generatePin(): String {
        val pin = Random.nextInt(0, 1_000_000).toString().padStart(6, '0')
        activePin = pin
        expiresAt = TimeSource.Monotonic.markNow() + PIN_TTL
        return pin
    }

    /** Single-use: a correct guess (or a timeout) clears the PIN so it can't be replayed. */
    fun verifyAndConsume(candidate: String): Boolean {
        val pin = activePin
        val valid = pin != null && pin == candidate && !expiresAt.hasPassedNow()
        if (valid || pin != null) activePin = null
        return valid
    }
}
