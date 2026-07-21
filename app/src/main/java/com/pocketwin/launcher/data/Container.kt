package com.pocketwin.launcher.data

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * One isolated Windows "environment": its own rootfs, its own Wine prefix, its own
 * installed shortcuts. Mirrors how Wine itself separates state via WINEPREFIX, so
 * containers don't share DLL/registry state with each other.
 */
@Serializable
data class Container(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val architecture: ContainerArchitecture = ContainerArchitecture.WIN64,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val displayWidth: Int = 1280,
    val displayHeight: Int = 720,
    val environmentVariables: Map<String, String> = emptyMap(),
    val shortcuts: List<InstalledShortcut> = emptyList(),
) {
    /** Directory name under the app's files/containers/ root. Stable even if the container is renamed. */
    val storageDirName: String get() = id
}

@Serializable
enum class ContainerArchitecture {
    /** 32-bit Windows apps, translated by Box86. */
    WIN32,

    /** 64-bit Windows apps, translated by Box64. Default — covers the vast majority of modern apps. */
    WIN64,
}

@Serializable
data class InstalledShortcut(
    val displayName: String,
    /** Path to the .exe relative to the container's C:\ drive (i.e. relative to drive_c/). */
    val relativeExePath: String,
    val addedAtEpochMillis: Long = System.currentTimeMillis(),
)
