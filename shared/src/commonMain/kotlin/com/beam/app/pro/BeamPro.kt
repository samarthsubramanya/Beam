package com.beam.app.pro

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

const val FREE_PAIRED_DEVICE_LIMIT = 2

/** What the paywall is being shown for, so it can explain itself. */
enum class ProFeature(val title: String, val pitch: String) {
    FILES("File transfers", "Send photos, documents, and any file between your devices."),
    MORE_DEVICES("More devices", "Free covers $FREE_PAIRED_DEVICE_LIMIT paired devices. Pro pairs as many as you like."),
}

/**
 * The single source of truth for "is this user Pro?".
 *
 * Phones read their own store entitlement. Desktop has no store SDK, so it inherits Pro from any
 * paired phone that reports it (see [BeamCore]); [localPro] is what a device reports to its peers.
 */
object BeamPro {
    private val billing = createBilling()
    private val inheritedPro = MutableStateFlow(false)

    /** True where this device can buy Pro itself (Android/iOS). */
    val canPurchase: Boolean get() = billing.supported

    val localPro: StateFlow<Boolean> = billing.isPro

    val isPro: StateFlow<Boolean> = if (billing.supported) billing.isPro else inheritedPro.asStateFlow()

    fun start() = billing.start()

    fun restore(onDone: (Boolean) -> Unit) = billing.restore(onDone)

    fun setInheritedPro(value: Boolean) {
        if (!billing.supported) inheritedPro.value = value
    }

    fun canPairAnother(pairedCount: Int): Boolean = isPro.value || pairedCount < FREE_PAIRED_DEVICE_LIMIT
}
