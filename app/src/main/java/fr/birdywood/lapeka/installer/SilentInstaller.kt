package fr.birdywood.lapeka.installer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import java.io.File

/**
 * Installs/updates APKs via the PackageInstaller session API.
 *
 * Silent behavior notes (see conversation context for the full tradeoff table):
 * - On API 31+, if this app is already the installer of record for the target
 *   package (i.e. it performed the initial install), calling
 *   setRequireUserAction(USER_ACTION_NOT_REQUIRED) lets updates proceed with
 *   NO confirmation dialog.
 * - The very first install of any package still shows one system dialog —
 *   Android requires that to establish the initial trust relationship.
 * - On API < 31, every install/update shows the confirmation dialog regardless.
 */
class SilentInstaller(private val context: Context) {
    var lastPackageName: String = ""
    var lastAppName: String = ""

    fun install(apkFile: File, packageName: String, appName: String, isUpdateOfOwnInstall: Boolean) {
        val packageInstaller = context.packageManager.packageInstaller
        lastPackageName = packageName
        lastAppName = appName

        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL
        ).apply {
            setAppPackageName(packageName)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isUpdateOfOwnInstall) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }

        val sessionId = packageInstaller.createSession(params)
        val session = packageInstaller.openSession(sessionId)

        session.use { s ->
            apkFile.inputStream().use { input ->
                s.openWrite("apk", 0, apkFile.length()).use { output ->
                    input.copyTo(output)
                    s.fsync(output)
                }
            }

            val intent = Intent(context, InstallResultReceiver::class.java).apply {
                action = ACTION_INSTALL_STATUS
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putExtra(EXTRA_APP_NAME, appName)
                putExtra(EXTRA_FILE_PATH, apkFile.absolutePath)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_MUTABLE else 0
            val pendingIntent = PendingIntent.getBroadcast(
                context, sessionId, intent, flags
            )

            s.commit(pendingIntent.intentSender)
        }
    }

    companion object {
        const val ACTION_INSTALL_STATUS = "fr.birdywood.lapeka.INSTALL_STATUS"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_FILE_PATH = "extra_file_path"
    }
}
