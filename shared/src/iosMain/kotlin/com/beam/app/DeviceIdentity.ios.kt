package com.beam.app

import kotlin.random.Random
import platform.Foundation.NSUserDefaults

actual fun persistentDeviceId(): String {
    val defaults = NSUserDefaults.standardUserDefaults
    defaults.stringForKey("device_id")?.let { return it }
    val fresh = "Beam-${Random.nextInt(1000, 9999)}"
    defaults.setObject(fresh, "device_id")
    return fresh
}
