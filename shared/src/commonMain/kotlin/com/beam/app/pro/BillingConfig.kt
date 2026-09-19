package com.beam.app.pro

/**
 * RevenueCat project configuration. SDK API keys are public (safe to ship), but leave them blank
 * until set: billing then stays disabled and Pro features remain locked.
 *
 * Use the project's public SDK keys (Google Play `goog_...`, App Store `appl_...`), or a RevenueCat
 * Test Store key (`test_...`) to try purchases without any store setup.
 */
object BillingConfig {
    // RevenueCat Test Store key (simulated purchases). Beam ships as a hackathon demo outside the app stores, so this stays;
    // swap in real store keys only if it's ever published to Google Play / the App Store.
    const val ANDROID_API_KEY = "test_erwllTgxRaRdBEowekwlArabWrs"
    const val IOS_API_KEY = "test_erwllTgxRaRdBEowekwlArabWrs"

    /** Entitlement identifier configured in the RevenueCat dashboard. */
    const val ENTITLEMENT_ID = "Thingsenz Pro"
}
