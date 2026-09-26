package com.beam.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.beam.app.cloud.FirebaseConfig
import com.beam.app.cloud.uploadForLink
import com.beam.app.transport.PlatformFile
import com.beam.app.transport.openUrl
import com.beam.app.transport.setClipboardText
import kotlinx.coroutines.launch

/** Beam Pro: upload a file to Firebase and get a link that works from any network. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkShareScreen(
    pickFile: () -> Unit,
    pickedFile: PlatformFile?,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var uploading by remember { mutableStateOf(false) }
    var link by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Share over the internet") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                "Upload a file and share the link with anyone, on any network. Works without pairing.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            if (!FirebaseConfig.isConfigured) {
                Text("Link sharing isn't configured in this build (FirebaseConfig).", color = MaterialTheme.colorScheme.error)
                return@Column
            }
            OutlinedButton(onClick = { link = null; failed = false; pickFile() }, modifier = Modifier.fillMaxWidth()) {
                Text(pickedFile?.let { "${it.name} · ${it.sizeBytes / 1024} KB" } ?: "Choose a file")
            }
            Spacer(Modifier.height(12.dp))
            Button(
                enabled = pickedFile != null && !uploading,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val file = pickedFile ?: return@Button
                    uploading = true; failed = false; link = null
                    scope.launch {
                        link = uploadForLink(file)
                        failed = link == null
                        uploading = false
                    }
                },
            ) {
                if (uploading) CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp) else Text("Upload & get link")
            }
            if (failed) {
                Spacer(Modifier.height(12.dp))
                Text("Upload failed. Check your connection and Firebase rules.", color = MaterialTheme.colorScheme.error)
            }
            link?.let { url ->
                Spacer(Modifier.height(16.dp))
                Text(url, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                Spacer(Modifier.height(8.dp))
                Row {
                    Button(onClick = { setClipboardText(url) }) { Text("Copy link") }
                    Spacer(Modifier.padding(4.dp))
                    OutlinedButton(onClick = { openUrl(url) }) { Text("Open") }
                }
            }
        }
    }
}
