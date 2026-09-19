package com.beam.app

import java.util.prefs.Preferences
import kotlin.random.Random

internal val jvmPrefs: Preferences by lazy { Preferences.userRoot().node("com/beam/app") }

actual fun persistentDeviceId(): String {
    jvmPrefs.get("device_id", null)?.let { return it }
    val fresh = "Beam-${Random.nextInt(1000, 9999)}"
    jvmPrefs.put("device_id", fresh)
    return fresh
}

actual fun prefsGetString(key: String): String? = jvmPrefs.get(key, null)
actual fun prefsPutString(key: String, value: String) { jvmPrefs.put(key, value) }
actual fun prefsGetBoolean(key: String, default: Boolean): Boolean = jvmPrefs.getBoolean(key, default)
actual fun prefsPutBoolean(key: String, value: Boolean) { jvmPrefs.putBoolean(key, value) }
