# Changelog

## 0.20.0 — 2026-09-02

- Publish the first production-signed APK with standard release naming and a dedicated long-term signing identity.
- Prevent duplicate desktop launchers during slow cold starts and make workspace status responses workspace-specific.
- Route Spark reconnects back to Termux instead of an unused X11 display.
- Use recovery-resistant LibreOffice launchers as the document defaults.
- Replace File Roller with Xarchiver after real-device testing found an ARM `Illegal instruction` crash in Ubuntu.
- Replace the OnePlus-conflicting three-finger help instruction with the reliable Settings control.

## 0.9.0 — 2026-09-01

- Return to TuxSpan automatically when the XFCE desktop session ends instead of leaving Termux:X11 on a black screen.
- Replace the oversized Workbench marketing area with a focused workspace status and primary action.
- Collapse desktop settings, app packs, and advanced connection tools until they are needed.
- Shorten Blueprint, Device, and Connections screens and use compact icon navigation in landscape.

## 0.8.9 — 2026-09-01

- Replace XFCE's misleading system-power dialog in rootless desktop profiles with a direct, confirmed Log Out action.
- Remove disabled Restart and Shut Down choices that cannot safely control the Android host from a PRoot session; workspace restart and stop remain available in TuxSpan.

## 0.8.8 — 2026-09-01

- Use XFCE's native 43 px HDPI window-decoration theme for the Mobile display profile so its readable 12 pt application titles are fully visible instead of being clipped by the 29 px standard title bar.
- Keep the standard decoration theme for the Compact profile, preserving its lower-density layout.

## 0.8.7 — 2026-09-01

- Keep one D-Bus session alive for the full XFCE lifetime and run XFCE's standard `startxfce4` bootstrap in the foreground so its XDG configuration and data paths are initialized correctly.
- Wait for a successful authenticated X client handshake before starting XFCE, eliminating the first-launch race that previously required retries.
- Do not send the asynchronous Termux:X11 stop broadcast during relaunch; it could reach the new server after startup and tear down its display.
- Use an MIT-MAGIC-COOKIE-protected loopback X connection and keep D-Bus/runtime files in a private home directory so desktop startup cannot delete live sockets from the shared temporary directory.
- Disable the unavailable accessibility bridge inside PRoot so GTK applications do not stall on a 25-second bus timeout.
- Install reviewed desktop shortcuts in the system application directory and link them onto the desktop, avoiding XFCE's untrusted-launcher prompt without relying on unavailable GVFS metadata.
- Add a local icon-theme override so those trusted links retain clean application artwork without symlink badges.
- Apply the same trusted-shortcut design to the optional Creator and Developer packs.
- Hide Termux:X11's extra-key panel with a persisted visibility update followed by a live refresh, removing the startup race that could leave the black controls visible.
- Remove the temporary launcher-trust diagnostic folder from upgraded Canvas desktops.

## 0.8.6 — 2026-09-01

- Launch Firefox ESR directly from both Canvas Web shortcuts instead of the generic browser selector that can time out inside PRoot.
- Register Firefox ESR as Canvas's default handler for HTTP, HTTPS and HTML content.
- Mark generated desktop launchers executable and trusted so XFCE does not show an untrusted-launcher prompt.

## 0.8.5 — 2026-09-01

- Add visibly wider breathing room between taskbar icons by scaling icon artwork to 84% while preserving the full 60 px launcher and running-app click targets.
- Remove temporary live-preview CSS before writing the managed profile, preventing duplicate spacing rules after an upgrade.

## 0.8.4 — 2026-09-01

- Restore enough desktop-label width for common names such as Terminal to render naturally on one line while retaining a compact two-column shortcut layout.
- Raise the Mobile dock bottom inset from 8 px to 14 px so its floating separation is clearly visible without sacrificing meaningful workspace.

## 0.8.3 — 2026-09-01

