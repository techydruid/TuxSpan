package dev.tuxspan.mobile.model

enum class WorkspaceKind {
    DESKTOP,
    TERMINAL,
}

data class WorkspaceRecipe(
    val id: String,
    val name: String,
    val eyebrow: String,
    val description: String,
    val image: String,
    val distroLabel: String,
    val kind: WorkspaceKind,
    val desktopPackage: String?,
    val estimatedStorageGb: Int,
    val recommendedRamGb: Int,
    val includedTools: List<String>,
)

object WorkspaceCatalog {
    val canvas = WorkspaceRecipe(
        id = "canvas",
        name = "Canvas",
        eyebrow = "BALANCED DESKTOP",
        description = "A calm Debian workspace for browsing, documents, and everyday development.",
        image = "debian:12",
        distroLabel = "Debian 12 + XFCE",
        kind = WorkspaceKind.DESKTOP,
        desktopPackage = "xfce4",
        estimatedStorageGb = 8,
        recommendedRamGb = 4,
        includedTools = listOf("Firefox ESR", "LibreOffice", "Photos & media", "PDF, files & archives"),
    )

    val studio = WorkspaceRecipe(
        id = "studio",
        name = "Studio",
        eyebrow = "CREATOR DESKTOP",
        description = "An Ubuntu workspace with a broader base for coding and creative packages.",
        image = "ubuntu:24.04",
        distroLabel = "Ubuntu 24.04 + XFCE",
        kind = WorkspaceKind.DESKTOP,
        desktopPackage = "xfce4",
        estimatedStorageGb = 10,
        recommendedRamGb = 6,
        includedTools = listOf("Web browser", "LibreOffice", "Photos & media", "PDF, files & archives"),
    )

    val spark = WorkspaceRecipe(
        id = "spark",
        name = "Spark",
        eyebrow = "LIGHTWEIGHT SHELL",
        description = "A tiny Alpine command-line lab for older or memory-constrained devices.",
        image = "alpine:3.23",
        distroLabel = "Alpine 3.23 terminal",
        kind = WorkspaceKind.TERMINAL,
        desktopPackage = null,
        estimatedStorageGb = 2,
        recommendedRamGb = 2,
        includedTools = listOf("Git", "Python", "OpenSSH", "Nano"),
    )

    val all = listOf(canvas, studio, spark)

    fun byId(id: String?): WorkspaceRecipe = all.firstOrNull { it.id == id } ?: canvas
}
