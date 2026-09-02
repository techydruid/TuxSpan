package dev.tuxspan.mobile.workspace

enum class InstallProgressStatus {
    RUNNING,
    READY,
    FAILED,
    UNKNOWN,
}

data class InstallProgress(
    val status: InstallProgressStatus,
    val percent: Int?,
    val step: Int,
    val totalSteps: Int,
    val title: String,
    val detail: String,
) {
    companion object {
        private const val MARKER = "TUXSPAN_PROGRESS|"

        fun starting(totalSteps: Int): InstallProgress = InstallProgress(
            status = InstallProgressStatus.RUNNING,
            percent = 5,
            step = 1,
            totalSteps = totalSteps,
            title = "Preparing Termux",
            detail = "Starting the reviewed installation in the background.",
        )

        fun parse(output: String): InstallProgress? {
            val line = output.lineSequence().lastOrNull { it.startsWith(MARKER) } ?: return null
            val fields = line.removePrefix(MARKER).split('|', limit = 6)
            if (fields.size != 6) return null
            val status = runCatching { InstallProgressStatus.valueOf(fields[0]) }.getOrNull()
                ?: return null
            return InstallProgress(
                status = status,
                percent = fields[1].toIntOrNull()?.takeIf { it in 0..100 },
                step = fields[2].toIntOrNull()?.coerceAtLeast(0) ?: 0,
                totalSteps = fields[3].toIntOrNull()?.coerceAtLeast(1) ?: 1,
                title = fields[4].ifBlank { "Working in Termux" },
                detail = fields[5],
            )
        }
    }
}
