package fr.birdywood.lapeka.data

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import fr.birdywood.lapeka.birdyauth.BirdyAuth
import fr.birdywood.lapeka.birdyauth.PreferenceSystem

class AppsRepository(
    private val packageManager: PackageManager,
    private val manifestConfig: ManifestConfig,
    private val apiService: AppsApiService = NetworkModule.appsApiService
) {
    val birdyAuth = BirdyAuth(manifestConfig.context)
    val preferenceSystem = PreferenceSystem(manifestConfig.context)

    suspend fun fetchTrackedApps(): List<TrackedApp> {
        require(manifestConfig.hasManifestUrl) { "Manifest URL is not configured" }

        //val remoteApps = apiService.getApps(manifestConfig.manifestUrl)
        val option = if (preferenceSystem.get("forceReload", false) == true)"?force=true" else ""
        val remoteApps = birdyAuth.request<List<RemoteAppInfo>>("GET", manifestConfig.manifestUrl+option, "",true).getOrNull() ?: emptyList()

        return remoteApps.map { remote ->
            val installed = getInstalledPackageInfo(remote.packageName)
            val installedVersionCode = installed?.let { getVersionCode(it) }

            val status = when {
                installed == null -> AppStatus.NOT_INSTALLED
                installedVersionCode != null && installedVersionCode < remote.versionCode ->
                    AppStatus.UPDATE_AVAILABLE
                else -> AppStatus.UP_TO_DATE
            }

            TrackedApp(
                remote = remote,
                installedVersionCode = installedVersionCode,
                installedVersionName = installed?.versionName,
                status = status
            )
        }

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
