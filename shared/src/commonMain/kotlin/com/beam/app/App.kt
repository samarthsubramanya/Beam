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
import com.beam.app.db.TransferEntry
import com.beam.app.discovery.localIpAddress
import com.beam.app.pairing.buildPairingUri
import com.beam.app.pro.BeamPro
import com.beam.app.pro.PaywallContent
import com.beam.app.theme.BeamTheme
import com.beam.app.transport.PlatformFile
import com.beam.app.transport.nowMillis
import com.beam.app.transport.openUrl
import com.beam.app.transport.pairWith
import com.beam.app.transport.rememberFilePicker
import com.beam.app.transport.sendFile
import com.beam.app.transport.sendText
import com.beam.app.ui.FileTransferScreen
import com.beam.app.ui.HomeScreen
import com.beam.app.ui.LicensesScreen
import com.beam.app.ui.MessageScreen
import com.beam.app.ui.PairingScreen
import com.beam.app.ui.SettingsScreen
import com.beam.app.ui.ShareTargetScreen
import kotlinx.coroutines.launch

private const val SLIDE_MS = 300

@Composable
fun App(shareContent: ShareContent? = null) {
    val themeMode by AppSettings.themeMode.collectAsState()
    BeamTheme(themeMode) {
        Surface(color = MaterialTheme.colorScheme.background) {
            BeamNavHost(shareContent)
        }
    }
}

