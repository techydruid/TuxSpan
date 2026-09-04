# Verification record

## 0.21.0 — 2026-09-04

Verified with JDK 17, Android SDK Platform 37.0, Build Tools 36.0.0, and the Gradle 9.7.1 wrapper:

```powershell
.\gradlew.bat --no-daemon test lint assembleRelease assembleDebug
```

- Build succeeded; 29 JVM unit tests passed with no failures or errors.
- Android lint: 0 errors and 1 warning for the deliberately fixed Termux `am` path in the guest keyboard bridge. This is not a clean/no-warning lint claim.
- Production APK: `dev.tuxspan.mobile`, version `0.21.0`, versionCode `59`, minimum SDK `26`, target SDK `37`, no debuggable flag.
- APK signature verified using the unchanged dedicated production certificate, SHA-256 `4764899DEC5E840DF9A8E814DD09732255E7B312129990DEA164B4C0329C6D6C`.
- Physical-device package-conflict diagnosis: a legacy debug-signed TuxSpan copy remained registered in a secondary Android user. It was marked never launched and had no app-data directory. Removing only that leftover TuxSpan registration allowed the official 0.20.0 production APK to install.
- In-place production upgrade from 0.20.0 to 0.21.0 succeeded with no uninstall/data-clear step. Android retained the TuxSpan app-data directory identifiers. Termux remained installed with its original data-directory identifiers; no Termux workspace was removed.
- Android reported successful cold activity startup for 0.21.0. Visual interaction was not retested during this check because the phone was locked.
- Development APK metadata uses `dev.tuxspan.mobile.debug`, version `0.21.0-dev`, and label **TuxSpan Dev**, separate from the production identity. Side-by-side installation with the production app succeeded; the temporary development test copy was then removed, leaving production installed.

Prior 0.21.0 device checks covered first-run onboarding, visible Termux setup, Canvas launch, and real Android Downloads access. This release check does not claim to re-test every Linux application or every Android device. Use the release's `SHA256SUMS.txt` for the final public APK hash; signing credentials remain outside the repository.

## 0.20.0

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

The public APK is signed with a dedicated TuxSpan release key. The keystore and passwords are stored outside the repository and must be backed up securely; losing them prevents Android from accepting future updates under the same application identity. Android vendor behavior and available hardware resources can affect compatibility, so device-specific problems should be reported through the repository.
