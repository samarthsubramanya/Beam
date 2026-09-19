package com.beam.app.pro

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

/** Store-backed purchase state. Only Android and iOS have a RevenueCat SDK; desktop reports [supported] = false. */
interface Billing {
    val supported: Boolean

    /** Whether this device's own store account holds the Pro entitlement. */
    val isPro: StateFlow<Boolean>

    fun start()

    /** Re-syncs purchases from the store; [onDone] receives whether Pro is now active. */
    fun restore(onDone: (Boolean) -> Unit)
}

expect fun createBilling(): Billing

/** RevenueCat's paywall on mobile; an explanation of how desktop gets Pro. */
@Composable
expect fun PaywallContent(onDismiss: () -> Unit)
