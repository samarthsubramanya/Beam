package com.beam.app

/** Thin key-value persistence, backed by the same platform-native store as [persistentDeviceId]. */
expect fun prefsGetString(key: String): String?
expect fun prefsPutString(key: String, value: String)
expect fun prefsGetBoolean(key: String, default: Boolean): Boolean
expect fun prefsPutBoolean(key: String, value: Boolean)