@Composable
private fun BeamNavHost(shareContent: ShareContent?) {
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val peers by BeamCore.discovery.peers.collectAsState()
    val pairedIds by BeamCore.pairedIds.collectAsState()
    val isPro by BeamPro.isPro.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var myPin by remember { mutableStateOf<String?>(null) }
    var pickedFile by remember { mutableStateOf<PlatformFile?>(null) }
    var selectedPeerId by remember { mutableStateOf<String?>(null) }
    val selectedPeer = peers.firstOrNull { it.id == selectedPeerId }
    val myQrData = myPin?.let { pin ->
        localIpAddress()?.let { ip ->
            BeamCore.port?.let { port -> buildPairingUri(BeamCore.deviceId, BeamCore.deviceId, ip, port, pin) }
        }
    }

    LaunchedEffect(Unit) {
        BeamCore.ensureStarted()
    }

    LaunchedEffect(Unit) {
        BeamCore.events.collect { event ->
            when (event) {
                is BeamEvent.Info -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    LaunchedEffect(shareContent) {
        if (shareContent != null) navController.navigate("share")
    }

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
                    deviceId = BeamCore.deviceId,
                    onSelectUnpaired = { peer -> selectedPeerId = peer.id; navController.navigate("pairing") },
                    onOpenMessage = { peer -> selectedPeerId = peer.id; navController.navigate("message") },
                    filesLocked = !isPro,
                    showProPromo = !isPro && !BeamPro.canPurchase,
                    onProPromoClick = { navController.navigate("paywall") },
                    onOpenFiles = { peer ->
                        if (isPro) {
                            selectedPeerId = peer.id
                            navController.navigate("files")
                        } else {
                            navController.navigate("paywall")
                        }
                    },
                    onOpenSettings = { navController.navigate("settings") },
                    onPairNewDevice = { selectedPeerId = null; navController.navigate("pairing") },
                )
            }
            composable("pairing") {
                PairingScreen(
                    peer = selectedPeer,
                    myPin = myPin,
                    myQrData = myQrData,
                    onGeneratePin = { myPin = BeamCore.pairingManager.generatePin() },
                    onAttemptPair = attempt@{ pin ->
                        val target = selectedPeer ?: return@attempt false
                        if (target.id !in pairedIds && !BeamPro.canPairAnother(pairedIds.size)) {
                            navController.navigate("paywall")
                            return@attempt false
                        }
                        val result = pairWith(target, BeamCore.deviceId, BeamCore.deviceId, pin)
                        if (result != null) {
                            BeamCore.repository.addPairedDevice(result.deviceId, result.deviceName, nowMillis())
                            BeamCore.refreshPaired()
                            snackbarHostState.showSnackbar("Paired with ${target.name}")
                            true
                        } else {
                            false
                        }
                    },
                    onQrScanned = scan@{ payload ->
                        if (payload.peer.id !in pairedIds && !BeamPro.canPairAnother(pairedIds.size)) {
                            navController.navigate("paywall")
                            return@scan false
                        }
                        val result = pairWith(payload.peer, BeamCore.deviceId, BeamCore.deviceId, payload.pin)
                        if (result != null) {
                            BeamCore.repository.addPairedDevice(result.deviceId, result.deviceName, nowMillis())
                            BeamCore.refreshPaired()
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
                        sendText(target, BeamCore.deviceId, BeamCore.deviceId, text)
                        BeamCore.repository.recordHistory(TransferEntry(target.name, "sent", "text", text, nowMillis()))
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
                        sendFile(target, BeamCore.deviceId, BeamCore.deviceId, file)
                        BeamCore.repository.recordHistory(TransferEntry(target.name, "sent", "file", file.name, nowMillis()))
                        snackbarHostState.showSnackbar("Sent to ${target.name}")
                        pickedFile = null
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable("paywall") {
                PaywallContent(onDismiss = { navController.popBackStack() })
            }
            composable("settings") {
                val themeMode by AppSettings.themeMode.collectAsState()
                val downloadPath by AppSettings.downloadPath.collectAsState()
                val launchOnLogin by AppSettings.launchOnLogin.collectAsState()
                SettingsScreen(
                    deviceId = BeamCore.deviceId,
                    themeMode = themeMode,
                    onThemeModeChange = AppSettings::setThemeMode,
                    downloadPath = downloadPath,
                    onDownloadPathChange = AppSettings::setDownloadPath,
                    isPro = isPro,
                    canPurchase = BeamPro.canPurchase,
                    onUpgrade = { navController.navigate("paywall") },
                    onRestorePurchases = {
                        BeamPro.restore { active ->
                            scope.launch {
                                snackbarHostState.showSnackbar(if (active) "Beam Pro restored" else "No purchases to restore")
                            }
                        }
                    },
                    launchOnLoginSupported = isLaunchOnLoginSupported(),
                    launchOnLogin = launchOnLogin,
                    onLaunchOnLoginChange = AppSettings::setLaunchOnLogin,
                    onCheckForUpdates = {
                        scope.launch {
                            when (val result = checkForUpdate()) {
                                is UpdateCheckResult.Available -> {
                                    snackbarHostState.showSnackbar("Update ${result.version} available — opening browser")
                                    openUrl(result.url)
                                }
                                UpdateCheckResult.UpToDate -> snackbarHostState.showSnackbar("You're on the latest version")
                                UpdateCheckResult.Failed -> snackbarHostState.showSnackbar("Couldn't check for updates")
                            }
                        }
                    },
                    onOpenLicenses = { navController.navigate("licenses") },
                    onBack = { navController.popBackStack() },
                )
            }
            composable("licenses") {
                LicensesScreen(onBack = { navController.popBackStack() })
            }
            composable("share") {
                val content = shareContent ?: return@composable
                ShareTargetScreen(
                    content = content,
                    pairedPeers = peers.filter { it.id in pairedIds },
                    onSend = send@{ peer ->
                        when (content) {
                            is ShareContent.Text -> {
                                sendText(peer, BeamCore.deviceId, BeamCore.deviceId, content.text)
                                BeamCore.repository.recordHistory(
                                    TransferEntry(peer.name, "sent", "text", content.text, nowMillis())
                                )
                            }
                            is ShareContent.File -> {
                                if (!isPro) {
                                    navController.navigate("paywall")
                                    return@send
                                }
                                sendFile(peer, BeamCore.deviceId, BeamCore.deviceId, content.file)
                                BeamCore.repository.recordHistory(
                                    TransferEntry(peer.name, "sent", "file", content.file.name, nowMillis())
                                )
                            }
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
