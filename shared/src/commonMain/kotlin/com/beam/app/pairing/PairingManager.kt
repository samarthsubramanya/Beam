package com.beam.app.pairing

import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private val PIN_TTL = 2.minutes

/** One-time 6-digit PIN, shown on this device and typed into the other to establish trust. */
class PairingManager {
    private var activePin: String? = null
    private var expiresAt: TimeMark = TimeSource.Monotonic.markNow()

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
