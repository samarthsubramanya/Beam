package com.beam.app

import com.beam.app.db.BeamRepository
import com.beam.app.db.DatabaseDriverFactory
import com.beam.app.db.TransferEntry
import com.beam.app.discovery.DiscoveryService
import com.beam.app.discovery.createDiscoveryService
import com.beam.app.pairing.PairingManager
import com.beam.app.transport.ReceivedFile
import com.beam.app.transport.TextPayload
import com.beam.app.transport.TransportServer
import com.beam.app.transport.nowMillis
import com.beam.app.transport.openUrl
import com.beam.app.transport.saveToDownloads
import com.beam.app.transport.setClipboardText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface BeamEvent {
    data class Info(val text: String) : BeamEvent
}

/**
 * Process-wide singleton owning discovery, the transport server, and persistence — deliberately
 * NOT scoped to a Composable. This lets an Android foreground service keep it alive across
 * Activity recreation/backgrounding, independent of whatever screen happens to be on-screen.
 */
object BeamCore {
    const val PORT = 53212

    val deviceId: String = persistentDeviceId()
    val repository: BeamRepository by lazy { BeamRepository(DatabaseDriverFactory()) }
    val pairingManager: PairingManager by lazy { PairingManager() }
    val discovery: DiscoveryService by lazy { createDiscoveryService() }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var server: TransportServer? = null

    private val _events = MutableSharedFlow<BeamEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<BeamEvent> = _events.asSharedFlow()

    private val _pairedIds = MutableStateFlow<Set<String>>(emptySet())
    val pairedIds: StateFlow<Set<String>> = _pairedIds.asStateFlow()

    val isRunning: Boolean get() = server != null

    fun ensureStarted() {
        if (server != null) return
        val newServer = TransportServer(
            repository = repository,
            pairingManager = pairingManager,
            localDeviceId = deviceId,
            localDeviceName = deviceId,
            onTextReceived = ::handleTextReceived,
            onFileReceived = ::handleFileReceived,
            onPaired = ::handlePaired,
            onUnauthorized = ::handleUnauthorized,
        )
        newServer.start(PORT)
        server = newServer
        discovery.start(localDeviceId = deviceId, localDeviceName = deviceId, servicePort = PORT)
        scope.launch { refreshPaired() }
    }

    fun stop() {
        discovery.stop()
        server?.stop()
        server = null
    }

    suspend fun refreshPaired() {
        _pairedIds.value = repository.pairedDeviceIds()
    }

    private fun handleTextReceived(payload: TextPayload) {
        scope.launch {
            repository.recordHistory(
                TransferEntry(payload.senderName, "received", "text", payload.content, nowMillis())
            )
            val trimmed = payload.content.trim()
            if (isUrl(trimmed)) {
                openUrl(trimmed)
                _events.emit(BeamEvent.Info("Opened link from ${payload.senderName}"))
            } else {
                setClipboardText(payload.content)
                _events.emit(BeamEvent.Info("${payload.senderName}: ${payload.content} (copied to clipboard)"))
            }
        }
    }

    private fun handleFileReceived(file: ReceivedFile) {
        scope.launch {
            val path = saveToDownloads(file.filename, file.bytes)
            repository.recordHistory(
                TransferEntry(file.senderName, "received", "file", file.filename, nowMillis())
            )
            _events.emit(BeamEvent.Info("Received ${file.filename} from ${file.senderName} -> $path"))
        }
    }

    /** Fires on the RECEIVING side of a /pair request — without this, only the initiator ever learns pairing succeeded. */
    private fun handlePaired(deviceId: String, deviceName: String) {
        scope.launch {
            refreshPaired()
            _events.emit(BeamEvent.Info("$deviceName paired with you"))
        }
    }

    private fun handleUnauthorized(deviceId: String?) {
        scope.launch {
            _events.emit(BeamEvent.Info("Blocked a message from an unpaired device (${deviceId ?: "unknown"})"))
        }
    }
}

private fun isUrl(text: String): Boolean = text.startsWith("http://") || text.startsWith("https://")
