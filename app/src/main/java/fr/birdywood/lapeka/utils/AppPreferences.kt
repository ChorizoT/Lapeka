package fr.birdywood.lapeka.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import fr.birdywood.lapeka.R
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


object AppPreferences {
    private const val PREF_NAME = "app_settings"
    private lateinit var preferences: SharedPreferences

    /* String ressources */
    private lateinit var darkmodeString: String

    fun init(context: Context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        darkmodeString = context.getString(R.string.settings_select_system_mode)
    }

    private fun <T> pref(defaultValue: T, key: String? = null) =
        Preference({ preferences }, { defaultValue }, key)
    private fun <T> pref(defaultValueProvider: () -> T, key: String? = null) =
        Preference({ preferences }, defaultValueProvider, key)


    /* Preferences */
    var themeMode: String by pref({ darkmodeString })
    var dynamicTheme: Boolean by pref(false)
    var welcomeDone: Boolean by pref(false)
    var labs: Boolean by pref(true)
    var lastVersionCode: Int by pref(-1)
    var showFeaturedApps: Boolean by pref(true)
    var manifestUrl: String by pref("https://lapeka.labs.birdywood.fr/data")

}


class Preference<T>(
    private val preferencesProvider: () -> SharedPreferences,
    private val defaultValueProvider: () -> T,
    private val customKey: String? = null
) : ReadWriteProperty<Any?, T> {

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val key = customKey ?: property.name
        val preferences = preferencesProvider()
        val defaultValue = defaultValueProvider()

        return when (defaultValue) {
            is String -> (preferences.getString(key, defaultValue) ?: defaultValue) as T
            is Int -> preferences.getInt(key, defaultValue) as T
            is Boolean -> preferences.getBoolean(key, defaultValue) as T
            is Float -> preferences.getFloat(key, defaultValue) as T
            is Long -> preferences.getLong(key, defaultValue) as T
            else -> throw IllegalArgumentException("Type non supporté")
        }
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val key = customKey ?: property.name
        val preferences = preferencesProvider()

        preferences.edit {
            when (value) {
                is String -> putString(key, value)
                is Int -> putInt(key, value)
                is Boolean -> putBoolean(key, value)
                is Float -> putFloat(key, value)
                is Long -> putLong(key, value)
                else -> throw IllegalArgumentException("Type non supporté")
            }
        }
    }
}