package dev.tuxspan.mobile.workspace

import dev.tuxspan.mobile.model.WorkspaceKind
import dev.tuxspan.mobile.model.WorkspaceCatalog
import dev.tuxspan.mobile.model.WorkspaceRecipe
import java.nio.charset.StandardCharsets
import java.util.Base64

object WorkspaceScripts {
    // Mozilla's official APT signing key, fetched from packages.mozilla.org and
    // pinned to the fingerprint published in Mozilla's Linux installation guide.
    private const val MOZILLA_APT_KEY_BASE64 =
        "LS0tLS1CRUdJTiBQR1AgUFVCTElDIEtFWSBCTE9DSy0tLS0tCgp4c0JOQkdDUnQ3TUJDQURrWUpISFFRb0w2dEtyVy9MYm1mUjls" +
            "ano3aWIyYVdubzRKTzNWS1F2THdqeVVNUHBxCi9TWFhNT254OGpYd2dXaXpwUHhRWURSSjBTUVhTOVVMSjFoWFJML09nTW5aQVl2" +
            "WURlVjJqQm5Lc0FJRWRpRy8KZTFxbThQNFc5cXBXSmMraE5xN0ZPVDEzUnpHV1J4NTdTZExXU1hvMEtlWTM4cjlsdmpqT21UL2N1" +
            "T2NtandsRApUOVhZZi9SU08reUovQXN5TWRBcitaYkRlUVVkOUhZSmlQZEkwNGxHYUdNMDJNakRNbngrbW9uYyt5NTR0K1orCnJ5" +
            "MVd0UWR6b1F0OWRIbElQbFYxdFIreFY1REhIc2VqQ1p4dTlUV3p6U2xMNXdmQkJlRXo3Ui9PSXppdkdKcFcKUWRKemQrMlFEWFNS" +
            "ZzlxMlhZV1A1WlZ0U2dqVlZKak5sYjZaQUJFQkFBSE5WRUZ5ZEdsbVlXTjBJRkpsWjJsegpkSEo1SUZKbGNHOXphWFJ2Y25rZ1Uy" +
            "bG5ibVZ5SUR4aGNuUnBabUZqZEMxeVpXZHBjM1J5ZVMxeVpYQnZjMmwwCmIzSjVMWE5wWjI1bGNrQm5iMjluYkdVdVkyOXRQc0xB" +
            "amdRVEFRb0FPQlloQkRXNm9MTStuck9XOVp5b09NQzYKWE9iY1l4V2pCUUpna2JlekFoc0RCUXNKQ0FjQ0JoVUtDUWdMQWdRV0Fn" +
            "TUJBaDRCQWhlQUFBb0pFTUM2WE9iYwpZeFdqK2lnSUFNRmg2RHJBWU1lcTlzYloxWkc2b0FNcmluVWhlR1FiRXFlNzZuSURRTnNa" +
            "bmhEd1oyd1dxZ1ZDCjdEZ09NcWxoUW1PbXptN002TnptcTJkdlB3cTN4QzJPZUk5ZlF5empUNzJkZUJUekxQN1BKb2s5UEpGT01k" +
            "TGYKSUxTc1VubU1zaGVRdDREVU8wallBWDJLVXVXT0lYWEphWjMxOVF5b1JOQlBZYTVxejdxWFM3d0hMT1k4OUlEcQpmSHQ2QXVk" +
            "OEVSNXpoeU95aHl0Y1lNZWFHQzFnMUlLV21nZXduaEVxMDJGYW50TUpHbG1tRmkyZUEwRVBEMDJHCkMzNzQyUUdxUnhMd2pXc201" +
            "L1RweXVVMjRFWUtSR0NSbTdRZFZJbzN1Z0ZTZXRLcm4wYnlPeFdHQnZ0dTRmSDgKWFd2WmtSVCt1K3l6SDFzNXlGWUJxYzJKVHJy" +
            "SnZSVT0KPVFudk4KLS0tLS1FTkQgUEdQIFBVQkxJQyBLRVkgQkxPQ0stLS0tLQo="

    private const val DESKTOP_STARTER_PACKAGES =
        "mousepad atril ristretto parole " +
            "at-spi2-core python3-pyatspi xinput " +
            "gstreamer1.0-plugins-base gstreamer1.0-plugins-good gstreamer1.0-plugins-bad " +
            "gstreamer1.0-plugins-ugly gstreamer1.0-libav ffmpegthumbnailer " +
            "xarchiver p7zip-full zip unzip unrar-free thunar-archive-plugin " +
            "galculator catfish plocate xfce4-screenshooter xfce4-taskmanager " +
            "libreoffice-writer libreoffice-calc libreoffice-impress libreoffice-gtk3 " +
            "hunspell-en-us fonts-noto-core fonts-noto-color-emoji fonts-liberation2 " +
            "nano curl wget htop xdg-utils desktop-file-utils"

    private const val CREATOR_PACKAGES =
        "gimp inkscape audacity openshot-qt scribus ffmpeg imagemagick lame"

    private const val DEVELOPER_PACKAGES =
        "build-essential clang cmake ninja-build pkg-config gdb git " +
            "python3 python3-pip python3-venv python3-dev nodejs npm " +
            "openjdk-17-jdk-headless golang-go rustc cargo " +
            "sqlite3 sqlitebrowser jq shellcheck openssh-client geany meld"

    fun workspaceName(recipe: WorkspaceRecipe): String = "tuxspan-${recipe.id}"

    fun install(recipe: WorkspaceRecipe): String {
        val name = workspaceName(recipe)
        val totalSteps = if (recipe.kind == WorkspaceKind.DESKTOP) 7 else 6
        val filesystemStep = if (recipe.kind == WorkspaceKind.DESKTOP) 5 else 4
        val guestStep = filesystemStep + 1
        val finalizeStep = totalSteps
        val guestSetup = when (recipe.kind) {
            WorkspaceKind.DESKTOP -> desktopGuestSetup(recipe)
            WorkspaceKind.TERMINAL -> terminalGuestSetup()
        }
        val hostDesktopSetup = if (recipe.kind == WorkspaceKind.DESKTOP) {
            """
                tuxspan_progress 30 4 $totalSteps 'Installing display support' 'Preparing Termux:X11 and audio packages.'
                pkg install -y x11-repo
                pkg install -y termux-x11-nightly pulseaudio
            """.trimIndent()
        } else {
            ""
        }
        val launcher = launcherScript(recipe)
        val encodedLauncher = Base64.getEncoder().encodeToString(
            launcher.toByteArray(StandardCharsets.UTF_8),
        )

        return """
            set -eu
            tuxspan_proot_tmp="${'$'}HOME/.local/state/tuxspan/proot-tmp"
            mkdir -p "${'$'}tuxspan_proot_tmp"
            chmod 700 "${'$'}tuxspan_proot_tmp"
            tuxspan_progress() {
              if [ -n "${'$'}{TUXSPAN_PROGRESS_FILE:-}" ]; then
                printf 'RUNNING|%s|%s|%s|%s|%s\n' \
                  "${'$'}1" "${'$'}2" "${'$'}3" "${'$'}4" "${'$'}5" > "${'$'}TUXSPAN_PROGRESS_FILE"
              fi
            }
            tuxspan_run_with_estimated_progress() {
              progress_value="${'$'}1"
              progress_cap="${'$'}2"
              progress_step="${'$'}3"
              progress_total="${'$'}4"
              progress_title="${'$'}5"
              progress_detail="${'$'}6"
              shift 6

              "${'$'}@" &
              progress_command_pid="${'$'}!"
              (
                while kill -0 "${'$'}progress_command_pid" >/dev/null 2>&1; do
                  tuxspan_progress \
                    "${'$'}progress_value" \
                    "${'$'}progress_step" \
                    "${'$'}progress_total" \
                    "${'$'}progress_title" \
                    "${'$'}progress_detail"
                  sleep 15
                  if [ "${'$'}progress_value" -lt "${'$'}progress_cap" ]; then
                    progress_value=${'$'}((progress_value + 1))
                  fi
                done
              ) &
              progress_ticker_pid="${'$'}!"

              if wait "${'$'}progress_command_pid"; then
                progress_exit_code=0
              else
                progress_exit_code="${'$'}?"
              fi
              kill "${'$'}progress_ticker_pid" >/dev/null 2>&1 || true
              wait "${'$'}progress_ticker_pid" >/dev/null 2>&1 || true
              return "${'$'}progress_exit_code"
            }
            tuxspan_progress 5 1 $totalSteps 'Preparing Termux' 'Starting the reviewed setup.'
            printf '\nTuxSpan is preparing %s.\n' '${recipe.name}'
            tuxspan_progress 12 2 $totalSteps 'Updating Termux' 'Refreshing package information.'
            pkg update -y
            tuxspan_progress 22 3 $totalSteps 'Installing Linux tools' 'Preparing the container manager.'
            pkg install -y proot-distro
            $hostDesktopSetup
            tuxspan_progress 42 $filesystemStep $totalSteps 'Creating Linux filesystem' 'Downloading or checking the ${recipe.image} image.'
            if proot-distro login --env "PROOT_TMP_DIR=${'$'}tuxspan_proot_tmp" '$name' -- /bin/true >/dev/null 2>&1; then
              printf 'Workspace %s already exists; keeping its files.\n' '$name'
            else
              proot-distro install --name '$name' '${recipe.image}'
            fi
            tuxspan_run_with_estimated_progress \
              58 92 $guestStep $totalSteps \
              'Installing Linux apps' \
              'Downloading and configuring the reviewed Linux applications.' \
              proot-distro login --env "PROOT_TMP_DIR=${'$'}tuxspan_proot_tmp" '$name' -- /bin/sh -lc ${shellQuote(guestSetup)}
            tuxspan_progress 94 $finalizeStep $totalSteps 'Finalizing workspace' 'Creating launchers and recording the completed setup.'
            mkdir -p "${'$'}HOME/.local/bin"
            printf '%s' '$encodedLauncher' | base64 -d > "${'$'}HOME/.local/bin/tuxspan-${recipe.id}"
            chmod 700 "${'$'}HOME/.local/bin/tuxspan-${recipe.id}"
            # A successful build or repair supersedes any earlier launch error.
            printf 'stopped\n' > "${'$'}HOME/.local/state/tuxspan/${recipe.id}.session"
            rm -f "${'$'}HOME/.local/state/tuxspan/${recipe.id}-launch.pid"
            : > "${'$'}HOME/.local/state/tuxspan/${recipe.id}-launch.log"
            printf '\n%s is ready. Return to TuxSpan and tap Verify.\n' '${recipe.name}'
        """.trimIndent()
    }

    fun verify(recipe: WorkspaceRecipe): String {
        val name = workspaceName(recipe)
        val guestCheck = if (recipe.kind == WorkspaceKind.DESKTOP) {
            "command -v startxfce4 >/dev/null && command -v dbus-run-session >/dev/null"
        } else {
            "/bin/true"
        }
        return "tuxspan_proot_tmp=\"${'$'}HOME/.local/state/tuxspan/proot-tmp\"; " +
            "mkdir -p \"${'$'}tuxspan_proot_tmp\"; chmod 700 \"${'$'}tuxspan_proot_tmp\"; " +
            "test -x \"${'$'}HOME/.local/bin/tuxspan-${recipe.id}\" && " +
            "proot-distro login --env \"PROOT_TMP_DIR=${'$'}tuxspan_proot_tmp\" '$name' -- /bin/sh -lc ${shellQuote(guestCheck)} " +
            ">/dev/null 2>&1 && printf 'TUXSPAN_READY'"
    }

