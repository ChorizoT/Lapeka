package fr.birdywood.lapeka.data

import fr.birdywood.lapeka.LapekaApp.Companion.auth
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    private val json = Json { ignoreUnknownKeys = true }

    // Placeholder base URL: every real request overrides the full path via @Url.
    private const val BASE_URL = "https://vps.birdywood.fr/"

    val appsApiService: AppsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(auth.httpClient)
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType())
            )
            .build()
            .create(AppsApiService::class.java)
    }
}
