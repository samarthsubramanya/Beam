package com.beam.app.pro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// RevenueCat has no desktop SDK, so desktop never sells Pro itself — see BeamPro.setInheritedPro.
private object DesktopBilling : Billing {
    override val supported = false
    override val isPro: StateFlow<Boolean> = MutableStateFlow(false)
    override fun start() {}
    override fun restore(onDone: (Boolean) -> Unit) = onDone(false)
}

actual fun createBilling(): Billing = DesktopBilling

@Composable
actual fun PaywallContent(onDismiss: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Beam Pro", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Pro is unlocked from your phone. Get Beam Pro in the Beam app on Android or iOS, then keep " +
                "that phone paired with this computer — this computer becomes Pro whenever it's nearby.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onDismiss) { Text("Got it") }
    }
}