    /** Streams the reviewed install to Termux while retaining the same output for diagnostics. */
    fun trackedInstall(recipe: WorkspaceRecipe): String {
        val stateDir = "${'$'}HOME/.local/state/tuxspan"
        val statusFile = "${'$'}state_dir/${recipe.id}.status"
        val logFile = "${'$'}state_dir/${recipe.id}.log"
        val progressFile = "${'$'}state_dir/${recipe.id}.progress"
        val totalSteps = if (recipe.kind == WorkspaceKind.DESKTOP) 7 else 6
        return buildString {
            appendLine("set -u")
            appendLine("state_dir=\"$stateDir\"")
            appendLine("status_file=\"$statusFile\"")
            appendLine("log_file=\"$logFile\"")
            appendLine("progress_file=\"$progressFile\"")
            appendLine("mkdir -p \"${'$'}state_dir\"")
            appendLine("printf 'running\\n' > \"${'$'}status_file\"")
            appendLine("export TUXSPAN_PROGRESS_FILE=\"${'$'}progress_file\"")
            appendLine("printf 'RUNNING|5|1|$totalSteps|Preparing Termux|Starting the reviewed setup.\\n' > \"${'$'}progress_file\"")
            appendLine(": > \"${'$'}log_file\"")
            appendLine("set +e")
            appendLine("(")
            appendLine(install(recipe))
            appendLine(") 2>&1 | tee -a \"${'$'}log_file\"")
            appendLine("exit_code=${'$'}{PIPESTATUS[0]}")
            appendLine("if [ \"${'$'}exit_code\" -eq 0 ]; then")
            appendLine("  printf 'ready\\n' > \"${'$'}status_file\"")
            appendLine("  printf 'READY|100|$totalSteps|$totalSteps|Setup complete|The workspace is ready to launch.\\n' > \"${'$'}progress_file\"")
            appendLine("  printf 'TUXSPAN_INSTALL_READY'")
            appendLine("else")
            appendLine("  printf 'failed\\n' > \"${'$'}status_file\"")
            appendLine("  printf 'FAILED|-1|0|$totalSteps|Setup needs attention|Open the setup details and retry; existing files are preserved.\\n' > \"${'$'}progress_file\"")
            appendLine("  printf 'Setup failed. Last log lines:\\n' >&2")
            appendLine("  tail -n 30 \"${'$'}log_file\" >&2 || true")
            appendLine("  exit \"${'$'}exit_code\"")
            appendLine("fi")
        }.trimEnd()
    }

    fun installProgress(recipe: WorkspaceRecipe): String {
        val totalSteps = if (recipe.kind == WorkspaceKind.DESKTOP) 7 else 6
        return """
            state_dir="${'$'}HOME/.local/state/tuxspan"
            status_file="${'$'}state_dir/${recipe.id}.status"
            progress_file="${'$'}state_dir/${recipe.id}.progress"
            status=${'$'}(cat "${'$'}status_file" 2>/dev/null || printf 'unknown')
            if [ -s "${'$'}progress_file" ]; then
              printf 'TUXSPAN_PROGRESS|'
              tail -n 1 "${'$'}progress_file"
            elif [ "${'$'}status" = 'running' ]; then
              printf 'TUXSPAN_PROGRESS|RUNNING|-1|0|$totalSteps|Installing Linux packages|The live installation is continuing in Termux.'
            elif [ "${'$'}status" = 'ready' ]; then
              printf 'TUXSPAN_PROGRESS|READY|100|$totalSteps|$totalSteps|Setup complete|The workspace is ready to launch.'
            elif [ "${'$'}status" = 'failed' ]; then
              printf 'TUXSPAN_PROGRESS|FAILED|-1|0|$totalSteps|Setup needs attention|Retry setup; existing downloaded files will be preserved.'
            else
              printf 'TUXSPAN_PROGRESS|UNKNOWN|-1|0|$totalSteps|Connecting to Termux|Waiting for the background setup to report its first step.'
            fi
        """.trimIndent()
    }

    fun discoverWorkspaces(): String = buildString {
        appendLine("set +e")
        appendLine("tuxspan_proot_tmp=\"${'$'}HOME/.local/state/tuxspan/proot-tmp\"")
        appendLine("mkdir -p \"${'$'}tuxspan_proot_tmp\"")
        appendLine("chmod 700 \"${'$'}tuxspan_proot_tmp\"")
        WorkspaceCatalog.all.forEach { recipe ->
            appendLine("status=${'$'}(cat \"${'$'}HOME/.local/state/tuxspan/${recipe.id}.status\" 2>/dev/null)")
            appendLine("if [ \"${'$'}status\" = 'running' ]; then")
            appendLine("  printf 'TUXSPAN_WORKSPACE|${recipe.id}|INSTALL_DISPATCHED\\n'")
            appendLine(
                "elif test -x \"${'$'}HOME/.local/bin/tuxspan-${recipe.id}\" && " +
                    "proot-distro login --env \"PROOT_TMP_DIR=${'$'}tuxspan_proot_tmp\" '${workspaceName(recipe)}' -- /bin/true >/dev/null 2>&1; then",
            )
            if (recipe.kind == WorkspaceKind.DESKTOP) {
                val healthCheck =
                    "command -v startxfce4 >/dev/null && command -v dbus-run-session >/dev/null"
                appendLine(
                    "  if proot-distro login --env \"PROOT_TMP_DIR=${'$'}tuxspan_proot_tmp\" '${workspaceName(recipe)}' -- /bin/sh -lc " +
                        "${shellQuote(healthCheck)} >/dev/null 2>&1; then",
                )
                appendLine("    printf 'TUXSPAN_WORKSPACE|${recipe.id}|READY\\n'")
                appendLine("  else")
                appendLine("    printf 'TUXSPAN_WORKSPACE|${recipe.id}|NEEDS_REPAIR\\n'")
                appendLine("  fi")
            } else {
                appendLine("  printf 'TUXSPAN_WORKSPACE|${recipe.id}|READY\\n'")
            }
            appendLine("fi")
        }
    }.trimEnd()

    /** A one-line, paste-safe fallback for Android builds that block foreground RUN_COMMAND sessions. */
    fun manualInstallCommand(recipe: WorkspaceRecipe): String {
        val encoded = Base64.getEncoder().encodeToString(
            install(recipe).toByteArray(StandardCharsets.UTF_8),
        )
        return "printf '%s' '$encoded' | base64 -d | bash"
    }

