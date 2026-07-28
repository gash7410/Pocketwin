# Third-party native binaries

The following prebuilt binaries live under
`app/src/main/jniLibs/arm64-v8a/` (packaged as `.so` because that's the only
location Android's package manager installs as executable at install time —
see README.md, "The Android W^X constraint"). None of these are original code
from this project; they are unmodified upstream builds.

| File | Actual binary | Version | Source | License |
|---|---|---|---|---|
| `libproot.so` | `proot` (aarch64) | 5.1.0 | [skirsten/proot-portable-android-binaries](https://skirsten.github.io/proot-portable-android-binaries/aarch64/proot) (Termux proot package build) | GPL-2.0-or-later — [proot-me/proot](https://github.com/proot-me/proot) |
| `libbox64.so` | `box64` (aarch64) | bundled in Box64Droid `stable` rootfs, built 2024-04-11 | Extracted from [Ilya114/Box64Droid](https://github.com/Ilya114/Box64Droid) release asset `box64droid-rootfs-chroot.tar.xz` (`usr/local/bin/box64`) | MIT — [ptitSeb/box64](https://github.com/ptitSeb/box64) |
| `libbox86.so` | `box86` (armhf) | bundled in Box64Droid `stable` rootfs, built 2024-04-11 | Extracted from the same archive (`usr/local/bin/box86`) | MIT — [ptitSeb/box86](https://github.com/ptitSeb/box86) |

Since this repository is public, the GPL-2.0 source-availability obligation
for `proot` is satisfied by proot's own public upstream repository above
(no modifications were made to it).

Both `box64` and `box86` were verified to have generic, non-hardcoded
`.interp` paths (`/lib/ld-linux-aarch64.so.1` and `/lib/ld-linux-armhf.so.3`
respectively) before being added here — i.e. they're portable builds meant
to run inside an arbitrary chroot/proot rootfs, not tied to another app's
package name.
