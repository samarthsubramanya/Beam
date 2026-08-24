package com.beam.app.transport

import android.content.Intent
import android.net.Uri
import com.beam.app.discovery.appContext

actual fun openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    appContext.startActivity(intent)
}
