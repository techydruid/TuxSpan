# Verification record

## 0.21.1 — 2026-09-04

- `test lint assembleRelease` succeeded with JDK 17 and SDK 37. The 33 JVM tests reported 32 passed and one skipped because Windows has no Bash executable. The skipped shell suite was then run successfully inside the connected phone's actual Termux Bash.
- The exported Bash suite tested successful setup, a simulated package failure with exit code 100, error propagation through `tee` and the foreground wrapper, no false success after failure, non-terminal completion without waiting for input, and syntax of the Canvas, Studio and Spark foreground scripts. Mock records were isolated in a separate temporary home, not the installed workspace.
- Android lint: zero errors and one existing `SdCardPath` warning concerning a deliberately fixed Termux executable path in generated shell/guest code.
- Production-signed APK: `dev.tuxspan.mobile`, version `0.21.1`, versionCode `60`, minimum SDK `26`, target SDK `37`, no debuggable flag. Signing certificate is unchanged: `4764899DEC5E840DF9A8E814DD09732255E7B312129990DEA164B4C0329C6D6C`.
- Updated the connected phone from production 0.21.0 in place. No app uninstall, data clear, or workspace reset was used.
- The original Canvas log identified a real `plocate` conffile prompt followed by `end of file on stdin at conffile prompt`. Before repair, `dpkg --audit` reported `plocate` unconfigured.
- Retried the existing Canvas installation through the updated app's **Run setup again** button. Termux opened with live package output. It automatically kept `/etc/updatedb.conf`, configured `plocate`, completed setup, printed **Canvas setup completed successfully**, and displayed its normal `$` prompt without any Enter/key press after starting setup.
- The retained configuration's SHA-256 was identical before and after repair. The log explicitly confirmed reuse of the existing `tuxspan-canvas` filesystem. No workspace was deleted or recreated.
- After repair: durable status `ready`, exit code `0`, progress `READY|100|7|7`, executable launcher, available `startxfce4` and `firefox-esr`, and empty `dpkg --audit` output. The returned interactive Termux prompt accepted the read-only verification commands.
- Returning to TuxSpan automatically showed Canvas as the active installed workspace; manual Verify was not needed.
- A separate simulated code-100 failure was also run with a real Termux terminal attached and an isolated test home. It printed the failure message and returned automatically to an interactive prompt. The real Canvas status remained ready.
- Use the release's attached `SHA256SUMS.txt` for the final APK checksum. The final release is rebuilt from the tagged source commit with the same production signing certificate.

Actual package recovery was tested on Canvas; Studio and Spark received script syntax/regression checks, not fresh full installations. This verification does not claim to cover every package-manager error or Android device. Publication and verification of the final public download follow [the release checklist](docs/RELEASING.md).

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
