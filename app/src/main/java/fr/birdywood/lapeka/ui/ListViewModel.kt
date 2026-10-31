package fr.birdywood.lapeka.ui

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import fr.birdywood.lapeka.BuildConfig
import fr.birdywood.lapeka.LapekaApp.Companion.auth
import fr.birdywood.lapeka.R
import fr.birdywood.lapeka.data.AppStatus
import fr.birdywood.lapeka.data.AppsRepository
import fr.birdywood.lapeka.data.FeaturedApp
import fr.birdywood.lapeka.data.ManifestConfig
import fr.birdywood.lapeka.data.TrackedApp
import fr.birdywood.lapeka.installer.ApkDownloader
import fr.birdywood.lapeka.installer.InstallEvent
import fr.birdywood.lapeka.installer.InstallEventBus
import fr.birdywood.lapeka.installer.NotificationHelper
import fr.birdywood.lapeka.installer.SilentInstaller
import fr.birdywood.lapeka.utils.AppPreferences
import fr.birdywood.lapeka.utils.FilterSelect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppListUiState(
    val isLoadingApps: Boolean = false,
    val apps: List<TrackedApp> = emptyList(),
    val featuredApps: List<FeaturedApp> = emptyList(),
    val isLoadingFeaturedApps: Boolean = false,
    val errorMessage: String? = null,
    val manifestUrl: String = "",
    val showFeaturedApps: Boolean = true,
    val showSettings: Boolean = false,
    val searchQuery: String = "",
    val filter: FilterSelect = FilterSelect.DEFAULT
)

class ListViewModel(application: Application) : AndroidViewModel(application) {

    private val manifestConfig = ManifestConfig(application)
    private val repository = AppsRepository(
        packageManager = application.packageManager,
        manifestConfig = manifestConfig
    )
    private val downloader = ApkDownloader(application)
    private val installer = SilentInstaller(application)
    private val notificationHelper = NotificationHelper(application)

