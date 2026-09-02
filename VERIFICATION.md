# Verification record

Verified on 2026-09-02 using Windows, Eclipse Temurin JDK 17.0.20.1, Android SDK Platform 37.0, Android SDK Build Tools 36.0.0, Android Gradle Plugin 9.3.0, and the bundled Gradle 9.7.1 wrapper.

Command:

```powershell
.\gradlew.bat --no-daemon test lint assembleRelease
```

The four documented `TUXSPAN_*` signing environment variables were supplied from private local credentials for the release build.

Result:

- Build: successful
- JVM unit tests: 29 passed, 0 failed
- Android lint: 0 findings
- Release APK package: `dev.tuxspan.mobile`
- Version: `0.20.0` (`versionCode` 58)
- Minimum SDK: 26 (Android 8)
- Target / compile SDK: 37
- Bundled TuxSpan runtime payload: none (no Linux bootstrap, PRoot binary, or X11 server)
- Release APK SHA-256: `B5CBCDC0E4D32A2580685A41404607A48C3B4E18FAE1CF0411E5C6281A8D212B`
- Signing certificate SHA-256: `4764899DEC5E840DF9A8E814DD09732255E7B312129990DEA164B4C0329C6D6C`

The public APK is signed with a dedicated TuxSpan release key. The keystore and passwords are stored outside the repository and must be backed up securely; losing them prevents Android from accepting future updates under the same application identity. Broader physical-device testing is still required before a 1.0 release.
