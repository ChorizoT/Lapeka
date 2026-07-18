package fr.birdywood.lapeka.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.birdywood.lapeka.data.AppStatus
import fr.birdywood.lapeka.data.AppsRepository
import fr.birdywood.lapeka.data.ManifestConfig
import fr.birdywood.lapeka.data.TrackedApp
import fr.birdywood.lapeka.installer.ApkDownloader
import fr.birdywood.lapeka.installer.InstallEvent
import fr.birdywood.lapeka.installer.InstallEventBus
import fr.birdywood.lapeka.installer.SilentInstaller
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppListUiState(
    val isLoading: Boolean = false,
    val apps: List<TrackedApp> = emptyList(),
    val errorMessage: String? = null,
    val manifestUrl: String = ""
)

class ListViewModel(application: Application) : AndroidViewModel(application) {

    private val manifestConfig = ManifestConfig(application)
    private val repository = AppsRepository(
        packageManager = application.packageManager,
        manifestConfig = manifestConfig
    )
    private val downloader = ApkDownloader(application)
    private val installer = SilentInstaller(application)

    private val _uiState = MutableStateFlow(
        AppListUiState(manifestUrl = manifestConfig.manifestUrl)
    )
    val uiState: StateFlow<AppListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            InstallEventBus.events.collect { event ->
                when (event) {
                    is InstallEvent.Success -> refresh()
                    is InstallEvent.Failure -> {
                        _uiState.value =
                            _uiState.value.copy(errorMessage = "Update failed: ${event.message}")
                        _uiState.value.apps.find { it.remote.packageName == installer.lastPackageName }?.let {
                            updateAppStatus(it.remote.id, AppStatus.ERROR)
                        }

                    }
                }
            }
        }
        if (manifestConfig.hasManifestUrl) refresh()
    }

    fun setManifestUrl(url: String) {
        manifestConfig.manifestUrl = url
        _uiState.value = _uiState.value.copy(manifestUrl = url)
        refresh()
    }

    fun refresh() {
        if (!manifestConfig.hasManifestUrl) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val apps = repository.fetchTrackedApps()
                _uiState.value = _uiState.value.copy(isLoading = false, apps = apps)
            } catch (e: Exception) {
                Log.e("Lapeka", e.message?: "Failed to fetch apps")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to fetch apps"
                )
            }
        }
    }

    fun installOrUpdate(app: TrackedApp) {
        viewModelScope.launch {
            updateAppStatus(app.remote.id, AppStatus.DOWNLOADING)

            val result = downloader.download(
                url = app.remote.apkUrl,
                fileName = "${app.remote.id}.apk",
                expectedSha256 = app.remote.sha256
            )

            when (result) {
                is ApkDownloader.Result.Success -> {
                    updateAppStatus(app.remote.id, AppStatus.INSTALLING)
                    val weAreInstallerOfRecord = isInstallerOfRecord(app.remote.packageName)
                    installer.install(
                        apkFile = result.file,
                        packageName = app.remote.packageName,
                        isUpdateOfOwnInstall = weAreInstallerOfRecord
                    )
                }
                is ApkDownloader.Result.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Download failed: ${result.message}"
                    )
                    updateAppStatus(app.remote.id, AppStatus.ERROR)
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
    }

    fun showErrorMessage(msg:String){
        _uiState.value =
            _uiState.value.copy(errorMessage = "Error: ${msg}")
    }
}
