package fr.birdywood.lapeka.data

import kotlinx.serialization.Serializable

/**
 * Mirrors one entry of the JSON array returned by Tom's manifest API endpoint.
 */
@Serializable
data class RemoteAppInfo(
    val id: String,
    val name: String,
    val desc: String,
    val packageName: String,
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val sha256: String? = null,
    val changelog: String? = null,
    val iconUrl: String? = null,
    val lastUpdate: Long
)

/**
 * UI/domain-level state combining remote info with what's actually installed on device.
 */
data class TrackedApp(
    val remote: RemoteAppInfo,
    val installedVersionCode: Long?,
    val installedVersionName: String?,
    val status: AppStatus
)

sealed class AppStatus {
    data object NotInstalled : AppStatus()
    data object UpToDate : AppStatus()
    data object UpdateAvailable : AppStatus()

    data object Queued : AppStatus()
    data class Downloading(val progress: Float) : AppStatus()
    data class Installing(val progress: Float = -1f) :
        AppStatus()     // -1 = indeterminate or between 0 and 100% depending on progress

    data class Success(val versionName: String?) : AppStatus()
    data class Failed(val stage: String, val message: String) : AppStatus()
    data object Cancelled : AppStatus()
}

@Serializable
data class FeaturedApp(
    val id: String,
    val name: String,
    val description: String,
    val tag: String,
    val img: String,
    val thumbail: String,
    val versions: List<VersionApp>,
    val default_index_version: Int,
    val date_update: String
)

@Serializable
data class VersionApp(
    val name: String,
    val version: String,
    val platform: String,
    val link: String,
    val downloadable: Boolean
)