    fun launch(
        recipe: WorkspaceRecipe,
        settings: ExperienceSettings = ExperienceSettings(),
        dpi: Int = 144,
    ): String {
        if (recipe.kind == WorkspaceKind.TERMINAL) {
            return "tuxspan_proot_tmp=\"${'$'}HOME/.local/state/tuxspan/proot-tmp\"; " +
                "mkdir -p \"${'$'}tuxspan_proot_tmp\"; chmod 700 \"${'$'}tuxspan_proot_tmp\"; " +
                "exec proot-distro login --env \"PROOT_TMP_DIR=${'$'}tuxspan_proot_tmp\" '${workspaceName(recipe)}'"
        }

        val name = workspaceName(recipe)
        val encodedProfile = Base64.getEncoder().encodeToString(
            guestExperienceScript(recipe, settings, dpi).toByteArray(StandardCharsets.UTF_8),
        )
        val cursorSize = settings.displayProfile.cursorSize
        val guestLaunch = """
            export DISPLAY=:1 PULSE_SERVER=127.0.0.1 XCURSOR_THEME=Adwaita XCURSOR_SIZE=$cursorSize
            unset XAUTHORITY
            export NO_AT_BRIDGE=0
            export XDG_CURRENT_DESKTOP=XFCE XDG_SESSION_DESKTOP=xfce DESKTOP_SESSION=xfce
            # PRoot cannot reliably expose Unix-domain sockets created below
            # its emulated root on every Android kernel. /tmp is the host-backed
            # X11 bind, so DBus and AT-SPI focus sockets remain reachable.
            export XDG_RUNTIME_DIR="/tmp/tuxspan-runtime-${recipe.id}"
            rm -rf "${'$'}XDG_RUNTIME_DIR"
            mkdir -p "${'$'}XDG_RUNTIME_DIR" "${'$'}HOME/.cache/sessions"
            chmod 700 "${'$'}XDG_RUNTIME_DIR"
            export TMPDIR=/tmp
            rm -f "${'$'}HOME/.cache/sessions/"*
            display_ready=false
            attempt=0
            while test "${'$'}attempt" -lt 45; do
              if test -S /tmp/.X11-unix/X1; then
                display_ready=true
                break
              fi
              attempt=${'$'}((attempt + 1))
              sleep 1
            done
            if test "${'$'}display_ready" != true; then
              printf 'Termux:X11 display socket was not available after 45 seconds.\n' >&2
              exit 1
            fi
            profile_file="${'$'}XDG_RUNTIME_DIR/experience-${'$'}${'$'}.sh"
            printf '%s' '$encodedProfile' | base64 -d > "${'$'}profile_file"
            chmod 700 "${'$'}profile_file"
            exec dbus-run-session -- /bin/sh -c \
              '(sleep 3; "${'$'}1") & exec startxfce4' tuxspan-session "${'$'}profile_file"
        """.trimIndent()
        val downloadsSetup = if (settings.downloadsBridgeEnabled) {
            """
                downloads_source=''
                for candidate in "${'$'}HOME/storage/downloads" /storage/emulated/0/Download /sdcard/Download; do
                  probe="${'$'}candidate/.tuxspan-write-probe-${'$'}${'$'}"
                  if test -d "${'$'}candidate" && \
                     ls -A "${'$'}candidate" >/dev/null 2>&1 && \
                     : > "${'$'}probe" 2>/dev/null; then
                    rm -f "${'$'}probe"
                    downloads_source="${'$'}candidate"
                    break
                  fi
                  rm -f "${'$'}probe" >/dev/null 2>&1 || true
                done
                if test -n "${'$'}downloads_source"; then
                  proot-distro login --env "PROOT_TMP_DIR=${'$'}proot_tmp_dir" '$name' -- /bin/mkdir -p /root/Android-Downloads >/dev/null 2>&1 || true
                  bind_args=(--bind "${'$'}downloads_source:/root/Android-Downloads")
                  downloads_ready=true
                else
                  printf 'Android Downloads permission or write access is not ready; starting without the bridge.\n' >&2
                fi
            """.trimIndent()
        } else {
            ""
        }

        return """
            set -u
            state_dir="${'$'}HOME/.local/state/tuxspan"
            status_file="${'$'}state_dir/${recipe.id}.session"
            token_file="${'$'}state_dir/desktop-session.token"
            mkdir -p "${'$'}state_dir"
            x11_tmp_dir="${'$'}state_dir/x11-tmp"
            mkdir -p "${'$'}x11_tmp_dir/.X11-unix"
            chmod 700 "${'$'}x11_tmp_dir"
            chmod 1777 "${'$'}x11_tmp_dir/.X11-unix"
            session_token="${recipe.id}-${'$'}${'$'}-${'$'}(date +%s)"
            printf '%s\n' "${'$'}session_token" > "${'$'}token_file"
            x11_pattern='^(termux-x11|.*/termux-x11) .*:1( |${'$'})'
            keyboard_pattern='^(python3|.*/python3) .*tuxspan-auto-keyboard[.]py( |${'$'})'
            dock_pattern='^((sh|dash)|.*/(sh|dash)) .*tuxspan-position-dock( |${'$'})'
            pulse_pattern='^(pulseaudio|.*/pulseaudio)( |${'$'})'
            atspi_launcher_pattern='^/usr/libexec/at-spi-bus-launcher( |${'$'})'
            atspi_registry_pattern='^/usr/libexec/at-spi2-registryd( |${'$'})'
            atspi_dbus_pattern='^(dbus-daemon|.*/dbus-daemon) --config-file=/usr/share/defaults/at-spi2/accessibility[.]conf( |${'$'})'
            owns_session() {
              test "${'$'}(cat "${'$'}token_file" 2>/dev/null)" = "${'$'}session_token"
            }
            set_session_state() {
              if owns_session; then printf '%s\n' "${'$'}1" > "${'$'}status_file"; fi
            }
            stop_owned_display() {
              if owns_session; then
                pkill -TERM -f "${'$'}keyboard_pattern" >/dev/null 2>&1 || true
                pkill -TERM -f "${'$'}dock_pattern" >/dev/null 2>&1 || true
                pkill -TERM -f "${'$'}pulse_pattern" >/dev/null 2>&1 || true
                pkill -TERM -f "${'$'}atspi_launcher_pattern" >/dev/null 2>&1 || true
                pkill -TERM -f "${'$'}atspi_registry_pattern" >/dev/null 2>&1 || true
                pkill -TERM -f "${'$'}atspi_dbus_pattern" >/dev/null 2>&1 || true
                sleep 0.2
                pkill -KILL -f "${'$'}atspi_launcher_pattern" >/dev/null 2>&1 || true
                pkill -KILL -f "${'$'}atspi_registry_pattern" >/dev/null 2>&1 || true
                pkill -KILL -f "${'$'}atspi_dbus_pattern" >/dev/null 2>&1 || true
                pkill -TERM -f "${'$'}x11_pattern" >/dev/null 2>&1 || true
                am broadcast -a com.termux.x11.ACTION_STOP -p com.termux.x11 >/dev/null 2>&1 || true
                rm -f "${'$'}x11_tmp_dir/.X11-unix/X1" "${'$'}x11_tmp_dir/.X1-lock"
              fi
            }
            set_session_state starting
            export DISPLAY=:1
            export PULSE_SERVER=127.0.0.1
            # Termux erases PREFIX/tmp whenever its application process starts.
            # PRoot's glue directory and the X11 socket must therefore live in
            # persistent app data. A later RunCommand may restart Termux and
            # erase PREFIX/tmp while the prepared X11 server is still alive.
            proot_tmp_dir="${'$'}state_dir/proot-tmp"
            mkdir -p "${'$'}proot_tmp_dir"
            chmod 700 "${'$'}proot_tmp_dir"
            export PROOT_TMP_DIR="${'$'}proot_tmp_dir"
            bind_args=()
            downloads_ready=false
            $downloadsSetup
            display_ready=false
            # The app prepares X11 before opening its Android activity. Reuse
            # that stable server so the first XFCE client never races the
            # activity surface. Direct/manual callers still get three retries.
            ready_streak=0
            for existing_attempt in 1 2 3 4 5 6; do
              if pgrep -f "${'$'}x11_pattern" >/dev/null 2>&1 && \
                 test -S "${'$'}x11_tmp_dir/.X11-unix/X1"; then
                ready_streak=${'$'}((ready_streak + 1))
                if test "${'$'}ready_streak" -ge 3; then
                  display_ready=true
                  break
                fi
              else
                ready_streak=0
              fi
              sleep 0.2
            done
            for display_attempt in 1 2 3; do
              if test "${'$'}display_ready" = true; then break; fi
              mkdir -p "${'$'}x11_tmp_dir/.X11-unix"
              rm -f "${'$'}x11_tmp_dir/.X11-unix/X1" "${'$'}x11_tmp_dir/.X1-lock"
              TMPDIR="${'$'}x11_tmp_dir" termux-x11 :1 -ac -dpi $dpi >/dev/null 2>&1 &
              ready_streak=0
              for ready_attempt in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20; do
                if pgrep -f "${'$'}x11_pattern" >/dev/null 2>&1 && \
                   test -S "${'$'}x11_tmp_dir/.X11-unix/X1"; then
                  ready_streak=${'$'}((ready_streak + 1))
                  if test "${'$'}ready_streak" -ge 3; then
                    display_ready=true
                    break
                  fi
                else
                  ready_streak=0
                fi
                sleep 0.3
              done
              if test "${'$'}display_ready" = true; then break; fi
              printf 'Termux:X11 start attempt %s did not stabilize; retrying.\n' "${'$'}display_attempt" >&2
              pkill -TERM -f "${'$'}x11_pattern" >/dev/null 2>&1 || true
              am broadcast -a com.termux.x11.ACTION_STOP -p com.termux.x11 >/dev/null 2>&1 || true
              sleep 1
            done
            if test "${'$'}display_ready" != true; then
              printf 'Termux:X11 server did not create a stable display socket after 3 attempts.\n' >&2
              set_session_state error
              stop_owned_display
              exit 1
            fi
            pulse_runtime_dir="${'$'}state_dir/pulse-runtime"
            pkill -TERM -f "${'$'}pulse_pattern" >/dev/null 2>&1 || true
            for pulse_stop_attempt in 1 2 3 4 5; do
              if ! pgrep -f "${'$'}pulse_pattern" >/dev/null 2>&1; then break; fi
              sleep 0.2
            done
            rm -rf "${'$'}pulse_runtime_dir"
            mkdir -p "${'$'}pulse_runtime_dir"
            chmod 700 "${'$'}pulse_runtime_dir"
            export PULSE_RUNTIME_PATH="${'$'}pulse_runtime_dir"
            pulse_log="${'$'}state_dir/pulse.log"
            : > "${'$'}pulse_log"
            # PulseAudio's self-daemonizing start mode may be reaped when
            # Android finishes the short RunCommand service. Keep the daemon
            # attached to this tracked launcher instead; the launcher lives
            # for the complete session and owns cleanup on every exit path.
            nohup pulseaudio --daemonize=no --exit-idle-time=-1 \
              --log-target="file:${'$'}pulse_log" \
              --load="module-native-protocol-tcp auth-ip-acl=127.0.0.1 auth-anonymous=1" \
              </dev/null >/dev/null 2>&1 &
            pulse_pid=${'$'}!
            pulse_ready=false
            if command -v pactl >/dev/null 2>&1; then
              for pulse_ready_attempt in 1 2 3 4 5 6 7 8 9 10; do
                if kill -0 "${'$'}pulse_pid" >/dev/null 2>&1 && \
                   pactl info >/dev/null 2>&1 && \
                   pactl list short modules 2>/dev/null | grep -Fq module-native-protocol-tcp; then
                  pulse_ready=true
                  break
                fi
                sleep 0.3
              done
            fi
            if test "${'$'}pulse_ready" != true; then
              printf 'PulseAudio did not become ready; continuing without desktop audio.\n' >&2
              tail -n 10 "${'$'}pulse_log" >&2 2>/dev/null || true
            fi
            set_session_state running
            if proot-distro login --env "PROOT_TMP_DIR=${'$'}proot_tmp_dir" --env "TUXSPAN_DOWNLOADS_READY=${'$'}downloads_ready" '$name' --bind "${'$'}x11_tmp_dir:/tmp" "${'$'}{bind_args[@]}" -- /bin/sh -lc ${shellQuote(guestLaunch)}; then
              set_session_state stopped
              stop_owned_display
            else
              exit_code=${'$'}?
              set_session_state error
              stop_owned_display
              exit "${'$'}exit_code"
            fi
        """.trimIndent()
    }

    /** Starts the long-running desktop independently and returns a useful result within seconds. */
    fun trackedLaunch(
        recipe: WorkspaceRecipe,
        settings: ExperienceSettings = ExperienceSettings(),
        dpi: Int = 144,
    ): String {
        val encoded = Base64.getEncoder().encodeToString(
            launch(recipe, settings, dpi).toByteArray(StandardCharsets.UTF_8),
        )
        return """
            set -u
            state_dir="${'$'}HOME/.local/state/tuxspan"
            command_file="${'$'}state_dir/${recipe.id}-launch.sh"
            status_file="${'$'}state_dir/${recipe.id}.session"
            log_file="${'$'}state_dir/${recipe.id}-launch.log"
            pid_file="${'$'}state_dir/${recipe.id}-launch.pid"
            token_file="${'$'}state_dir/desktop-session.token"
            mkdir -p "${'$'}state_dir"
            proot_tmp_dir="${'$'}state_dir/proot-tmp"
            mkdir -p "${'$'}proot_tmp_dir"
            chmod 700 "${'$'}proot_tmp_dir"
            export PROOT_TMP_DIR="${'$'}proot_tmp_dir"
            x11_tmp_dir="${'$'}state_dir/x11-tmp"
            mkdir -p "${'$'}x11_tmp_dir/.X11-unix"
            chmod 700 "${'$'}x11_tmp_dir"
            chmod 1777 "${'$'}x11_tmp_dir/.X11-unix"
            handoff_token="handoff-${recipe.id}-${'$'}${'$'}-${'$'}(date +%s)"
            printf '%s\n' "${'$'}handoff_token" > "${'$'}token_file"
            for stale_status in "${'$'}state_dir"/*.session; do
              if test -f "${'$'}stale_status"; then printf 'stopped\n' > "${'$'}stale_status"; fi
            done
            for stale_pid_file in "${'$'}state_dir"/*-launch.pid; do
              if ! test -f "${'$'}stale_pid_file"; then continue; fi
              stale_command_file="${'$'}{stale_pid_file%-launch.pid}-launch.sh"
              stale_pid=${'$'}(cat "${'$'}stale_pid_file" 2>/dev/null || true)
              case "${'$'}stale_pid" in
                ''|*[!0-9]*) ;;
                *)
                  # Processes may disappear between reading the pid file and
                  # opening /proc. Redirect stderr before the volatile input
                  # redirection so this normal race never poisons a launch.
                  stale_cmdline=${'$'}(tr '\0' ' ' 2>/dev/null < "/proc/${'$'}stale_pid/cmdline" || true)
                  if printf '%s' "${'$'}stale_cmdline" | grep -Fq "${'$'}stale_command_file"; then
                    pkill -TERM -P "${'$'}stale_pid" >/dev/null 2>&1 || true
                    kill -TERM "${'$'}stale_pid" >/dev/null 2>&1 || true
                  fi
                  ;;
              esac
              rm -f "${'$'}stale_pid_file"
            done
            # Anchor the executable token. A loose full-command match can see
            # this script inside bash -lc's argv and terminate its own wrapper.
            proot_pattern='^(proot|.*/proot) .*proot-distro/containers/tuxspan-'
            x11_pattern='^(termux-x11|.*/termux-x11) .*:1( |${'$'})'
            keyboard_pattern='^(python3|.*/python3) .*tuxspan-auto-keyboard[.]py( |${'$'})'
            dock_pattern='^((sh|dash)|.*/(sh|dash)) .*tuxspan-position-dock( |${'$'})'
            pulse_pattern='^(pulseaudio|.*/pulseaudio)( |${'$'})'
            atspi_launcher_pattern='^/usr/libexec/at-spi-bus-launcher( |${'$'})'
            atspi_registry_pattern='^/usr/libexec/at-spi2-registryd( |${'$'})'
            atspi_dbus_pattern='^(dbus-daemon|.*/dbus-daemon) --config-file=/usr/share/defaults/at-spi2/accessibility[.]conf( |${'$'})'
            pkill -TERM -f "${'$'}proot_pattern" >/dev/null 2>&1 || true
            pkill -TERM -f "${'$'}keyboard_pattern" >/dev/null 2>&1 || true
            pkill -TERM -f "${'$'}dock_pattern" >/dev/null 2>&1 || true
            pkill -TERM -f "${'$'}pulse_pattern" >/dev/null 2>&1 || true
            for attempt in 1 2 3 4 5 6 7 8 9 10 11 12; do
              if ! pgrep -f "${'$'}proot_pattern" >/dev/null 2>&1; then break; fi
              sleep 0.5
            done
            if pgrep -f "${'$'}proot_pattern" >/dev/null 2>&1; then
              pkill -KILL -f "${'$'}proot_pattern" >/dev/null 2>&1 || true
            fi
            # Accessibility services are children of the old PRoot desktop.
            # Stop them only after the container has exited so a stale bus
            # cannot survive a workspace switch and capture the next session.
            pkill -TERM -f "${'$'}atspi_launcher_pattern" >/dev/null 2>&1 || true
            pkill -TERM -f "${'$'}atspi_registry_pattern" >/dev/null 2>&1 || true
            pkill -TERM -f "${'$'}atspi_dbus_pattern" >/dev/null 2>&1 || true
            sleep 0.2
            pkill -KILL -f "${'$'}atspi_launcher_pattern" >/dev/null 2>&1 || true
            pkill -KILL -f "${'$'}atspi_registry_pattern" >/dev/null 2>&1 || true
            pkill -KILL -f "${'$'}atspi_dbus_pattern" >/dev/null 2>&1 || true
            mkdir -p "${'$'}x11_tmp_dir/.X11-unix"
            printf '%s' '$encoded' | base64 -d > "${'$'}command_file"
            chmod 700 "${'$'}command_file"
            : > "${'$'}log_file"
            nohup bash "${'$'}command_file" >> "${'$'}log_file" 2>&1 </dev/null &
            launch_pid=${'$'}!
            printf '%s\n' "${'$'}launch_pid" > "${'$'}pid_file"
            launch_ready=false
            attempt=0
            while test "${'$'}attempt" -lt 90; do
              if grep -qx 'running' "${'$'}status_file" 2>/dev/null && \
                 kill -0 "${'$'}launch_pid" >/dev/null 2>&1 && \
                 pgrep -f "${'$'}x11_pattern" >/dev/null 2>&1 && \
                 test -S "${'$'}x11_tmp_dir/.X11-unix/X1" && \
                 pgrep -f '^(proot|.*/proot) .*proot-distro/containers/tuxspan-${recipe.id}/' >/dev/null 2>&1; then
                launch_ready=true
                break
              fi
              if ! kill -0 "${'$'}launch_pid" >/dev/null 2>&1; then
                break
              fi
              attempt=${'$'}((attempt + 1))
              sleep 1
            done
            if test "${'$'}launch_ready" = true; then
              printf 'TUXSPAN_LAUNCH_STARTED'
            else
              if kill -0 "${'$'}launch_pid" >/dev/null 2>&1; then
                printf 'Launch timed out while the process was still starting.\n' >&2
                pkill -TERM -P "${'$'}launch_pid" >/dev/null 2>&1 || true
                kill -TERM "${'$'}launch_pid" >/dev/null 2>&1 || true
              fi
              pkill -TERM -f '^(proot|.*/proot) .*proot-distro/containers/tuxspan-${recipe.id}/' >/dev/null 2>&1 || true
              pkill -TERM -f "${'$'}x11_pattern" >/dev/null 2>&1 || true
              am broadcast -a com.termux.x11.ACTION_STOP -p com.termux.x11 >/dev/null 2>&1 || true
              printf 'error\n' > "${'$'}status_file"
              printf 'Current launch state: ' >&2
              cat "${'$'}status_file" >&2 2>/dev/null || printf 'unknown\n' >&2
              printf 'Desktop launch failed. Last log lines:\n' >&2
              tail -n 35 "${'$'}log_file" >&2 || true
              exit 1
            fi
        """.trimIndent()
    }

