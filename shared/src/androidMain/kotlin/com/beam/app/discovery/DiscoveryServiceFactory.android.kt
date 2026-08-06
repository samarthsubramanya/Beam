package com.beam.app.discovery

import android.content.Context

/** Set once from Application.onCreate — avoids pulling in Koin just for this. */
lateinit var appContext: Context

actual fun createDiscoveryService(): DiscoveryService = AndroidDiscoveryService(appContext)
