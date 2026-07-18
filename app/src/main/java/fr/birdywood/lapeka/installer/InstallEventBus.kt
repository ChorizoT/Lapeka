package fr.birdywood.lapeka.installer

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

sealed class InstallEvent {
    data class Success(val packageName: String) : InstallEvent()
    data class Failure(val packageName: String, val message: String) : InstallEvent()
}

/**
 * PackageInstaller session results land in InstallResultReceiver, which runs
 * outside any ViewModel's lifecycle. This bus bridges that callback to
 * whatever UI is currently observing, without needing a bound service.
 */
object InstallEventBus {
    private val _events = MutableSharedFlow<InstallEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<InstallEvent> = _events

    fun emit(event: InstallEvent) {
        _events.tryEmit(event)
    }
}
