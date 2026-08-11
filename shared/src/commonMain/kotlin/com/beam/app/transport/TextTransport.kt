package com.beam.app.transport

import com.beam.app.discovery.Peer
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
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

/** Receives text and file sends from other Beam devices on the LAN. One per app process — both routes share one port. */
class TransportServer(
    private val onTextReceived: (TextPayload) -> Unit,
    private val onFileReceived: (ReceivedFile) -> Unit,
) {
    private var server: EmbeddedServer<*, *>? = null

    fun start(port: Int) {
        server = embeddedServer(CIO, port = port) {
            install(ServerContentNegotiation) { json() }
            routing {
                post("/text") {
                    onTextReceived(call.receive<TextPayload>())
                    call.respond(HttpStatusCode.OK)
                }
                post("/file") {
                    val filename = call.request.headers["X-Filename"] ?: "unnamed"
                    val sender = call.request.headers["X-Sender"] ?: "unknown"
                    onFileReceived(ReceivedFile(sender, filename, call.receive<ByteArray>()))
                    call.respond(HttpStatusCode.OK)
                }
            }
        }.start(wait = false)
    }

    fun stop() {
        server?.stop(gracePeriodMillis = 200, timeoutMillis = 500)
        server = null
    }
}

internal val transportHttpClient by lazy { HttpClient { install(ContentNegotiation) { json() } } }

suspend fun sendText(peer: Peer, senderName: String, content: String) {
    transportHttpClient.post("http://${peer.host}:${peer.port}/text") {
        contentType(ContentType.Application.Json)
        setBody(TextPayload(senderName, content))
    }
}

/** ponytail: reads the whole file into memory (client and server) — fine under the 25MB free-tier cap, stream if that cap ever lifts. */
suspend fun sendFile(peer: Peer, senderName: String, file: PlatformFile) {
    transportHttpClient.post("http://${peer.host}:${peer.port}/file") {
        header("X-Filename", file.name)
        header("X-Sender", senderName)
        contentType(ContentType.Application.OctetStream)
        setBody(file.bytes())
    }
}
