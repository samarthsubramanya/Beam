package com.beam.app.cloud

import com.beam.app.transport.PlatformFile
import com.beam.app.transport.nowMillis
import com.beam.app.transport.transportHttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
private data class SignUpResponse(val idToken: String)

@Serializable
private data class UploadResponse(val name: String, val downloadTokens: String)

/**
 * Uploads [file] to Firebase Storage (REST, so it works on every target including desktop) under an
 * anonymous account and returns a shareable download URL that works from any network.
 * Returns null on any failure.
 */
suspend fun uploadForLink(file: PlatformFile): String? = runCatching {
    val auth = transportHttpClient.post("https://identitytoolkit.googleapis.com/v1/accounts:signUp") {
        parameter("key", FirebaseConfig.API_KEY)
        contentType(ContentType.Application.Json)
        setBody("{\"returnSecureToken\":true}")
    }
    if (!auth.status.isSuccess()) return@runCatching null
    val token = auth.body<SignUpResponse>().idToken

    // Random folder makes the link unguessable and stops name collisions.
    val path = "beam/${nowMillis()}-${Random.nextLong().toULong().toString(36)}/${file.name}"
    val upload = transportHttpClient.post("https://firebasestorage.googleapis.com/v0/b/${FirebaseConfig.STORAGE_BUCKET}/o") {
        parameter("name", path)
        header(HttpHeaders.Authorization, "Firebase $token")
        contentType(ContentType.Application.OctetStream)
        setBody(file.bytes())
    }
    if (!upload.status.isSuccess()) return@runCatching null
    val meta = upload.body<UploadResponse>()
    "https://firebasestorage.googleapis.com/v0/b/${FirebaseConfig.STORAGE_BUCKET}/o/" +
        "${meta.name.encodeURLParameter()}?alt=media&token=${meta.downloadTokens}"
}.getOrNull()