    /** Starts only the native X11 server; the app opens the Android surface next. */
    fun prepareDisplay(dpi: Int): String = """
        set -u
        state_dir="${'$'}HOME/.local/state/tuxspan"
        x11_tmp_dir="${'$'}state_dir/x11-tmp"
        mkdir -p "${'$'}x11_tmp_dir/.X11-unix"
        chmod 700 "${'$'}x11_tmp_dir"
        chmod 1777 "${'$'}x11_tmp_dir/.X11-unix"
        x11_pattern='^(termux-x11|.*/termux-x11) .*:1( |${'$'})'
        keyboard_pattern='^(python3|.*/python3) .*tuxspan-auto-keyboard[.]py( |${'$'})'
        dock_pattern='^((sh|dash)|.*/(sh|dash)) .*tuxspan-position-dock( |${'$'})'
        pkill -TERM -f "${'$'}keyboard_pattern" >/dev/null 2>&1 || true
        pkill -TERM -f "${'$'}dock_pattern" >/dev/null 2>&1 || true
        pkill -TERM -f "${'$'}x11_pattern" >/dev/null 2>&1 || true
        am broadcast -a com.termux.x11.ACTION_STOP -p com.termux.x11 >/dev/null 2>&1 || true
        for stop_attempt in 1 2 3 4 5 6 7 8; do
          if ! pgrep -f "${'$'}x11_pattern" >/dev/null 2>&1; then break; fi
          sleep 0.25
        done
        if pgrep -f "${'$'}x11_pattern" >/dev/null 2>&1; then
          pkill -KILL -f "${'$'}x11_pattern" >/dev/null 2>&1 || true
        fi
        display_ready=false
        for display_attempt in 1 2 3; do
          mkdir -p "${'$'}x11_tmp_dir/.X11-unix"
          rm -f "${'$'}x11_tmp_dir/.X11-unix/X1" "${'$'}x11_tmp_dir/.X1-lock"
          TMPDIR="${'$'}x11_tmp_dir" termux-x11 :1 -ac -dpi $dpi >/dev/null 2>&1 &
          ready_streak=0
          for ready_attempt in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20; do
            if pgrep -f "${'$'}x11_pattern" >/dev/null 2>&1 && \
               test -S "${'$'}x11_tmp_dir/.X11-unix/X1"; then
              ready_streak=${'$'}((ready_streak + 1))
              if test "${'$'}ready_streak" -ge 3; then
                display_ready=true
                break
              fi
            else
              ready_streak=0
            fi
            sleep 0.3
          done
          if test "${'$'}display_ready" = true; then break; fi
          pkill -TERM -f "${'$'}x11_pattern" >/dev/null 2>&1 || true
          am broadcast -a com.termux.x11.ACTION_STOP -p com.termux.x11 >/dev/null 2>&1 || true
          sleep 1
        done
        if test "${'$'}display_ready" = true; then
          printf 'TUXSPAN_DISPLAY_READY'
        else
          printf 'Termux:X11 could not create a stable display after 3 attempts.\n' >&2
          exit 1
        fi
    """.trimIndent()

    fun x11Preferences(settings: ExperienceSettings): String = """
        set -eu
        if ! am broadcast \
          -a com.termux.x11.CHANGE_PREFERENCE \
          -p com.termux.x11 \
          --es fullscreen true \
          --es forceOrientation "${settings.screenOrientation.x11Value}" \
          --es hideCutout false \
          --es displayResolutionMode native \
          --es displayScale 100 \
          --es touchMode "${settings.touchMode.x11Value}" \
          --es showMouseHelper false \
          --es showAdditionalKbd true \
          --es showIMEWhileExternalConnected false \
          --es backButtonAction "toggle soft keyboard" \
          --es swipeUpAction "toggle additional key bar" \
          --es swipeDownAction "no action" \
          --es clipboardEnable true \
          --es screenIdleTimeout "${settings.performancePreset.idleTimeout}" \
          >/dev/null 2>&1; then
          printf 'Termux:X11 did not accept the input and display preferences.\n' >&2
          exit 1
        fi

        # showAdditionalKbd=true also resets additionalKbdVisible=true. Persist
        # the desired hidden state in a separate completed broadcast, then send
        # an ordinary preference change so the running activity refreshes the
        # toolbar after its X client connection is ready.
        sleep 0.4
        if ! am broadcast \
          -a com.termux.x11.CHANGE_PREFERENCE \
          -p com.termux.x11 \
          --es additionalKbdVisible false \
          >/dev/null 2>&1; then
          printf 'Termux:X11 did not save the hidden controls state.\n' >&2
          exit 1
        fi
        sleep 0.4
        if ! am broadcast \
          -a com.termux.x11.CHANGE_PREFERENCE \
          -p com.termux.x11 \
          --es showMouseHelper false \
          >/dev/null 2>&1; then
          printf 'Termux:X11 did not refresh the controls state.\n' >&2
          exit 1
        fi

        printf 'TUXSPAN_X11_READY'
    """.trimIndent()

    fun showX11Controls(): String = x11ControlsVisibility(visible = true)

    fun hideX11Controls(): String = x11ControlsVisibility(visible = false)

    private fun x11ControlsVisibility(visible: Boolean): String = """
        set -u
        if ! am broadcast \
          -a com.termux.x11.CHANGE_PREFERENCE \
          -p com.termux.x11 \
          --es showAdditionalKbd true \
          >/dev/null 2>&1; then
          printf 'Termux:X11 is not ready to change its controls.\n' >&2
          exit 1
        fi
        sleep 0.4
        if ! am broadcast \
          -a com.termux.x11.CHANGE_PREFERENCE \
          -p com.termux.x11 \
          --es additionalKbdVisible ${visible.toString()} \
          >/dev/null 2>&1; then
          printf 'Termux:X11 did not save the controls visibility.\n' >&2
          exit 1
        fi
        sleep 0.4
        if ! am broadcast \
          -a com.termux.x11.CHANGE_PREFERENCE \
          -p com.termux.x11 \
          --es showMouseHelper false \
          >/dev/null 2>&1; then
          printf 'Termux:X11 did not refresh the controls visibility.\n' >&2
          exit 1
        fi
        printf 'TUXSPAN_X11_CONTROLS_${if (visible) "SHOWN" else "HIDDEN"}'
    """.trimIndent()

    /** Kept for compatibility with the 0.2.x unit-test and command surface. */
    fun hideX11AdditionalKeys(): String = x11Preferences(ExperienceSettings())

    fun sessionStatus(recipe: WorkspaceRecipe): String = """
        x11_pattern='^(termux-x11|.*/termux-x11) .*:1( |${'$'})'
        x11_tmp_dir="${'$'}HOME/.local/state/tuxspan/x11-tmp"
        pid_file="${'$'}HOME/.local/state/tuxspan/${recipe.id}-launch.pid"
        command_file="${'$'}HOME/.local/state/tuxspan/${recipe.id}-launch.sh"
        status_file="${'$'}HOME/.local/state/tuxspan/${recipe.id}.session"
        launch_pid=${'$'}(cat "${'$'}pid_file" 2>/dev/null || true)
        launch_cmdline=${'$'}(tr '\0' ' ' 2>/dev/null < "/proc/${'$'}launch_pid/cmdline" || true)
        if test -n "${'$'}launch_pid" && \
           kill -0 "${'$'}launch_pid" >/dev/null 2>&1 && \
           printf '%s' "${'$'}launch_cmdline" | grep -Fq "${'$'}command_file" && \
           grep -qx 'running' "${'$'}status_file" 2>/dev/null && \
           pgrep -f "${'$'}x11_pattern" >/dev/null 2>&1 && \
           test -S "${'$'}x11_tmp_dir/.X11-unix/X1" && \
           pgrep -f '^(proot|.*/proot) .*proot-distro/containers/tuxspan-${recipe.id}/' >/dev/null 2>&1; then
          printf 'TUXSPAN_RUNNING'
        else
          if test -f "${'$'}status_file" && grep -qx 'error' "${'$'}status_file"; then
            printf 'TUXSPAN_ERROR\n'
            tail -n 12 "${'$'}HOME/.local/state/tuxspan/${recipe.id}-launch.log" 2>/dev/null || true
          else
            printf 'TUXSPAN_STOPPED'
          fi
        fi
    """.trimIndent()

