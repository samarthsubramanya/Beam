package com.beam.app

import com.beam.app.transport.transportHttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.serialization.Serializable

const val BEAM_VERSION = "1.0.0"

private const val LATEST_RELEASE_URL = "https://api.github.com/repos/samarthsubramanya/Beam/releases/latest"

@Serializable
private data class GithubRelease(val tag_name: String, val html_url: String)

sealed interface UpdateCheckResult {
    data class Available(val version: String, val url: String) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data object Failed : UpdateCheckResult
}

/** Compares the running version against the latest GitHub release tag for samarthsubramanya/Beam. */
suspend fun checkForUpdate(): UpdateCheckResult = try {
    val release: GithubRelease = transportHttpClient.get(LATEST_RELEASE_URL) {
        header("User-Agent", "Beam-App")
    }.body()
    val latest = release.tag_name.removePrefix("v")
    if (latest != BEAM_VERSION) UpdateCheckResult.Available(latest, release.html_url) else UpdateCheckResult.UpToDate
} catch (e: Exception) {
    UpdateCheckResult.Failed
}
