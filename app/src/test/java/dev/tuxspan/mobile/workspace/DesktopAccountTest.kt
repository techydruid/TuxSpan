package dev.tuxspan.mobile.workspace

import dev.tuxspan.mobile.model.WorkspaceCatalog
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class DesktopAccountTest {
    @Test
    fun desktopInstallationAndLaunchUseTheManagedAccount() {
        listOf(WorkspaceCatalog.canvas, WorkspaceCatalog.studio).forEach { recipe ->
            val install = WorkspaceScripts.install(recipe)
            assertTrue(install.contains("sudo passwd util-linux procps"))
            assertTrue(install.contains("useradd -M -U -d /home/tuxspan"))
            assertTrue(install.contains("visudo -cf /etc/sudoers.d/tuxspan"))
            assertTrue(WorkspaceScripts.verify(recipe).contains(DesktopAccount.READY))
            assertTrue(WorkspaceScripts.launch(recipe).contains("runuser -u tuxspan -- env HOME=/home/tuxspan"))
            assertTrue(WorkspaceScripts.installBundle(recipe, "creator").contains("runuser -u tuxspan"))
        }
        assertFalse(WorkspaceScripts.install(WorkspaceCatalog.spark).contains("useradd"))
        assertFalse(WorkspaceScripts.launch(WorkspaceCatalog.spark).contains("runuser"))
    }

    @Test
    fun accountSetupIsCheckedBeforeItIsMarkedComplete() {
        val setup = DesktopAccount.setup()
        assertTrue(setup.contains("pgrep -x xfce4-session"))
        assertTrue(setup.indexOf("visudo -cf") < setup.indexOf("touch ${DesktopAccount.READY}"))
        assertTrue(setup.indexOf("sudo -n id -u") < setup.indexOf("touch ${DesktopAccount.READY}"))
        assertTrue(DesktopAccount.asUser("vlc").contains("cd \"${'$'}HOME\" || exit 1"))
        assertFalse(setup.contains("rm -rf /root"))
        assertFalse(setup.contains("sed -i")) // Never patch VLC, sudo, or package-manager binaries.
    }

    @Test
    fun exportAccountAndMigrationFixturesForRealDeviceTests() {
        val dir = File("build/account-regression").apply { mkdirs() }
        File(dir, "account-setup.sh").writeText(DesktopAccount.setup() + "\n")
        File(dir, "migrate.py").writeText(DesktopAccount.migrateHome() + "\n")
        listOf(WorkspaceCatalog.canvas, WorkspaceCatalog.studio).forEach { recipe ->
            File(dir, "${recipe.id}-launch.sh").writeText(WorkspaceScripts.launch(recipe) + "\n")
            File(dir, "${recipe.id}-stop.sh").writeText(WorkspaceScripts.stop(recipe) + "\n")
            File(dir, "${recipe.id}-downloads-launch.sh").writeText(
                WorkspaceScripts.launch(recipe, ExperienceSettings(downloadsBridgeEnabled = true)) + "\n",
            )
            File(dir, "${recipe.id}-profile.sh").writeText(WorkspaceScripts.guestExperienceScript(recipe, ExperienceSettings(), 192) + "\n")
        }
        File(dir, "migration-test.py").writeText("""
            import os, runpy, sys, tempfile
            from pathlib import Path
            script = Path(__file__).with_name('migrate.py')
            with tempfile.TemporaryDirectory(prefix='tuxspan-account-test-') as temporary:
                base = Path(temporary)
                old, new, outside = base/'old', base/'new', base/'outside'
                for folder in (old, new, outside): folder.mkdir()
                (old/'Documents').mkdir()
                (old/'Documents'/'sample.txt').write_text('my document')
                (old/'existing.txt').write_text('old content')
                (new/'existing.txt').write_text('keep new content')
                (old/'Android-Downloads').mkdir()
                (old/'Android-Downloads'/'phone.txt').write_text('do not copy')
                (old/'.cache').mkdir()
                (old/'.cache'/'cache.txt').write_text('do not copy')
                (old/'Downloads').symlink_to(old/'Android-Downloads')
                (old/'external').symlink_to(outside)
                (outside/'unchanged.txt').write_text('outside')
                (old/'collision').mkdir()
                (old/'collision'/'must-not-write.txt').write_text('blocked')
                (new/'collision').symlink_to(outside)
                sys.argv = [str(script), str(old), str(new), str(os.getuid()), str(os.getgid())]
                runpy.run_path(str(script), run_name='__main__')
                assert (old/'Documents'/'sample.txt').read_text() == 'my document'
                assert (new/'Documents'/'sample.txt').read_text() == 'my document'
                assert (new/'existing.txt').read_text() == 'keep new content'
                assert not (new/'Android-Downloads').exists()
                assert not (new/'.cache').exists()
                assert os.readlink(new/'Downloads') == str(new/'Android-Downloads')
                assert (new/'external').is_symlink()
                assert not (outside/'must-not-write.txt').exists()
                (new/'Documents'/'sample.txt').write_text('new edits')
                runpy.run_path(str(script), run_name='__main__')
                assert (new/'Documents'/'sample.txt').read_text() == 'new edits'
                assert (outside/'unchanged.txt').read_text() == 'outside'
                print('PASS: original data preserved; retries preserve edits; links not followed; cache and Android bind excluded')
        """.trimIndent() + "\n")
        assertTrue(File(dir, "migrate.py").isFile)
    }
}
