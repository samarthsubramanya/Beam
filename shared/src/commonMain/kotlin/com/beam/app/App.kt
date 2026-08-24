package com.beam.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.beam.app.db.BeamRepository
import com.beam.app.db.DatabaseDriverFactory
import com.beam.app.db.TransferEntry
import com.beam.app.discovery.Peer
import com.beam.app.discovery.createDiscoveryService
import com.beam.app.pairing.PairingManager
import com.beam.app.theme.BeamTheme
import com.beam.app.transport.PlatformFile
import com.beam.app.transport.TransportServer
import com.beam.app.transport.nowMillis
import com.beam.app.transport.pairWith
import com.beam.app.transport.rememberFilePicker
import com.beam.app.transport.saveToDownloads
import com.beam.app.transport.sendFile
import com.beam.app.transport.sendText
import com.beam.app.transport.setClipboardText
import kotlin.random.Random
import kotlinx.coroutines.launch

private const val PORT = 53212

@Composable
fun App() {
    BeamTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen() {
    val scope = rememberCoroutineScope()
    val service = remember { createDiscoveryService() }
    val deviceId = remember { "Beam-${Random.nextInt(1000, 9999)}" }
    val repository = remember { BeamRepository(DatabaseDriverFactory()) }
    val pairingManager = remember { PairingManager() }
    val peers by service.peers.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var message by remember { mutableStateOf("") }
    var pickedFile by remember { mutableStateOf<PlatformFile?>(null) }
    var myPin by remember { mutableStateOf<String?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pairingTarget by remember { mutableStateOf<Peer?>(null) }
    var pairedIds by remember { mutableStateOf(setOf<String>()) }

    suspend fun refreshPaired() {
        pairedIds = repository.pairedDeviceIds()
    }

    DisposableEffect(Unit) {
        val server = TransportServer(
            repository = repository,
            pairingManager = pairingManager,
            localDeviceId = deviceId,
            localDeviceName = deviceId,
            onTextReceived = { payload ->
                setClipboardText(payload.content)
                scope.launch {
                    repository.recordHistory(
                        TransferEntry(payload.senderName, "received", "text", payload.content, nowMillis())
                    )
                    snackbarHostState.showSnackbar("${payload.senderName}: ${payload.content} (copied to clipboard)")
                }
            },
            onFileReceived = { file ->
                val path = saveToDownloads(file.filename, file.bytes)
                scope.launch {
                    repository.recordHistory(
                        TransferEntry(file.senderName, "received", "file", file.filename, nowMillis())
                    )
                    snackbarHostState.showSnackbar("Received ${file.filename} from ${file.senderName}")
                }
            },
        )
        server.start(PORT)
        service.start(localDeviceId = deviceId, localDeviceName = deviceId, servicePort = PORT)
        onDispose {
            service.stop()
            server.stop()
        }
    }

    LaunchedEffect(Unit) { refreshPaired() }

    val pickFile = rememberFilePicker { pickedFile = it }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Beam", style = MaterialTheme.typography.titleLarge)
                        Text(
                            deviceId,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showPinDialog = true }) {
                        Icon(Icons.Filled.Lock, contentDescription = "Show pairing PIN")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ComposeBar(
                message = message,
                onMessageChange = { message = it },
                pickedFile = pickedFile,
                onPickFile = pickFile,
                onClearFile = { pickedFile = null },
            )

            Text(
                if (peers.isEmpty()) "" else "${peers.size} device${if (peers.size == 1) "" else "s"} nearby",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (peers.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(peers) { peer ->
                        PeerCard(
                            peer = peer,
                            isPaired = peer.id in pairedIds,
                            canSend = message.isNotBlank() || pickedFile != null,
                            onPairClick = { pairingTarget = peer },
                            onSendClick = {
                                val file = pickedFile
                                scope.launch {
                                    if (file != null) {
                                        sendFile(peer, deviceId, deviceId, file)
                                        repository.recordHistory(
                                            TransferEntry(peer.name, "sent", "file", file.name, nowMillis())
                                        )
                                    } else {
                                        sendText(peer, deviceId, deviceId, message)
                                        repository.recordHistory(
                                            TransferEntry(peer.name, "sent", "text", message, nowMillis())
                                        )
                                    }
                                    snackbarHostState.showSnackbar("Sent to ${peer.name}")
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (showPinDialog) {
        PinDialog(
            pin = myPin,
            onGenerate = { myPin = pairingManager.generatePin() },
            onDismiss = { showPinDialog = false },
        )
    }

    pairingTarget?.let { target ->
        PairDialog(
            peerName = target.name,
            onConfirm = { pin ->
                scope.launch {
                    val result = pairWith(target, deviceId, deviceId, pin)
                    if (result != null) {
                        repository.addPairedDevice(result.deviceId, result.deviceName, nowMillis())
                        refreshPaired()
                        snackbarHostState.showSnackbar("Paired with ${target.name}")
                    } else {
                        snackbarHostState.showSnackbar("Wrong PIN — try again")
                    }
                }
                pairingTarget = null
            },
            onDismiss = { pairingTarget = null },
        )
    }
}

@Composable
private fun ComposeBar(
    message: String,
    onMessageChange: (String) -> Unit,
    pickedFile: PlatformFile?,
    onPickFile: () -> Unit,
    onClearFile: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = message,
                onValueChange = onMessageChange,
                placeholder = { Text("Message to send") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            Spacer(Modifier.size(8.dp))
            FilledIconButton(onClick = onPickFile) {
                Icon(Icons.Filled.Add, contentDescription = "Attach a file")
            }
        }
        pickedFile?.let { file ->
            Row(
                Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    "File ready: ${file.name}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                IconButton(onClick = onClearFile, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear picked file")
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Searching for devices on your network…",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PeerCard(
    peer: Peer,
    isPaired: Boolean,
    canSend: Boolean,
    onPairClick: () -> Unit,
    onSendClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(peer.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${peer.host}:${peer.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isPaired) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Done,
                        contentDescription = "Paired",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    FilledIconButton(onClick = onSendClick, enabled = canSend) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            } else {
                FilledTonalButton(onClick = onPairClick) {
                    Text("Pair")
                }
            }
        }
    }
}

@Composable
private fun PinDialog(pin: String?, onGenerate: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your pairing PIN") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Share this with a device you want to pair — they'll type it in to trust you.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (pin != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(pin, style = MaterialTheme.typography.displaySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onGenerate) { Text(if (pin == null) "Generate" else "Regenerate") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}

@Composable
private fun PairDialog(peerName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pair with $peerName") },
        text = {
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it },
                label = { Text("PIN shown on that device") },
                singleLine = true,
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(pin) }, enabled = pin.isNotBlank()) { Text("Pair") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
