package fr.birdywood.lapeka.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Retrofit requires a base URL even when every call overrides it with @Url,
 * so NetworkModule sets a harmless placeholder base and every real call here
 * passes the full manifest endpoint URL explicitly (configurable by the user
 * in Settings, stored via ManifestConfig).
 */
interface AppsApiService {

    // ---- Manifest (user-configurable URL) ----
    @GET
    suspend fun getApps(
        @Url manifestUrl: String,
        @Query("force") force: Boolean? = null
    ): Response<List<RemoteAppInfo>>

    // ---- Featured apps (fixed BirdyWood URL, injected by NetworkModule) ----
    @GET("project/projects.json")
    suspend fun getFeaturedApps(): Response<List<FeaturedApp>>
}
