package com.beam.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.beam.app.transport.platformFileFromUri

class MainActivity : ComponentActivity() {
    private var shareContentState = mutableStateOf<ShareContent?>(null)
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        shareContentState.value = extractShareContent(intent)
        ensureNotificationPermission()
        setContent {
            val shareContent by shareContentState
            App(shareContent = shareContent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        shareContentState.value = extractShareContent(intent)
    }

    override fun onResume() {
        super.onResume()
        ContextCompat.startForegroundService(this, Intent(this, BeamConnectionService::class.java))
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun extractShareContent(intent: Intent?): ShareContent? {
        if (intent?.action != Intent.ACTION_SEND) return null
        val type = intent.type ?: return null
        return if (type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { ShareContent.Text(it) }
        } else {
            @Suppress("DEPRECATION")
            val uri = intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
            uri?.let { ShareContent.File(platformFileFromUri(it)) }
        }
    }
}
