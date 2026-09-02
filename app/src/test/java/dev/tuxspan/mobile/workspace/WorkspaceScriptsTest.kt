package dev.tuxspan.mobile.workspace

import dev.tuxspan.mobile.model.WorkspaceCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.util.Base64

class WorkspaceScriptsTest {
    @Test
    fun defaultExperienceUsesReadablePhoneScale() {
        val settings = ExperienceSettings()

        assertEquals(DisplayProfile.MOBILE, settings.displayProfile)
        assertEquals(192, settings.displayProfile.adaptiveDpi(480))
        assertEquals(48, settings.displayProfile.cursorSize)
        assertEquals(2, settings.displayProfile.panelAutoHideBehavior)
        assertEquals(144, DisplayProfile.DESKTOP.adaptiveDpi(480))
        assertEquals(24, DisplayProfile.DESKTOP.cursorSize)
        assertEquals(0, DisplayProfile.DESKTOP.panelAutoHideBehavior)
    }

    @Test
    fun mobileProfileUsesOneWorkspaceAndVisibleFloatingDock() {
        val profile = WorkspaceScripts.guestExperienceScript(
            WorkspaceCatalog.canvas,
            ExperienceSettings(),
            192,
        )

        assertTrue(profile.contains("/general/workspace_count int '1'"))
        assertTrue(profile.contains("rm -rf \"${'$'}HOME/Desktop/Launcher trust FAIL\""))
        assertTrue(profile.contains("/general/theme string 'Default-hdpi'"))
        assertTrue(profile.contains("/general/title_font string 'Sans Bold 12'"))
        assertTrue(profile.contains("plugin_type=${'$'}(xfconf-query"))
        assertTrue(profile.contains("pager)"))
        assertTrue(profile.contains("actions)"))
        assertTrue(profile.contains("-t string -s '+logout-dialog'"))
        assertTrue(profile.contains("${'$'}plugin_path/appearance\" uint '0'"))
        assertTrue(profile.contains("${'$'}plugin_path/ask-confirmation\" bool 'true'"))
        assertFalse(profile.contains("-s '+shutdown'"))
        assertFalse(profile.contains("-s '+restart'"))
        assertTrue(profile.contains("/panels/panel-2/autohide-behavior uint '0'"))
        assertTrue(profile.contains("/panels/panel-2/position string 'p=10;x=0;y=0'"))
        assertTrue(profile.contains("/panels/panel-2/enable-struts bool 'false'"))
        assertTrue(profile.contains("tuxspan-position-dock"))
        assertTrue(profile.contains("base64 -d > \"${'$'}dock_positioner\""))
        assertTrue(profile.contains("nohup \"${'$'}dock_positioner\" '60' '14'"))
        assertTrue(profile.contains("/plugins/plugin-200"))
        assertTrue(profile.contains("'applicationsmenu'"))
        assertTrue(profile.contains("tuxspan-writer.desktop"))
        assertTrue(profile.contains("tuxspan-media.desktop"))
        assertTrue(profile.contains("tuxspan-photos.desktop"))
        assertTrue(profile.contains("-t int -s 206 -t int -s 207"))
        assertTrue(profile.contains("-t int -s 200 -t int -s 201"))
        assertTrue(profile.contains("create_trusted_launcher tuxspan-photos Photos ristretto org.xfce.ristretto"))
        assertTrue(profile.contains("create_trusted_launcher tuxspan-media Media parole org.xfce.parole"))
        assertTrue(profile.contains("create_trusted_launcher tuxspan-firefox Firefox 'firefox-esr' 'firefox-esr'"))
        assertTrue(profile.contains("tuxspan-firefox.desktop"))
        assertFalse(profile.contains("create_trusted_launcher tuxspan-web Web"))
        assertTrue(profile.contains("xdg-settings set default-web-browser 'firefox-esr.desktop'"))
        assertTrue(profile.contains("xdg-mime default 'firefox-esr.desktop' x-scheme-handler/http x-scheme-handler/https text/html"))
        assertTrue(profile.contains("launcher_path=\"/usr/local/share/applications/${'$'}launcher_id.desktop\""))
        assertTrue(profile.contains("ln -sfn"))
        assertFalse(profile.contains("metadata::trusted"))
        assertFalse(profile.contains("Exec=x-www-browser"))
        assertTrue(profile.contains("rm -f \"${'$'}HOME/Desktop/Files.desktop\" \"${'$'}HOME/Desktop/Geany.desktop\""))
        assertTrue(profile.contains("/desktop-icons/file-icons/show-filesystem bool 'false'"))
        assertTrue(profile.contains("-XfdesktopIconView-ellipsize-icon-labels: 0"))
        assertTrue(profile.contains("-XfdesktopIconView-cell-text-width-proportion: 2.55"))
        assertTrue(profile.contains("-XfdesktopIconView-cell-spacing: 6"))
        assertTrue(profile.contains("-XfdesktopIconView-cell-padding: 4"))
        assertTrue(profile.contains("#applicationmenu-button, #launcher-button"))
        assertTrue(profile.contains("padding-left: 8px"))
        assertTrue(profile.contains("margin-left: 5px"))
        assertTrue(profile.contains("TUXSPAN_LIVE_ICON_GAP_BEGIN"))
        assertTrue(profile.contains("-gtk-icon-transform: scale(0.84)"))
        assertTrue(profile.contains("/panels/panel-2/size uint '60'"))
        assertTrue(profile.contains("/plugins/plugin-207 -n -t string -s 'tasklist'"))
        assertTrue(profile.contains("/plugins/plugin-207/show-labels bool 'false'"))
        assertTrue(profile.contains("/plugins/plugin-207/grouping bool 'true'"))
        assertTrue(profile.contains("TUXSPAN_DESKTOP_LABELS_BEGIN"))
        assertTrue(profile.contains("Name=TuxSpan"))
        assertTrue(profile.contains("emblem-symbolic-link.svg"))
        assertTrue(profile.contains("/Net/IconThemeName string 'TuxSpan'"))
        assertTrue(profile.contains("xdg-mime default org.xfce.Parole.desktop"))
        assertTrue(profile.contains("xdg-mime default tuxspan-writer.desktop"))
        assertTrue(profile.contains("libreoffice --norestore --writer"))
        assertTrue(profile.contains("xdg-mime default tuxspan-calc.desktop"))
        assertTrue(profile.contains("xdg-mime default tuxspan-impress.desktop"))
        assertTrue(profile.contains("tuxspan-auto-keyboard.py"))
        assertTrue(profile.contains("python3-pyatspi"))
        assertTrue(profile.contains("exec env NO_AT_BRIDGE=0 python3"))
        assertTrue(profile.contains("for inherited_gid in ${'$'}(id -G 2>/dev/null)"))
        assertTrue(profile.contains("tuxspan-android-%s:x:%s:"))
        assertTrue(profile.contains("TUXSPAN_DOWNLOADS_READY"))
        assertTrue(profile.contains("file:///root/Android-Downloads Android Downloads"))
        assertTrue(profile.contains("rmdir \"${'$'}HOME/Downloads\""))
        assertTrue(profile.contains("ln -sfn \"${'$'}HOME/Android-Downloads\" \"${'$'}HOME/Downloads\""))
        assertTrue(profile.contains("light-locker.desktop nm-applet.desktop"))
        assertTrue(profile.contains("polkit-gnome-authentication-agent-1.desktop"))
        assertTrue(profile.contains("Hidden=true"))
        assertTrue(profile.contains("tuxspan-autoconfig.js"))
        assertTrue(profile.contains("browser.sessionstore.resume_from_crash"))
        assertTrue(profile.contains("browser.shell.checkDefaultBrowser"))
        assertTrue(profile.contains("defaultPref"))

        val encodedPositioner = profile
            .substringAfter("printf '%s' '")
            .substringBefore("' | base64 -d > \"${'$'}dock_positioner\"")
        val positioner = String(
            Base64.getDecoder().decode(encodedPositioner),
            StandardCharsets.UTF_8,
        )
        assertTrue(positioner.contains("xrandr --current"))
        assertTrue(positioner.contains("dock_x=${'$'}((screen_width / 2))"))
        assertTrue(positioner.contains("dock_y=${'$'}((screen_height - panel_size / 2 - bottom_inset))"))
    }

