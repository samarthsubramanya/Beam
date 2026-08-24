package com.beam.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import qrcode.QRCode

/** Draws the QR matrix directly on a Canvas — no bitmap/image codec needed, works identically on every target. */
@Composable
fun QrCodeView(data: String, modifier: Modifier = Modifier) {
    val matrix = remember(data) { QRCode(data = data).rawData }
    val dark = MaterialTheme.colorScheme.onSurface
    val light = MaterialTheme.colorScheme.surface

    Canvas(modifier.size(200.dp).background(light)) {
        val rows = matrix.size
        if (rows == 0) return@Canvas
        val cols = matrix[0].size
        val cellSize = size.minDimension / maxOf(rows, cols)
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (matrix[r][c].dark) {
                    drawRect(
                        color = dark,
                        topLeft = Offset(c * cellSize, r * cellSize),
                        size = Size(cellSize, cellSize),
                    )
                }
            }
        }
    }
}
