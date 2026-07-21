package com.pocketwin.launcher.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketwin.launcher.PocketWinApp
import com.pocketwin.launcher.data.Container
import com.pocketwin.launcher.data.ContainerArchitecture
import com.pocketwin.launcher.data.InstalledShortcut
import com.pocketwin.launcher.engine.ContainerEngine
import com.pocketwin.launcher.engine.EngineComponent
import kotlinx.coroutines.launch
import java.io.File

class PocketWinViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<PocketWinApp>()
    private val containerRepository = app.containerRepository
    private val componentManager = app.componentManager
    private val containerEngine = ContainerEngine(app, containerRepository, componentManager)

    val containers = containerRepository.containers
    val componentStates = componentManager.states

    private var runningProcess: Process? = null

    fun createContainer(name: String, architecture: ContainerArchitecture) {
        viewModelScope.launch {
            containerRepository.create(Container(name = name, architecture = architecture))
        }
    }

    fun deleteContainer(container: Container) {
        viewModelScope.launch { containerRepository.delete(container) }
    }

    fun installComponent(component: EngineComponent) {
        viewModelScope.launch { runCatching { componentManager.install(component) } }
    }

    /**
     * Copies the user-picked .exe (from a SAF [uri], since scoped storage means we can't
     * assume a plain filesystem path) into the container's drive_c/, then records it as a
     * shortcut so it shows up in the container's app list.
     */
    fun importExecutable(container: Container, uri: Uri, displayName: String) {
        viewModelScope.launch {
            val driveC = File(containerRepository.containerStorageDir(container), "prefix/drive_c")
            driveC.mkdirs()
            val destFile = File(driveC, displayName)
            app.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
            val shortcut = InstalledShortcut(displayName = displayName, relativeExePath = displayName)
            containerRepository.update(container.copy(shortcuts = container.shortcuts + shortcut))
        }
    }

    /** Requires both a rootfs and a Wine build to already be installed via [installComponent]. */
    fun run(container: Container, rootfs: EngineComponent, wineBuild: EngineComponent, shortcut: InstalledShortcut) {
        viewModelScope.launch {
            runCatching {
                runningProcess?.destroy()
                runningProcess = containerEngine.launch(container, rootfs, wineBuild, shortcut.relativeExePath)
            }
        }
    }

    fun stopRunning() {
        runningProcess?.destroy()
        runningProcess = null
    }

    fun isComponentInstalled(component: EngineComponent) = componentManager.isInstalled(component)
}
