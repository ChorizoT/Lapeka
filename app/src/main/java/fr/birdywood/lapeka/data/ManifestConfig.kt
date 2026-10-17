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

    private val prefs: SharedPreferences =
        context.getSharedPreferences("lapeka_config", Context.MODE_PRIVATE)

    var manifestUrl: String
        get() = AppPreferences.manifestUrl//prefs.getString(KEY_MANIFEST_URL, "https://lapeka.labs.birdywood.fr/data") ?: ""
        set(value)  { AppPreferences.manifestUrl = value }

    val hasManifestUrl: Boolean
        get() = manifestUrl.isNotBlank()

    companion object {
        private const val KEY_MANIFEST_URL = "manifest_url"
    }
}
