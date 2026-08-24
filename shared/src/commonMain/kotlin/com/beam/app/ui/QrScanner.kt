package com.beam.app.ui

import androidx.compose.runtime.Composable

/** Returns a launcher for the platform QR scanner, or null where scanning isn't supported yet. */
@Composable
expect fun rememberQrScanner(onScanned: (String) -> Unit): (() -> Unit)?
