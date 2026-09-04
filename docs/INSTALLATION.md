# Installing TuxSpan

This guide is written for first-time users. TuxSpan runs Linux through Termux and, for graphical desktops, Termux:X11. It does not require root and does not replace Android.

## Before you begin

- Android 8 or newer
- A stable Wi-Fi connection
- At least 3 GB free for Spark, or 8–10 GB free for Canvas or Studio
- A charged phone or charger; desktop setup can take several minutes

See [Compatibility](COMPATIBILITY.md) if you are unsure which workspace fits your device.

## Official downloads

| Download | Used for | Get it here |
|---|---|---|
| TuxSpan | Setup and workspace controls | [Latest TuxSpan release](https://github.com/techydruid/TuxSpan/releases/latest) |
| Termux | Every workspace | [Termux on F-Droid](https://f-droid.org/en/packages/com.termux/) |
| Termux:X11 nightly | Canvas and Studio desktops | [Termux:X11 nightly release](https://github.com/termux/termux-x11/releases/tag/nightly) |

Use Termux from F-Droid. On the Termux:X11 nightly page, expand **Assets** and download **`termux-x11-universal-debug.apk`**. Do not download the `sharedUid` APK when using F-Droid Termux.

Termux and its plugins must come from compatible signing sources. Do not mix an F-Droid Termux installation with a GitHub `sharedUid` plugin build. The old Google Play build of Termux is not supported.

## Step-by-step setup

1. Download and install **Termux** from F-Droid. If Android blocks the APK, allow installation from the browser or file manager you used, then retry.
2. For Canvas or Studio, download and install **`termux-x11-universal-debug.apk`** from the Termux:X11 nightly release. Spark does not require Termux:X11.
3. Download and install the latest **TuxSpan** APK from this repository.
4. Open TuxSpan. The Home screen shows a four-step **Connect TuxSpan** checklist before any workspace is selected.
5. Confirm Termux is installed. Confirm Termux:X11 too if you want Canvas or Studio; it is optional for Spark.
6. Tap **Grant permission** under **Allow Termux commands**, then approve Android's **Run commands in Termux environment** prompt.
7. Under **Enable Termux consent**, tap **Copy**, then **Open Termux**. Paste the command and press Enter:

   ```sh
   mkdir -p ~/.termux && printf '\nallow-external-apps=true\n' >> ~/.termux/termux.properties && termux-reload-settings
   ```

8. Return to TuxSpan and tap **I ran the command — check connection**. When all required steps are checked, tap **Choose a Linux workspace**.
9. Select **Canvas**, **Studio**, or **Spark**, open its review screen, and inspect the generated setup recipe.
10. Tap **Run reviewed setup**. TuxSpan opens Termux automatically, where the real package download and installation output remains visible.
11. Keep Termux running. You can return to TuxSpan at any time to see the current milestone and an advancing estimated percentage. The estimate may move more slowly while Android configures a large package, so Termux remains the source of the exact live output. Tap **View live installation in Termux** to return to it.
12. Do not force-stop either app or change networks during the download. When the workspace is ready, launch it from **Home**.

The first desktop launch may take longer than later launches. Canvas and Studio open in landscape by default.

## Android Downloads inside Linux

Open TuxSpan's storage guide and grant Termux access when Android asks. After the bridge is ready, the phone's shared Downloads folder appears inside the Linux workspace as:

```text
~/Android-Downloads
```

If Android does not show a permission prompt, open **Android Settings > Apps > Termux > Permissions**, grant the available file or media access, and run the storage setup again. Menu names vary by phone manufacturer and Android version.

## Keyboard, touch and mouse

- Android Back toggles the software keyboard while a desktop is open.
- **Settings > Input** lets you choose touchpad or direct-touch behavior and show or hide the desktop controls.
- A connected physical keyboard suppresses the software keyboard automatically.
- A Bluetooth or USB-OTG mouse and keyboard provide the best desktop experience, but are optional.
- Layout, input and power changes may require a workspace restart.

## Updating TuxSpan

Download the newer production APK from the latest release and install it over the current production version. Updating TuxSpan does not require uninstalling Termux or deleting Linux workspaces.

Very old debug builds used a different Android signing identity. If Android refuses a one-time upgrade from one of those builds, uninstall only TuxSpan and install the production APK. Do not clear or uninstall Termux unless you intentionally want to remove the Linux environments stored there.

If Android still reports a package conflict after TuxSpan was removed from the main user, check other Android users/profiles for a leftover legacy TuxSpan copy. Do not delete the profile itself. See [package-conflict recovery](TROUBLESHOOTING.md#tuxspan-apk-conflicts-with-an-existing-package) before removing anything. New development builds have a separate **TuxSpan Dev** identity and are not production updates.

If something does not work, continue with [Troubleshooting](TROUBLESHOOTING.md).
