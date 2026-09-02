# TuxSpan

**A brand-neutral Linux workspace launcher for Android.**

TuxSpan turns a compatible Android 8+ phone or tablet into a portable Linux workspace without root. It inspects the device, recommends a workload, shows the exact setup recipe, and launches that recipe through Termux's user-approved command API. Desktop blueprints render through the separately installed Termux:X11 app; lightweight devices can use a terminal-only blueprint.

TuxSpan is an original native Android project. Its product language, interface, Kotlin code, recipe model, device-fit system, and visual identity were created independently. It does not bundle or modify another project's APK, bootstrap, native libraries, artwork, or source code.

TuxSpan provides guided installation, reliable workspace controls, device-aware recommendations, and a mobile-friendly Linux desktop experience. It supports touchpad and direct-touch input, software and physical keyboards, portrait and landscape layouts, Android file access, and optional application packs for everyday, creative, and development work.

## Download

[Download the latest TuxSpan release](https://github.com/techydruid/TuxSpan/releases/latest)

Official APKs are production-signed with TuxSpan's dedicated release certificate. Download updates only from this repository and verify the checksum shown in the matching GitHub release before installing.

## Why TuxSpan is different

- **Capability-first:** recommendations are based on Android version, ABI, memory, storage, and Vulkan feature detection—not Samsung-specific features.
- **Inspectable recipes:** users can read the entire generated command before it runs.
- **Separate, updateable runtime:** official Termux and Termux:X11 apps remain separate. TuxSpan ships no opaque native runtime binaries.
- **Three workload shapes:** Canvas (Debian desktop), Studio (Ubuntu desktop), and Spark (Alpine terminal).
- **Current Android target:** targets API 37 rather than relying on legacy target-SDK behavior.
- **Original interface:** native Jetpack Compose with TuxSpan's own lime/violet “stretch” identity and Workbench / Blueprints / Device Lab information architecture.
- **Mobile-first desktop:** readable adaptive DPI and icon sizing by default, larger window/terminal text and pointer, direct-touch/touchpad modes, hidden-by-default extra keys, and explicit recovery controls that do not conflict with manufacturer gestures.
- **Useful on first launch:** desktop recipes include a browser, LibreOffice, PDF/text/image/media viewers, common media codecs, file and archive tools, search, screenshot, task manager, calculator, fonts, and terminal utilities; creator and larger developer tools stay optional.
- **Reliable sessions:** visible Starting / Running / Stopped / Error state with Launch, Reconnect, Restart, and scoped Stop controls.
- **Optional file bridge:** Android Downloads can be exposed at `~/Android-Downloads` after the user grants Termux storage access.

## Application packs

- **Starter (installed automatically):** the selected web browser, LibreOffice Writer/Calc/Impress, Atril, Mousepad, Ristretto, Parole with common GStreamer codecs, Thunar, Xarchiver, Catfish, Screenshooter, Task Manager, Galculator, archive utilities, spelling support, fonts and core terminal tools.
- **Creator (optional):** GIMP, Inkscape, Audacity, OpenShot, Scribus, FFmpeg, ImageMagick and LAME. These are CPU-capable ARM packages; accelerated 3D suites and capture tools are intentionally excluded because their behavior through Android, PRoot and Termux:X11 is not dependable across phones.
- **Developer (optional):** GCC/build-essential, Clang, CMake, Ninja, GDB, Git, Python/pip/venv, Node.js/npm, OpenJDK 17, Go, Rust/Cargo, SQLite and DB Browser, jq, ShellCheck, OpenSSH, Geany and Meld.

Starter shortcuts stay uncluttered at the desktop level: Firefox, Writer, Photos, Media and Terminal, plus XFCE's Home and Trash icons. Creator and Developer GUI shortcuts are grouped into folders when those optional packs are installed.

## Compatibility

| Device | Expected experience |
|---|---|
| Android 8+, 64-bit, 6 GB+ RAM, 10 GB+ free | Canvas or Studio desktop |
| Android 8+, 64-bit, 4 GB+ RAM, 8 GB+ free | Canvas desktop with modest multitasking |
| Android 8+, supported 32/64-bit ABI, 2 GB+ RAM, 3 GB+ free | Spark terminal |
| Android below 8, unsupported ABI, or severely constrained storage | Not supported |

“Works on any Android phone” cannot honestly mean identical desktop performance on every device. Android version, 32/64-bit userspace, vendor background limits, GPU stack, thermal behavior, and available storage all matter. TuxSpan handles this with compatibility tiers and fallbacks. See [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md).

## Runtime requirements

1. [Termux from F-Droid](https://f-droid.org/packages/com.termux/) for every blueprint.
2. [Termux:X11 nightly](https://github.com/termux/termux-x11/releases/tag/nightly) for Canvas and Studio.
3. TuxSpan's **Run commands in Termux environment** permission.
4. Termux's separate `allow-external-apps=true` consent switch.

TuxSpan explains and links each requirement inside the app. Do not mix Termux and plugin APKs signed by different distribution sources.

## User flow

1. Open **Device lab** and review the capability tier.
2. Pick Canvas, Studio, or Spark under **Blueprints**.
3. Install the required companion apps from their official sources.
4. Grant TuxSpan the Termux command permission.
5. Copy the one-time Termux opt-in command shown in **Connections**, paste it into Termux, and run it.
6. Tap **Review**, inspect the exact command, then **Run reviewed setup**.
7. When Termux reports completion, return to TuxSpan and tap **Verify**.
8. Launch the workspace from **Workbench**.
9. Choose Mobile or Desktop layout, touch mode, and a power preset. Restart the session after changing layout or power settings.

Termux:X11's extra-key bar is verified hidden when TuxSpan launches a desktop. Use **Settings > Input > Show desktop controls** when you need it, then hide it again from the same screen. Android Back toggles the software keyboard, while connecting an external alphabetic keyboard suppresses the IME automatically. Multi-finger shortcuts are intentionally avoided because several manufacturers reserve them for system gestures such as screenshots.

## Build

Requirements:

- JDK 17
- Android SDK 37.0
- Android SDK Build Tools 36.0.0 or newer

```bash
./gradlew test lint assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Production builds read signing material from `TUXSPAN_KEYSTORE_PATH`, `TUXSPAN_KEYSTORE_PASSWORD`, `TUXSPAN_KEY_ALIAS`, and `TUXSPAN_KEY_PASSWORD`. Never commit a keystore or its credentials. With those variables configured, run `./gradlew assembleRelease`; the signed APK is written to `app/build/outputs/apk/release/app-release.apk`.

## Architecture

```text
TuxSpan Compose UI
   ├── DeviceInspector ──> fit tier and blueprint guidance
   ├── WorkspaceScripts ─> fixed, reviewable shell recipe
   ├── TermuxBridge ─────> documented RUN_COMMAND intent + explicit permission
   └── WorkspaceRepository
                         │
                         ▼
               Official Termux app
                  ├── PRoot Distro guest
                  └── Termux:X11 (desktop blueprints)
```

More detail is in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Security and privacy

The Termux command permission is powerful. TuxSpan deliberately uses fixed local recipes, presents setup commands before execution, has no analytics SDK, and includes no remote-command feature. Read [SECURITY.md](SECURITY.md) before distributing a modified build.

## Naming and affiliation

TuxSpan is independent software and is not affiliated with or endorsed by Termux, Termux:X11, Debian, Ubuntu, Alpine, Google, or any Android device manufacturer. Product names are used only to describe compatibility.

## License

TuxSpan's original source code is available under the [MIT License](LICENSE). Software installed or used alongside TuxSpan remains under its own license. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
