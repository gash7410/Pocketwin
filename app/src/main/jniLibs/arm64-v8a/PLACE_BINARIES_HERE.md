# Native engine binaries go in this directory

This directory is packaged straight into the APK's native library path
(`app/build.gradle.kts` → `sourceSets["main"].jniLibs.srcDirs`), which is the one
location Android still lets you `exec()` from after the API 29 W^X restriction locks down
execution from files written to app-writable storage at runtime. See `README.md` →
"Native engine binaries" for the full explanation.

Place these files here, named exactly as shown (the `lib*.so` naming is required so the
Android build tooling and package installer treat them as native libraries):

| File | What it is | Where to get it |
|---|---|---|
| `libproot.so` | The `proot` binary, compiled for `arm64-v8a` | Build from the upstream proot source, or use a build from a source you trust and can verify |
| `libbox64.so` | The `box64` binary, compiled for `arm64-v8a` | Build from upstream box64 source, or a trusted prebuilt release |
| `libbox86.so` | *(optional)* `box86`, compiled for `armeabi-v7a`-compatible ARM, for 32-bit Windows apps | Build from upstream box86 source, or a trusted prebuilt release |

This project intentionally does **not** hardcode download URLs for these — verify
provenance and licensing (proot: GPL-2.0, box86/box64: GPL-2.0/MIT dual bits) yourself
before bundling and redistributing them in a built APK.

Once real binaries are here, delete this placeholder file.