    @Test
    fun studioProfileMigratesToMozillaFirefoxDeb() {
        val profile = WorkspaceScripts.guestExperienceScript(
            WorkspaceCatalog.studio,
            ExperienceSettings(),
            192,
        )

        assertTrue(profile.contains("create_trusted_launcher tuxspan-firefox Firefox 'firefox' 'firefox'"))
        assertTrue(profile.contains("default-web-browser 'firefox.desktop'"))
        assertTrue(profile.contains("https://packages.mozilla.org/apt mozilla main"))
        assertTrue(profile.contains("apt-get install -y firefox"))
        assertTrue(profile.contains("/usr/lib/firefox"))
        assertTrue(profile.contains("tuxspan.cfg"))
        assertTrue(profile.contains("rm -f \"${'$'}HOME/Desktop/Web.desktop\""))
        assertFalse(profile.contains("epiphany-browser"))
    }

    @Test
    fun desktopRecipeUsesPinnedImageAndNamespacedWorkspace() {
        val script = WorkspaceScripts.install(WorkspaceCatalog.canvas)

        assertTrue(script.contains("debian:12"))
        assertTrue(script.contains("tuxspan-canvas"))
        assertTrue(script.contains("termux-x11-nightly"))
        assertTrue(script.contains("mousepad atril ristretto parole"))
        assertTrue(script.contains("gstreamer1.0-plugins-good"))
        assertTrue(script.contains("libreoffice-writer libreoffice-calc libreoffice-impress"))
        assertTrue(script.contains("xarchiver p7zip-full"))
        assertTrue(script.contains("galculator catfish plocate"))
        assertTrue(script.contains("xfce4-screenshooter xfce4-taskmanager"))
        assertTrue(script.contains("PRUNE_BIND_MOUNTS=\"yes\""))
        assertTrue(script.contains("/sdcard /storage"))
        assertTrue(script.contains("proot-distro login --env \"PROOT_TMP_DIR=${'$'}tuxspan_proot_tmp\" 'tuxspan-canvas' -- /bin/sh -lc"))
        assertTrue(script.contains("proot-tmp"))
        assertFalse(script.contains("proot-distro list"))
        assertTrue(script.contains("| base64 -d >"))
        assertFalse(script.contains("TUXSPAN_LAUNCHER"))

        val encodedLauncher = script
            .substringAfter("printf '%s' '")
            .substringBefore("' | base64 -d >")
        val launcher = String(
            Base64.getDecoder().decode(encodedLauncher),
            StandardCharsets.UTF_8,
        )
        assertTrue(launcher.contains("dbus-run-session -- startxfce4"))
        assertTrue(launcher.contains("NO_AT_BRIDGE=0"))
        assertTrue(launcher.startsWith("#!/data/data/com.termux/files/usr/bin/bash"))
    }

