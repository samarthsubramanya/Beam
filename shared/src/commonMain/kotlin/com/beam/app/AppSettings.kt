package com.beam.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private const val KEY_THEME_MODE = "theme_mode"
private const val KEY_DOWNLOAD_PATH = "download_path"
private const val KEY_LAUNCH_ON_LOGIN = "launch_on_login"

/**
 * User-configurable app settings, persisted per-platform and exposed as [StateFlow]s so the UI
 * (theme, settings screen) reacts immediately when one changes.
 */
object AppSettings {
    private val _themeMode = MutableStateFlow(
        runCatching { ThemeMode.valueOf(prefsGetString(KEY_THEME_MODE) ?: "") }.getOrDefault(ThemeMode.SYSTEM)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefsPutString(KEY_THEME_MODE, mode.name)
    }

    /**
     * Where received files are saved. Desktop: an absolute directory path. Android/iOS: a
     * subfolder name under the platform's fixed Downloads/Documents location (no arbitrary
     * paths there without Storage Access Framework / a security-scoped bookmark).
     */
    private val _downloadPath = MutableStateFlow(prefsGetString(KEY_DOWNLOAD_PATH))
    val downloadPath: StateFlow<String?> = _downloadPath.asStateFlow()

    fun setDownloadPath(path: String?) {
        _downloadPath.value = path
        prefsPutString(KEY_DOWNLOAD_PATH, path.orEmpty())
    }

    private val _launchOnLogin = MutableStateFlow(prefsGetBoolean(KEY_LAUNCH_ON_LOGIN, false))
    val launchOnLogin: StateFlow<Boolean> = _launchOnLogin.asStateFlow()

    fun setLaunchOnLogin(enabled: Boolean) {
        _launchOnLogin.value = enabled
        prefsPutBoolean(KEY_LAUNCH_ON_LOGIN, enabled)
        applyLaunchOnLogin(enabled)
    }
}

/** True only where the OS has a concept of per-app "start at login" Beam can register with (desktop). */
expect fun isLaunchOnLoginSupported(): Boolean

/** Registers/unregisters Beam to start at OS login. No-op where [isLaunchOnLoginSupported] is false. */
expect fun applyLaunchOnLogin(enabled: Boolean)

/** True only where a real folder picker is available (desktop). Mobile uses a subfolder-name field instead. */
expect fun isDownloadDirectoryPickerSupported(): Boolean

/** Opens a native folder picker and returns the chosen absolute path, or null if cancelled/unsupported. */
expect fun pickDownloadDirectory(): String?
