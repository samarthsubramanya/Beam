package com.beam.app.transport

import androidx.compose.runtime.Composable

data class PlatformFile(val name: String, val sizeBytes: Long, val bytes: suspend () -> ByteArray)

/** Returns a launch function — call it (e.g. from a button's onClick) to open the platform file picker. */
@Composable
expect fun rememberFilePicker(onPicked: (PlatformFile) -> Unit): () -> Unit

/** Writes bytes to the platform's user-visible downloads location; returns where it landed. */
expect fun saveToDownloads(filename: String, bytes: ByteArray): String
