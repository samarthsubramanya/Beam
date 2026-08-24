package com.beam.app.ui

import androidx.compose.runtime.Composable

// Desktop pairs by showing its QR for a phone to scan, not the other way around — no webcam scanner here.
@Composable
actual fun rememberQrScanner(onScanned: (String) -> Unit): (() -> Unit)? = null
