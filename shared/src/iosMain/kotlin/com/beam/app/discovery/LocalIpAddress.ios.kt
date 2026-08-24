package com.beam.app.discovery

// TODO: getifaddrs isn't exposed in this Kotlin/Native platform-libs set (needs a custom cinterop
// def). Not implemented yet — the QR-pairing display just won't show on iOS, PIN pairing still works.
actual fun localIpAddress(): String? = null
