# PocketWin

An Android app scaffold for running Windows desktop applications inside sandboxed
"containers," using the same general technique as projects like Winlator: a Linux rootfs,
Wine (the real upstream Linux Wine build, not a rewrite), and Box86/Box64 to translate
x86/x86_64 instructions to ARM at runtime, all sandboxed via `proot`.

**Status: early scaffold, not yet a working emulator.** This gives you a real Android
Studio project — Gradle setup, data layer, download/verification pipeline, the
proot/box/wine launch logic, and a Compose UI — but it does **not** ship the native
`proot`/`box86`/`box64` binaries or a Wine/rootfs manifest, and it does not implement GUI
output (rendering a Wine window onto an Android surface). Those are substantial pieces of
work in their own right; see "What's not done yet" below.

## Why not "compile Wine from scratch for Android"?

Real projects in this space (Winlator and others) don't recompile Wine for
ARM/Android — they run the *standard Linux x86/x86_64 Wine build* through Box86/Box64,
which translate x86 instructions to ARM at the binary level. That's what this scaffold is
built around too. Reinventing Wine, Box86/Box64, or a Vulkan-based DirectX translation
layer from first principles is a multi-year undertaking for a dedicated team, not something
any single project should attempt from zero.

## Architecture

```
Android app (this project)
 └─ proot (jniLibs binary)          — sandboxed chroot into a Linux rootfs, no root needed
     └─ rootfs (downloaded, extracted under app-private storage)
         └─ box64 / box86 (jniLibs binaries, bind-mounted into the rootfs view)
             └─ wine64 / wine (from a downloaded Wine build, bind-mounted in)
                 └─ your .exe (copied into the container's drive_c/)
```

- **Containers** (`data/Container.kt`) are independent Windows environments — separate
  Wine prefix, separate installed apps — the same isolation model Wine itself uses via
  `WINEPREFIX`.
- **Engine components** (`engine/EngineComponent.kt`) are the big *data* downloads: a
  rootfs and a Wine build. `ComponentDownloader` streams them, verifies SHA-256 before
  anything is extracted or trusted, and `ComponentManager` tracks install state.
- **ContainerEngine** (`engine/ContainerEngine.kt`) builds the actual `proot … -- box64
  wine64 C:\app.exe` command line and launches it, logging to a per-container file.

### The Android W^X constraint (why binaries live in `jniLibs/`, not downloaded)

Since Android 10 (API 29), SELinux policy blocks executing files that were written to
app-writable storage at runtime — a file downloaded to `filesDir` can't just be `chmod
+x`'d and exec'd. The one place that stays executable is `nativeLibraryDir`, which the
package manager populates *at install time* from the APK's `jniLibs/<abi>/` directory. That
is why `proot`, `box64`, and `box86` must be bundled into the APK at build time (see
`app/src/main/jniLibs/arm64-v8a/PLACE_BINARIES_HERE.md`) rather than fetched by
`ComponentDownloader` — only the *data* payloads (rootfs, Wine build) go through the
runtime downloader, since they're loaded by box64/wine rather than exec'd directly by
Android.

## What's not done yet

- **GUI output.** Wine renders an X11 window; getting that onto an Android `Surface` needs
  an embedded X server or Wayland compositor — comparable in scope to the separate
  Termux-X11 project. Right now `ContainerEngine` can run headless/console Windows
  binaries and log their output, but there's no on-screen window yet. This is the single
  biggest remaining piece of work.
- **Native binaries.** `libproot.so` / `libbox64.so` / `libbox86.so` are not included —
  drop in binaries you've built or sourced and verified yourself (see the placeholder file
  in `jniLibs/arm64-v8a/`). GPL licensing means redistributing them in a built APK carries
  source-availability obligations.
- **`ComponentCatalog.bundled` is empty on purpose** — this project doesn't hardcode
  third-party download URLs it can't vouch for. Populate it (or point `manifestUrl` at a
  manifest you host) with a rootfs and Wine build you trust; `EngineComponent` requires a
  `sha256` so `ComponentDownloader` still refuses to install anything that doesn't match.
- **32-bit (Box86) path is wired up but untested** — `abiFilters` currently only builds
  `arm64-v8a`; add `armeabi-v7a` once that path is validated.
- No storage permission / SAF-scoped-storage handling beyond the single-file
  `OpenDocument` picker used for importing an `.exe`.
- `engine/ComponentDownloadService.kt` (a foreground service for installs that survive
  the app being backgrounded) exists but isn't wired into the UI yet — `PocketWinViewModel`
  currently runs installs in `viewModelScope`, which won't survive the process dying
  mid-download of a large rootfs/Wine payload. Swap `installComponent()` to call
  `ComponentDownloadService.buildIntent()` + `startForegroundService()` to fix this.

## Distribution note

Play Store policy has historically been unfriendly to apps whose primary purpose is
running another OS's executables (this is part of why Winlator distributes via
GitHub/F-Droid-style sideloading rather than Play). Plan your distribution channel with
that in mind before investing further.

## Building

This environment doesn't have the Android SDK/NDK/JDK installed, so none of this has been
compiled or run yet — treat it as a reviewed starting point, not a verified build. To
actually build it:

1. Open this directory in Android Studio (Koala/2024.1 or newer recommended for AGP 8.5 +
   Kotlin 1.9.24, both pinned in `build.gradle.kts`).
2. Let it sync — it'll pull the Android Gradle Plugin, Kotlin, and Compose BOM from
   Google/Maven Central.
3. Drop real `libproot.so` / `libbox64.so` / (`libbox86.so`) into
   `app/src/main/jniLibs/arm64-v8a/` and delete the placeholder file there.
4. Populate `ComponentCatalog.bundled` in `engine/EngineComponent.kt` with a rootfs and
   Wine build entry (id, version, `targetAbi`, `downloadUrl`, `sha256`, `sizeBytes`).
5. Build/run on a physical arm64 device (an emulator won't help — you need working ARM
   ptrace behavior for `proot`, and there's little point emulating an emulator).

## Project layout

```
app/src/main/java/com/pocketwin/launcher/
  data/       Container model + JSON-backed repository
  engine/     Component catalog, downloader/verifier, proot/box/wine launcher
  ui/         Compose screens, ViewModel, Windows-11-flavored Material3 theme
```