    @Test
    fun terminalRecipeDoesNotInstallX11Packages() {
        val script = WorkspaceScripts.install(WorkspaceCatalog.spark)

        assertTrue(script.contains("alpine:3.23"))
        assertFalse(script.contains("termux-x11-nightly"))
        assertFalse(script.contains("startxfce4"))
    }

    @Test
    fun generatedRecipesDoNotFetchUnreviewedRemoteScripts() {
        WorkspaceCatalog.all.forEach { recipe ->
            val script = WorkspaceScripts.install(recipe)
            assertFalse(script.lineSequence().any { it.trimStart().startsWith("curl ") })
            assertFalse(script.lineSequence().any { it.trimStart().startsWith("wget ") })
            assertFalse(script.contains("raw.githubusercontent.com"))
        }
    }

    @Test
    fun starterBundleMatchesTheDefaultReviewedDesktopApps() {
        val starter = WorkspaceScripts.installBundle(WorkspaceCatalog.canvas, "starter")

        assertTrue(starter.contains("apt-get update"))
        assertTrue(starter.contains("mousepad atril ristretto parole"))
        assertTrue(starter.contains("at-spi2-core python3-pyatspi xinput"))
        assertTrue(starter.contains("gstreamer1.0-libav"))
        assertTrue(starter.contains("libreoffice-writer libreoffice-calc libreoffice-impress"))
        assertTrue(starter.contains("xarchiver p7zip-full"))
        assertTrue(starter.contains("xdg-mime default xarchiver.desktop"))
        assertTrue(starter.contains("catfish plocate xfce4-screenshooter xfce4-taskmanager"))
        assertTrue(starter.contains("fonts-noto-color-emoji"))
        assertTrue(starter.contains("create_trusted_launcher tuxspan-photos"))
        assertTrue(starter.contains("create_trusted_launcher tuxspan-media"))
        assertTrue(starter.contains("create_trusted_launcher tuxspan-firefox"))
        assertTrue(starter.contains("xdg-settings set default-web-browser"))
        assertTrue(starter.contains("firefox-esr.desktop"))
        assertTrue(starter.contains("/usr/local/share/applications"))
        assertTrue(starter.contains("create_trusted_launcher tuxspan-photos"))
        assertTrue(starter.contains("ln -sfn"))
        assertFalse(starter.contains("metadata::trusted"))
        assertFalse(starter.contains("Exec=x-www-browser"))
        assertTrue(starter.contains("xdg-mime default atril.desktop"))
        assertFalse(starter.contains("gimp"))
        assertFalse(starter.contains("nodejs"))
    }

