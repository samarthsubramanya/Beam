package com.beam.app

import android.content.Context
import com.beam.app.discovery.appContext
import kotlin.random.Random

actual fun persistentDeviceId(): String {
    val prefs = appContext.getSharedPreferences("beam_prefs", Context.MODE_PRIVATE)
    prefs.getString("device_id", null)?.let { return it }
    val fresh = "Beam-${Random.nextInt(1000, 9999)}"
    prefs.edit().putString("device_id", fresh).apply()
    return fresh
}
