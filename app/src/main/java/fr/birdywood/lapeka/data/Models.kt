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

//{
//        "id": "litem2",
//        "name": "Litem2",
//        "description": "Avec son interface épuré et moderne, l'application Litem est de retour sous la version 2.0. Elle rend possible le partage de listes entre amis en toute facilité. <br/> <br/> Nécessite un compte BirdyWood.",
//        "tag": "productivity",
//        "img": "https://www.birdywood.fr/img/Litem2",
//        "thumbail": "https://birdywood.fr/img/background-supermarket",
//        "versions": [
//            {
//                "name": "Litem 2",
//                "version": "2.0",
//                "platform": "Web",
//                "link": "https://litem.birdywood.fr",
//                "downloadable": false
//            },
//            {
//                "name": "Litem 2",
//                "version": "2.0",
//                "platform": "Playstore",
//                "link": "https://play.google.com/store/apps/details?id=com.bw.litem2",
//                "downloadable": false
//            }
//        ],
//        "default_index_version": 0,
//        "date_update": "2025-10-25"
//    }

@Serializable
@JsonClass(generateAdapter = true)
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
@JsonClass(generateAdapter = true)
data class VersionApp(
    val name: String,
    val version: String,
    val platform: String,
    val link: String,
    val downloadable: Boolean
)