    fun stop(recipe: WorkspaceRecipe): String = """
        set +e
        state_dir="${'$'}HOME/.local/state/tuxspan"
        token_file="${'$'}state_dir/desktop-session.token"
        pid_file="${'$'}HOME/.local/state/tuxspan/${recipe.id}-launch.pid"
        command_file="${'$'}HOME/.local/state/tuxspan/${recipe.id}-launch.sh"
        status_file="${'$'}state_dir/${recipe.id}.session"
        proot_tmp_dir="${'$'}state_dir/proot-tmp"
        x11_tmp_dir="${'$'}state_dir/x11-tmp"
        mkdir -p "${'$'}proot_tmp_dir"
        chmod 700 "${'$'}proot_tmp_dir"
        export PROOT_TMP_DIR="${'$'}proot_tmp_dir"
        stop_token="stopping-${recipe.id}-${'$'}${'$'}-${'$'}(date +%s)"
        active_token=${'$'}(cat "${'$'}token_file" 2>/dev/null || true)
        owns_display=false
        case "${'$'}active_token" in
          ${recipe.id}-*) owns_display=true ;;
          '') if grep -qx 'running' "${'$'}status_file" 2>/dev/null; then owns_display=true; fi ;;
        esac
        if test "${'$'}owns_display" = true; then
          printf '%s\n' "${'$'}stop_token" > "${'$'}token_file"
        fi
        launch_pid=${'$'}(cat "${'$'}pid_file" 2>/dev/null || true)
        case "${'$'}launch_pid" in
          ''|*[!0-9]*) ;;
          *)
            launch_cmdline=${'$'}(tr '\0' ' ' 2>/dev/null < "/proc/${'$'}launch_pid/cmdline" || true)
            if printf '%s' "${'$'}launch_cmdline" | grep -Fq "${'$'}command_file"; then
              pkill -TERM -P "${'$'}launch_pid" >/dev/null 2>&1
              kill -TERM "${'$'}launch_pid" >/dev/null 2>&1
            fi
            ;;
        esac
        proot_pattern='^(proot|.*/proot) .*proot-distro/containers/tuxspan-${recipe.id}/'
        x11_pattern='^(termux-x11|.*/termux-x11) .*:1( |${'$'})'
        keyboard_pattern='^(python3|.*/python3) .*tuxspan-auto-keyboard[.]py( |${'$'})'
        dock_pattern='^((sh|dash)|.*/(sh|dash)) .*tuxspan-position-dock( |${'$'})'
        pulse_pattern='^(pulseaudio|.*/pulseaudio)( |${'$'})'
        atspi_launcher_pattern='^/usr/libexec/at-spi-bus-launcher( |${'$'})'
        atspi_registry_pattern='^/usr/libexec/at-spi2-registryd( |${'$'})'
        atspi_dbus_pattern='^(dbus-daemon|.*/dbus-daemon) --config-file=/usr/share/defaults/at-spi2/accessibility[.]conf( |${'$'})'
        pkill -TERM -f "${'$'}proot_pattern" >/dev/null 2>&1
        for attempt in 1 2 3 4 5 6 7 8; do
          if ! pgrep -f "${'$'}proot_pattern" >/dev/null 2>&1; then break; fi
          sleep 0.5
        done
        if pgrep -f "${'$'}proot_pattern" >/dev/null 2>&1; then
          pkill -KILL -f "${'$'}proot_pattern" >/dev/null 2>&1
        fi
        if test "${'$'}(cat "${'$'}token_file" 2>/dev/null)" = "${'$'}stop_token"; then
          pkill -TERM -f "${'$'}keyboard_pattern" >/dev/null 2>&1
          pkill -TERM -f "${'$'}dock_pattern" >/dev/null 2>&1
          pkill -TERM -f "${'$'}pulse_pattern" >/dev/null 2>&1
          pkill -TERM -f "${'$'}atspi_launcher_pattern" >/dev/null 2>&1
          pkill -TERM -f "${'$'}atspi_registry_pattern" >/dev/null 2>&1
          pkill -TERM -f "${'$'}atspi_dbus_pattern" >/dev/null 2>&1
          sleep 0.2
          pkill -KILL -f "${'$'}atspi_launcher_pattern" >/dev/null 2>&1
          pkill -KILL -f "${'$'}atspi_registry_pattern" >/dev/null 2>&1
          pkill -KILL -f "${'$'}atspi_dbus_pattern" >/dev/null 2>&1
          pkill -TERM -f "${'$'}x11_pattern" >/dev/null 2>&1
          am broadcast -a com.termux.x11.ACTION_STOP -p com.termux.x11 >/dev/null 2>&1
          mkdir -p "${'$'}x11_tmp_dir/.X11-unix"
          rm -f "${'$'}x11_tmp_dir/.X11-unix/X1" "${'$'}x11_tmp_dir/.X1-lock"
        fi
        mkdir -p "${'$'}state_dir"
        printf 'stopped\n' > "${'$'}status_file"
        rm -f "${'$'}pid_file"
        printf 'TUXSPAN_STOPPED'
    """.trimIndent()

    fun requestStorageAccess(): String = """
        set -u
        if ! command -v termux-setup-storage >/dev/null 2>&1; then
          printf 'termux-setup-storage is not installed. Update the termux-tools package.\n' >&2
          exit 1
        fi
        # RUN_COMMAND may begin a fraction of a second before Android finishes
        # bringing the Termux activity to the foreground. Permission dialogs
        # requested during that transition can be suppressed by recent Android
        # versions, so let the visible Termux session settle first.
        sleep 2
        # The official helper asks for terminal input whenever ~/storage
        # already exists. TuxSpan may be repairing a partial first attempt, so
        # answer that safe rebuild prompt non-interactively before Android's
        # own permission dialog is shown by Termux.
        printf 'y\n' | termux-setup-storage
        sleep 1
        printf 'TUXSPAN_STORAGE_REQUESTED'
    """.trimIndent()

    fun storageAccessStatus(): String = """
        set -u
        downloads_source=''
        for candidate in "${'$'}HOME/storage/downloads" /storage/emulated/0/Download /sdcard/Download; do
          probe="${'$'}candidate/.tuxspan-write-probe-${'$'}${'$'}"
          if test -d "${'$'}candidate" && \
             ls -A "${'$'}candidate" >/dev/null 2>&1 && \
             : > "${'$'}probe" 2>/dev/null; then
            rm -f "${'$'}probe"
            downloads_source="${'$'}candidate"
            break
          fi
          rm -f "${'$'}probe" >/dev/null 2>&1 || true
        done
        if test -n "${'$'}downloads_source"; then
          mkdir -p "${'$'}HOME/storage"
          if ! test -e "${'$'}HOME/storage/downloads" && ! test -L "${'$'}HOME/storage/downloads"; then
            ln -s "${'$'}downloads_source" "${'$'}HOME/storage/downloads" >/dev/null 2>&1 || true
          fi
          printf 'TUXSPAN_STORAGE_READY|%s' "${'$'}downloads_source"
        else
          printf 'TUXSPAN_STORAGE_NOT_READY'
        fi
    """.trimIndent()

    fun remove(recipe: WorkspaceRecipe): String {
        val name = workspaceName(recipe)
        return """
            set -eu
            proot-distro remove '$name'
            rm -f "${'$'}HOME/.local/bin/tuxspan-${recipe.id}"
            printf 'TUXSPAN_REMOVED'
        """.trimIndent()
    }

    fun installBundle(recipe: WorkspaceRecipe, bundleId: String): String {
        val isAlpine = recipe.image.startsWith("alpine")
        val packages = when (bundleId) {
            "starter" -> "$DESKTOP_STARTER_PACKAGES ${browserPackage(recipe)}"
            "office" -> "libreoffice-writer libreoffice-calc libreoffice-impress libreoffice-gtk3"
            "creator", "creative" -> CREATOR_PACKAGES
            "developer" -> if (isAlpine) {
                "build-base clang cmake ninja git python3 py3-pip nodejs npm " +
                    "openjdk17-jdk go rust cargo sqlite jq shellcheck openssh-client-default"
            } else {
                DEVELOPER_PACKAGES
            }
            else -> error("Unknown bundle: $bundleId")
        }
        val managerCommand = if (isAlpine) {
            "apk update; apk add $packages"
        } else {
            buildString {
                appendLine("export DEBIAN_FRONTEND=noninteractive")
                if (bundleId == "starter") appendLine(browserRepositorySetup(recipe))
                appendLine("apt-get update")
                append("apt-get install -y $packages")
            }
        }
        val desktopSetup = if (recipe.kind == WorkspaceKind.DESKTOP) {
            when (bundleId) {
                "starter" -> starterDesktopSetup(recipe)
                "creator", "creative" -> creatorDesktopSetup()
                "developer" -> developerDesktopSetup()
                else -> ""
            }
        } else {
            ""
        }
        val guestCommand = buildString {
            appendLine("set -eu")
            appendLine(managerCommand)
            if (desktopSetup.isNotBlank()) appendLine(desktopSetup)
        }.trimEnd()
        return "tuxspan_proot_tmp=\"${'$'}HOME/.local/state/tuxspan/proot-tmp\"; " +
            "mkdir -p \"${'$'}tuxspan_proot_tmp\"; chmod 700 \"${'$'}tuxspan_proot_tmp\"; " +
            "proot-distro login --env \"PROOT_TMP_DIR=${'$'}tuxspan_proot_tmp\" '${workspaceName(recipe)}' -- /bin/sh -lc " +
            shellQuote(guestCommand)
    }

    private fun browserPackage(recipe: WorkspaceRecipe): String =
        if (recipe.image.startsWith("debian")) "firefox-esr" else "firefox"

    private fun browserCommand(recipe: WorkspaceRecipe): String = browserPackage(recipe)

    private fun browserDesktopId(recipe: WorkspaceRecipe): String =
        if (recipe.image.startsWith("debian")) "firefox-esr.desktop" else "firefox.desktop"

    private fun browserIcon(recipe: WorkspaceRecipe): String =
        if (recipe.image.startsWith("debian")) "firefox-esr" else "firefox"

    private fun browserRepositorySetup(recipe: WorkspaceRecipe): String =
        if (recipe.image.startsWith("debian")) {
            ""
        } else {
            """
            install -d -m 0755 /etc/apt/keyrings
            printf '%s' '$MOZILLA_APT_KEY_BASE64' | base64 -d > /etc/apt/keyrings/packages.mozilla.org.asc
            printf '%s\n' 'deb [signed-by=/etc/apt/keyrings/packages.mozilla.org.asc] https://packages.mozilla.org/apt mozilla main' > /etc/apt/sources.list.d/mozilla.list
            printf '%s\n' 'Package: *' 'Pin: origin packages.mozilla.org' 'Pin-Priority: 1000' '' 'Package: firefox' 'Pin: release o=Ubuntu' 'Pin-Priority: -1' > /etc/apt/preferences.d/mozilla
            """.trimIndent()
        }