- Increase the Mobile dock height from 54 px to 60 px for larger, easier-to-target application icons.
- Increase horizontal spacing between pinned and running-app icons while keeping the dock centered and dynamically sized.

## 0.8.2 — 2026-09-01

- Tighten the desktop shortcut grid while retaining complete, wrapping labels and large mobile-friendly icons.
- Give pinned taskbar launchers balanced spacing, rounded hover feedback and compact outer padding.
- Add an icon-only grouped Window Buttons section that grows the centered taskbar as applications open and uses XFCE's overflow behavior when space becomes constrained.

## 0.8.1 — 2026-09-01

- Show complete desktop shortcut names instead of truncating them with ellipses.
- Widen each desktop icon's text area on the mobile profile while allowing long names to wrap across lines.

## 0.8.0 — 2026-09-01

- Expand the default Starter pack into a complete lightweight everyday desktop: browser, LibreOffice, PDF and text readers, image viewer, media player with common codecs, Thunar, archive tools, search, screenshot, task manager, calculator, fonts and terminal utilities.
- Assign sensible default applications for documents, images, audio, video, plain text, folders and LibreOffice formats.
- Replace duplicate Files and developer-oriented Geany desktop shortcuts with Web, Writer, Photos, Media and Terminal; keep Home and Trash while hiding the duplicate File System icon.
- Update the floating dock to Applications, Web, Files, Writer, Media, Photos and Terminal.
- Expand the optional Creator pack with GIMP, Inkscape, Audacity, OpenShot, Scribus, FFmpeg, ImageMagick and LAME, organized in a Creator Tools desktop folder.
- Expand the optional Developer pack with C/C++ build and debugging tools, Python, Node.js, Java, Go, Rust, SQLite, ShellCheck, SSH, Geany, Meld and DB Browser, organized in a Developer Tools desktop folder.
- Continue excluding tools that depend heavily on unavailable Android hardware integration or reliable accelerated desktop OpenGL.

## 0.7.1 — 2026-09-01

- Keep the launcher dock centered when Termux:X11 changes from its temporary startup resolution to the phone's final landscape resolution.
- Recalculate the dock position after later orientation or display-size changes.
- Reduce the Mobile dock's bottom inset from 24 px to 8 px so it preserves more usable desktop space.

## 0.7.0 — 2026-09-01

- Install a reviewed Starter pack with new desktop workspaces: LibreOffice Writer, Calc and Impress; Mousepad; Atril; Ristretto; File Roller; Galculator; Geany; useful fonts; and common terminal utilities.
- Keep GIMP/Inkscape and the compiler/Node.js toolchain as optional Creative and Developer packs.
- Add a Starter repair action for workspaces created by earlier versions.
- Run optional pack installation in the background with an in-app progress state and completion result.
- Replace the inherited XFCE dock contents with Applications, Web, Files, Writer, Geany and Terminal launchers.
- Raise the advertised desktop storage targets to account for the additional applications.

## 0.6.3 — 2026-09-01

- Simplify the Mobile profile to one XFCE workspace and replace the workspace-switcher block with a minimal spacer.
- Keep the bottom launcher dock visible instead of auto-hiding it.
- Center the dock and float it 24 px above the bottom edge to avoid the Android gesture area.
- Preserve the auto-hiding top Applications panel and the larger Mobile text and pointer.

## 0.6.2 — 2026-09-01

- Always auto-hide the XFCE Applications panel in the default Mobile profile.
- Reveal the panel by moving the pointer to the top edge while retaining Alt+F1, Alt+F2, desktop shortcuts, and the desktop context menu.
- Keep the panel permanently visible in the optional compact Desktop profile.

## 0.6.1 — 2026-09-01

- Increase the default Mobile pointer from 32 px to 48 px.
- Export the X cursor theme and size before XFCE starts so the larger pointer applies throughout the session.
- Keep the compact Desktop profile at a 24 px pointer.

## 0.6.0 — 2026-09-01

