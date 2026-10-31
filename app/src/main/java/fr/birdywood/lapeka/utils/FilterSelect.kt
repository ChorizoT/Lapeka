package fr.birdywood.lapeka.utils

import fr.birdywood.lapeka.R
import fr.birdywood.lapeka.data.AppStatus
import fr.birdywood.lapeka.data.TrackedApp

enum class FilterSelect (val idString: Int) {
    DEFAULT(R.string.filter_default),
    INSTALLED_APP(R.string.filter_installed),
    UPDATE_AVAILABLE(R.string.filter_update),
    NOT_INSTALLED(R.string.filter_not_installed)

}

fun List<TrackedApp>.filterApps(filter: FilterSelect): List<TrackedApp> {
    return when (filter) {
        FilterSelect.DEFAULT -> this.sortedByDescending { it.status != AppStatus.NotInstalled }
        //FilterSelect.A_TO_Z -> this.sortedByDescending{ it.remote.name }
        FilterSelect.INSTALLED_APP -> this.filter { it.status == AppStatus.UpToDate }
        FilterSelect.UPDATE_AVAILABLE -> this.filter { it.status == AppStatus.UpdateAvailable }
        FilterSelect.NOT_INSTALLED -> this.filter { it.status == AppStatus.NotInstalled }
    }

}