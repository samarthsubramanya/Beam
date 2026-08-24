package com.beam.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@Composable
actual fun rememberQrScanner(onScanned: (String) -> Unit): (() -> Unit)? {
    val launcher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let(onScanned)
    }
    return {
        launcher.launch(
            ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                setPrompt("Scan a Beam pairing QR code")
                setBeepEnabled(false)
                setOrientationLocked(false)
            }
        )
    }
}
