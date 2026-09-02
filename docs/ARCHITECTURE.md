# Architecture

## Product boundary

TuxSpan is a controller and recipe launcher. It does not pretend to be a Linux kernel, redistribute a Termux bootstrap, or embed an X server. This boundary is intentional:

- official runtime projects can update independently;
- TuxSpan avoids relocating prefix-sensitive native binaries;
- the APK stays small and auditable;
- users retain the two explicit Termux consent gates;
- support can focus on orchestration and device guidance.

## Android app

The app is a single native Kotlin module using Jetpack Compose and Material 3.

| Area | Responsibility |
|---|---|
| `model/WorkspaceRecipe.kt` | Immutable, branded blueprint definitions |
| `platform/DeviceProfile.kt` | Android/ABI/RAM/storage/Vulkan inspection and tiering |
| `platform/CompanionStatus.kt` | Detect Termux, Termux:X11, and command permission |
| `workspace/WorkspaceScripts.kt` | Generate pinned, namespaced, reviewable recipes |
| `workspace/WorkspaceRepository.kt` | Remember the selected workspace and setup phase |
| `termux/TermuxBridge.kt` | Send a reviewed command through Termux RUN_COMMAND |
| `termux/CommandResultService.kt` | Receive bounded verification output |
| `ui/` | Original TuxSpan visual system and screens |

## Command path

TuxSpan uses Termux's documented `com.termux.RUN_COMMAND` service. Android requires the user to grant `com.termux.permission.RUN_COMMAND`; Termux separately requires `allow-external-apps=true`. The app cannot silently bypass either gate.

Setup commands run in a visible Termux terminal. Verification runs in the background and returns only a marker confirming that TuxSpan's namespaced launcher and PRoot Distro entry exist.

## Workspace namespace

Every managed guest and launcher is scoped to a blueprint:

- PRoot Distro guest: `tuxspan-<recipe>`
- launcher: `~/.local/bin/tuxspan-<recipe>`

Removal asks for confirmation and targets only those two names. The recipe generator does not use a recursive filesystem delete.

## Linux guests

- Canvas: `debian:12`, XFCE, Firefox ESR, Git, Python.
- Studio: `ubuntu:24.04`, XFCE, Epiphany, Git, Python. Epiphany avoids Ubuntu's Snap-based Firefox package, which is unsuitable in PRoot.
- Spark: `alpine:3.23`, Git, Python, OpenSSH, Nano.

Image tags are pinned to avoid unexpectedly selecting a newer major distribution. PRoot Distro still resolves the architecture-specific OCI image and verifies/downloads content according to its own implementation.

## Desktop launch

The generated launcher creates a per-workspace MIT-MAGIC-COOKIE, starts Termux:X11 on authenticated loopback display `127.0.0.1:1`, starts PulseAudio if available, then enters the guest and runs XFCE's standard `startxfce4` bootstrap as the foreground owner of a `dbus-run-session`. D-Bus and other runtime files use a private directory under the guest home rather than the shared temporary directory. This initializes XFCE's XDG paths while keeping the same D-Bus alive for the desktop's entire lifetime. The guest also sets `NO_AT_BRIDGE=1` because Android PRoot does not provide a usable accessibility bus; this prevents GTK applications from stalling while they wait for one. TuxSpan opens the Termux:X11 Android activity after dispatch.

Reviewed desktop shortcuts live in `/usr/local/share/applications` and are linked into the desktop folder. XFCE therefore treats them as installed applications without requiring per-file GVFS trust metadata, which is not dependable before the desktop D-Bus exists.

GPU behavior belongs to the Termux:X11/Mesa stack and the device. TuxSpan detects a Vulkan feature flag only for guidance; it does not claim or force a specific vendor driver.

## Future standalone mode

A standalone embedded runtime should be a separate, explicitly licensed milestone—not a hidden copy operation. It would require:

1. reproducible source builds for every native library;
2. a package prefix built for TuxSpan's application ID;
3. a machine-readable bootstrap bill of materials;
4. corresponding source and license delivery beside every APK;
5. multi-ABI CI and physical-device testing;
6. a maintained embedded X server integration.

Until those conditions are met, companion mode is the safer distributable architecture.
