# Verification record

## 0.22.1 — 2026-09-04

- `testDebugUnitTest assembleRelease lintRelease` succeeded. JVM results: 36 tests, 35 passed and one skipped because Windows has no Bash. Android lint: zero errors and one existing `SdCardPath` warning. The exported Canvas/Studio account, launch, stop, Downloads-launch and desktop-profile scripts passed `bash -n` inside real Termux.
- Production APK: `dev.tuxspan.mobile`, version `0.22.1`, versionCode `62`, minimum SDK `26`, target SDK `37`, not debuggable. Signature verified against the unchanged production certificate `4764899DEC5E840DF9A8E814DD09732255E7B312129990DEA164B4C0329C6D6C`.
- Updated the connected phone in place from 0.22.0 without clearing TuxSpan or Termux data. Tested against the official Termux:X11 build `1.03.01-6d3c688-27.08.26` with an external alphabetic Bluetooth keyboard and a Bluetooth mouse connected.
- Reproduced the floating-keyboard bug using Android key events injected with the connected keyboard's device ID, source and scancodes. With `hardwareKbdScancodesWorkaround=true`, the text reached the Linux terminal but Gboard appeared (`mInputShown=true`). With the setting disabled, text, Shift, Backspace, cursor movement and Ctrl+C worked without Gboard appearing (`mInputShown=false`). These are hardware-source injection tests, not a physical Bluetooth radio/typing test.
- Briefly disconnected only the keyboard, leaving the mouse connected. Android Back successfully opened the software keyboard. After the keyboard reconnected, repeated hardware-source typing and Ctrl+C checks kept the software keyboard hidden. Automatic software-keyboard appearance on text-field focus was not verified in this run; it is separate from the physical-keyboard fix.
- Restarted Canvas through TuxSpan and verified the direct-key handling preference, external-keyboard IME suppression and normal desktop startup persisted. Android's global `show_ime_with_hard_keyboard` setting was left unchanged. Restored the original trackpad touch mode after direct-touch UI testing.
- Observed an untrusted-launcher dialog and a subsequent Terminal desktop-shortcut timeout. The installed Terminal itself worked from the dock. Changing only executable permissions did not resolve XFCE's trust check.
- After installing the corrected build and restarting the desktop, opened the Terminal desktop shortcut, closed it and reopened it successfully without a trust prompt. Opened Photos from its desktop shortcut successfully too. Read-only checks confirmed Terminal, Photos and Firefox shortcuts resolve to their generated files under `/usr/local/share/applications`; no blanket launcher-security bypass was applied.
- Confirmed the existing Canvas normal-user sudo setup and installed VLC remained available. No Linux workspace or personal data was removed. The public release is rebuilt from its source commit; the attached `SHA256SUMS.txt` identifies the final download.

Verification covers the reported keyboard popup and starter-shortcut launch failure on the connected Canvas desktop, not every keyboard layout, Android keyboard app or Linux application. Full Studio/Spark runtime and creator/developer pack installations were not repeated for this patch.

## 0.22.0 — 2026-09-04

- `testDebugUnitTest assembleRelease lintRelease` succeeded. JVM results: 36 tests, 35 passed, one skipped because this Windows host has no Bash. The exported real Bash regression suite then passed in Termux, including success/failure propagation, exit code 100, automatic prompt return, and syntax of all three installation recipes. The generated desktop account, launch, stop and profile scripts also passed shell syntax checks.
- Android lint: zero errors and one existing `SdCardPath` warning. Production APK: `dev.tuxspan.mobile`, version `0.22.0`, versionCode `61`, minimum SDK `26`, target SDK `37`, not debuggable. Verified the unchanged production signing certificate: `4764899DEC5E840DF9A8E814DD09732255E7B312129990DEA164B4C0329C6D6C`.
- Updated the connected phone in place from 0.21.1 without uninstalling or clearing either app. Tested the account migration on its existing Canvas desktop. Original `/root` data is kept; the new account has its own `/home/tuxspan`.
- Ran migration fixtures inside the real Debian guest: originals retained, existing destination files and edits preserved across retries, no destination symlink traversal, absolute old-home links rewritten, and cache/Android Downloads content excluded from copying.
- Exercised the new account-required repair state and **Repair Canvas > Run reviewed setup**. The reviewed installation reused the existing container and packages, validated sudo, printed successful completion, and returned to the Termux prompt without an extra key press. Returning to TuxSpan recognized Canvas as ready.
- Installed unmodified Debian VLC 3.0.23 using `sudo apt install -y vlc` from the regular account. Confirmed the version command and the graphical VLC window, including its first-run privacy dialog; no VLC binary patch, root override or metadata-network consent was applied.
- A one-time check launched inside the actual XFCE session verified a non-root `tuxspan` account, `/home/tuxspan`, working passwordless guest sudo, ordinary apt installation commands, and a read/write round trip through the desktop's Android Downloads bind. Test files in Downloads were removed immediately.
- Tested the app's Restart control: Canvas returned to running, the account and sudo remained usable, a saved home file survived, and the regenerated persistent launcher used the normal account. Removed the temporary test file, test autostart entry and account-marker backup afterward.
- In a separate temporary Ubuntu 24.04 guest on the phone, the exact account setup validated Ubuntu's packaged sudo and a normal-user `sudo apt install -y hello` completed; `hello` ran successfully. The account guard correctly refused migration while a graphical desktop was still running; closing that desktop allowed the test to continue. This was not a full Studio desktop installation.
- Removed the temporary Ubuntu test container and staged test scripts after verification; the installed Canvas workspace, original data and VLC installation were retained.
- In-place installation of the signed 0.22.0 APK succeeded under the existing production identity. The public APK is rebuilt from the release commit; use its attached `SHA256SUMS.txt` for the final download checksum.

These checks validate the normal-account installation path, not every Linux application or media codec. PRoot/Android/CPU restrictions still apply. Full Studio graphical installation and Spark runtime were not repeated for this change.

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
