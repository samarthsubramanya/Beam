package com.beam.app

import kotlin.random.Random
import platform.Foundation.NSUserDefaults

private val defaults get() = NSUserDefaults.standardUserDefaults

actual fun persistentDeviceId(): String {
    defaults.stringForKey("device_id")?.let { return it }
    val fresh = "Beam-${Random.nextInt(1000, 9999)}"
    defaults.setObject(fresh, "device_id")
    return fresh
}

actual fun prefsGetString(key: String): String? = defaults.stringForKey(key)
actual fun prefsPutString(key: String, value: String) { defaults.setObject(value, key) }
actual fun prefsGetBoolean(key: String, default: Boolean): Boolean =
    if (defaults.objectForKey(key) == null) default else defaults.boolForKey(key)
actual fun prefsPutBoolean(key: String, value: Boolean) { defaults.setBool(value, key) }
