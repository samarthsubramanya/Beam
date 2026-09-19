package com.beam.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.beam.app.ThemeMode
import com.beam.app.isDownloadDirectoryPickerSupported
import com.beam.app.pickDownloadDirectory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    deviceId: String,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    downloadPath: String?,
    onDownloadPathChange: (String?) -> Unit,
    isPro: Boolean,
    canPurchase: Boolean,
    onUpgrade: () -> Unit,
    onRestorePurchases: () -> Unit,
    launchOnLoginSupported: Boolean,
    launchOnLogin: Boolean,
    onLaunchOnLoginChange: (Boolean) -> Unit,
    onCheckForUpdates: () -> Unit,
    onOpenLicenses: () -> Unit,
    onBack: () -> Unit,
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showDownloadPathDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            SectionLabel("Device")
            ListItem(
                headlineContent = { Text("This device") },
                supportingContent = { Text(deviceId) },
                leadingContent = { Icon(Icons.Filled.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()

            SectionLabel("Beam Pro")
            ListItem(
                headlineContent = { Text(if (isPro) "Beam Pro is active" else "Free plan") },
                supportingContent = {
                    Text(
                        when {
                            isPro && !canPurchase -> "Unlocked by your paired phone"
                            isPro -> "File transfers and unlimited devices"
                            canPurchase -> "Unlock file transfers and unlimited devices"
                            else -> "Get Beam Pro on your phone and keep it paired with this computer"
                        }
                    )
                },
                trailingContent = if (!isPro && canPurchase) {
                    { TextButton(onClick = onUpgrade) { Text("Upgrade") } }
                } else null,
                modifier = Modifier.fillMaxWidth(),
            )
            if (canPurchase) {
                ListItem(
                    headlineContent = { Text("Restore purchases") },
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onRestorePurchases),
                )
            }

            HorizontalDivider()

            SectionLabel("Appearance")
            ListItem(
                headlineContent = { Text("Theme") },
                supportingContent = { Text(themeMode.label()) },
                modifier = Modifier.fillMaxWidth().clickable { showThemeDialog = true },
            )

            HorizontalDivider()

            SectionLabel("Storage")
            ListItem(
                headlineContent = { Text("Download location") },
                supportingContent = { Text(downloadPath?.takeIf { it.isNotBlank() } ?: "Default") },
                modifier = Modifier.fillMaxWidth().clickable {
                    if (isDownloadDirectoryPickerSupported()) {
                        pickDownloadDirectory()?.let(onDownloadPathChange)
                    } else {
                        showDownloadPathDialog = true
                    }
                },
            )

            if (launchOnLoginSupported) {
                HorizontalDivider()
                SectionLabel("Startup")
                ListItem(
                    headlineContent = { Text("Launch on login") },
                    supportingContent = { Text("Start Beam automatically when you sign in") },
                    trailingContent = {
                        Switch(checked = launchOnLogin, onCheckedChange = onLaunchOnLoginChange)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            HorizontalDivider()

            SectionLabel("About")
            ListItem(
                headlineContent = { Text("Check for updates") },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().clickable(onClick = onCheckForUpdates),
            )
            ListItem(
                headlineContent = { Text("Open source licenses") },
                leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenLicenses),
            )
        }
    }

    if (showThemeDialog) {
        ThemeDialog(
            current = themeMode,
            onSelect = { onThemeModeChange(it); showThemeDialog = false },
            onDismiss = { showThemeDialog = false },
        )
    }

    if (showDownloadPathDialog) {
        DownloadPathDialog(
            current = downloadPath,
            onConfirm = { onDownloadPathChange(it); showDownloadPathDialog = false },
            onDismiss = { showDownloadPathDialog = false },
        )
    }
}

@Composable
private fun ThemeDialog(current: ThemeMode, onSelect: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Theme") },
        text = {
            Column {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .selectable(selected = mode == current, onClick = { onSelect(mode) })
                            .padding(vertical = 8.dp),
                    ) {
                        RadioButton(selected = mode == current, onClick = { onSelect(mode) })
                        Text(mode.label(), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun DownloadPathDialog(current: String?, onConfirm: (String?) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(current.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Download subfolder") },
        text = {
            Column {
                Text("Files land in a folder of this name under your device's Downloads.")
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Beam") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(text.trim()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "System default"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
    )
}
