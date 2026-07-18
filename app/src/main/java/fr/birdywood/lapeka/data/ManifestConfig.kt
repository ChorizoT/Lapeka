package fr.birdywood.lapeka.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Minimal persisted config — just the manifest endpoint URL. Kept as plain
 * SharedPreferences rather than DataStore to avoid an extra dependency for
 * a single string value.
 */
class ManifestConfig(val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("lapeka_config", Context.MODE_PRIVATE)

    var manifestUrl: String
        get() = prefs.getString(KEY_MANIFEST_URL, "https://lapeka.labs.birdywood.fr/data") ?: ""
        set(value) = prefs.edit().putString(KEY_MANIFEST_URL, value).apply()

    val hasManifestUrl: Boolean
        get() = manifestUrl.isNotBlank()

    companion object {
        private const val KEY_MANIFEST_URL = "manifest_url"
    }
}
