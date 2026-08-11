package com.beam.app.transport

import com.beam.app.discovery.toByteArray
import com.beam.app.discovery.toNSData
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.writeToFile
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeItem
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberFilePicker(onPicked: (PlatformFile) -> Unit): () -> Unit {
    val delegate = remember {
        object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(
                controller: UIDocumentPickerViewController,
                didPickDocumentsAtURLs: List<*>,
            ) {
                val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: return
                val accessing = url.startAccessingSecurityScopedResource()
                val data = NSData.dataWithContentsOfURL(url)
                if (accessing) url.stopAccessingSecurityScopedResource()
                val name = url.lastPathComponent ?: "file"
                if (data != null) {
                    onPicked(PlatformFile(name, data.length.toLong()) { data.toByteArray() })
                }
            }
        }
    }
    return {
        val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeItem))
        picker.delegate = delegate
        UIApplication.sharedApplication.keyWindow
            ?.rootViewController
            ?.presentViewController(picker, animated = true, completion = null)
    }
}

actual fun saveToDownloads(filename: String, bytes: ByteArray): String {
    val docsDir = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        .first() as String
    val path = "$docsDir/$filename"
    bytes.toNSData().writeToFile(path, atomically = true)
    return path
}
