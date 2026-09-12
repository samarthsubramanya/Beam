package com.beam.app

/**
 * A stable id/name for this device, generated once and persisted — NOT regenerated per launch.
 * A fresh random id every launch broke pairing across restarts (the peer's stored paired-id would
 * never match again) and left stale mDNS ghost entries piling up on discovering devices.
 */
expect fun persistentDeviceId(): String
