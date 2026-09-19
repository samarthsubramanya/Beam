package com.beam.app.transport

import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.beam.app.AppSettings
import com.beam.app.discovery.appContext

/** Shared by the file picker and by MainActivity's ACTION_SEND handling (Share sheet -> Beam with a file). */
fun platformFileFromUri(uri: Uri): PlatformFile {
    val resolver = appContext.contentResolver
    var name = "file"
    var size = 0L
    resolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }
                ?.let { name = cursor.getString(it) }
            cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }
                ?.let { size = cursor.getLong(it) }
        }
    }
    return PlatformFile(name, size) { resolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0) }
}

@Composable
actual fun rememberFilePicker(onPicked: (PlatformFile) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onPicked(platformFileFromUri(it)) }
    }
    return { launcher.launch("*/*") }
}

actual fun saveToDownloads(filename: String, bytes: ByteArray): String {
    val resolver = appContext.contentResolver
    val subfolder = AppSettings.downloadPath.value?.trim('/')?.takeIf { it.isNotBlank() }
    val relativePath = if (subfolder != null) "${Environment.DIRECTORY_DOWNLOADS}/$subfolder" else Environment.DIRECTORY_DOWNLOADS
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
        put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
    }
    val uri: Uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        ?: error("Could not create download entry")
    resolver.openOutputStream(uri)?.use { it.write(bytes) }
    return uri.toString()
}
