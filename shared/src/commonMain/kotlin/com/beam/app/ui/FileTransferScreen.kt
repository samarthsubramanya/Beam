package com.beam.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.beam.app.discovery.Peer
import com.beam.app.transport.PlatformFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileTransferScreen(
    peer: Peer?,
    pickFile: () -> Unit,
    pickedFile: PlatformFile?,
    onSend: suspend (PlatformFile) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(peer?.let { "Send file · ${it.name}" } ?: "Send file") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (peer == null) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("This device is no longer nearby.", textAlign = TextAlign.Center)
            }
            return@Scaffold
        }

        val scope = rememberCoroutineScope()
        var state by remember { mutableStateOf(SendState.IDLE) }

        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth().clickable(onClick = pickFile),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Filled.AddCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        pickedFile?.name ?: "Tap to choose a file",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    pickedFile?.let {
                        Text(
                            "${it.sizeBytes / 1024} KB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val file = pickedFile ?: return@Button
                    state = SendState.SENDING
                    scope.launch {
                        onSend(file)
                        state = SendState.SENT
                        delay(1200)
                        state = SendState.IDLE
                    }
                },
                enabled = pickedFile != null && state != SendState.SENDING,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AnimatedContent(
                    targetState = state,
                    transitionSpec = { scaleIn() togetherWith scaleOut() },
                    label = "sendFileState",
                ) { current ->
                    when (current) {
                        SendState.IDLE -> Row {
                            Icon(Icons.Filled.Share, contentDescription = null)
                            Text("  Send file")
                        }
                        SendState.SENDING -> CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        SendState.SENT -> Row {
                            Icon(Icons.Filled.Check, contentDescription = null)
                            Text("  Sent")
                        }
                    }
                }
            }

            if (pickedFile != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = pickFile, modifier = Modifier.fillMaxWidth()) {
                    Text("Choose a different file")
                }
            }
        }
    }
}
