package com.beam.app

import java.util.prefs.Preferences
import kotlin.random.Random

actual fun persistentDeviceId(): String {
    val prefs = Preferences.userRoot().node("com/beam/app")
    prefs.get("device_id", null)?.let { return it }
    val fresh = "Beam-${Random.nextInt(1000, 9999)}"
    prefs.put("device_id", fresh)
    return fresh
}
