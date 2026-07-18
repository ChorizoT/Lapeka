package fr.birdywood.lapeka.data

import retrofit2.http.GET
import retrofit2.http.Url

/**
 * Retrofit requires a base URL even when every call overrides it with @Url,
 * so NetworkModule sets a harmless placeholder base and every real call here
 * passes the full manifest endpoint URL explicitly (configurable by the user
 * in Settings, stored via ManifestConfig).
 */
interface AppsApiService {

    @GET
    suspend fun getApps(@Url manifestUrl: String): List<RemoteAppInfo>
}
