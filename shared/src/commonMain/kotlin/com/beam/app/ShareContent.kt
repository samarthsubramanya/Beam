package com.beam.app

import com.beam.app.transport.PlatformFile

/** What the OS share sheet handed Beam — either plain text/a URL, or a file (image, PDF, etc). */
sealed interface ShareContent {
    data class Text(val text: String) : ShareContent
    data class File(val file: PlatformFile) : ShareContent
}