    private fun starterDesktopSetup(recipe: WorkspaceRecipe): String {
        val browserCommand = browserCommand(recipe)
        val browserDesktopId = browserDesktopId(recipe)
        val browserIcon = browserIcon(recipe)
        return """
        mkdir -p "${'$'}HOME/Desktop" /usr/local/share/applications
        rm -f "${'$'}HOME/Desktop/Files.desktop" "${'$'}HOME/Desktop/Geany.desktop" \
          "${'$'}HOME/Desktop/Web.desktop" "${'$'}HOME/Desktop/Firefox.desktop" \
          /usr/local/share/applications/tuxspan-web.desktop
        create_trusted_launcher() {
          launcher_id="${'$'}1"; launcher_name="${'$'}2"; launcher_exec="${'$'}3"; launcher_icon="${'$'}4"; desktop_link="${'$'}5"
          launcher_path="/usr/local/share/applications/${'$'}launcher_id.desktop"
          printf '%s\n' '[Desktop Entry]' 'Version=1.0' 'Type=Application' "Name=${'$'}launcher_name" "Exec=${'$'}launcher_exec" "Icon=${'$'}launcher_icon" 'Terminal=false' > "${'$'}launcher_path"
          chmod 644 "${'$'}launcher_path"
          ln -sfn "${'$'}launcher_path" "${'$'}desktop_link"
        }
        create_trusted_launcher tuxspan-firefox Firefox '$browserCommand' '$browserIcon' "${'$'}HOME/Desktop/Firefox.desktop"
        create_trusted_launcher tuxspan-writer Writer 'libreoffice --norestore --writer' libreoffice-writer "${'$'}HOME/Desktop/Writer.desktop"
        create_trusted_launcher tuxspan-photos Photos ristretto org.xfce.ristretto "${'$'}HOME/Desktop/Photos.desktop"
        create_trusted_launcher tuxspan-media Media parole org.xfce.parole "${'$'}HOME/Desktop/Media.desktop"
        create_trusted_launcher tuxspan-terminal Terminal xfce4-terminal utilities-terminal "${'$'}HOME/Desktop/Terminal.desktop"
        create_trusted_launcher tuxspan-calc Calc 'libreoffice --norestore --calc' libreoffice-calc /tmp/tuxspan-calc.desktop
        create_trusted_launcher tuxspan-impress Impress 'libreoffice --norestore --impress' libreoffice-impress /tmp/tuxspan-impress.desktop
        rm -f /tmp/tuxspan-calc.desktop /tmp/tuxspan-impress.desktop
        xdg-settings set default-web-browser '$browserDesktopId' >/dev/null 2>&1 || true
        xdg-mime default '$browserDesktopId' x-scheme-handler/http x-scheme-handler/https text/html
        xdg-mime default org.xfce.ristretto.desktop image/jpeg image/png image/gif image/webp image/bmp image/tiff
        xdg-mime default org.xfce.Parole.desktop video/mp4 video/webm video/x-matroska video/quicktime audio/mpeg audio/ogg audio/flac audio/x-wav
        xdg-mime default atril.desktop application/pdf application/postscript image/vnd.djvu
        xdg-mime default org.xfce.mousepad.desktop text/plain text/csv
        xdg-mime default thunar.desktop inode/directory
        xdg-mime default xarchiver.desktop application/zip application/x-7z-compressed application/x-rar application/x-tar application/gzip application/x-bzip2 application/x-xz
        xdg-mime default tuxspan-writer.desktop application/msword application/vnd.openxmlformats-officedocument.wordprocessingml.document application/vnd.oasis.opendocument.text
        xdg-mime default tuxspan-calc.desktop application/vnd.ms-excel application/vnd.openxmlformats-officedocument.spreadsheetml.sheet application/vnd.oasis.opendocument.spreadsheet
        xdg-mime default tuxspan-impress.desktop application/vnd.ms-powerpoint application/vnd.openxmlformats-officedocument.presentationml.presentation application/vnd.oasis.opendocument.presentation
        """.trimIndent()
    }

    private fun creatorDesktopSetup(): String = """
        creator_dir="${'$'}HOME/Desktop/Creator Tools"
        launcher_dir=/usr/local/share/applications
        mkdir -p "${'$'}creator_dir" "${'$'}launcher_dir"
        create_pack_launcher() {
          launcher_id="${'$'}1"; launcher_name="${'$'}2"; launcher_exec="${'$'}3"; launcher_icon="${'$'}4"; desktop_link="${'$'}5"
          launcher_path="${'$'}launcher_dir/${'$'}launcher_id.desktop"
          printf '%s\n' '[Desktop Entry]' 'Version=1.0' 'Type=Application' "Name=${'$'}launcher_name" "Exec=${'$'}launcher_exec" "Icon=${'$'}launcher_icon" 'Terminal=false' > "${'$'}launcher_path"
          chmod 644 "${'$'}launcher_path"
          ln -sfn "${'$'}launcher_path" "${'$'}desktop_link"
        }
        create_pack_launcher tuxspan-creator-gimp GIMP gimp gimp "${'$'}creator_dir/GIMP.desktop"
        create_pack_launcher tuxspan-creator-inkscape Inkscape inkscape org.inkscape.Inkscape "${'$'}creator_dir/Inkscape.desktop"
        create_pack_launcher tuxspan-creator-audacity Audacity audacity audacity "${'$'}creator_dir/Audacity.desktop"
        create_pack_launcher tuxspan-creator-openshot OpenShot openshot-qt openshot-qt "${'$'}creator_dir/OpenShot.desktop"
        create_pack_launcher tuxspan-creator-scribus Scribus scribus scribus "${'$'}creator_dir/Scribus.desktop"
    """.trimIndent()

    private fun developerDesktopSetup(): String = """
        developer_dir="${'$'}HOME/Desktop/Developer Tools"
        launcher_dir=/usr/local/share/applications
        mkdir -p "${'$'}developer_dir" "${'$'}launcher_dir"
        create_pack_launcher() {
          launcher_id="${'$'}1"; launcher_name="${'$'}2"; launcher_exec="${'$'}3"; launcher_icon="${'$'}4"; desktop_link="${'$'}5"
          launcher_path="${'$'}launcher_dir/${'$'}launcher_id.desktop"
          printf '%s\n' '[Desktop Entry]' 'Version=1.0' 'Type=Application' "Name=${'$'}launcher_name" "Exec=${'$'}launcher_exec" "Icon=${'$'}launcher_icon" 'Terminal=false' > "${'$'}launcher_path"
          chmod 644 "${'$'}launcher_path"
          ln -sfn "${'$'}launcher_path" "${'$'}desktop_link"
        }
        create_pack_launcher tuxspan-developer-geany Geany geany geany "${'$'}developer_dir/Geany.desktop"
        create_pack_launcher tuxspan-developer-sqlite 'DB Browser for SQLite' sqlitebrowser sqlitebrowser "${'$'}developer_dir/DB-Browser.desktop"
        create_pack_launcher tuxspan-developer-meld Meld meld org.gnome.meld "${'$'}developer_dir/Meld.desktop"
    """.trimIndent()

    private fun desktopGuestSetup(recipe: WorkspaceRecipe): String {
        val browserPackage = browserPackage(recipe)
        val browserRepositorySetup = browserRepositorySetup(recipe)
        return """
            set -eu
            export DEBIAN_FRONTEND=noninteractive
            # PRoot exposes Android storage as guest paths. Prevent locate's
            # first-run indexer from walking phone storage during package setup.
            printf '%s\n' \
              'PRUNE_BIND_MOUNTS="yes"' \
              'PRUNEPATHS="/data /dev /media /mnt /proc /sdcard /storage /sys /tmp /var/spool"' \
              > /etc/updatedb.conf
            apt-get update
            apt-get install -y ca-certificates
            $browserRepositorySetup
            apt-get update
            apt-get install -y ${recipe.desktopPackage} xfce4-terminal dbus-x11 x11-utils $browserPackage git python3 ca-certificates $DESKTOP_STARTER_PACKAGES
            mkdir -p /root/.config/xfce4
            printf 'TuxSpan guest packages installed.\n'
        """.trimIndent()
    }

    private fun terminalGuestSetup(): String = """
        set -eu
        apk update
        apk add git python3 openssh nano ca-certificates
        printf 'TuxSpan guest packages installed.\n'
    """.trimIndent()

    private fun launcherScript(recipe: WorkspaceRecipe): String = when (recipe.kind) {
        WorkspaceKind.DESKTOP -> """
            #!/data/data/com.termux/files/usr/bin/bash
            set -eu
            export DISPLAY=:1
            export PULSE_SERVER=127.0.0.1
            export NO_AT_BRIDGE=0
            export XDG_CURRENT_DESKTOP=XFCE XDG_SESSION_DESKTOP=xfce DESKTOP_SESSION=xfce
            state_dir="${'$'}HOME/.local/state/tuxspan"
            mkdir -p "${'$'}state_dir"
            proot_tmp_dir="${'$'}state_dir/proot-tmp"
            mkdir -p "${'$'}proot_tmp_dir"
            chmod 700 "${'$'}proot_tmp_dir"
            x11_tmp_dir="${'$'}state_dir/x11-tmp"
            mkdir -p "${'$'}x11_tmp_dir/.X11-unix"
            chmod 700 "${'$'}x11_tmp_dir"
            chmod 1777 "${'$'}x11_tmp_dir/.X11-unix"
            unset XAUTHORITY
            rm -f "${'$'}x11_tmp_dir/.X11-unix/X1" "${'$'}x11_tmp_dir/.X1-lock"
            TMPDIR="${'$'}x11_tmp_dir" termux-x11 :1 -ac >/dev/null 2>&1 &
            pulse_runtime_dir="${'$'}state_dir/pulse-runtime"
            rm -rf "${'$'}pulse_runtime_dir"
            mkdir -p "${'$'}pulse_runtime_dir"
            chmod 700 "${'$'}pulse_runtime_dir"
            export PULSE_RUNTIME_PATH="${'$'}pulse_runtime_dir"
            pulse_log="${'$'}state_dir/pulse.log"
            : > "${'$'}pulse_log"
            nohup pulseaudio --daemonize=no --exit-idle-time=-1 \
              --log-target="file:${'$'}pulse_log" \
              --load="module-native-protocol-tcp auth-ip-acl=127.0.0.1 auth-anonymous=1" \
              </dev/null >/dev/null 2>&1 &
            sleep 2
            exec proot-distro login --env "PROOT_TMP_DIR=${'$'}proot_tmp_dir" '${workspaceName(recipe)}' --bind "${'$'}x11_tmp_dir:/tmp" -- /bin/sh -lc \
              'export DISPLAY=:1 PULSE_SERVER=127.0.0.1 NO_AT_BRIDGE=0 XDG_CURRENT_DESKTOP=XFCE XDG_SESSION_DESKTOP=xfce DESKTOP_SESSION=xfce; unset XAUTHORITY; export XDG_RUNTIME_DIR="/tmp/tuxspan-runtime-${recipe.id}"; rm -rf "${'$'}XDG_RUNTIME_DIR"; mkdir -p "${'$'}XDG_RUNTIME_DIR"; chmod 700 "${'$'}XDG_RUNTIME_DIR"; export TMPDIR=/tmp; exec dbus-run-session -- startxfce4'
        """.trimIndent()

        WorkspaceKind.TERMINAL -> """
            #!/data/data/com.termux/files/usr/bin/bash
            set -eu
            proot_tmp_dir="${'$'}HOME/.local/state/tuxspan/proot-tmp"
            mkdir -p "${'$'}proot_tmp_dir"
            chmod 700 "${'$'}proot_tmp_dir"
            exec proot-distro login --env "PROOT_TMP_DIR=${'$'}proot_tmp_dir" '${workspaceName(recipe)}'
        """.trimIndent()
    }

