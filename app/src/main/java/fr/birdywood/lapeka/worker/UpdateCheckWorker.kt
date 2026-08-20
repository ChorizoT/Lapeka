package fr.birdywood.lapeka.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import fr.birdywood.lapeka.MainActivity
import fr.birdywood.lapeka.R
import fr.birdywood.lapeka.data.AppStatus
import fr.birdywood.lapeka.data.AppsRepository
import fr.birdywood.lapeka.data.ManifestConfig
import fr.birdywood.lapeka.installer.ApkDownloader
import fr.birdywood.lapeka.installer.SilentInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateCheckWorker(
    val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        Log.d("Lapeka", "CheckWorker - Running update check")
        val manifestConfig = ManifestConfig(applicationContext)
        if (!manifestConfig.hasManifestUrl) return Result.success()

        val repository = AppsRepository(
            packageManager = applicationContext.packageManager,
            manifestConfig = manifestConfig
        )
        /*val downloader = ApkDownloader(applicationContext)
        val installer = SilentInstaller(applicationContext)*/

        return try {
            val apps = repository.fetchTrackedApps()
            val updatable = apps.filter { it.status == AppStatus.UPDATE_AVAILABLE }

            createNotificationChannel(context)
            for (app in updatable) {
                // Create an explicit intent for an Activity in your app.
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent: PendingIntent =
                    PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
                val bitmap = app.remote.iconUrl?.let { downloadBitmap(context, it) }

                val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setContentTitle(app.remote.name)
                    .setContentText(context.getString(R.string.notif_update_text))
                    .setLargeIcon(Icon.createWithBitmap(bitmap))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    // Set the intent that fires when the user taps the notification.
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                notificationManager.notify(NOTIFICATION_ID, builder.build())
                /*val fileName = "${app.remote.id}.apk"
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
                        appName = app.remote.name,
                        isUpdateOfOwnInstall = weAreInstallerOfRecord
                    )
                }*/
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

    fun createNotificationChannel(context: Context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not in the Support Library.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.channel_check_name)
            val descriptionText = context.getString(R.string.channel_check_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system.

            notificationManager.createNotificationChannel(channel)
        }
    }

    private suspend fun downloadBitmap(context: Context, url: String): Bitmap? = withContext(
        Dispatchers.IO) {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false) // Requis pour eviter les erreurs d'affichage dans la notification
            .build()

        val result = loader.execute(request)
        if (result is SuccessResult) {
            (result.drawable as? BitmapDrawable)?.bitmap
        } else {
            null
        }
    }

    companion object {
        const val WORK_NAME = "lapeka_check"
        const val CHANNEL_ID = "lapeka_check_channel"
        const val NOTIFICATION_ID = 2008
    }
}
