package fr.birdywood.lapeka.data

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

/**
 * Mirrors one entry of the JSON array returned by Tom's manifest API endpoint.
 */
@Serializable
@JsonClass(generateAdapter = true)
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

enum class AppStatus {
    NOT_INSTALLED,
    UP_TO_DATE,
    UPDATE_AVAILABLE,
    DOWNLOADING,
    INSTALLING,
    ERROR
}
