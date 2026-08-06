package com.beam.app.transport

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.beam.app.discovery.appContext

actual fun setClipboardText(text: String) {
    val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Beam", text))
}
