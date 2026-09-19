package com.beam.app

actual fun isLaunchOnLoginSupported(): Boolean = false
actual fun applyLaunchOnLogin(enabled: Boolean) { /* no-op: Android has no "start at login" concept */ }
actual fun isDownloadDirectoryPickerSupported(): Boolean = false
actual fun pickDownloadDirectory(): String? = null
