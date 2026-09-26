package com.beam.app.cloud

/**
 * Firebase project used for Beam Pro link sharing (see README "Link sharing"). Both values are public
 * client config, safe to ship. Leave blank to disable the feature.
 */
object FirebaseConfig {
    /** Web API key: Firebase console → Project settings → General. */
    const val API_KEY = ""

    /** Storage bucket, e.g. "my-project.firebasestorage.app". */
    const val STORAGE_BUCKET = ""

    val isConfigured get() = API_KEY.isNotBlank() && STORAGE_BUCKET.isNotBlank()
}
