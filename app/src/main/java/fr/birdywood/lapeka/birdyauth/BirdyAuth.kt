package fr.birdywood.lapeka.birdyauth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.browser.customtabs.CustomTabsIntent
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.security.MessageDigest
import java.security.KeyStore
import java.security.SecureRandom
import java.util.concurrent.Executors
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import androidx.core.net.toUri
import kotlin.time.Instant

/**
 * Single entry point for BirdyAuth on Android.
 *
 *   val auth = BirdyAuth(context, serviceId = "6a5a...")
 *
 * PKCE login flow:
 *   auth.login()                   -> custom tab opens ?redirect=...&code=<SHA-256(verifier)>
 *   deeplink lapeka://auth?code=.. -> auth.handleDeepLink(uri)  (sends authCode + verifier, gets the token)
 */
class BirdyAuth(
    context: Context,
    private val serviceId: String,
    private val deepLinkScheme: String,
    private val deepLinkHost: String,
    private val host: String = "https://auth.birdywood.fr",
) {
    private val appContext = context.applicationContext
    private val store = SecureStore(appContext)
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    // ---------- Public state ----------

    private val _account = MutableStateFlow(store.account(json))
    /** Cached account (null = logged out). Call [refresh] to validate it with the server. */
    val account: StateFlow<Account?> = _account.asStateFlow()

    val isLoggedIn: Boolean get() = store.token != null
    val token: String? get() = store.token

    /** Receives auth events (called on the thread that called the suspend function / clearSession). */
    var listener: BirdyAuthListener? = null

    // ---------- Networking ----------

    /** OkHttp client with [BaTokenInterceptor]. Reuse it for your own APIs. */
    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(BaTokenInterceptor(getToken = { store.token }, setToken = { store.token = it }))
        .build()

    /** Retrofit with auth + kotlinx.serialization: auth.retrofit(url).create(Api::class.java) */
    fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl.trimEnd('/') + "/")
        .client(httpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val api: AuthApi = retrofit(host).create(AuthApi::class.java)

    // ---------- Actions: custom tabs ----------

    /** Starts the login flow (PKCE). */
    fun login() = openTab(authUrl(LOGIN_PATH, newChallenge()))

    /** Opens the account creation page. After sign-up the flow behaves like a login. */
    fun openSignUp() = openTab(authUrl(SIGNUP_PATH, newChallenge()))

    /** Opens the account settings page. */
    fun openAccountSettings() = openTab(host.toUri())

    /** Clears the local session and opens the disconnect page to kill the server session. */
    fun logout() {
        clearSession()
        openTab(authUrl(DISCONNECT_PATH, challenge = null))
    }

    /** Clears the local session only (no browser). */
    fun clearSession() {
        val wasLoggedIn = isLoggedIn
        store.clear()
        _account.value = null
        if (wasLoggedIn) listener?.onLoggedOut()
    }

    // ---------- Deeplink ----------

    fun isAuthDeepLink(uri: Uri): Boolean =
        uri.scheme == deepLinkScheme && uri.host == deepLinkHost

    /**
     * Call from your Activity (onCreate / onNewIntent) with intent.data.
     * Exchanges the authorization code + the PKCE verifier for the real token.
     */
    suspend fun handleDeepLink(uri: Uri): Result<Account> = runCatching {
        require(isAuthDeepLink(uri)) { "Not a BirdyAuth deeplink" }
        val authCode = uri.getQueryParameter("code") ?: error("Missing authorization code")
        val verifier = store.pendingVerifier ?: error("No login in progress")
        store.pendingVerifier = null

        val response = api.exchange(ExchangeRequest(codeVerifier = verifier, authorizationCode = authCode))
        store.token = response.token
        try {
            loadAccount()
        } catch (e: Exception) {
            store.token = null // new token rejected (e.g. not authorized): don't keep it
            throw e
        }
    }.onSuccess {
        listener?.onLoggedIn(it)
    }.onFailure {
        reportFailure(it)
    }

    // ---------- Session validation ----------

    /**
     * Re-fetches the account and checks the service authorization.
     * Logs out locally only when the server rejects us (401 / not authorized),
     * not on network errors.
     */
    suspend fun refresh(): Result<Account> {
        if (!isLoggedIn) return Result.failure(IllegalStateException("Not logged in"))
        return runCatching { loadAccount() }.onFailure {
            if (it is NotAuthorizedException || (it is HttpException && it.code() == 401)) clearSession()
            reportFailure(it)
        }
    }

    private fun reportFailure(e: Throwable) {
        if (e is NotAuthorizedException) listener?.onNotAuthorized() else listener?.onError(e)
    }

    private suspend fun loadAccount(): Account {
        val authorized = api.isAuthorized(serviceId).use { it.string().contains("true") }
        if (!authorized) throw NotAuthorizedException()
        val account = api.getUser()
        store.saveAccount(json, account)
        _account.value = account
        return account
    }

    // ---------- Helpers ----------

    /** Generates and stores a verifier, returns its S256 challenge to put in the URL. */
    private fun newChallenge(): String {
        val verifier = base64Url(ByteArray(32).also { SecureRandom().nextBytes(it) })
        store.pendingVerifier = verifier
        return base64Url(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))
    }

    private fun base64Url(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    private fun authUrl(path: String, challenge: String?): Uri =
        Uri.parse(host).buildUpon()
            .path(path)
            .appendQueryParameter("redirect", "$deepLinkScheme://$deepLinkHost")
            .apply { if (challenge != null) appendQueryParameter("code", challenge) }
            .build()

    private fun openTab(uri: Uri) {
        val tab = CustomTabsIntent.Builder().build()
        tab.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        tab.launchUrl(appContext, uri)
    }

    companion object {
        const val COOKIE_NAME = "BA_token"
        // Adjust to your server routes
        private const val LOGIN_PATH = "/"
        private const val SIGNUP_PATH = "/register"
        private const val DISCONNECT_PATH = "/disconnect"
    }
}

