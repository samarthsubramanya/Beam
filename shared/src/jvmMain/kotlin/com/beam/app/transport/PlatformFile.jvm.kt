package com.beam.app.transport

import androidx.compose.runtime.Composable
import com.beam.app.AppSettings
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun rememberFilePicker(onPicked: (PlatformFile) -> Unit): () -> Unit = {
    val dialog = FileDialog(null as Frame?, "Choose a file", FileDialog.LOAD)
    dialog.isVisible = true
    val name = dialog.file
    val dir = dialog.directory
    if (name != null && dir != null) {
        val file = File(dir, name)
        onPicked(PlatformFile(file.name, file.length()) { file.readBytes() })
    }
}

actual fun saveToDownloads(filename: String, bytes: ByteArray): String {
    val configured = AppSettings.downloadPath.value?.takeIf { it.isNotBlank() }
    val downloads = (configured?.let(::File) ?: File(System.getProperty("user.home"), "Downloads"))
        .apply { mkdirs() }
    val out = File(downloads, filename)
    out.writeBytes(bytes)
    return out.absolutePath
}
