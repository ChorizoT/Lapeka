package fr.birdywood.lapeka.data

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import fr.birdywood.lapeka.birdyauth.BirdyAuth
import java.io.IOException

class AppsRepository(
    private val packageManager: PackageManager,
    private val manifestConfig: ManifestConfig,
    private val apiService: AppsApiService = NetworkModule.appsApiService
) {
    suspend fun fetchTrackedApps(forceReload: Boolean = false): List<TrackedApp> {
        if (!manifestConfig.hasManifestUrl) return emptyList()

        val response = apiService.getApps(manifestConfig.manifestUrl, force = forceReload)
        if (!response.isSuccessful) throw IOException("Manifest fetch failed: HTTP ${response.code()}")

        val remoteApps = response.body() ?: emptyList()
        return remoteApps.map { remote ->
            val installed = getInstalledPackageInfo(remote.packageName)
            val installedVersionCode = installed?.let { getVersionCode(it) }

            val status = when {
                installed == null -> AppStatus.NotInstalled
                installedVersionCode != null && installedVersionCode < remote.versionCode ->
                    AppStatus.UpdateAvailable

                else -> AppStatus.UpToDate
            }

            TrackedApp(
                remote = remote,
                installedVersionCode = installedVersionCode,
                installedVersionName = installed?.versionName,
                status = status
            )
        }

    }

    suspend fun fetchFeaturedApps(): List<FeaturedApp> {
        val response = apiService.getFeaturedApps()
        if (!response.isSuccessful) throw IOException("Featured fetch failed: HTTP ${response.code()}")

        var apps = response.body() ?: emptyList()

        apps = apps.filter { it.img != "" && !it.img.substringBefore('?').substringBefore('#').substringAfterLast('.', "").equals("svg", ignoreCase = true)} //Exclude apps without image and apps with image in SVG
        apps = apps.filter { it.id != "lapeka" } // Remove Lapeka app
        return apps
    }

    private fun getInstalledPackageInfo(packageName: String): PackageInfo? {
        return try {
            packageManager.getPackageInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun getVersionCode(info: PackageInfo): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    }
}
