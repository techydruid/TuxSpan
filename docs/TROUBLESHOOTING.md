# Troubleshooting TuxSpan

Start with the exact status and recovery action shown on TuxSpan's Home screen. Avoid deleting a workspace unless you are prepared to lose the files stored inside it.

## Setup finishes but Verify says not ready

1. Return to Termux and confirm that the setup command has actually reached its completion message.
2. Confirm that Termux came from F-Droid and that `allow-external-apps=true` was enabled using the command in [Installation](INSTALLATION.md).
3. Return to TuxSpan and tap **Verify** again.
4. If the setup was interrupted, use the workspace's resume or repair action. TuxSpan recipes are designed to keep an existing container rather than blindly replacing it.

If Termux reports that a container already exists, do not remove it. Return to TuxSpan and use Verify, Resume, or Repair so the existing data is preserved.

## Workspace says Needs attention or does not launch

- Read the explanation and latest log lines shown by TuxSpan; **Needs attention** is a status, not the diagnosis.
- Use **Reconnect** if the desktop is already running but its viewer was closed.
- Use **Restart** to stop only the selected workspace session and launch it again.
- Make sure Android has not force-stopped Termux or Termux:X11.
- Disable battery restrictions for TuxSpan, Termux, and Termux:X11 if the phone repeatedly kills the session in the background.
- Verify that the regular Termux:X11 universal APK is installed for Canvas or Studio.

If the problem repeats, capture the diagnostic log before retrying so the useful error is not replaced by later output.

## Black screen after leaving a desktop

Return to TuxSpan and use **Reconnect** or **Restart**. Do not use the Linux desktop's Log Out command as a normal exit method; it ends the desktop session. TuxSpan's scoped Stop or Restart controls preserve the installed workspace and its files.

## Bottom desktop controls remain visible

Open **TuxSpan > Settings > Input** and turn off **Show desktop controls**, then restart or reconnect the desktop if requested. The controls belong to Termux:X11, not the XFCE taskbar. TuxSpan intentionally avoids multi-finger hide gestures because several Android manufacturers reserve them for screenshots or other system actions.

## Software keyboard does not appear

- Press Android Back once while the Termux:X11 desktop is focused.
- Check **TuxSpan > Settings > Input** and use the keyboard action there.
- Disconnect any physical keyboard Android still considers active.
- Tap the text field again after changing touch mode.

## Android Downloads is missing

The bridge appears as `~/Android-Downloads` only after Android grants Termux storage access.

1. Run the storage setup from TuxSpan again.
2. If no prompt appears, open **Android Settings > Apps > Termux > Permissions** and grant the available file or media access.
3. Return to TuxSpan, repair the storage bridge, and reopen the Linux file manager.

Exact Android setting names vary between manufacturers.

## Termux or Termux:X11 will not install together

The APKs are probably signed by different sources. The recommended combination is:

- [Termux from F-Droid](https://f-droid.org/en/packages/com.termux/)
- `termux-x11-universal-debug.apk` from the [Termux:X11 nightly release](https://github.com/termux/termux-x11/releases/tag/nightly)

Do not use the `sharedUid` Termux:X11 APK with F-Droid Termux. Back up important Termux data before removing any existing Termux app or plugin to correct a source mismatch.

## Setup is slow or Android closes it

- Keep the phone charging and use stable Wi-Fi.
- Keep Termux visible during the initial installation.
- Remove battery restrictions for the three apps.
- Close memory-heavy Android apps.
- Choose Canvas or Spark on lower-memory devices.
- Confirm that the phone has enough free space before retrying.

## Reporting a reproducible problem

Open a [bug report](https://github.com/techydruid/TuxSpan/issues/new/choose) and include:

- TuxSpan version and selected workspace
- phone model, Android version, CPU architecture, RAM and free storage
- Termux source/version and Termux:X11 version
- exact steps that reproduce the issue
- TuxSpan's sanitized diagnostic log and a screenshot, if helpful

Remove usernames, file paths, tokens, IP addresses and other private information before posting logs publicly.
