package com.beam.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.beam.app.discovery.Peer
import com.beam.app.discovery.createDiscoveryService
import com.beam.app.transport.PlatformFile
import com.beam.app.transport.TransportServer
import com.beam.app.transport.rememberFilePicker
import com.beam.app.transport.saveToDownloads
import com.beam.app.transport.sendFile
import com.beam.app.transport.sendText
import com.beam.app.transport.setClipboardText
import kotlin.random.Random
import kotlinx.coroutines.launch

/**
 * Root composable for Beam. Currently just a discovery debug screen — replaced
 * by the real device-radar UI in Phase 5. Proves LAN discovery works end to end.
 */
@Composable
fun App() {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    ) {
        Surface {
            DiscoveryDebugScreen()
        }
    }
}

private const val DEBUG_PORT = 53212

@Composable
private fun DiscoveryDebugScreen() {
    val scope = rememberCoroutineScope()
    val service = remember { createDiscoveryService() }
    val deviceName = remember { "Beam-${Random.nextInt(1000, 9999)}" }
    val peers by service.peers.collectAsState()

    var message by remember { mutableStateOf("") }
    var lastReceived by remember { mutableStateOf("(nothing yet)") }
    var pickedFile by remember { mutableStateOf<PlatformFile?>(null) }

    DisposableEffect(Unit) {
        val server = TransportServer(
            onTextReceived = { payload ->
                lastReceived = "text from ${payload.senderName}: ${payload.content}"
                setClipboardText(payload.content)
            },
            onFileReceived = { file ->
                val path = saveToDownloads(file.filename, file.bytes)
                lastReceived = "file from ${file.senderName}: ${file.filename} -> $path"
            },
        )
        server.start(DEBUG_PORT)
        service.start(localDeviceId = deviceName, localDeviceName = deviceName, servicePort = DEBUG_PORT)
        onDispose {
            service.stop()
            server.stop()
        }
    }

    val pickFile = rememberFilePicker { pickedFile = it }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("This device: $deviceName")
        Text("Last received: $lastReceived")
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Message to send") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = pickFile) {
            Text(pickedFile?.let { "File picked: ${it.name}" } ?: "Pick a file to send instead")
        }
        Text("Tap a peer below to send the message, or the picked file if one is selected:")
        LazyColumn {
            items(peers) { peer: Peer ->
                Text(
                    "${peer.name} — ${peer.host}:${peer.port}",
                    modifier = Modifier.fillMaxWidth().clickable {
                        val file = pickedFile
                        scope.launch {
                            if (file != null) sendFile(peer, deviceName, file) else sendText(peer, deviceName, message)
                        }
                    },
                )
            }
        }
    }
}
