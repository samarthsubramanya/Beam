package com.beam.app.transport

import com.beam.app.db.BeamRepository
import com.beam.app.discovery.Peer
import com.beam.app.pairing.PairingManager
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

@Serializable
data class TextPayload(val senderName: String, val content: String)

data class ReceivedFile(val senderName: String, val filename: String, val bytes: ByteArray)

@Serializable
data class PairRequest(val deviceId: String, val deviceName: String, val pin: String)

@Serializable
data class PairResponse(val deviceId: String, val deviceName: String)

private const val DEVICE_ID_HEADER = "X-Device-Id"
private const val DEVICE_NAME_HEADER = "X-Device-Name"

/**
 * Receives pairing, text, and file requests from other Beam devices on the LAN.
 * One per app process — all routes share one port. /text and /file are gated to
 * already-paired device ids; pair first via /pair.
 */
class TransportServer(
    private val repository: BeamRepository,
    private val pairingManager: PairingManager,
    private val localDeviceId: String,
    private val localDeviceName: String,
    private val onTextReceived: (TextPayload) -> Unit,
    private val onFileReceived: (ReceivedFile) -> Unit,
    private val onPaired: (deviceId: String, deviceName: String) -> Unit,
    private val onUnauthorized: (deviceId: String?) -> Unit,
) {
    private var server: EmbeddedServer<*, *>? = null

    fun start(port: Int) {
        server = embeddedServer(CIO, port = port) {
            install(ServerContentNegotiation) { json() }
            routing {
                post("/pair") {
                    val request = call.receive<PairRequest>()
                    if (pairingManager.verifyAndConsume(request.pin)) {
                        repository.addPairedDevice(request.deviceId, request.deviceName, nowMillis())
                        onPaired(request.deviceId, request.deviceName)
                        call.respond(PairResponse(localDeviceId, localDeviceName))
                    } else {
                        call.respond(HttpStatusCode.Forbidden)
                    }
                }
                post("/text") {
                    if (!requirePaired(call)) return@post
                    onTextReceived(call.receive<TextPayload>())
                    call.respond(HttpStatusCode.OK)
                }
                post("/file") {
                    if (!requirePaired(call)) return@post
                    val filename = call.request.headers["X-Filename"] ?: "unnamed"
                    val sender = call.request.headers[DEVICE_NAME_HEADER] ?: "unknown"
                    onFileReceived(ReceivedFile(sender, filename, call.receive<ByteArray>()))
                    call.respond(HttpStatusCode.OK)
                }
            }
        }.start(wait = false)
    }

    private suspend fun requirePaired(call: ApplicationCall): Boolean {
        val deviceId = call.request.headers[DEVICE_ID_HEADER]
        if (deviceId == null || !repository.isPaired(deviceId)) {
            call.respond(HttpStatusCode.Forbidden)
            onUnauthorized(deviceId)
            return false
        }
        return true
    }

    fun stop() {
        server?.stop(gracePeriodMillis = 200, timeoutMillis = 500)
        server = null
    }
}

internal val transportHttpClient by lazy {
    HttpClient {
        install(ContentNegotiation) {
            // GitHub's release JSON carries many fields we don't model; don't fail on the ones we ignore.
            json(kotlinx.serialization.json.Json { ignoreUnknownKeys = true })
        }
    }
}

fun nowMillis(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()

suspend fun sendText(peer: Peer, localDeviceId: String, senderName: String, content: String) {
    transportHttpClient.post("http://${peer.host}:${peer.port}/text") {
        header(DEVICE_ID_HEADER, localDeviceId)
        contentType(ContentType.Application.Json)
        setBody(TextPayload(senderName, content))
    }
}

/** ponytail: reads the whole file into memory (client and server) — fine under the 25MB free-tier cap, stream if that cap ever lifts. */
suspend fun sendFile(peer: Peer, localDeviceId: String, senderName: String, file: PlatformFile) {
    transportHttpClient.post("http://${peer.host}:${peer.port}/file") {
        header(DEVICE_ID_HEADER, localDeviceId)
        header(DEVICE_NAME_HEADER, senderName)
        header("X-Filename", file.name)
        contentType(ContentType.Application.OctetStream)
        setBody(file.bytes())
    }
}

/** Returns the responder's device id/name on success, so both sides can record each other as trusted. */
suspend fun pairWith(peer: Peer, localDeviceId: String, localDeviceName: String, pin: String): PairResponse? {
    val response: HttpResponse = transportHttpClient.post("http://${peer.host}:${peer.port}/pair") {
        contentType(ContentType.Application.Json)
        setBody(PairRequest(localDeviceId, localDeviceName, pin))
    }
    return if (response.status == HttpStatusCode.OK) response.body() else null
}
