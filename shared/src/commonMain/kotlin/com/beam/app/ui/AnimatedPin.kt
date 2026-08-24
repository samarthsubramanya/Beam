package com.beam.app.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** A 6-digit PIN whose tiles pop in one by one when it first appears. */
@Composable
fun AnimatedPin(pin: String, modifier: Modifier = Modifier) {
    var revealedCount by remember(pin) { mutableStateOf(0) }

    LaunchedEffect(pin) {
        revealedCount = 0
        repeat(pin.length) {
            delay(60)
            revealedCount++
        }
    }

    Row(modifier, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
        pin.forEachIndexed { index, digit ->
            val shown = index < revealedCount
            AnimatedContent(
                targetState = shown,
                transitionSpec = {
                    (scaleIn(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn()) togetherWith fadeOutInstant()
                },
                label = "pinDigit",
            ) { visible ->
                Box(
                    Modifier
                        .size(width = 40.dp, height = 52.dp)
                        .background(
                            if (visible) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            RoundedCornerShape(8.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (visible) {
                        Text(
                            digit.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

private fun fadeOutInstant() = androidx.compose.animation.fadeOut(tween(0))
