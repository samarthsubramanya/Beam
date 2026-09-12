package com.beam.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.application
import beam.shared.generated.resources.Res
import beam.shared.generated.resources.app_icon
import org.jetbrains.compose.resources.painterResource

fun main() = application {
    BeamCore.ensureStarted()
    // Also covers Cmd+Q / SIGTERM, which skip onCloseRequest — kills+cold-restarts otherwise still
    // leave a stale mDNS registration behind for other devices to see until it expires.
    Runtime.getRuntime().addShutdownHook(Thread { BeamCore.stop() })

    var isWindowVisible by remember { mutableStateOf(true) }
    val icon = painterResource(Res.drawable.app_icon)

    Tray(
        icon = icon,
        tooltip = "Beam",
        menu = {
            Item("Show Beam", onClick = { isWindowVisible = true })
            Item("Quit", onClick = {
                BeamCore.stop()
                exitApplication()
            })
        },
    )

    Window(
        // Hide, don't quit: Beam needs to keep discovery/the transport server running in the
        // background so other devices can still find and pair with it while the window is closed.
        onCloseRequest = { isWindowVisible = false },
        visible = isWindowVisible,
        title = "Beam",
        icon = icon,
    ) {
        App()
    }
}
