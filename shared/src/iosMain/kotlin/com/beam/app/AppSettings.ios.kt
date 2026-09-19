package com.beam.app

actual fun isLaunchOnLoginSupported(): Boolean = false
actual fun applyLaunchOnLogin(enabled: Boolean) { /* no-op: iOS apps can't register to launch at "login" */ }
actual fun isDownloadDirectoryPickerSupported(): Boolean = false
actual fun pickDownloadDirectory(): String? = null
