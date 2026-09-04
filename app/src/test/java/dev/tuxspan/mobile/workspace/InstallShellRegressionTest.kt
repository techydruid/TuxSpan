package dev.tuxspan.mobile.workspace

import dev.tuxspan.mobile.model.WorkspaceCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

/** Run real Bash control-flow tests on CI and export the same tests for Termux. */
class InstallShellRegressionTest {
    private fun fixtureDirectory(): File = File("build/install-shell-regression").apply { mkdirs() }

    private fun writeFixtures(): File {
        val directory = fixtureDirectory()
        val recipe = WorkspaceCatalog.canvas
        val success = WorkspaceScripts.trackedInstall(recipe, "set -eu\nprintf 'Mock packages configured.\\n'")
        val failure = WorkspaceScripts.trackedInstall(
            recipe,
            "set -eu\nprintf 'E: simulated package failure\\n' >&2\nbash -c 'exit 100'\nprintf 'MUST_NOT_RUN\\n'",
        )
        File(directory, "success.sh").writeText(WorkspaceScripts.returnToTermuxPrompt(success))
        File(directory, "failure.sh").writeText(WorkspaceScripts.returnToTermuxPrompt(failure))
        WorkspaceCatalog.all.forEach {
            File(directory, "${it.id}-syntax.sh").writeText(WorkspaceScripts.foregroundInstall(it))
        }
        val harness = File(directory, "regression.sh")
        harness.writeText("""
            #!/usr/bin/env bash
            set -eu
            fixtures=${'$'}(cd -- "${'$'}(dirname -- "${'$'}0")" && pwd)
            # All mock status and logs stay in a new disposable test home.
            export HOME=${'$'}(mktemp -d "${'$'}fixtures/mock-home.XXXXXX")
            for recipe in canvas studio spark; do
              bash -n "${'$'}fixtures/${'$'}recipe-syntax.sh"
            done
            set +e
            bash "${'$'}fixtures/failure.sh" </dev/null > "${'$'}HOME/failure-output" 2>&1
            failure_exit=${'$'}?
            set -e
            test "${'$'}failure_exit" -eq 100
            test "${'$'}(cat "${'$'}HOME/.local/state/tuxspan/canvas.exit-code")" = 100
            test "${'$'}(cat "${'$'}HOME/.local/state/tuxspan/canvas.status")" = failed
            grep -q 'FAILED|.*code 100.*simulated package failure' "${'$'}HOME/.local/state/tuxspan/canvas.progress"
            ! grep -q 'MUST_NOT_RUN\|TUXSPAN_INSTALL_READY' "${'$'}HOME/failure-output"
            printf 'PASS: failure preserves code 100, reports the cause, and never announces success.\n'
            bash "${'$'}fixtures/success.sh" </dev/null > "${'$'}HOME/success-output" 2>&1
            test "${'$'}(cat "${'$'}HOME/.local/state/tuxspan/canvas.exit-code")" = 0
            test "${'$'}(cat "${'$'}HOME/.local/state/tuxspan/canvas.status")" = ready
            grep -q '^READY|100|' "${'$'}HOME/.local/state/tuxspan/canvas.progress"
            grep -q 'Canvas setup completed successfully' "${'$'}HOME/success-output"
            printf 'PASS: success records ready and prints a completion message.\n'
            printf 'PASS: non-interactive callers finish without waiting for input.\n'
            printf 'PASS: all three generated foreground scripts pass bash -n.\n'
        """.trimIndent() + "\n")
        return harness
    }

    @Test
    fun exportTheExactGeneratedScriptsForDeviceRegressionTests() {
        val harness = writeFixtures()
        assertTrue(harness.isFile)
        assertTrue(File(harness.parentFile, "canvas-syntax.sh").readText().contains("--force-confold"))
    }

    @Test
    fun bashPreservesSuccessAndFailureResultsWithoutWaitingForInput() {
        val harness = writeFixtures()
        val bashAvailable = runCatching {
            ProcessBuilder("bash", "--version").start().waitFor() == 0
        }.getOrDefault(false)
        assumeTrue("Bash is unavailable on this host; run exported harness in Termux.", bashAvailable)
        val output = File(harness.parentFile, "result.txt")
        val process = ProcessBuilder("bash", harness.absolutePath)
            .redirectErrorStream(true).redirectOutput(output).start()
        val finished = process.waitFor(30, TimeUnit.SECONDS)
        if (!finished) process.destroyForcibly()
        assertTrue("Installation wrapper waited for input", finished)
        assertEquals(output.readText(), 0, process.exitValue())
    }
}
