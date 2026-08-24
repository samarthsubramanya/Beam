package com.beam.app.pairing

import com.beam.app.discovery.Peer

private const val SCHEME = "beam://pair"

data class PairingQrPayload(val peer: Peer, val pin: String)

/** Deliberately unencoded — id/name/host/pin are always alnum-or-dot by construction in this app. */
fun buildPairingUri(deviceId: String, deviceName: String, host: String, port: Int, pin: String): String =
    "$SCHEME?id=$deviceId&name=$deviceName&host=$host&port=$port&pin=$pin"

fun parsePairingUri(text: String): PairingQrPayload? {
    if (!text.startsWith(SCHEME)) return null
    val params = text.substringAfter('?', "")
        .split('&')
        .filter { it.isNotBlank() }
        .associate { part ->
            val pieces = part.split('=', limit = 2)
            pieces[0] to pieces.getOrElse(1) { "" }
        }
    val id = params["id"] ?: return null
    val name = params["name"] ?: return null
    val host = params["host"] ?: return null
    val port = params["port"]?.toIntOrNull() ?: return null
    val pin = params["pin"] ?: return null
    return PairingQrPayload(Peer(id, name, host, port), pin)
}
