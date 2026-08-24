package com.beam.app

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.beam.app.db.BeamRepository
import com.beam.app.db.DatabaseDriverFactory
import com.beam.app.db.TransferEntry
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
import com.beam.app.ui.FileTransferScreen
import com.beam.app.ui.HomeScreen
import com.beam.app.ui.LicensesScreen
import com.beam.app.ui.MessageScreen
import com.beam.app.ui.PairingScreen
import com.beam.app.ui.SettingsScreen
import kotlin.random.Random
import kotlinx.coroutines.launch

private const val PORT = 53212
private const val SLIDE_MS = 300

@Composable
fun App() {
    BeamTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            BeamNavHost()
        }
    }
}

@Composable
private fun BeamNavHost() {
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val discovery = remember { createDiscoveryService() }
    val deviceId = remember { "Beam-${Random.nextInt(1000, 9999)}" }
    val repository = remember { BeamRepository(DatabaseDriverFactory()) }
    val pairingManager = remember { PairingManager() }
    val peers by discovery.peers.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var pairedIds by remember { mutableStateOf(setOf<String>()) }
    var myPin by remember { mutableStateOf<String?>(null) }
    var pickedFile by remember { mutableStateOf<PlatformFile?>(null) }
    var selectedPeerId by remember { mutableStateOf<String?>(null) }
    val selectedPeer = peers.firstOrNull { it.id == selectedPeerId }

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
        discovery.start(localDeviceId = deviceId, localDeviceName = deviceId, servicePort = PORT)
        onDispose {
            discovery.stop()
            server.stop()
        }
    }

    LaunchedEffect(Unit) { refreshPaired() }

    val pickFile = rememberFilePicker { pickedFile = it }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) { data -> Snackbar(snackbarData = data) } },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = androidx.compose.ui.Modifier.padding(padding),
            enterTransition = {
                slideInHorizontally(animationSpec = tween(SLIDE_MS), initialOffsetX = { it }) + fadeIn()
            },
            exitTransition = {
                slideOutHorizontally(animationSpec = tween(SLIDE_MS), targetOffsetX = { -it / 4 }) + fadeOut()
            },
            popEnterTransition = {
                slideInHorizontally(animationSpec = tween(SLIDE_MS), initialOffsetX = { -it / 4 }) + fadeIn()
            },
            popExitTransition = {
                slideOutHorizontally(animationSpec = tween(SLIDE_MS), targetOffsetX = { it }) + fadeOut()
            },
        ) {
            composable("home") {
                HomeScreen(
                    peers = peers,
                    pairedIds = pairedIds,
                    deviceId = deviceId,
                    onSelectUnpaired = { peer -> selectedPeerId = peer.id; navController.navigate("pairing") },
                    onOpenMessage = { peer -> selectedPeerId = peer.id; navController.navigate("message") },
                    onOpenFiles = { peer -> selectedPeerId = peer.id; navController.navigate("files") },
                    onOpenSettings = { navController.navigate("settings") },
                )
            }
            composable("pairing") {
                PairingScreen(
                    peer = selectedPeer,
                    myPin = myPin,
                    onGeneratePin = { myPin = pairingManager.generatePin() },
                    onAttemptPair = attempt@{ pin ->
                        val target = selectedPeer ?: return@attempt false
                        val result = pairWith(target, deviceId, deviceId, pin)
                        if (result != null) {
                            repository.addPairedDevice(result.deviceId, result.deviceName, nowMillis())
                            refreshPaired()
                            snackbarHostState.showSnackbar("Paired with ${target.name}")
                            true
                        } else {
                            false
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable("message") {
                MessageScreen(
                    peer = selectedPeer,
                    onSend = send@{ text ->
                        val target = selectedPeer ?: return@send
                        sendText(target, deviceId, deviceId, text)
                        repository.recordHistory(TransferEntry(target.name, "sent", "text", text, nowMillis()))
                        snackbarHostState.showSnackbar("Sent to ${target.name}")
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable("files") {
                FileTransferScreen(
                    peer = selectedPeer,
                    pickFile = pickFile,
                    pickedFile = pickedFile,
                    onSend = send@{ file ->
                        val target = selectedPeer ?: return@send
                        sendFile(target, deviceId, deviceId, file)
                        repository.recordHistory(TransferEntry(target.name, "sent", "file", file.name, nowMillis()))
                        snackbarHostState.showSnackbar("Sent to ${target.name}")
                        pickedFile = null
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable("settings") {
                SettingsScreen(
                    deviceId = deviceId,
                    onOpenLicenses = { navController.navigate("licenses") },
                    onBack = { navController.popBackStack() },
                )
            }
            composable("licenses") {
                LicensesScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
