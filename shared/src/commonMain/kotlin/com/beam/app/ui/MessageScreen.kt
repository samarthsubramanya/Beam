package com.beam.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageScreen(peer: Peer?, onSend: suspend (String) -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(peer?.let { "Message · ${it.name}" } ?: "Message") },
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
        var message by remember { mutableStateOf("") }
        var state by remember { mutableStateOf(SendState.IDLE) }

        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Message") },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val toSend = message
                    state = SendState.SENDING
                    scope.launch {
                        onSend(toSend)
                        message = ""
                        state = SendState.SENT
                        delay(1200)
                        state = SendState.IDLE
                    }
                },
                enabled = message.isNotBlank() && state != SendState.SENDING,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AnimatedContent(
                    targetState = state,
                    transitionSpec = { (scaleIn() togetherWith scaleOut()) },
                    label = "sendButtonState",
                ) { current ->
                    when (current) {
                        SendState.IDLE -> Row {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                            Text("  Send")
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
        }
    }
}

