package com.pocketwin.launcher.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Persists the container list as a single JSON file. Containers are metadata only here;
 * the actual rootfs/Wine prefix bytes live under [containerStorageDir] and are managed
 * by the engine layer, not this class.
 */
class ContainerRepository(context: Context) {

    private val appContext = context.applicationContext
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val indexFile = File(appContext.filesDir, "containers.json")

    private val _containers = MutableStateFlow<List<Container>>(loadFromDisk())
    val containers: StateFlow<List<Container>> = _containers

    /** Root directory for a given container's rootfs, Wine prefix, and installed files. */
    fun containerStorageDir(container: Container): File =
        File(appContext.filesDir, "containers/${container.storageDirName}").apply { mkdirs() }

    suspend fun create(container: Container) = withContext(Dispatchers.IO) {
        containerStorageDir(container) // ensure directory exists up front
        _containers.update { it + container }
        persist()
    }

    suspend fun update(container: Container) = withContext(Dispatchers.IO) {
        _containers.update { list -> list.map { if (it.id == container.id) container else it } }
        persist()
    }

    suspend fun delete(container: Container) = withContext(Dispatchers.IO) {
        containerStorageDir(container).deleteRecursively()
        _containers.update { list -> list.filterNot { it.id == container.id } }
        persist()
    }

    fun get(id: String): Container? = _containers.value.find { it.id == id }

    private fun loadFromDisk(): List<Container> {
        if (!indexFile.exists()) return emptyList()
        return runCatching { json.decodeFromString<List<Container>>(indexFile.readText()) }
            .getOrDefault(emptyList())
    }

    private fun persist() {
        indexFile.writeText(json.encodeToString(_containers.value))
    }
}
