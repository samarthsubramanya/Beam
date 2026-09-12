package com.beam.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    // TODO: revisit a menu-bar Tray for persistence later — it broke window creation last attempt
    // (Tray present -> Window never painted, even after fixing init-order; needs more investigation).
    BeamCore.ensureStarted()
    // Also covers Cmd+Q / SIGTERM, which skip onCloseRequest — kills+cold-restarts otherwise still
    // leave a stale mDNS registration behind for other devices to see until it expires.
    Runtime.getRuntime().addShutdownHook(Thread { BeamCore.stop() })

    Window(
        onCloseRequest = {
            BeamCore.stop()
            exitApplication()
        },
        title = "Beam",
    ) {
        App()
    }
}
