# Compatibility policy

## Supported baseline

- Android 8.0 (API 26) or later.
- Android ABI reported as `arm64-v8a`, `armeabi-v7a`, `x86_64`, or `x86`.
- About 2 GB RAM and 3 GB free app storage for the Spark terminal baseline.
- A 64-bit userspace, about 4 GB RAM, and 7 GB free storage for the Canvas desktop baseline.
- A separately supported Termux build from F-Droid or the Termux GitHub project.

## Tiers

### Desktop ready

64-bit, at least about 6 GB total RAM and 10 GB free storage. Canvas and Studio should be practical, although application performance still varies.

### Balanced

64-bit, at least about 4 GB total RAM and 7 GB free storage. Canvas is preferred. Avoid heavy multitasking and large creative applications.

### Terminal first

At least about 2 GB RAM and 3 GB free storage. Spark is recommended. A desktop is not blocked by the code, but it is not presented as a good experience.

### Unsupported

Android below 8, an unrecognized ABI, or resources below the terminal baseline.

## Manufacturer independence

No Samsung API, DeX API, One UI setting, or manufacturer string is required. TuxSpan also does not require Motorola Ready For, Xiaomi PC Mode, desktop-mode developer options, USB-C display output, or a root manager.

External display is optional. Users may run the desktop on the phone screen, use Android's supported display-out path, or use their own screen-mirroring solution.

## Known variability

- **Background process limits:** Android vendors can stop or throttle Termux processes. Battery settings differ by manufacturer and release.
- **32-bit devices:** package availability is narrower; Spark is the supported target.
- **GPU:** Vulkan presence is not equivalent to working Turnip/Zink acceleration. Non-Adreno and older GPUs may use different Mesa paths or software rendering.
- **Thermals:** sustained compilation, desktop use, and local AI can throttle a phone heavily.
- **Input:** touch interaction is provided by Termux:X11. Keyboard/mouse behavior depends on Android and the device.
- **Display-out:** many USB-C phones lack DisplayPort Alt Mode even when they use a USB-C connector.
- **Distribution packages:** upstream repositories evolve. Pinned base images reduce but do not eliminate package-level change.

## Test matrix for a stable release

At minimum, validate:

- recent Pixel / stock-like Android, arm64;
- Samsung One UI, low- and high-memory arm64 devices;
- Xiaomi/HyperOS or another aggressive background manager;
- MediaTek/Mali arm64 device;
- Qualcomm/Adreno arm64 device;
- Android 8 or 9 arm64 device;
- a supported 32-bit ARM device for Spark;
- x86_64 Android emulator for UI and command-contract tests.

Record install time, guest launch, X11 launch, keyboard/mouse, audio, suspend/resume, 30-minute stability, and clean removal.

