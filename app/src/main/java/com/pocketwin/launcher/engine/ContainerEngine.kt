package com.pocketwin.launcher.engine

import android.content.Context
import com.pocketwin.launcher.data.Container
import com.pocketwin.launcher.data.ContainerArchitecture
import com.pocketwin.launcher.data.ContainerRepository
import java.io.File

/**
 * Builds and launches the proot → box86/box64 → wine process chain for a [Container].
 *
 * How this fits together (see README.md "Architecture" for the full picture):
 *  - proot (native/jniLibs binary) gives us a chroot-like view of [rootfsDir] without root.
 *  - Inside that view we bind-mount the app's own nativeLibraryDir (so box64/box86 are
 *    reachable *inside* the chroot, since proot resolves the launch command against the
 *    new root) and the installed Wine build directory.
 *  - box64/box86 (native/jniLibs binaries) translate the x86/x86_64 Wine ELF binaries to
 *    ARM at runtime; they are not recompiled, they run the real upstream Linux Wine build.
 *  - wine then loads and runs the target Windows .exe.
 *
 * GUI output is intentionally out of scope here: rendering a Wine X11 window onto an
 * Android surface needs an embedded X server / Wayland compositor (comparable in scope to
 * Termux-X11), which isn't implemented in this scaffold. Until that lands, this can drive
 * console/headless Windows binaries and diagnostics (e.g. `wine cmd`, `winecfg` logging)
 * but won't show a Windows GUI on screen.
 */
class ContainerEngine(
    private val context: Context,
    private val containerRepository: ContainerRepository,
    private val componentManager: ComponentManager,
) {
    /** Where inside the proot'd rootfs we bind the app's nativeLibraryDir (proot/box64/box86). */
    private val bindMountedNativeLibDir = "/pocketwin-native"

    /** Where inside the proot'd rootfs we bind the installed Wine build. */
    private val bindMountedWineDir = "/pocketwin-wine"

    class MissingComponentException(message: String) : Exception(message)

    /**
     * Pure function building the full proot argv for launching [exeRelativePath] (relative
     * to the container's drive_c/) inside [container]. Kept side-effect free so it's easy
     * to unit test without an actual device/emulator.
     */
    fun buildLaunchArgs(
        container: Container,
        rootfs: EngineComponent,
        wineBuild: EngineComponent,
        exeRelativePath: String,
    ): List<String> {
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val rootfsDir = componentManager.installDir(rootfs)
        val wineDir = componentManager.installDir(wineBuild)
        // exeRelativePath is relative to prefixDir/drive_c/, matching where importExecutable() drops files.
        val prefixDir = File(containerRepository.containerStorageDir(container), "prefix")

        val translator = when (container.architecture) {
            ContainerArchitecture.WIN64 -> "libbox64.so"
            ContainerArchitecture.WIN32 -> "libbox86.so"
        }
        val wineBinary = when (container.architecture) {
            ContainerArchitecture.WIN64 -> "bin/wine64"
            ContainerArchitecture.WIN32 -> "bin/wine"
        }

        return buildList {
            add("$nativeLibDir/libproot.so")
            add("-0") // fake root inside the sandbox; wine/box expect a writable-looking environment
            add("-r"); add(rootfsDir.absolutePath)
            add("-b"); add("/dev")
            add("-b"); add("/proc")
            add("-b"); add("/sys")
            add("-b"); add("$nativeLibDir:$bindMountedNativeLibDir")
            add("-b"); add("${wineDir.absolutePath}:$bindMountedWineDir")
            add("-b"); add("${prefixDir.absolutePath}:/root/.wine")
            add("-w"); add("/root")
            add("$bindMountedNativeLibDir/$translator")
            add("$bindMountedWineDir/$wineBinary")
            add("C:\\${exeRelativePath.replace('/', '\\')}")
        }
    }

    fun buildLaunchEnvironment(container: Container): Map<String, String> = buildMap {
        put("WINEPREFIX", "/root/.wine")
        put("WINEARCH", if (container.architecture == ContainerArchitecture.WIN32) "win32" else "win64")
        put("WINEDEBUG", "-all")
        putAll(container.environmentVariables)
    }

    /**
     * Spawns the process chain, streaming combined stdout/stderr to a log file under the
     * container's directory (`engine.log`) so a run can be diagnosed after the fact from
     * the container detail screen.
     */
    fun launch(
        container: Container,
        rootfs: EngineComponent,
        wineBuild: EngineComponent,
        exeRelativePath: String,
    ): Process {
        if (!componentManager.isInstalled(rootfs)) {
            throw MissingComponentException("Rootfs '${rootfs.displayName}' is not installed for container '${container.name}'.")
        }
        if (!componentManager.isInstalled(wineBuild)) {
            throw MissingComponentException("Wine build '${wineBuild.displayName}' is not installed for container '${container.name}'.")
        }

        val logFile = File(containerRepository.containerStorageDir(container), "engine.log")
        val args = buildLaunchArgs(container, rootfs, wineBuild, exeRelativePath)
        val processBuilder = ProcessBuilder(args)
            .directory(context.filesDir)
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))

        processBuilder.environment().putAll(buildLaunchEnvironment(container))
        return processBuilder.start()
    }
}