    @Test
    fun creatorPackUsesArmFriendlyEditorsAndOrganizesItsShortcuts() {
        val creator = WorkspaceScripts.installBundle(WorkspaceCatalog.canvas, "creator")

        assertTrue(creator.contains("gimp inkscape audacity openshot-qt scribus"))
        assertTrue(creator.contains("ffmpeg imagemagick lame"))
        assertTrue(creator.contains("Desktop/Creator Tools"))
        assertTrue(creator.contains("create_pack_launcher tuxspan-creator-gimp GIMP"))
        assertTrue(creator.contains("tuxspan-creator-audacity Audacity"))
        assertTrue(creator.contains("tuxspan-creator-openshot OpenShot"))
        assertTrue(creator.contains("ln -sfn"))
        assertFalse(creator.contains("blender"))
        assertFalse(creator.contains("obs-studio"))
    }

    @Test
    fun developerPackCoversCommonLanguageAndDebuggingWorkflows() {
        val developer = WorkspaceScripts.installBundle(WorkspaceCatalog.canvas, "developer")

        assertTrue(developer.contains("build-essential clang cmake ninja-build pkg-config gdb"))
        assertTrue(developer.contains("python3-pip python3-venv python3-dev nodejs npm"))
        assertTrue(developer.contains("openjdk-17-jdk-headless golang-go rustc cargo"))
        assertTrue(developer.contains("sqlite3 sqlitebrowser jq shellcheck openssh-client geany meld"))
        assertTrue(developer.contains("Desktop/Developer Tools"))
        assertTrue(developer.contains("create_pack_launcher tuxspan-developer-geany Geany"))
        assertTrue(developer.contains("tuxspan-developer-sqlite"))
        assertTrue(developer.contains("DB Browser for SQLite"))
        assertTrue(developer.contains("ln -sfn"))
    }

    @Test
    fun shellQuoteEscapesSingleQuotes() {
        assertEquals("'one'\"'\"'two'", WorkspaceScripts.shellQuote("one'two"))
    }

    @Test
    fun removalTargetsOnlySelectedNamespace() {
        val script = WorkspaceScripts.remove(WorkspaceCatalog.studio)

        assertTrue(script.contains("proot-distro remove 'tuxspan-studio'"))
        assertFalse(script.contains("rm -rf"))
    }

    @Test
    fun manualInstallCommandRoundTripsReviewedScript() {
        val command = WorkspaceScripts.manualInstallCommand(WorkspaceCatalog.canvas)
        val encoded = command.substringAfter("printf '%s' '").substringBefore("'")
        val decoded = String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8)

