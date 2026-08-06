package com.beam.app.discovery

actual fun createDiscoveryService(): DiscoveryService = IosDiscoveryService()
