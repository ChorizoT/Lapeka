package fr.birdywood.lapeka.birdyauth

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import androidx.core.net.toUri
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.jvm.java


class BirdyAuth (val ctx: Context) {
    companion object {
        const val TAG = "BirdyAuth"
        const val HOST: String = "https://auth.birdywood.fr"
        const val COOKIE_NAME = "BA_token"
        const val SERVICE_ID = "6a5a6f630b4cf396cb99dc93"
    }
    var client: OkHttpClient = OkHttpClient()
    val moshi = Moshi.Builder().build()
    private val accountJsonAdapter = moshi.adapter(Account::class.java)
    private val preferenceSystem = PreferenceSystem(ctx)

    // STATE
    var token: String?
        get() {
            val value = preferenceSystem.get<String>("token", "")
            return if (value == "") null else value
        }
        set(value) {
            if (value == null) {
                preferenceSystem.set("token", "")
            }else{
                preferenceSystem.set("token", value)
            }
        }

    var account: Account?
        get(){
            val json = preferenceSystem.get<String>("account", "")
            if (json.isNullOrBlank()) return null
            return try {
                accountJsonAdapter.fromJson(json)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse account JSON", e)
                null
            }
        }
        set(value){
            if (value == null) {
                preferenceSystem.set("account", "")
            } else {
                preferenceSystem.set("account", accountJsonAdapter.toJson(value))
            }
        }

    suspend fun check(logged:()->Unit= {}, loggingFailed:(reason: String)->Unit = {}) = withContext(Dispatchers.IO) {
        val currentToken = token
        if (currentToken.isNullOrEmpty()) {
            Log.d(TAG, "No token found, redirecting to login")
            loggingFailed("No token found")
            return@withContext
        }

        val url = "$HOST/api/user"

        val request = Request.Builder()
            .url(url)
            .addHeader("Cookie", "$COOKIE_NAME=$currentToken")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.d(TAG, "Check failed with code ${response.code}, redirecting to login")
                    loggingFailed("Check failed with code ${response.code}")
                    return@withContext
                }

                val bodyString = response.body.string()
                Log.d(TAG, bodyString)
                var result = true
                if (SERVICE_ID != "") result = verifAuthorization()
                if (result){
                    account = accountJsonAdapter.fromJson(bodyString)

                    Log.d(TAG, account.toString())
                    logged()
                }else{
                    Log.d(TAG, "Not Authorized")
                    loggingFailed("Not Authorized")
                    return@withContext
                }

            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error during check", e)
            throw e
        }
    }

    suspend fun verifAuthorization(): Boolean = withContext(Dispatchers.IO) {
        val currentToken = token ?: return@withContext false
        val url = "$HOST/api/isAuthorized/$SERVICE_ID"

        val request = Request.Builder()
            .url(url)
            .addHeader("Cookie", "$COOKIE_NAME=$currentToken")
            .build()

        return@withContext try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body.string()
                    // Assuming the API returns a boolean or a JSON with "authorized": true
                    body.contains("true")
                } else {
                    false
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Authorization check failed", e)
            false
        }
    }

    fun redirectToLogin() {
        val deeplink = "lapeka://auth"
        val encodedDeeplink = Uri.encode(deeplink)
        val loginUrl = "$HOST/?redirect=$encodedDeeplink"

        val intent = CustomTabsIntent.Builder().build()
        intent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.launchUrl(ctx, loginUrl.toUri())
    }

    fun editAccount(){
        val url = HOST

        val intent = CustomTabsIntent.Builder().build()
        intent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.launchUrl(ctx, url.toUri())
    }

    suspend inline fun <reified T : Any> request(
        method: String,
        url: String,
        body: String = "",
        credentials: Boolean = true
    ): Result<T?> = withContext(Dispatchers.IO) {

        runCatching {
            Log.d(TAG, "$method $url")
            val requestBody = if (method == "GET" || method == "HEAD") null else body.toRequestBody("application/json".toMediaType())
            val requestBuilder = Request.Builder()
                .url(url)
                .method(method, requestBody)
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "Lapeka Agent")

            if (credentials) {
                token?.let {
                    requestBuilder.addHeader("Cookie", "$COOKIE_NAME=$it")
                }
            }

            val type = object : TypeToken<T>() {}.type

            this@BirdyAuth.client.newCall(requestBuilder.build()).execute().use { response ->
                val responseBody = response.body.string()
                if (!response.isSuccessful) {
                    Log.e(TAG, "HTTP ${response.code} | $responseBody")
                    throw IOException("HTTP ${response.code}")
                }

                Log.d(TAG, "Result: $responseBody")
                moshi.adapter<T>(type).fromJson(responseBody)
            }
        }.onFailure {
            Log.e(TAG, "Request failed: ${it.message}", it)
        }
    }
}

@JsonClass(generateAdapter = true)
data class Account(
    val _id:String,
    val username: String,
    val firstname: String,
    val lastname: String,
    val email: String,
    val img: String,
    val authorizations: List<Authorization>,
    val birthdate: Long,
    val creationDate: String,
    val lastUpdated: String
)

@JsonClass(generateAdapter = true)
data class Authorization(
    val _id_service : String,
    val displayName: String,
    val icon: String,
    val url: String,
    val enabled: Boolean
)


class PreferenceSystem (val ctx: Context) {
    private val preferences: SharedPreferences =
        ctx.getSharedPreferences("Findu", Context.MODE_PRIVATE)

    fun <T> set(key: String, value: T) {
        val editor: SharedPreferences.Editor = preferences.edit()
        when (value) {
            is String -> editor.putString(key, value as String)
            is Int -> editor.putInt(key, value as Int)
            is Boolean -> editor.putBoolean(key, value as Boolean)
            is Float -> editor.putFloat(key, value as Float)
            is Long -> editor.putLong(key, value as Long)
            else -> throw UnsupportedOperationException("Not yet implemented")
        }
        editor.apply()

        /*(ctx as? Activity)?.let { activity ->
            activity.runOnUiThread {
                activity.recreate()
            }
        }*/
    }
    fun <T> get(key: String, defaultValue: T): T {
        return when (defaultValue) {
            is String -> preferences.getString(key, defaultValue)
            is Int -> preferences.getInt(key, defaultValue)
            is Boolean -> preferences.getBoolean(key, defaultValue)
            is Float -> preferences.getFloat(key, defaultValue)
            is Long -> preferences.getLong(key, defaultValue)
            else -> throw UnsupportedOperationException("Not yet implemented: " + (defaultValue?.let { it::class.simpleName } ?: "null"))
        } as T
    }

    fun remove(key: String) {
        val editor: SharedPreferences.Editor = preferences.edit()
        editor.remove(key)
        editor.apply()
    }
    fun clear() {
        val editor: SharedPreferences.Editor = preferences.edit()
        editor.clear()
        editor.apply()
    }
    fun contains(key: String): Boolean {
        return preferences.contains(key)
    }
    fun keys(): Set<String> {
        return preferences.all.keys
    }
}

/**
 * Helper class to capture generic type information at runtime for Moshi.
 * This is necessary because Moshi's [Moshi.adapter] method with a [Class] argument
 * suffers from type erasure (e.g., List<RemoteAppInfo> becomes just List).
 */
@PublishedApi
internal abstract class TypeToken<T> {
    val type: Type
        get() = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0]
}