class NotAuthorizedException : Exception("Account is not authorized for this service")

/** Override only what you need. */
interface BirdyAuthListener {
    /** Login completed (deeplink exchanged, account loaded and authorized). */
    fun onLoggedIn(account: Account) {}
    /** Local session cleared (logout, 401 or not authorized during refresh). */
    fun onLoggedOut() {}
    /** The account is valid but has no access to this service. */
    fun onNotAuthorized() {}
    /** Any other failure (network, missing code, server error...). */
    fun onError(error: Throwable) {}
}

// ---------- Interceptor ----------

/** Sends the BA_token cookie and keeps the token up to date from Set-Cookie headers. */
class BaTokenInterceptor(
    private val getToken: () -> String?,
    private val setToken: (String) -> Unit,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder().apply {
            getToken()?.let { addHeader("Cookie", "${BirdyAuth.COOKIE_NAME}=$it") }
        }.build()

        val response = chain.proceed(request)

        response.headers("Set-Cookie").forEach { setCookie ->
            if (setCookie.startsWith("${BirdyAuth.COOKIE_NAME}=")) {
                val value = setCookie.substringAfter("=").substringBefore(";")
                if (value.isNotBlank()) setToken(value)
            }
        }
        return response
    }
}

// ---------- Retrofit API ----------

private interface AuthApi {
    @POST("api/token")
    suspend fun exchange(@Body body: ExchangeRequest): TokenResponse

    @GET("api/user")
    suspend fun getUser(): Account

    @GET("api/isAuthorized/{serviceId}")
    suspend fun isAuthorized(@Path("serviceId") serviceId: String): ResponseBody
}

@Serializable
data class ExchangeRequest(val codeVerifier: String, val authorizationCode: String)

@Serializable
data class TokenResponse(val token: String)