    internal fun guestExperienceScript(
        recipe: WorkspaceRecipe,
        settings: ExperienceSettings,
        dpi: Int,
    ): String {
        val mobile = settings.displayProfile == DisplayProfile.MOBILE
        val fontName = if (mobile) "Sans 12" else "Sans 10"
        val titleFont = if (mobile) "Sans Bold 12" else "Sans Bold 10"
        val windowTheme = if (mobile) "Default-hdpi" else "Default"
        val terminalFont = if (mobile) "Monospace 13" else "Monospace 11"
        val iconSize = if (mobile) 56 else 40
        val cursorSize = settings.displayProfile.cursorSize
        val bottomPanelSize = if (mobile) 60 else 46
        val bottomPanelInset = if (mobile) 14 else 6
        val showFilesystemIcon = !mobile
        val compositor = settings.performancePreset == PerformancePreset.BALANCED
        val dockPositioner = """
            #!/bin/sh
            panel_size="${'$'}1"
            bottom_inset="${'$'}2"
            previous_size=''
            while :; do
              screen_size=${'$'}(xrandr --current 2>/dev/null | awk '${'$'}2 ~ /\*/ { print ${'$'}1; exit }')
              if ! printf '%s' "${'$'}screen_size" | grep -Eq '^[0-9]+x[0-9]+${'$'}'; then
                screen_size=${'$'}(xdpyinfo 2>/dev/null | awk '/dimensions:/ { print ${'$'}2; exit }')
              fi
              if printf '%s' "${'$'}screen_size" | grep -Eq '^[0-9]+x[0-9]+${'$'}' && test "${'$'}screen_size" != "${'$'}previous_size"; then
                screen_width=${'$'}{screen_size%x*}
                screen_height=${'$'}{screen_size#*x}
                dock_x=${'$'}((screen_width / 2))
                dock_y=${'$'}((screen_height - panel_size / 2 - bottom_inset))
                xfconf-query -c xfce4-panel -p /panels/panel-2/position -s "p=0;x=${'$'}dock_x;y=${'$'}dock_y" >/dev/null 2>&1
                previous_size="${'$'}screen_size"
              fi
              sleep 2
            done
        """.trimIndent()
        val encodedDockPositioner = Base64.getEncoder().encodeToString(
            dockPositioner.toByteArray(StandardCharsets.UTF_8),
        )
        val encodedAutoKeyboardWatcher = Base64.getEncoder().encodeToString(
            autoKeyboardWatcherScript().toByteArray(StandardCharsets.UTF_8),
        )
        val shortcutSetup = if (mobile) {
            starterDesktopSetup(recipe)
        } else {
            "rm -f \"${'$'}HOME/Desktop/Terminal.desktop\" \"${'$'}HOME/Desktop/Files.desktop\" \"${'$'}HOME/Desktop/Web.desktop\" \"${'$'}HOME/Desktop/Firefox.desktop\" \"${'$'}HOME/Desktop/Writer.desktop\" \"${'$'}HOME/Desktop/Geany.desktop\" \"${'$'}HOME/Desktop/Photos.desktop\" \"${'$'}HOME/Desktop/Media.desktop\""
        }
        val browserMigration = if (recipe.image.startsWith("debian")) {
            ""
        } else {
            """
            rm -f "${'$'}HOME/Desktop/Web.desktop" /usr/local/share/applications/tuxspan-web.desktop
            if ! command -v firefox >/dev/null 2>&1; then
              export DEBIAN_FRONTEND=noninteractive
              dpkg --configure -a >/tmp/tuxspan-firefox-migration.log 2>&1 || true
              ${browserRepositorySetup(recipe)}
              apt-get update >>/tmp/tuxspan-firefox-migration.log 2>&1 && \
                apt-get install -y firefox >>/tmp/tuxspan-firefox-migration.log 2>&1
            fi
            """.trimIndent()
        }
        val firefoxStabilitySetup = """
            # A force-stopped desktop makes Firefox think Android killed it in
            # a crash. Disable only the automatic crash-restore page; normal
            # history and the user's saved browsing data remain untouched.
            for firefox_root in /usr/lib/firefox /usr/lib/firefox-esr; do
              if test -d "${'$'}firefox_root/defaults/pref"; then
                printf '%s\n' \
                  '// TuxSpan Firefox defaults' \
                  'pref("general.config.filename", "tuxspan.cfg");' \
                  'pref("general.config.obscure_value", 0);' \
                  > "${'$'}firefox_root/defaults/pref/tuxspan-autoconfig.js"
                printf '%s\n' \
                  '// TuxSpan Firefox defaults' \
                  'defaultPref("browser.sessionstore.resume_from_crash", false);' \
                  'defaultPref("browser.shell.checkDefaultBrowser", false);' \
                  > "${'$'}firefox_root/tuxspan.cfg"
              fi
            done
        """.trimIndent()
        val archiveMigration = """
            # Ubuntu's File Roller can terminate with SIGILL on some ARM phone
            # CPUs. Xarchiver is lightweight and has been validated in both
            # TuxSpan desktop recipes, so migrate existing workspaces once.
            if ! command -v xarchiver >/dev/null 2>&1; then
              export DEBIAN_FRONTEND=noninteractive
              apt-get update >/tmp/tuxspan-archive-migration.log 2>&1 && \
                apt-get install -y xarchiver >>/tmp/tuxspan-archive-migration.log 2>&1
            fi
        """.trimIndent()
        return """
            #!/bin/sh
            set +e
            # Android passes supplementary numeric groups through PRoot. Some
            # do not exist in the guest's /etc/group, which makes every new
            # terminal print a distracting `groups` warning. Give only those
            # inherited IDs stable local names without changing membership.
            for inherited_gid in ${'$'}(id -G 2>/dev/null); do
              if ! awk -F: -v gid="${'$'}inherited_gid" '${'$'}3 == gid { found=1 } END { exit !found }' /etc/group >/dev/null 2>&1; then
                printf 'tuxspan-android-%s:x:%s:\n' "${'$'}inherited_gid" "${'$'}inherited_gid" >> /etc/group
              fi
            done
            # Expose the Android bridge under the conventional Downloads name
            # and in GTK file choosers, but never replace a real user folder.
            gtk_bookmarks="${'$'}HOME/.config/gtk-3.0/bookmarks"
            mkdir -p "${'$'}HOME/.config/gtk-3.0"
            touch "${'$'}gtk_bookmarks"
            sed -i '\|file:///root/Android-Downloads|d' "${'$'}gtk_bookmarks"
            if test "${'$'}{TUXSPAN_DOWNLOADS_READY:-false}" = true && test -d "${'$'}HOME/Android-Downloads"; then
              # XFCE may pre-create an empty local Downloads directory. It is
              # safe to replace only that empty shell; never touch user files.
              if test -d "${'$'}HOME/Downloads" && ! test -L "${'$'}HOME/Downloads"; then
                rmdir "${'$'}HOME/Downloads" >/dev/null 2>&1 || true
              fi
              if ! test -e "${'$'}HOME/Downloads" || test -L "${'$'}HOME/Downloads"; then
                ln -sfn "${'$'}HOME/Android-Downloads" "${'$'}HOME/Downloads"
              fi
              printf 'file:///root/Android-Downloads Android Downloads\n' >> "${'$'}gtk_bookmarks"
            elif test -L "${'$'}HOME/Downloads" && test "${'$'}(readlink "${'$'}HOME/Downloads")" = "${'$'}HOME/Android-Downloads"; then
              rm -f "${'$'}HOME/Downloads"
            fi
            # Android owns networking, locking, authentication, and calendar
            # alarms. Their Linux desktop agents require system services that
            # intentionally do not exist in PRoot, so keep them from crashing
            # or wasting memory on every desktop launch.
            autostart_dir="${'$'}HOME/.config/autostart"
            mkdir -p "${'$'}autostart_dir"
            for autostart_id in light-locker.desktop nm-applet.desktop polkit-gnome-authentication-agent-1.desktop org.gnome.Evolution-alarm-notify.desktop; do
              printf '%s\n' \
                '[Desktop Entry]' \
                'Type=Application' \
                'Name=TuxSpan disabled host service' \
                'Hidden=true' \
                > "${'$'}autostart_dir/${'$'}autostart_id"
            done
            set_value() {
              channel="${'$'}1"; path="${'$'}2"; type="${'$'}3"; value="${'$'}4"
              if xfconf-query -c "${'$'}channel" -p "${'$'}path" >/dev/null 2>&1; then
                xfconf-query -c "${'$'}channel" -p "${'$'}path" -s "${'$'}value" >/dev/null 2>&1
              else
                xfconf-query -c "${'$'}channel" -p "${'$'}path" -n -t "${'$'}type" -s "${'$'}value" >/dev/null 2>&1
              fi
            }
            icon_theme="${'$'}HOME/.local/share/icons/TuxSpan"
            mkdir -p "${'$'}icon_theme/scalable/emblems"
            printf '%s\n' \
              '[Icon Theme]' \
              'Name=TuxSpan' \
              'Comment=Adwaita with clean TuxSpan desktop launchers' \
              'Inherits=Adwaita,hicolor' \
              'Directories=scalable/emblems' \
              '' \
              '[scalable/emblems]' \
              'Size=16' \
              'MinSize=1' \
              'MaxSize=512' \
              'Type=Scalable' \
              'Context=Emblems' > "${'$'}icon_theme/index.theme"
            printf '%s\n' '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16"></svg>' > "${'$'}icon_theme/scalable/emblems/emblem-symbolic-link.svg"
            gtk-update-icon-cache -f "${'$'}icon_theme" >/dev/null 2>&1 || true
            set_value xsettings /Gtk/FontName string '$fontName'
            set_value xsettings /Net/IconThemeName string 'TuxSpan'
            set_value xsettings /Gtk/CursorThemeName string 'Adwaita'
            set_value xsettings /Gtk/CursorThemeSize int '$cursorSize'
            set_value xsettings /Xft/DPI int '$dpi'
            set_value xfce4-desktop /desktop-icons/icon-size uint '$iconSize'
            rm -rf "${'$'}HOME/Desktop/Launcher trust FAIL"
            gtk_css="${'$'}HOME/.config/gtk-3.0/gtk.css"
            mkdir -p "${'$'}HOME/.config/gtk-3.0"
            touch "${'$'}gtk_css"
            sed -i '/TUXSPAN_LIVE_ICON_GAP_BEGIN/,/TUXSPAN_LIVE_ICON_GAP_END/d' "${'$'}gtk_css"
            sed -i '/TUXSPAN_DESKTOP_LABELS_BEGIN/,/TUXSPAN_DESKTOP_LABELS_END/d' "${'$'}gtk_css"
            printf '%s\n' \
              '/* TUXSPAN_DESKTOP_LABELS_BEGIN */' \
              'XfdesktopIconView.view {' \
              '  -XfdesktopIconView-ellipsize-icon-labels: 0;' \
              '  -XfdesktopIconView-cell-text-width-proportion: 2.55;' \
              '  -XfdesktopIconView-cell-spacing: 6;' \
              '  -XfdesktopIconView-cell-padding: 4;' \
              '}' \
              '.xfce4-panel#XfcePanelWindow {' \
              '  padding-left: 8px;' \
              '  padding-right: 8px;' \
              '  border-radius: 12px;' \
              '}' \
              '#applicationmenu-button, #launcher-button {' \
              '  margin-left: 5px;' \
              '  margin-right: 5px;' \
              '  border-radius: 8px;' \
              '}' \
              '#applicationmenu-button:hover, #launcher-button:hover {' \
              '  background-color: rgba(255, 255, 255, 0.12);' \
              '}' \
              '#launcher-button image, .xfce4-panel .tasklist .toggle image {' \
              '  -gtk-icon-transform: scale(0.84);' \
              '}' \
              '.xfce4-panel .tasklist .toggle {' \
              '  margin-left: 5px;' \
              '  margin-right: 5px;' \
              '  border-radius: 8px;' \
              '  background-color: rgba(255, 255, 255, 0.05);' \
              '}' \
              '.xfce4-panel .tasklist .toggle:hover {' \
              '  background-color: rgba(255, 255, 255, 0.14);' \
              '}' \
              '.xfce4-panel .tasklist .toggle:checked {' \
              '  background-color: rgba(172, 255, 76, 0.24);' \
              '}' \
              '/* TUXSPAN_DESKTOP_LABELS_END */' >> "${'$'}gtk_css"
            set_value xfce4-desktop /desktop-icons/file-icons/show-filesystem bool '$showFilesystemIcon'
            set_value xfce4-desktop /desktop-icons/file-icons/show-home bool 'true'
            set_value xfce4-desktop /desktop-icons/file-icons/show-trash bool 'true'
            # Remove XFCE's top panel instead of auto-hiding it. An auto-hidden
            # top-edge panel opens directly over maximized window controls,
            # making minimize and maximize frustrating with a mouse. The
            # Applications menu and running-window list remain in panel 2.
            xfconf-query -c xfce4-panel -p /panels -r >/dev/null 2>&1 || true
            xfconf-query -c xfce4-panel -p /panels -n -a -t int -s 2 >/dev/null 2>&1
            xfconf-query -c xfce4-panel -p /panels/panel-1 -r -R >/dev/null 2>&1 || true
            set_value xfce4-panel /panels/panel-2/autohide-behavior uint '0'
            set_value xfce4-panel /panels/panel-2/size uint '$bottomPanelSize'
            set_value xfce4-panel /panels/panel-2/nrows uint '1'
            set_value xfce4-panel /panels/panel-2/mode uint '0'
            set_value xfce4-panel /panels/panel-2/length-adjust bool 'true'
            set_value xfce4-panel /panels/panel-2/position-locked bool 'true'
            set_value xfce4-panel /panels/panel-2/enable-struts bool 'false'
            set_value xfce4-panel /panels/panel-2/position string 'p=10;x=0;y=0'
            dock_config="${'$'}HOME/.config/xfce4/panel"
            mkdir -p "${'$'}dock_config"
            create_dock_launcher() {
              plugin_id="${'$'}1"; desktop_file="${'$'}2"; app_name="${'$'}3"; app_exec="${'$'}4"; app_icon="${'$'}5"
              launcher_dir="${'$'}dock_config/launcher-${'$'}plugin_id"
              mkdir -p "${'$'}launcher_dir"
              printf '%s\n' '[Desktop Entry]' 'Version=1.0' 'Type=Application' "Name=${'$'}app_name" "Exec=${'$'}app_exec" "Icon=${'$'}app_icon" 'Terminal=false' > "${'$'}launcher_dir/${'$'}desktop_file"
              chmod 700 "${'$'}launcher_dir/${'$'}desktop_file"
              xfconf-query -c xfce4-panel -p "/plugins/plugin-${'$'}plugin_id" -r -R >/dev/null 2>&1 || true
              xfconf-query -c xfce4-panel -p "/plugins/plugin-${'$'}plugin_id" -n -t string -s 'launcher' >/dev/null 2>&1
              xfconf-query -c xfce4-panel -p "/plugins/plugin-${'$'}plugin_id/items" -n -a -t string -s "${'$'}desktop_file" >/dev/null 2>&1
            }
            xfconf-query -c xfce4-panel -p /plugins/plugin-200 -r -R >/dev/null 2>&1 || true
            xfconf-query -c xfce4-panel -p /plugins/plugin-200 -n -t string -s 'applicationsmenu' >/dev/null 2>&1
            set_value xfce4-panel /plugins/plugin-200/show-button-title bool 'false'
            create_dock_launcher 201 'tuxspan-firefox.desktop' 'Firefox' '${browserCommand(recipe)}' '${browserIcon(recipe)}'
            create_dock_launcher 202 'tuxspan-files.desktop' 'Files' 'thunar' 'system-file-manager'
            create_dock_launcher 203 'tuxspan-writer.desktop' 'Writer' 'libreoffice --norestore --writer' 'libreoffice-writer'
            create_dock_launcher 204 'tuxspan-media.desktop' 'Media' 'parole' 'org.xfce.parole'
            create_dock_launcher 205 'tuxspan-photos.desktop' 'Photos' 'ristretto' 'org.xfce.ristretto'
            create_dock_launcher 206 'tuxspan-terminal.desktop' 'Terminal' 'xfce4-terminal' 'utilities-terminal'
            xfconf-query -c xfce4-panel -p /plugins/plugin-207 -r -R >/dev/null 2>&1 || true
            xfconf-query -c xfce4-panel -p /plugins/plugin-207 -n -t string -s 'tasklist' >/dev/null 2>&1
            set_value xfce4-panel /plugins/plugin-207/show-labels bool 'false'
            set_value xfce4-panel /plugins/plugin-207/grouping bool 'true'
            set_value xfce4-panel /plugins/plugin-207/flat-buttons bool 'true'
            set_value xfce4-panel /plugins/plugin-207/show-handle bool 'false'
            set_value xfce4-panel /plugins/plugin-207/show-tooltips bool 'true'
            set_value xfce4-panel /plugins/plugin-207/include-all-workspaces bool 'false'
            set_value xfce4-panel /plugins/plugin-207/include-all-monitors bool 'false'
            set_value xfce4-panel /plugins/plugin-207/show-only-minimized bool 'false'
            xfconf-query -c xfce4-panel -p /panels/panel-2/plugin-ids -r >/dev/null 2>&1 || true
            xfconf-query -c xfce4-panel -p /panels/panel-2/plugin-ids -n -a \
              -t int -s 200 -t int -s 201 -t int -s 202 -t int -s 203 -t int -s 204 -t int -s 205 -t int -s 206 -t int -s 207 \
              >/dev/null 2>&1
            dock_positioner="${'$'}HOME/.local/bin/tuxspan-position-dock"
            mkdir -p "${'$'}HOME/.local/bin"
            printf '%s' '$encodedDockPositioner' | base64 -d > "${'$'}dock_positioner"
            chmod 700 "${'$'}dock_positioner"
            set_value xfwm4 /general/workspace_count int '1'
            set_value xfwm4 /general/theme string '$windowTheme'
            set_value xfwm4 /general/title_font string '$titleFont'
            set_value xfwm4 /general/use_compositing bool '$compositor'
            set_value xfwm4 /general/cycle_raise bool 'true'
            terminal_config="${'$'}HOME/.config/xfce4/terminal/terminalrc"
            mkdir -p "${'$'}HOME/.config/xfce4/terminal"
            touch "${'$'}terminal_config"
            if grep -q '^FontName=' "${'$'}terminal_config"; then
              sed -i 's/^FontName=.*/FontName=$terminalFont/' "${'$'}terminal_config"
            else
              printf '\nFontName=%s\n' '$terminalFont' >> "${'$'}terminal_config"
            fi
            $browserMigration
            $firefoxStabilitySetup
            $archiveMigration
            $shortcutSetup
            auto_keyboard_watcher="${'$'}HOME/.local/bin/tuxspan-auto-keyboard.py"
            printf '%s' '$encodedAutoKeyboardWatcher' | base64 -d > "${'$'}auto_keyboard_watcher"
            chmod 700 "${'$'}auto_keyboard_watcher"
            pkill -f '^(python3|.*/python3) .*tuxspan-auto-keyboard[.]py( |${'$'})' >/dev/null 2>&1 || true
            (
              if ! python3 -c 'import pyatspi' >/dev/null 2>&1; then
                export DEBIAN_FRONTEND=noninteractive
                apt-get install -y at-spi2-core python3-pyatspi
              fi
              exec env NO_AT_BRIDGE=0 python3 "${'$'}auto_keyboard_watcher"
            ) >/tmp/tuxspan-auto-keyboard.log 2>&1 &
            xfce4-panel --restart >/dev/null 2>&1
            nohup "${'$'}dock_positioner" '$bottomPanelSize' '$bottomPanelInset' >/tmp/tuxspan-dock-position.log 2>&1 &
        """.trimIndent()
    }

