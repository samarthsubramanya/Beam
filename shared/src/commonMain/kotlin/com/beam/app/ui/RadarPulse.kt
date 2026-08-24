package com.beam.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Three expanding, fading rings around a solid center dot — the "searching for devices" visual. */
@Composable
fun RadarPulse(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "radar")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "radarProgress",
    )
    val color = MaterialTheme.colorScheme.primary
    Box(modifier.size(160.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(160.dp)) {
            val maxRadius = size.minDimension / 2f
            listOf(0f, 0.33f, 0.66f).forEach { phaseOffset ->
                val phase = (progress + phaseOffset) % 1f
                drawCircle(
                    color = color.copy(alpha = (1f - phase) * 0.5f),
                    radius = maxRadius * phase,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
            drawCircle(color = color, radius = 8.dp.toPx())
        }
    }
}
