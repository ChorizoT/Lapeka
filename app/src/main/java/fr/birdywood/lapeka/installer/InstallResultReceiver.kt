package fr.birdywood.lapeka.installer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import java.io.File

class InstallResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SilentInstaller.ACTION_INSTALL_STATUS) return

        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE
        )
        val packageName = intent.getStringExtra(SilentInstaller.EXTRA_PACKAGE_NAME)
        val appName = intent.getStringExtra(SilentInstaller.EXTRA_APP_NAME) ?: ""
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        val filePath = intent.getStringExtra(SilentInstaller.EXTRA_FILE_PATH)

        val notificationHelper = NotificationHelper(context)

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // Only happens on first install, or API < 31, or when we're not
                // the installer of record yet. Must forward the confirmation UI.
                val confirmIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                confirmIntent?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it)
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                Log.i(TAG, "Install/update succeeded for $packageName")
                InstallEventBus.emit(InstallEvent.Success(packageName ?: ""))
                notificationHelper.showSuccessNotification(appName)
                deleteFile(filePath)
            }
            else -> {
                Log.e(TAG, "Install/update failed for $packageName: $message (status=$status)")
                InstallEventBus.emit(InstallEvent.Failure(packageName ?: "", message ?: "Unknown error"))
                notificationHelper.showErrorNotification(appName, message)
                deleteFile(filePath)
            }
        }
    }

    private fun deleteFile(path: String?) {
        path?.let {
            try {
                val file = File(it)
                if (file.exists()) {
                    val deleted = file.delete()
                    Log.d(TAG, "Deleted cache file $it: $deleted")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete file $path", e)
            }
        }
    }

    companion object {
        private const val TAG = "InstallResultReceiver"
    }
}