// ---------- Models ----------

@Serializable
data class Account(
    val _id: String,
    val username: String,
    val firstname: String,
    val lastname: String,
    val email: String,
    val img: String,
    val authorizations: List<Authorization>,
    val birthdate: Long,
    val creationDate: Instant,
    val lastUpdated: Instant,
)

@Serializable
data class Authorization(
    val _id_service: String,
    val displayName: String,
    val icon: String,
    val url: String,
    val enabled: Boolean,
)

// ---------- Encrypted storage (DataStore + Android Keystore AES-GCM) ----------

private val Context.birdyAuthDataStore by preferencesDataStore(
    name = "birdyauth_secure",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/**
 * DataStore holds the values, each one encrypted with an AES-256-GCM key kept in the Android Keystore.
 * Values are cached in memory (loaded once at creation): reads are instant and the OkHttp interceptor
 * never touches the disk. Writes are persisted in the background, in order.
 */
private class SecureStore(context: Context) {
    private val dataStore = context.applicationContext.birdyAuthDataStore
    private val scope = CoroutineScope(
        SupervisorJob() + Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    )

    @Volatile private var tokenCache: String? = null
    @Volatile private var verifierCache: String? = null
    @Volatile private var accountCache: String? = null

    init {
        // One-time blocking read (a few ms) so isLoggedIn / account are correct from the first frame.
        // A value that can't be decrypted (lost Keystore key) is simply ignored = logged out.
        runBlocking(Dispatchers.IO) {
            val prefs = runCatching { dataStore.data.first() }.getOrNull()
            tokenCache = prefs?.get(KEY_TOKEN)?.let(Crypto::decrypt)
            verifierCache = prefs?.get(KEY_VERIFIER)?.let(Crypto::decrypt)
            accountCache = prefs?.get(KEY_ACCOUNT)?.let(Crypto::decrypt)
        }
    }

    var token: String?
        get() = tokenCache
        set(value) { tokenCache = value; persist(KEY_TOKEN, value) }

    /** PKCE verifier of the login in progress (survives process death while the tab is open). */
    var pendingVerifier: String?
        get() = verifierCache
        set(value) { verifierCache = value; persist(KEY_VERIFIER, value) }

    fun account(json: Json): Account? =
        (accountCache?.let { runCatching { json.decodeFromString(Account.serializer(), it) }.getOrNull() })//?: Account("", "", "", "", "", "", emptyList(), 0, "", "")

    fun saveAccount(json: Json, account: Account) {
        val raw = json.encodeToString(Account.serializer(), account)
        accountCache = raw
        persist(KEY_ACCOUNT, raw)
    }

    fun clear() {
        tokenCache = null
        verifierCache = null
        accountCache = null
        scope.launch { runCatching { dataStore.edit { it.clear() } } }
    }

    private fun persist(key: Preferences.Key<String>, value: String?) {
        scope.launch {
            runCatching {
                dataStore.edit { prefs ->
                    if (value == null) prefs.remove(key) else prefs[key] = Crypto.encrypt(value)
                }
            }
        }
    }

    private companion object {
        val KEY_TOKEN = stringPreferencesKey("token")
        val KEY_ACCOUNT = stringPreferencesKey("account")
        val KEY_VERIFIER = stringPreferencesKey("pending_verifier")
    }
}

private object Crypto {
    private const val ALIAS = "birdyauth_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            generateKey()
        }
    }

    /** Returns base64(iv + ciphertext). The Keystore generates a fresh random IV on every call. */
    fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val bytes = cipher.iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /** Returns null if the value can't be decrypted (corrupted data or lost key). */
    fun decrypt(encoded: String): String? = runCatching {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes, 0, IV_SIZE))
        }
        String(cipher.doFinal(bytes, IV_SIZE, bytes.size - IV_SIZE), Charsets.UTF_8)
    }.getOrNull()
}