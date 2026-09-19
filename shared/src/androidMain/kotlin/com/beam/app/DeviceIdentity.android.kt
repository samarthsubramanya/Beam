package com.beam.app

import android.content.Context
import com.beam.app.discovery.appContext
import kotlin.random.Random

private val prefs by lazy { appContext.getSharedPreferences("beam_prefs", Context.MODE_PRIVATE) }

actual fun persistentDeviceId(): String {
    prefs.getString("device_id", null)?.let { return it }
    val fresh = "Beam-${Random.nextInt(1000, 9999)}"
    prefs.edit().putString("device_id", fresh).apply()
    return fresh
}

actual fun prefsGetString(key: String): String? = prefs.getString(key, null)
actual fun prefsPutString(key: String, value: String) { prefs.edit().putString(key, value).apply() }
actual fun prefsGetBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
actual fun prefsPutBoolean(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }
