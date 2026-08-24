package com.beam.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.beam.app.discovery.Peer
import com.beam.app.pairing.PairingQrPayload
import com.beam.app.pairing.parsePairingUri
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingScreen(
    peer: Peer?,
    myPin: String?,
    myQrData: String?,
    onGeneratePin: () -> Unit,
    onAttemptPair: suspend (String) -> Boolean,
    onQrScanned: suspend (PairingQrPayload) -> Boolean,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var scanFeedback by remember { mutableStateOf<String?>(null) }
    val launchScanner = rememberQrScanner { scanned ->
        val payload = parsePairingUri(scanned)
        if (payload == null) {
            scanFeedback = "That wasn't a Beam pairing code."
        } else {
            scope.launch {
                val success = onQrScanned(payload)
                scanFeedback = if (success) {
                    "Paired with ${payload.peer.name}!"
                } else {
                    "That code has expired — ask them to regenerate it."
                }
                if (success) onBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(peer?.let { "Pair with ${it.name}" } ?: "Pair a device") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (launchScanner != null) {
                Button(onClick = launchScanner, modifier = Modifier.fillMaxWidth()) {
                    Text("Scan a QR code to pair instantly")
                }
                scanFeedback?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Spacer(Modifier.height(20.dp))
            }

            if (peer != null) {
                var pinInput by remember { mutableStateOf("") }
                var isPairing by remember { mutableStateOf(false) }
                var showError by remember { mutableStateOf(false) }

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Enter the PIN shown on ${peer.name}", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = {
                                pinInput = it
                                showError = false
                            },
                            label = { Text("PIN") },
                            singleLine = true,
                            isError = showError,
                            supportingText = {
                                AnimatedVisibility(showError) {
                                    Text("Wrong PIN — try again", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(6.dp))
                        Button(
                            onClick = {
                                isPairing = true
                                scope.launch {
                                    val success = onAttemptPair(pinInput)
                                    isPairing = false
                                    if (success) onBack() else showError = true
                                }
                            },
                            enabled = pinInput.isNotBlank() && !isPairing,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (isPairing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(end = 8.dp).size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                            Text("Pair")
                        }
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 24.dp))
            } else if (launchScanner == null) {
                Text(
                    "This device is no longer nearby.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                )
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Show them your PIN or QR code",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    if (myPin != null) {
                        AnimatedPin(myPin)
                        if (myQrData != null) {
                            Spacer(Modifier.height(16.dp))
                            QrCodeView(myQrData)
                        }
                        Spacer(Modifier.height(6.dp))
                        TextButton(onClick = onGeneratePin) { Text("Regenerate") }
                    } else {
                        Button(onClick = onGeneratePin) { Text("Generate PIN & QR") }
                    }
                }
            }
        }
    }
}