    private val _uiState = MutableStateFlow(
        AppListUiState(manifestUrl = manifestConfig.manifestUrl, showSettings = manifestConfig.manifestUrl.isBlank())
    )
    val uiState: StateFlow<AppListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            InstallEventBus.events.collect { event ->
                when (event) {
                    is InstallEvent.Success -> lastAppIsInstalled()
                    is InstallEvent.Failure -> {
                        _uiState.value =
                            _uiState.value.copy(errorMessage = "Update failed: ${event.message}")
                        _uiState.value.apps.find { it.remote.packageName == installer.lastPackageName }?.let {
                            updateAppStatus(it.remote.id, AppStatus.Cancelled)
                        }

                    }
                }
            }
        }
        if (manifestConfig.hasManifestUrl && (auth.isLoggedIn || BuildConfig.IS_FOSS)) refresh()
        if (AppPreferences.showFeaturedApps){
            getFeaturedApp()
        }else{
            _uiState.value = _uiState.value.copy(showFeaturedApps = false)
        }

    }

    fun setManifestUrl(url: String) {
        manifestConfig.manifestUrl = url
        _uiState.value = _uiState.value.copy(manifestUrl = url)
        refresh()
    }

    fun refresh(forceReload: Boolean = false) {
        if (!manifestConfig.hasManifestUrl) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingApps = true, errorMessage = null)
            try {
                val apps = repository.fetchTrackedApps(forceReload)
                _uiState.value = _uiState.value.copy(isLoadingApps = false, apps = apps)
            } catch (e: Exception) {
                Log.e("Lapeka", e.message?: "Failed to fetch apps")
                _uiState.value = _uiState.value.copy(
                    isLoadingApps = false,
                    errorMessage = e.message ?: "Failed to fetch apps"
                )
            }
        }
    }

    fun getFeaturedApp(){
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingFeaturedApps = true, errorMessage = null)
            try {
                val apps = repository.fetchFeaturedApps()
                _uiState.value = _uiState.value.copy(isLoadingFeaturedApps = false, featuredApps = apps)
            } catch (e: Exception) {
                Log.e("Lapeka", e.message?: "Failed to fetch featured apps")
                _uiState.value = _uiState.value.copy(
                    isLoadingFeaturedApps = false,
                    errorMessage = e.message ?: "Failed to fetch featured apps"
                )
            }
        }
    }

    fun installOrUpdate(app: TrackedApp) {
        viewModelScope.launch {
            updateAppStatus(app.remote.id, AppStatus.Downloading(0f))
            notificationHelper.showProgressNotification(app.remote.name, 0)
            var lastProgress = 0;
            val result = downloader.download(
                url = app.remote.apkUrl,
                fileName = "${app.remote.id}.apk",
                expectedSha256 = app.remote.sha256,
                onProgress = { progress ->
                    updateAppStatus(app.remote.id, AppStatus.Downloading(progress.toFloat()/100f))
                    if(progress - lastProgress > 5 || progress == 100) {
                        notificationHelper.showProgressNotification(app.remote.name, progress)
                        lastProgress = progress
                    }
                }
            )

            when (result) {
                is ApkDownloader.Result.Success -> {
                    updateAppStatus(app.remote.id, AppStatus.Installing())
                    notificationHelper.showInstallingNotification(app.remote.name)
                    val weAreInstallerOfRecord = isInstallerOfRecord(app.remote.packageName)
                    installer.install(
                        apkFile = result.file,
                        packageName = app.remote.packageName,
                        appName = app.remote.name,
                          isUpdateOfOwnInstall = weAreInstallerOfRecord
                    )
                }

                is ApkDownloader.Result.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Download failed: ${result.message}"
                    )
                    updateAppStatus(app.remote.id, AppStatus.Failed("Installation", "Failed"))
                    notificationHelper.showErrorNotification(app.remote.name, result.message)
                }
            }
        }
    }

    private fun isInstallerOfRecord(packageName: String): Boolean {
        return try {
            val pm = getApplication<Application>().packageManager
            val installerPackage = if (android.os.Build.VERSION.SDK_INT >= 30) {
                pm.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(packageName)
            }
            installerPackage == getApplication<Application>().packageName
        } catch (e: Exception) {
            false
        }
    }

    private fun updateAppStatus(id: String, status: AppStatus) {
        _uiState.value = _uiState.value.copy(
            apps = _uiState.value.apps.map {
                if (it.remote.id == id) it.copy(status = status) else it
            }
        )
        checkQueueAndInstall()
    }

    fun showErrorMessage(msg:String){
        _uiState.value =
            _uiState.value.copy(errorMessage = "Error: ${msg}")
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun shareLapeka(){
        val downloadUrl = application.getString(R.string.app_homepage)
        val shareMessage = application.getString(R.string.share_message, downloadUrl)
        val shareTitle = application.getString(R.string.share_title)

        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                shareMessage
            )
            putExtra(Intent.EXTRA_TITLE, shareTitle)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(shareIntent)
    }

    fun checkQueueAndInstall(){
        val currentItems = _uiState.value.apps

        val isAnythingInstalling = currentItems.any { it.status is AppStatus.Installing || it.status is AppStatus.Downloading }
        if(isAnythingInstalling) return

        val nextItemToInstall = currentItems.firstOrNull { it.status == AppStatus.Queued }

        if (nextItemToInstall != null){
            updateAppStatus(nextItemToInstall.remote.id, AppStatus.Downloading(-1f))
            installOrUpdate(nextItemToInstall)
        }
    }

    fun addToQueue(app: TrackedApp){
        updateAppStatus(app.remote.id, AppStatus.Queued)
    }

    fun removeToQueue(app: TrackedApp){
        updateAppStatus(app.remote.id, AppStatus.Cancelled)
    }

    fun lastAppIsInstalled(){
        val apps = _uiState.value.apps
        val lastApp = apps.firstOrNull{it.status is AppStatus.Installing}
        lastApp?.let {
            updateAppStatus(it.remote.id, AppStatus.UpToDate)
            _uiState.value = _uiState.value.copy(
                apps = _uiState.value.apps.map { app ->
                    if (app.remote.id == lastApp.remote.id) app.copy(installedVersionCode = lastApp.remote.versionCode, installedVersionName = lastApp.remote.versionName) else app
                })
        }
    }

    fun setFilter(filter: FilterSelect){
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    fun toggleSettings(value: Boolean){
        _uiState.value = _uiState.value.copy(showSettings = value)
    }
}
