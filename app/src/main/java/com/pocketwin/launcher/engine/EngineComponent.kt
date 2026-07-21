package com.pocketwin.launcher.engine

import kotlinx.serialization.Serializable

/**
 * A downloadable *data* payload the engine needs at runtime: a Linux rootfs, or a
 * pre-built Wine tree (wine binaries + DLLs + Mono/Gecko).
 *
 * Deliberately excludes proot/box86/box64 themselves — those are ELF executables and
 * Android's post-API-29 W^X policy blocks execution of arbitrary files written to app
 * storage at runtime. They must instead be bundled at build time under
 * app/src/main/jniLibs/<abi>/lib<name>.so, which the OS installs into nativeLibraryDir
 * (the one writable-at-install-time location that stays executable). See README.md.
 */
@Serializable
data class EngineComponent(
    val id: String,
    val kind: ComponentKind,
    val displayName: String,
    val version: String,
    /** ABI this payload was built for, e.g. "arm64-v8a". Data-only components (rootfs) may be "any". */
    val targetAbi: String,
    val downloadUrl: String,
    val sha256: String,
    val sizeBytes: Long,
)

@Serializable
enum class ComponentKind {
    /** Minimal Linux userland (glibc, /usr, /etc) that proot chroots into. */
    ROOTFS,

    /** A Linux x86/x86_64 Wine build (the actual upstream wine binaries + DLLs), run under Box86/Box64 translation. */
    WINE_BUILD,
}

/**
 * The list of components this build knows how to fetch. Ships empty on purpose: we don't
 * hardcode third-party binary URLs into the app, since (a) we can't vouch for a URL's
 * availability or integrity ahead of time, and (b) redistribution terms differ per
 * component (Wine is LGPL, some rootfs images have their own licenses). Point
 * [manifestUrl] at a manifest you host and trust, or populate [bundled] with entries you've
 * vetted yourself before shipping a release build.
 *
 * Expected manifest JSON shape: a JSON array of [EngineComponent].
 */
object ComponentCatalog {
    const val manifestUrl: String? = null

    val bundled: List<EngineComponent> = emptyList()
}