        assertEquals(WorkspaceScripts.install(WorkspaceCatalog.canvas), decoded)
        assertTrue(command.endsWith("| base64 -d | bash"))
        assertFalse(command.contains('\n'))
    }

    @Test
    fun verificationHealthChecksContainerWithoutParsingHumanOutput() {
        val script = WorkspaceScripts.verify(WorkspaceCatalog.canvas)

        assertTrue(script.contains("proot-distro login --env \"PROOT_TMP_DIR=${'$'}tuxspan_proot_tmp\" 'tuxspan-canvas' -- /bin/sh -lc"))
        assertTrue(script.contains("proot-tmp"))
        assertTrue(script.contains("command -v startxfce4"))
        assertTrue(script.contains("command -v dbus-run-session"))
        assertFalse(script.contains("proot-distro list"))
        assertTrue(script.contains("TUXSPAN_READY"))
    }

    @Test
    fun discoveryChecksEveryBlueprintAndEmitsStableWorkspaceRecords() {
        val script = WorkspaceScripts.discoverWorkspaces()

        WorkspaceCatalog.all.forEach { recipe ->
            assertTrue(script.contains("${recipe.id}.status"))
            assertTrue(script.contains("tuxspan-${recipe.id}"))
            assertTrue(script.contains("TUXSPAN_WORKSPACE|${recipe.id}|READY"))
            assertTrue(script.contains("TUXSPAN_WORKSPACE|${recipe.id}|INSTALL_DISPATCHED"))
            if (recipe.kind == dev.tuxspan.mobile.model.WorkspaceKind.DESKTOP) {
                assertTrue(script.contains("TUXSPAN_WORKSPACE|${recipe.id}|NEEDS_REPAIR"))
            }
        }
    }

    @Test
    fun trackedInstallCapturesLogsAndReturnsCompactMarker() {
        val script = WorkspaceScripts.trackedInstall(WorkspaceCatalog.canvas)

        assertTrue(script.contains("canvas.status"))
        assertTrue(script.contains("canvas.log"))
        assertTrue(script.contains("canvas.progress"))
        assertTrue(script.contains("Installing Linux apps"))
        assertTrue(script.contains("READY|100|7|7|Setup complete"))
        assertTrue(script.contains("TUXSPAN_INSTALL_READY"))
        assertTrue(script.contains("canvas.session"))
        assertTrue(script.contains("canvas-launch.log"))
        assertTrue(script.contains("tail -n 30"))
        assertTrue(script.contains("TuxSpan is preparing"))
        assertFalse(script.contains("here-document"))
        assertFalse(script.contains("TUXSPAN_LAUNCHER"))
    }

    @Test
    fun progressProbeReportsDurableMachineReadableStages() {
        val script = WorkspaceScripts.installProgress(WorkspaceCatalog.studio)

        assertTrue(script.contains("studio.progress"))
        assertTrue(script.contains("studio.status"))
        assertTrue(script.contains("TUXSPAN_PROGRESS|"))
        assertTrue(script.contains("RUNNING|-1|0|7"))
        assertTrue(script.contains("READY|100|7|7"))
        assertTrue(script.contains("FAILED|-1|0|7"))
    }

    @Test
    fun progressParserKeepsStageAndDisplayCopyTogether() {
        val progress = InstallProgress.parse(
            "noise\nTUXSPAN_PROGRESS|RUNNING|58|6|7|Installing Linux apps|Downloading reviewed apps.",
        )

        requireNotNull(progress)
        assertEquals(InstallProgressStatus.RUNNING, progress.status)
        assertEquals(58, progress.percent)
        assertEquals(6, progress.step)
        assertEquals(7, progress.totalSteps)
        assertEquals("Installing Linux apps", progress.title)
        assertEquals("Downloading reviewed apps.", progress.detail)
    }

    @Test
    fun x11PreferencesUseNativeFullscreenAndKeepGestureRecovery() {
        val command = WorkspaceScripts.x11Preferences(ExperienceSettings())

        assertTrue(command.contains("com.termux.x11.CHANGE_PREFERENCE"))
        assertTrue(command.contains("input and display preferences"))
        assertTrue(command.contains("--es showAdditionalKbd true"))
        assertTrue(command.contains("--es additionalKbdVisible false"))
        assertFalse(command.contains("com.termux.x11.ACTION_CUSTOM"))
        assertTrue(command.contains("--es displayResolutionMode native"))
        assertTrue(command.contains("--es touchMode \"1\""))
        assertTrue(command.contains("--es forceOrientation \"landscape\""))
        assertTrue(command.contains("--es hideCutout false"))
        assertTrue(command.contains("--es swipeUpAction \"toggle additional key bar\""))
        assertTrue(command.contains("--es swipeDownAction \"no action\""))
        assertTrue(command.contains("--es backButtonAction \"toggle soft keyboard\""))
        assertTrue(command.contains("--es showIMEWhileExternalConnected false"))
        assertTrue(command.contains("--es showMouseHelper false"))
        assertTrue(command.contains("--es fullscreen true"))
        assertTrue(command.contains("TUXSPAN_X11_READY"))
        assertFalse(command.contains("showAdditionalKbd\"=\"true"))
    }

    @Test
    fun dynamicLaunchAppliesProfileAndOptionalDownloadsBind() {
        val settings = ExperienceSettings(downloadsBridgeEnabled = true)
        val command = WorkspaceScripts.launch(WorkspaceCatalog.canvas, settings, 168)

        assertTrue(command.contains("termux-x11 :1 -ac -dpi 168"))
        assertTrue(command.contains("XCURSOR_THEME=Adwaita XCURSOR_SIZE=48"))
        assertTrue(command.contains("DISPLAY=:1"))
        assertTrue(command.contains("unset XAUTHORITY"))
        assertTrue(command.contains("com.termux.x11.ACTION_STOP"))
        assertTrue(command.contains("x11_pattern='^(termux-x11|.*/termux-x11) .*:1("))
        assertFalse(command.contains("[t]ermux-x11.*:1"))
        assertTrue(command.contains("desktop-session.token"))
        assertTrue(command.contains("owns_session"))
        assertTrue(command.contains("stop_owned_display"))
        assertTrue(command.contains("module-native-protocol-tcp"))
        assertTrue(command.contains("PULSE_RUNTIME_PATH=\"${'$'}pulse_runtime_dir\""))
        assertTrue(command.contains("pulse-runtime"))
        assertTrue(command.contains("pulseaudio --daemonize=no --exit-idle-time=-1"))
        assertTrue(command.contains("--load=\"module-native-protocol-tcp auth-ip-acl=127.0.0.1 auth-anonymous=1\""))
        assertTrue(command.contains("pulse_ready_attempt in 1 2 3 4 5 6 7 8 9 10"))
        assertTrue(command.contains("kill -0 \"${'$'}pulse_pid\""))
        assertFalse(command.contains("pulseaudio --start"))
        assertTrue(command.contains("stable display socket after 3 attempts"))
        assertTrue(command.contains("PROOT_TMP_DIR"))
        assertTrue(command.contains("proot-tmp"))
        assertTrue(command.contains("login --env \"PROOT_TMP_DIR=${'$'}proot_tmp_dir\""))
        assertTrue(command.contains("ready_streak"))
        assertTrue(command.contains("if test -S /tmp/.X11-unix/X1"))
        assertTrue(command.contains("display socket was not available after 45 seconds"))
        assertTrue(command.contains("rm -f \"${'$'}x11_tmp_dir/.X11-unix/X1\""))
        assertTrue(command.contains("Android-Downloads"))
        assertTrue(command.contains("--bind"))
        assertTrue(command.contains("/storage/emulated/0/Download"))
        assertTrue(command.contains("/sdcard/Download"))
        assertTrue(command.contains(".tuxspan-write-probe-"))
        assertTrue(command.contains("bind_args=(--bind \"${'$'}downloads_source:/root/Android-Downloads\")"))
        assertTrue(command.contains("downloads_ready=true"))
        assertTrue(command.contains("--env \"TUXSPAN_DOWNLOADS_READY=${'$'}downloads_ready\""))
        assertTrue(command.contains("xfce4"))
        assertTrue(command.contains("NO_AT_BRIDGE=0"))
        assertTrue(command.contains("dbus-run-session"))
        assertTrue(command.contains("/bin/sh -c"))
        assertTrue(command.contains("exec startxfce4"))
        assertTrue(command.contains("XDG_RUNTIME_DIR=\"/tmp/tuxspan-runtime-canvas\""))
        assertTrue(command.contains("host-backed"))
        assertTrue(command.contains("export TMPDIR=/tmp"))
        assertTrue(command.contains("--bind \"${'$'}x11_tmp_dir:/tmp\""))
        assertFalse(command.contains("--shared-tmp"))
        assertFalse(command.contains("-listen tcp"))
        assertFalse(command.contains("MIT-MAGIC-COOKIE"))
        assertFalse(command.contains("xdpyinfo"))
        assertFalse(command.contains("dbus-launch"))
        assertFalse(command.contains("curl "))
        assertFalse(command.contains("wget "))
    }

    @Test
    fun storagePermissionRequestRepairsPartialSetupWithoutWaitingForTerminalInput() {
        val command = WorkspaceScripts.requestStorageAccess()

        assertTrue(command.contains("command -v termux-setup-storage"))
        assertTrue(command.contains("printf 'y\\n' | termux-setup-storage"))
        assertTrue(command.contains("TUXSPAN_STORAGE_REQUESTED"))
    }

    @Test
    fun storageStatusRequiresRealReadWriteAccessAndRepairsDownloadsLink() {
        val command = WorkspaceScripts.storageAccessStatus()

        assertTrue(command.contains("${'$'}HOME/storage/downloads"))
        assertTrue(command.contains("/storage/emulated/0/Download"))
        assertTrue(command.contains(".tuxspan-write-probe-"))
        assertTrue(command.contains("TUXSPAN_STORAGE_READY"))
        assertTrue(command.contains("TUXSPAN_STORAGE_NOT_READY"))
        assertTrue(command.contains("ln -s \"${'$'}downloads_source\""))
    }

    @Test
    fun autoKeyboardWatcherRequestsImeOnlyForEditableFocusSessions() {
        val watcher = WorkspaceScripts.autoKeyboardWatcherScript()

        assertTrue(watcher.contains("object:state-changed:focused"))
        assertTrue(watcher.contains("STATE_EDITABLE"))
        assertTrue(watcher.contains("ROLE_PASSWORD_TEXT"))
        assertTrue(watcher.contains("ROLE_TERMINAL"))
        assertTrue(watcher.contains("keyboard_requested"))
        assertTrue(watcher.contains("[\"stdbuf\", \"-oL\", \"xinput\", \"test-xi2\", \"--root\"]"))
        assertTrue(watcher.contains("xinput pointer monitor stopped"))
        assertTrue(watcher.contains("(TouchEnd)"))
        assertTrue(watcher.contains("(ButtonRelease)"))
        assertTrue(watcher.contains("focused_text_control"))
        assertTrue(watcher.contains("request_android_keyboard(\"pointer focus\")"))
        assertFalse(watcher.contains("request_android_keyboard(\"accessibility focus\")"))
        assertTrue(watcher.contains("com.termux.x11.ACTION_CUSTOM"))
        assertTrue(watcher.contains("backButton"))
        assertTrue(watcher.contains("/data/data/com.termux/files/usr/bin/am"))
        assertTrue(watcher.contains("\"--user\", \"0\""))
    }

    @Test
    fun trackedLaunchReturnsQuickMarkerAndCapturesFailures() {
        val command = WorkspaceScripts.trackedLaunch(
            WorkspaceCatalog.canvas,
            ExperienceSettings(),
            160,
        )
        assertTrue(command.contains("canvas-launch.log"))
        assertTrue(command.contains("canvas-launch.pid"))
        assertTrue(command.contains("desktop-session.token"))
        assertTrue(command.contains("*-launch.pid"))
        assertTrue(command.contains("/proc/${'$'}stale_pid/cmdline"))
        assertTrue(command.contains("stale_cmdline="))
        assertTrue(command.contains("2>/dev/null < \"/proc/${'$'}stale_pid/cmdline\" || true"))
        assertTrue(command.contains("proot_pattern='^(proot|.*/proot) .*proot-distro/containers/tuxspan-'"))
        assertTrue(command.contains("pulse_pattern='^(pulseaudio|.*/pulseaudio)( |"))
        assertTrue(command.contains("pkill -KILL -f \"${'$'}atspi_dbus_pattern\""))
        assertFalse(command.contains("[p]root.*proot-distro/containers/tuxspan-"))
        assertTrue(command.contains("sleep 0.5"))
        assertTrue(command.contains("while test \"${'$'}attempt\" -lt 90"))
        assertTrue(command.contains("nohup bash"))
        assertTrue(command.contains("launch_ready=true"))
        assertTrue(command.contains("kill -0"))
        assertTrue(command.contains("proot-tmp"))
        assertTrue(command.contains("Current launch state"))
        assertTrue(command.contains("TUXSPAN_LAUNCH_STARTED"))
        assertTrue(command.contains("tail -n 35"))
        assertFalse(command.contains("here-document"))
    }

    @Test
    fun displayPreparationRequiresAStableSocketBeforeOpeningAndroidSurface() {
        val command = WorkspaceScripts.prepareDisplay(192)

        assertTrue(command.contains("termux-x11 :1 -ac -dpi 192"))
        assertTrue(command.contains("test -S \"${'$'}x11_tmp_dir/.X11-unix/X1\""))
        assertTrue(command.contains("ready_streak"))
        assertTrue(command.contains(".X1-lock"))
        assertTrue(command.contains("TUXSPAN_DISPLAY_READY"))
        assertTrue(command.contains("display_attempt in 1 2 3"))
    }

    @Test
    fun failedSessionStatusIncludesRecentLaunchDiagnostics() {
        val command = WorkspaceScripts.sessionStatus(WorkspaceCatalog.studio)

        assertTrue(command.contains("TUXSPAN_ERROR\\n"))
        assertTrue(command.contains("studio-launch.log"))
        assertTrue(command.contains("tail -n 12"))
    }

    @Test
    fun stopAndStatusStayInsideSelectedWorkspaceNamespace() {
        val stop = WorkspaceScripts.stop(WorkspaceCatalog.canvas)
        val status = WorkspaceScripts.sessionStatus(WorkspaceCatalog.canvas)

        assertTrue(stop.contains("proot_pattern='^(proot|.*/proot) .*proot-distro/containers/tuxspan-canvas/'"))
        assertFalse(stop.contains("[p]root.*proot-distro/containers/tuxspan-canvas/"))
        assertTrue(stop.contains("pkill -KILL"))
        assertTrue(stop.contains("PROOT_TMP_DIR"))
        assertTrue(stop.contains("desktop-session.token"))
        assertTrue(stop.contains("stopping-canvas"))
        assertTrue(stop.contains("com.termux.x11.ACTION_STOP"))
        assertFalse(stop.contains("pkill -TERM -x proot"))
        assertFalse(stop.contains("pkill -TERM -x xfce4-session"))
        assertTrue(status.contains("TUXSPAN_RUNNING"))
        assertTrue(status.contains("launch_cmdline="))
        assertTrue(status.contains("2>/dev/null < \"/proc/${'$'}launch_pid/cmdline\" || true"))
        assertTrue(status.contains("TUXSPAN_STOPPED"))
        assertTrue(status.contains("canvas-launch.pid"))
    }

    @Test
    fun portraitAndDirectTouchRemainSelectable() {
        val settings = ExperienceSettings(
            screenOrientation = ScreenOrientation.PORTRAIT,
            touchMode = TouchMode.DIRECT,
        )
        val command = WorkspaceScripts.x11Preferences(settings)

        assertTrue(command.contains("--es forceOrientation \"portrait\""))
        assertTrue(command.contains("--es touchMode \"3\""))
    }

    @Test
    fun explicitControlVisibilityCommandsUseSupportedPreferenceSyntax() {
        val show = WorkspaceScripts.showX11Controls()
        val hide = WorkspaceScripts.hideX11Controls()

        assertTrue(show.contains("--es showAdditionalKbd true"))
        assertTrue(show.contains("--es additionalKbdVisible true"))
        assertTrue(show.contains("--es showMouseHelper false"))
        assertFalse(show.contains("com.termux.x11.ACTION_CUSTOM"))
        assertTrue(show.contains("TUXSPAN_X11_CONTROLS_SHOWN"))
        assertTrue(hide.contains("--es showAdditionalKbd true"))
        assertTrue(hide.contains("--es additionalKbdVisible false"))
        assertTrue(hide.contains("--es showMouseHelper false"))
        assertFalse(hide.contains("com.termux.x11.ACTION_CUSTOM"))
        assertTrue(hide.contains("TUXSPAN_X11_CONTROLS_HIDDEN"))
    }
}
