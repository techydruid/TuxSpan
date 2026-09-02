# Verification record

Verified on 2026-09-02 using Windows, Eclipse Temurin JDK 17.0.20.1, Android SDK Platform 37.0, Android SDK Build Tools 36.0.0, Android Gradle Plugin 9.3.0, and the bundled Gradle 9.7.1 wrapper.

Command:

```powershell
.\gradlew.bat --no-daemon test assembleDebug lint
```

Result:

- Build: successful
- JVM unit tests: 29 passed, 0 failed
- Android lint: 0 findings
- Debug APK package: `dev.tuxspan.mobile`
- Version: `0.20.0` (`versionCode` 58)
- Minimum SDK: 26 (Android 8)
- Target / compile SDK: 37
- Bundled TuxSpan runtime payload: none (no Linux bootstrap, PRoot binary, or X11 server)
- Debug APK SHA-256: `3B225C7657E59540748E443C136F8081305A6561A4998E5360FADC39CF42513F`

The debug APK is signed with the automatically generated Android debug key. It is suitable for evaluation, not a public production release. A release owner must configure and protect a production signing key, complete physical-device testing, and publish matching source and notices.
