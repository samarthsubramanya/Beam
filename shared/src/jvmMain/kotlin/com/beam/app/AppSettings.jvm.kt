package com.beam.app

import java.io.File
import javax.swing.JFileChooser

actual fun isLaunchOnLoginSupported(): Boolean = true

private val osName: String = System.getProperty("os.name").lowercase()

actual fun applyLaunchOnLogin(enabled: Boolean) {
    when {
        osName.contains("mac") -> applyMacLaunchAgent(enabled)
        osName.contains("win") -> applyWindowsRunKey(enabled)
        else -> applyLinuxAutostart(enabled)
    }
}

/**
 * ponytail: assumes the currently-running JVM's command line is a valid relaunch command. True for
 * a packaged jpackage app (a native launcher); for `./gradlew :desktopApp:run` in dev this instead
 * captures a throwaway dev classpath — fine for testing the toggle, not meant to survive a rebuild.
 */
private fun currentLaunchCommand(): List<String>? =
    ProcessHandle.current().info().commandLine().orElse(null)
        ?.split(" ")
        ?.filter { it.isNotBlank() }
        ?.takeIf { it.isNotEmpty() }

private fun applyMacLaunchAgent(enabled: Boolean) {
    val plist = File(System.getProperty("user.home"), "Library/LaunchAgents/com.beam.app.plist")
    if (!enabled) {
        plist.delete()
        return
    }
    val command = currentLaunchCommand() ?: return
    val argsXml = command.joinToString("\n") { "        <string>${it.xmlEscaped()}</string>" }
    plist.parentFile.mkdirs()
    plist.writeText(
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
        <plist version="1.0">
        <dict>
            <key>Label</key>
            <string>com.beam.app</string>
            <key>ProgramArguments</key>
            <array>
        $argsXml
            </array>
            <key>RunAtLoad</key>
            <true/>
        </dict>
        </plist>
        """.trimIndent()
    )
}

private fun applyWindowsRunKey(enabled: Boolean) {
    val keyPath = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"
    if (enabled) {
        val command = currentLaunchCommand() ?: return
        ProcessBuilder(
            "reg", "add", keyPath, "/v", "Beam", "/t", "REG_SZ", "/d", command.joinToString(" "), "/f",
        ).start().waitFor()
    } else {
        ProcessBuilder("reg", "delete", keyPath, "/v", "Beam", "/f").start().waitFor()
    }
}

private fun applyLinuxAutostart(enabled: Boolean) {
    val desktopFile = File(System.getProperty("user.home"), ".config/autostart/beam.desktop")
    if (!enabled) {
        desktopFile.delete()
        return
    }
    val command = currentLaunchCommand() ?: return
    desktopFile.parentFile.mkdirs()
    desktopFile.writeText(
        """
        [Desktop Entry]
        Type=Application
        Name=Beam
        Exec=${command.joinToString(" ")}
        X-GNOME-Autostart-enabled=true
        """.trimIndent()
    )
}

private fun String.xmlEscaped(): String =
    replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

actual fun isDownloadDirectoryPickerSupported(): Boolean = true

actual fun pickDownloadDirectory(): String? {
    val chooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        dialogTitle = "Choose a download folder"
    }
    val result = chooser.showOpenDialog(null)
    return if (result == JFileChooser.APPROVE_OPTION) chooser.selectedFile?.absolutePath else null
}