- Make the readable Mobile layout the default for new and existing installations.
- Launch supported high-density phones at up to 192 DPI instead of the compact 144 DPI desktop profile.
- Enlarge XFCE application text, window titles, desktop icons, panel controls, terminal text, and the pointer together.
- Keep the compact Desktop layout available for external displays and users who prefer more workspace.

## 0.5.0 — 2026-09-01

- Keep touchpad input and landscape orientation as the clean phone-only defaults.
- Map Android Back to the software keyboard without reopening the extra-key bar.
- Let Termux:X11 automatically suppress the IME when an external alphabetic keyboard connects.
- Reserve three-finger swipe up for the optional extra-key bar and disable swipe down to avoid manufacturer screenshot conflicts.
- Keep the mouse-helper overlay disabled and explain tap, right-click, scroll, keyboard, and recovery gestures in the one-time guide.
- Apply preferences through Termux:X11's native exported preference receiver, avoiding the nightly command helper's response timeout.

## 0.4.2 — 2026-09-01

- Hide the Termux:X11 extra-keys panel through its live `ACTION_CUSTOM` control path.
- Work around Termux:X11 1.03.01 persisting `additionalKbdVisible=false` without refreshing the active toolbar.
- Validate the show-then-toggle sequence against a connected OnePlus 10T running Termux:X11 1.03.01-6d3c688.
- Make the Workbench Show/Hide controls actions verify their final persisted state.

## 0.4.1 — 2026-09-01

- Fix Termux:X11 preference arguments to use the supported `key:value` command syntax.
- Verify that the extra-keys panel is actually hidden instead of suppressing preference errors.
- Retry launch-time preferences while the X11 activity becomes ready.
- Add explicit **Hide controls now** and **Show controls** recovery actions in Workbench.

## 0.2.2 — 2026-08-31

- Hide the Termux:X11 additional keys panel automatically after Launch Canvas.
- Preserve Termux:X11's gesture-based ability to reveal the panel temporarily.
- Run desktop launch through Termux's background runner to avoid opening an unnecessary terminal session.

## 0.2.1 — 2026-08-31

- Replace the launcher here-document with Base64 decoding so nested setup wrappers cannot corrupt its delimiter.
- Build tracked setup scripts line-by-line to preserve exact shell structure.
- Add regression coverage for the reported unexpected-end-of-file failure.

## 0.2.0 — 2026-08-31

- Run setup as one background Termux job instead of creating foreground terminal sessions.
- Capture setup output in a per-blueprint log and return a compact success or failure result to TuxSpan.
- Display installation state and failure details directly in the setup sheet.
- Eliminate the Android foreground-session race that could leave commands queued in Termux notifications.

## 0.1.2 — 2026-08-31

- Detect an existing PRoot container with a direct login health check instead of parsing human-readable list output.
- Resume partially completed workspaces without attempting to create the same container again.
- Use the same robust health check during workspace verification.

## 0.1.1 — 2026-08-31

- Open Termux after setup dispatch instead of leaving users at an apparently idle screen.
- Automatically copy a paste-safe, single-line manual setup fallback.
- Add retry, manual-copy, and open-Termux actions after setup starts.
- Show verification and dispatch errors inside the setup sheet.
- Stop indefinite verification spinners when Android does not deliver a command result.

## 0.1.0 — 2026-08-31

- Introduced the original TuxSpan brand and native Compose interface.
- Added Workbench, Blueprints, Device Lab, and Connections screens.
- Added capability tiers based on API level, ABI, RAM, storage, and Vulkan feature detection.
- Added Canvas (Debian/XFCE), Studio (Ubuntu/XFCE), and Spark (Alpine terminal) recipes.
- Added the permission-gated Termux RUN_COMMAND bridge and result callback service.
- Added setup review, verification, launch, package bundles, and confirmation-gated removal.
- Added responsive navigation for phone and larger screens.
- Added original adaptive launcher artwork.
- Added unit tests, lint, Android CI, security guidance, compatibility policy, third-party notices, and an independent-design record.
