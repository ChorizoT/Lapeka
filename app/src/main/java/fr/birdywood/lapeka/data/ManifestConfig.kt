package fr.birdywood.lapeka.data

import android.content.Context
import android.content.SharedPreferences
import fr.birdywood.lapeka.utils.AppPreferences

/**
 * Minimal persisted config — just the manifest endpoint URL. Kept as plain
 * SharedPreferences rather than DataStore to avoid an extra dependency for
 * a single string value.
 */
class ManifestConfig(val context: Context) {

    var manifestUrl: String
        get() = AppPreferences.manifestUrl
        set(value)  { AppPreferences.manifestUrl = value }

    val hasManifestUrl: Boolean
        get() = manifestUrl.isNotBlank()
}
