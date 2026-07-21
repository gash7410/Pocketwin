package com.pocketwin.launcher.engine

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.io.File

sealed interface ComponentInstallState {
    data object NotInstalled : ComponentInstallState
    data class Downloading(val bytesRead: Long, val totalBytes: Long) : ComponentInstallState
    data object Extracting : ComponentInstallState
    data object Installed : ComponentInstallState
    data class Failed(val message: String) : ComponentInstallState
}

/**
 * Tracks which [EngineComponent]s are installed under the app's engine/ directory and
 * drives the download → verify → extract pipeline. A component is considered installed
 * once its directory contains the `.installed` marker written after successful extraction —
 * that marker (not just directory existence) is what survives a killed-mid-extraction retry.
 */
class ComponentManager(context: Context) {

    private val appContext = context.applicationContext
    private val downloader = ComponentDownloader()
    private val engineRoot = File(appContext.filesDir, "engine")

    private val _states = MutableStateFlow<Map<String, ComponentInstallState>>(emptyMap())
    val states: StateFlow<Map<String, ComponentInstallState>> = _states

    fun installDir(component: EngineComponent): File = File(engineRoot, "${component.kind.name.lowercase()}/${component.id}-${component.version}")

    fun isInstalled(component: EngineComponent): Boolean =
        File(installDir(component), ".installed").exists()

    fun refreshState(component: EngineComponent) {
        _states.update { it + (component.id to if (isInstalled(component)) ComponentInstallState.Installed else ComponentInstallState.NotInstalled) }
    }

    suspend fun install(component: EngineComponent) {
        if (isInstalled(component)) {
            _states.update { it + (component.id to ComponentInstallState.Installed) }
            return
        }
        val destDir = installDir(component)
        val archiveFile = File(appContext.cacheDir, "engine_downloads/${component.id}-${component.version}.archive")
        try {
            downloader.download(component, archiveFile) { bytesRead, totalBytes ->
                _states.update { it + (component.id to ComponentInstallState.Downloading(bytesRead, totalBytes)) }
            }
            _states.update { it + (component.id to ComponentInstallState.Extracting) }
            destDir.deleteRecursively()
            downloader.extract(archiveFile, destDir)
            File(destDir, ".installed").writeText(component.version)
            _states.update { it + (component.id to ComponentInstallState.Installed) }
        } catch (t: Throwable) {
            destDir.deleteRecursively()
            _states.update { it + (component.id to ComponentInstallState.Failed(t.message ?: t::class.simpleName.orEmpty())) }
            throw t
        } finally {
            archiveFile.delete()
        }
    }
}
