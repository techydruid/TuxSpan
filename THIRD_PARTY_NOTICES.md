# Third-party notices

TuxSpan's APK does not bundle Termux, Termux:X11, PRoot, PRoot Distro, a Linux root filesystem, or native Linux/X11 libraries.

The Android build uses AndroidX and Jetpack Compose libraries under their respective licenses. Gradle resolves those dependencies from Google's Maven repository and Maven Central. Release automation should export exact dependency licenses for the artifact being published.

At runtime, the user may separately install or download:

- [Termux](https://github.com/termux/termux-app)
- [Termux:X11](https://github.com/termux/termux-x11)
- [PRoot Distro](https://github.com/termux/proot-distro)
- Debian, Ubuntu, or Alpine OCI/root filesystem content
- distribution packages selected by the user

Those components are independent works governed by their own licenses and distribution terms. TuxSpan does not change those terms.

The literal Android intent action and extra names in `TermuxContract` implement Termux's public RUN_COMMAND interoperability contract. Product and project names are used descriptively. No endorsement is implied.