    /**
     * Bridges Linux accessibility focus to Termux:X11's Android IME action.
     * Termux:X11 itself cannot identify widgets inside the X11 framebuffer, so
     * the guest reports only real editable-focus transitions. Android 15 must
     * be given user 0 explicitly; its implicit current-user value (-2) is
     * rejected when `am` observes PRoot's synthetic Linux uid. Termux:X11
     * suppresses the IME when a hardware keyboard is connected
     * (`showIMEWhileExternalConnected=false`).
     */
    internal fun autoKeyboardWatcherScript(): String = """
        #!/usr/bin/python3
        import atexit
        import os
        import signal
        import subprocess
        import threading
        import time

        os.environ["NO_AT_BRIDGE"] = "0"

        import pyatspi

        TERMUX_AM = "/data/data/com.termux/files/usr/bin/am"
        TEXT_ROLES = {
            pyatspi.ROLE_ENTRY,
            pyatspi.ROLE_PASSWORD_TEXT,
            pyatspi.ROLE_TERMINAL,
        }

        state_lock = threading.Lock()
        focused_editable = False
        keyboard_requested = False
        last_request = 0.0
        input_monitor = None
        stopping = threading.Event()

        def is_editable(accessible):
            try:
                state = accessible.getState()
                role = accessible.getRole()
                return role in TEXT_ROLES or state.contains(pyatspi.STATE_EDITABLE)
            except Exception:
                return False

        def request_android_keyboard(reason):
            global keyboard_requested, last_request
            with state_lock:
                now = time.monotonic()
                if keyboard_requested or now - last_request < 0.8:
                    return
                keyboard_requested = True
                last_request = now
            try:
                print(f"request keyboard: {reason}", flush=True)
                subprocess.run(
                    [
                        TERMUX_AM,
                        "broadcast",
                        "--user", "0",
                        "-a", "com.termux.x11.ACTION_CUSTOM",
                        "-p", "com.termux.x11",
                        "--es", "what", "backButton",
                    ],
                    check=False,
                    stdin=subprocess.DEVNULL,
                    stdout=subprocess.DEVNULL,
                    stderr=subprocess.DEVNULL,
                    timeout=5,
                )
            except (OSError, subprocess.TimeoutExpired):
                with state_lock:
                    keyboard_requested = False

        def focused_text_control():
            def walk(accessible, depth=0):
                if depth > 18:
                    return False
                try:
                    state = accessible.getState()
                    if state.contains(pyatspi.STATE_FOCUSED) and is_editable(accessible):
                        return True
                    child_count = accessible.childCount
                except Exception:
                    return False
                for index in range(child_count):
                    try:
                        if walk(accessible.getChildAtIndex(index), depth + 1):
                            return True
                    except Exception:
                        continue
                return False

            try:
                return walk(pyatspi.Registry.getDesktop(0))
            except Exception:
                return False

        def reconcile_after_pointer_release():
            global focused_editable, keyboard_requested
            # GTK/Firefox update AT-SPI focus just after the X input event.
            time.sleep(0.16)
            editable = focused_text_control()
            with state_lock:
                focused_editable = editable
                if not editable:
                    keyboard_requested = False
            if editable:
                request_android_keyboard("pointer focus")

        def monitor_pointer_releases():
            global input_monitor
            try:
                # xinput uses block buffering when stdout is a pipe. Force
                # line buffering so a touch release reaches this watcher
                # immediately instead of only when the monitor exits.
                input_monitor = subprocess.Popen(
                    ["stdbuf", "-oL", "xinput", "test-xi2", "--root"],
                    stdin=subprocess.DEVNULL,
                    stdout=subprocess.PIPE,
                    stderr=subprocess.DEVNULL,
                    text=True,
                    bufsize=1,
                )
                assert input_monitor.stdout is not None
                for line in input_monitor.stdout:
                    if stopping.is_set():
                        break
                    if "(TouchEnd)" in line or "(ButtonRelease)" in line:
                        threading.Thread(
                            target=reconcile_after_pointer_release,
                            daemon=True,
                        ).start()
                if not stopping.is_set():
                    print(
                        f"xinput pointer monitor stopped ({input_monitor.returncode})",
                        flush=True,
                    )
            except (OSError, AssertionError):
                print("xinput pointer monitor unavailable", flush=True)

        def shutdown(*_):
            stopping.set()
            if input_monitor is not None and input_monitor.poll() is None:
                input_monitor.terminate()
            try:
                pyatspi.Registry.stop()
            except Exception:
                pass

        atexit.register(shutdown)
        signal.signal(signal.SIGTERM, shutdown)
        signal.signal(signal.SIGINT, shutdown)

        def on_focus(event):
            global focused_editable, keyboard_requested
            if not event.detail1:
                return
            editable = is_editable(event.source)
            with state_lock:
                focused_editable = editable
                if not editable:
                    keyboard_requested = False
            # Focus may change while Termux:X11 is still in the background
            # (for example while a launcher command opens Terminal). Do not
            # toggle Android's IME here. A real pointer release in the visible
            # X11 activity is the safe moment to request it.

        pyatspi.Registry.registerEventListener(
            on_focus,
            "object:state-changed:focused",
        )
        threading.Thread(target=monitor_pointer_releases, daemon=True).start()
        print("TuxSpan automatic keyboard watcher ready", flush=True)
        pyatspi.Registry.start()
    """.trimIndent()

    internal fun shellQuote(value: String): String =
        "'" + value.replace("'", "'\"'\"'") + "'"
}
