package com.beam.app.transport

import com.beam.app.discovery.Peer
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
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

/** Receives text sends from other Beam devices on the LAN. One per app process. */
class TextTransportServer(private val onTextReceived: (TextPayload) -> Unit) {
    private var server: EmbeddedServer<*, *>? = null

    fun start(port: Int) {
        server = embeddedServer(CIO, port = port) {
            install(ServerContentNegotiation) { json() }
            routing {
                post("/text") {
                    val payload = call.receive<TextPayload>()
                    onTextReceived(payload)
                    call.respond(io.ktor.http.HttpStatusCode.OK)
                }
            }
        }.start(wait = false)
    }

    fun stop() {
        server?.stop(gracePeriodMillis = 200, timeoutMillis = 500)
        server = null
    }
}

private val client by lazy { HttpClient { install(ContentNegotiation) { json() } } }

suspend fun sendText(peer: Peer, senderName: String, content: String) {
    client.post("http://${peer.host}:${peer.port}/text") {
        contentType(ContentType.Application.Json)
        setBody(TextPayload(senderName, content))
    }
}
