package dev.tuxspan.mobile.workspace

/** Guest-only account setup. It never changes Android/Termux accounts or host permissions. */
internal object DesktopAccount {
    const val USER = "tuxspan"
    const val HOME = "/home/tuxspan"
    const val READY = "/var/lib/tuxspan/desktop-account-v1"

    fun asUser(command: String): String =
        "runuser -u $USER -- env HOME=$HOME USER=$USER LOGNAME=$USER /bin/sh -c " +
            WorkspaceScripts.shellQuote("cd \"${'$'}HOME\" || exit 1; $command")

    fun check(): String = "test -f $READY && test \"${'$'}(id -u $USER)\" != 0 && " +
        "test \"${'$'}(${asUser("sudo -n id -u")})\" = 0"

    fun setup(): String = """
        set -eu
        account_state=/var/lib/tuxspan
        mkdir -p "${'$'}account_state"
        # A running desktop may still be writing Firefox profiles and documents.
        if ! test -f $READY && pgrep -x xfce4-session >/dev/null 2>&1; then
          printf 'Error: Close the Linux desktop in TuxSpan before updating its account. Files have not been moved or deleted.\n' >&2
          exit 1
        fi
        if ! id $USER >/dev/null 2>&1; then
          if test -e $HOME || test -L $HOME; then
            printf 'Error: $HOME already exists without the TuxSpan account; refusing to overwrite it.\n' >&2
            exit 1
          fi
          useradd -M -U -d $HOME -s /bin/bash $USER
        fi
        if test "${'$'}(getent passwd $USER | cut -d: -f6)" != '$HOME' || test "${'$'}(id -u $USER)" = 0 || test -L $HOME; then
          printf 'Error: The existing TuxSpan Linux account has an unexpected home or UID.\n' >&2
          exit 1
        fi
        if ! test -f $READY; then
          printf 'Preparing your regular Linux account. Keeping the original /root files.\n'
          python3 -c ${WorkspaceScripts.shellQuote(migrateHome())} /root $HOME "${'$'}(id -u $USER)" "${'$'}(id -g $USER)"
        fi
        # Explicit convenience policy inside the PRoot guest only. PRoot is not
        # a security boundary and this grants no Android root privileges.
        install -d -m 0755 /etc/sudoers.d
        printf '%s\n' '$USER ALL=(ALL:ALL) NOPASSWD: ALL' 'Defaults:$USER !use_pty' > /etc/sudoers.d/tuxspan
        chmod 0440 /etc/sudoers.d/tuxspan
        visudo -cf /etc/sudoers.d/tuxspan
        if test "${'$'}(${asUser("sudo -n id -u")})" != 0; then
          printf 'Error: This PRoot version could not run sudo. Account setup is not complete.\n' >&2
          exit 1
        fi
        touch $READY
    """.trimIndent()

    /** Copy, never move, old personal data. No symlink traversal, cache, or Android bind copies. */
    internal fun migrateHome(): String = """
        import os, shutil, stat, sys, tempfile
        from pathlib import Path

        source, target = map(Path, sys.argv[1:3])
        uid, gid = map(int, sys.argv[3:5])
        if source.is_symlink() or target.is_symlink() or not source.is_dir():
            raise RuntimeError('Unexpected home path; no files were migrated')
        if source.resolve() == target.resolve() or source.resolve() in target.resolve().parents:
            raise RuntimeError('Homes must be separate directories')

        def copy_missing(src, dst):
            mode = src.lstat().st_mode
            # Existing user content wins, including dangling links. Never walk
            # a destination symlink on an interrupted/retried migration.
            exists = os.path.lexists(dst)
            if exists and (dst.is_symlink() or not (stat.S_ISDIR(mode) and dst.is_dir())):
                return
            if stat.S_ISLNK(mode):
                link = os.readlink(src)
                if link == str(source) or link.startswith(str(source) + '/'):
                    link = str(target) + link[len(str(source)):]
                dst.symlink_to(link)
                os.lchown(dst, uid, gid)
            elif stat.S_ISDIR(mode):
                if not exists:
                    dst.mkdir(mode=0o700)
                    os.chown(dst, uid, gid)
                for child in src.iterdir():
                    copy_missing(child, dst / child.name)
            elif stat.S_ISREG(mode) and not exists:
                # A failed copy never leaves a truncated final file that a
                # retry would mistake for user-created content.
                fd, temporary = tempfile.mkstemp(prefix='.tuxspan-import-', dir=dst.parent)
                try:
                    os.close(fd)
                    shutil.copy2(src, temporary)
                    os.chown(temporary, uid, gid)
                    if not os.path.lexists(dst):
                        os.rename(temporary, dst)
                finally:
                    if os.path.exists(temporary):
                        os.unlink(temporary)

        target.mkdir(mode=0o700, parents=True, exist_ok=True)
        os.chown(target, uid, gid)
        for entry in source.iterdir():
            if entry.name not in {'.cache', 'Android-Downloads', '.Xauthority', '.ICEauthority'}:
                copy_missing(entry, target / entry.name)
        # Fresh workspaces also get the distribution's normal shell defaults.
        skeleton = Path('/etc/skel')
        if skeleton.is_dir():
            for entry in skeleton.iterdir():
                copy_missing(entry, target / entry.name)
        print('Home prepared; original files remain in ' + str(source))
    """.trimIndent()
}
