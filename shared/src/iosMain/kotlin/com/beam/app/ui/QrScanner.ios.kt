package com.beam.app.ui

import androidx.compose.runtime.Composable

// TODO: AVFoundation-based scanner. Scanning a QR shown on another device isn't implemented yet on iOS —
// this device can still show its own QR (QrCodeView) for another device to scan.
@Composable
actual fun rememberQrScanner(onScanned: (String) -> Unit): (() -> Unit)? = null
