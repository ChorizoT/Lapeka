package fr.birdywood.lapeka.worker

import android.content.Context
import android.content.pm.PackageManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import fr.birdywood.lapeka.data.AppStatus
import fr.birdywood.lapeka.data.AppsRepository
import fr.birdywood.lapeka.data.ManifestConfig
import fr.birdywood.lapeka.installer.ApkDownloader
import fr.birdywood.lapeka.installer.SilentInstaller

class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val manifestConfig = ManifestConfig(applicationContext)
        if (!manifestConfig.hasManifestUrl) return Result.success()

        val repository = AppsRepository(
            packageManager = applicationContext.packageManager,
            manifestConfig = manifestConfig
        )
        val downloader = ApkDownloader(applicationContext)
        val installer = SilentInstaller(applicationContext)

        return try {
            val apps = repository.fetchTrackedApps()
            val updatable = apps.filter { it.status == AppStatus.UPDATE_AVAILABLE }

            for (app in updatable) {
                val fileName = "${app.remote.id}.apk"
                val downloadResult = downloader.download(
                    url = app.remote.apkUrl,
                    fileName = fileName,
                    expectedSha256 = app.remote.sha256
                )

                if (downloadResult is ApkDownloader.Result.Success) {
                    val weAreInstallerOfRecord = isInstallerOfRecord(app.remote.packageName)
                    installer.install(
                        apkFile = downloadResult.file,
                        packageName = app.remote.packageName,
                        isUpdateOfOwnInstall = weAreInstallerOfRecord
                    )
                }
                // Failures here are best-effort: worker keeps going and will
                // retry the whole set on the next periodic run.
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun isInstallerOfRecord(packageName: String): Boolean {
        return try {
            val pm = applicationContext.packageManager
            val installerPackage = if (android.os.Build.VERSION.SDK_INT >= 30) {
                pm.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(packageName)
            }
            installerPackage == applicationContext.packageName
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    companion object {
        const val WORK_NAME = "lapeka_check"
    }
}
