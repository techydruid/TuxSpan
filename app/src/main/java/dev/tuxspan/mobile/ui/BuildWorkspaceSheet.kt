package dev.tuxspan.mobile.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.tuxspan.mobile.MainUiState
import dev.tuxspan.mobile.model.WorkspaceKind
import dev.tuxspan.mobile.workspace.WorkspacePhase
import dev.tuxspan.mobile.workspace.InstallProgressStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildWorkspaceSheet(
    state: MainUiState,
    installScript: String,
    onDismiss: () -> Unit,
    onTogglePreview: () -> Unit,
    onDownloadTermux: () -> Unit,
    onDownloadX11: () -> Unit,
    onGrantPermission: () -> Unit,
    onOpenTermux: () -> Unit,
    onCopyOptIn: () -> Unit,
    onCopyManualSetup: () -> Unit,
    onInstall: () -> Unit,
    onVerify: () -> Unit,
) {
    val recipe = state.selectedRecipe
    val needsX11 = recipe.kind == WorkspaceKind.DESKTOP
    val dependenciesReady = state.companions.termuxInstalled &&
        state.companions.runCommandGranted &&
        (!needsX11 || state.companions.x11Installed)
    val installDispatched =
        state.workspace(recipe.id)?.phase == WorkspacePhase.INSTALL_DISPATCHED
    val installStatus = state.installProgress
    val installationRunning = installStatus?.status == InstallProgressStatus.RUNNING ||
        installStatus?.status == InstallProgressStatus.UNKNOWN
    val scrollState = rememberScrollState()

    LaunchedEffect(installationRunning) {
        if (installationRunning) scrollState.animateScrollTo(0)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(start = 18.dp, end = 18.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Eyebrow("Review before running")
            Text("Build ${recipe.name}", style = MaterialTheme.typography.headlineSmall)
            Text(
                recipe.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            installStatus?.let { progress ->
                InstallProgressPanel(progress)
            }
            if (installationRunning) {
                Text(
                    "You can leave this screen while setup continues. Keep the phone online, avoid force-stopping Termux, and TuxSpan will update each completed stage here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    PlanLine("Image", recipe.image)
                    PlanLine("Interface", recipe.distroLabel)
                    PlanLine("Memory guidance", "${recipe.recommendedRamGb} GB RAM or more")
                    if (needsX11) {
                        PlanLine("Starter apps", "Web, office, PDF, photos, media and files")
                    }
                    PlanLine("Storage target", "${recipe.estimatedStorageGb} GB or more")
                    PlanLine("Root access", "Not used")
                }
            }
            ReadinessRow(
                label = "Termux",
                detail = if (state.companions.termuxInstalled) "Installed" else "Install from F-Droid",
                ready = state.companions.termuxInstalled,
                action = {
                    if (!state.companions.termuxInstalled) {
                        TextButton(onClick = onDownloadTermux) { Text("Get") }
                    }
                },
            )
            if (needsX11) {
                ReadinessRow(
                    label = "Termux:X11",
                    detail = if (state.companions.x11Installed) "Installed" else "Install the official nightly app",
                    ready = state.companions.x11Installed,
                    action = {
                        if (!state.companions.x11Installed) {
                            TextButton(onClick = onDownloadX11) { Text("Get") }
                        }
                    },
                )
            }
            ReadinessRow(
                label = "Run command permission",
                detail = if (state.companions.runCommandGranted) "Granted" else "Android permission required",
                ready = state.companions.runCommandGranted,
                action = {
                    if (!state.companions.runCommandGranted) {
                        TextButton(onClick = onGrantPermission) { Text("Grant") }
                    }
                },
            )
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("One-time Termux consent", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Copy the opt-in command, open Termux, paste it, and press Enter. TuxSpan cannot silently enable this setting.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onCopyOptIn, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Copy")
                        }
                        OutlinedButton(onClick = onOpenTermux, modifier = Modifier.weight(1f)) {
                            Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Open")
                        }
                    }
                }
            }
            OutlinedButton(onClick = onTogglePreview, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Code, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (state.commandPreviewVisible) "Hide command" else "Inspect exact command")
            }
            if (state.commandPreviewVisible) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = installScript,
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(15.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    )
                }
            }
            if (installDispatched) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onVerify,
                        enabled = state.activeOperation == null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            when (state.activeOperation) {
                                "install" -> "Installing in Termux"
                                "verify" -> "Checking…"
                                else -> "Verify finished setup"
                            },
                        )
                    }
                    OutlinedButton(
                        onClick = onInstall,
                        enabled = state.activeOperation == null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Run setup again")
                    }
                    OutlinedButton(onClick = onCopyManualSetup, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Copy manual setup")
                    }
                    OutlinedButton(onClick = onOpenTermux, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open Termux")
                    }
                }
            } else {
                Button(
                    onClick = onInstall,
                    enabled = dependenciesReady && state.activeOperation == null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.activeOperation == "install") "Installation running…" else "Run reviewed setup")
                }
            }
            state.buildMessage?.let { feedback ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        feedback,
                        modifier = Modifier.padding(15.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            Text(
                "Desktop setup also installs the reviewed everyday Starter pack and assigns the default apps for common files. Downloads come from the Termux and selected distribution repositories, may take several minutes, and can use significant data.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlanLine(label: String, value: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
