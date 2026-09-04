package dev.tuxspan.mobile.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.tuxspan.mobile.MainUiState
import dev.tuxspan.mobile.SessionStatus
import dev.tuxspan.mobile.model.WorkspaceCatalog
import dev.tuxspan.mobile.model.WorkspaceKind
import dev.tuxspan.mobile.model.WorkspaceRecipe
import dev.tuxspan.mobile.ui.BrandLockup
import dev.tuxspan.mobile.ui.Eyebrow
import dev.tuxspan.mobile.ui.InstallProgressPanel
import dev.tuxspan.mobile.ui.InitialSetupCard
import dev.tuxspan.mobile.ui.NoWorkspaceCard
import dev.tuxspan.mobile.ui.theme.Coral
import dev.tuxspan.mobile.ui.theme.Lime
import dev.tuxspan.mobile.workspace.WorkspacePhase
import dev.tuxspan.mobile.workspace.InstallProgressStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkbenchScreen(
    state: MainUiState,
    onBuild: () -> Unit,
    onLaunch: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onReconnect: () -> Unit,
    onVerify: () -> Unit,
    onOpenBlueprints: () -> Unit,
    onOpenSettings: () -> Unit,
    onShowGuide: () -> Unit,
    onInstallBundle: (String) -> Unit,
    onRemove: () -> Unit,
    onDownloadTermux: () -> Unit,
    onDownloadX11: () -> Unit,
    onGrantPermission: () -> Unit,
    onCopyOptIn: () -> Unit,
    onOpenTermux: () -> Unit,
    onVerifyConsent: () -> Unit,
    onCompleteInitialSetup: () -> Unit,
) {
    val activeWorkspace = state.activeWorkspace
    val recipe = activeWorkspace?.let { WorkspaceCatalog.byId(it.recipeId) }
        ?: state.selectedRecipe
    val phase = activeWorkspace?.phase ?: state.workspace(recipe.id)?.phase
    var appsVisible by rememberSaveable { mutableStateOf(false) }
    var overflowVisible by rememberSaveable { mutableStateOf(false) }
    var aboutVisible by rememberSaveable { mutableStateOf(false) }
    var diagnosticsVisible by rememberSaveable { mutableStateOf(false) }
    val appsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 18.dp,
            top = 12.dp,
            end = 18.dp,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandLockup()
                Spacer(Modifier.weight(1f))
                Box(Modifier.wrapContentSize(Alignment.TopEnd)) {
                    IconButton(onClick = { overflowVisible = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = overflowVisible,
                        onDismissRequest = { overflowVisible = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Help & controls", style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.HelpOutline, null) },
                            onClick = {
                                overflowVisible = false
                                onShowGuide()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Session details", style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = { Icon(Icons.Outlined.Info, null) },
                            onClick = {
                                overflowVisible = false
                                diagnosticsVisible = true
                            },
                        )
                        if (phase == WorkspacePhase.READY && recipe.kind == WorkspaceKind.DESKTOP) {
                            DropdownMenuItem(
                                text = { Text("Repair starter apps", style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = { Icon(Icons.Outlined.Build, null) },
                                enabled = state.activeOperation != "bundle",
                                onClick = {
                                    overflowVisible = false
                                    onInstallBundle("starter")
                                },
                            )
                        }
                        if (state.activeWorkspace != null) {
                            DropdownMenuItem(
                                text = { Text("Remove workspace", style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) },
                                onClick = {
                                    overflowVisible = false
                                    onRemove()
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("About TuxSpan", style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = { Icon(Icons.Outlined.Info, null) },
                            onClick = {
                                overflowVisible = false
                                aboutVisible = true
                            },
                        )
                    }
                }
            }
        }
        item {
            when {
                state.initialSetupRequired -> InitialSetupCard(
                    state = state,
                    onDownloadTermux = onDownloadTermux,
                    onDownloadX11 = onDownloadX11,
                    onGrantPermission = onGrantPermission,
                    onCopyOptIn = onCopyOptIn,
                    onOpenTermux = onOpenTermux,
                    onVerifyConsent = onVerifyConsent,
                    onContinue = onCompleteInitialSetup,
                )

                activeWorkspace == null -> NoWorkspaceCard(onChooseWorkspace = onOpenBlueprints)

                else -> WorkspaceHero(
                    state = state,
                    recipe = recipe,
                    phase = phase,
                    onBuild = onBuild,
                    onLaunch = onLaunch,
                    onStop = onStop,
                    onRestart = onRestart,
                    onReconnect = onReconnect,
                    onVerify = onVerify,
                    onOpenBlueprints = onOpenBlueprints,
                    onOpenSettings = onOpenSettings,
                    onOpenApps = { appsVisible = true },
                )
            }
        }
    }

    if (appsVisible) {
        ModalBottomSheet(
            onDismissRequest = { appsVisible = false },
            sheetState = appsSheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 18.dp, end = 18.dp, bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Apps & tools", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Add or repair curated app packs inside this workspace.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ToolBundles(
                    desktop = recipe.kind == WorkspaceKind.DESKTOP,
                    installingBundleId = state.installingBundleId,
                    onInstall = onInstallBundle,
                )
            }
        }
    }

    if (diagnosticsVisible) {
        AlertDialog(
            onDismissRequest = { diagnosticsVisible = false },
            title = { Text("Session details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailLine("Workspace", recipe.name)
                    DetailLine("Session", state.sessionStatus.label)
                    DetailLine("Termux", if (state.companions.termuxInstalled) "Ready" else "Missing")
                    DetailLine("Termux:X11", if (state.companions.x11Installed) "Ready" else "Missing")
                    Text(
                        "Launch log: ~/.local/state/tuxspan/${recipe.id}-launch.log",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { diagnosticsVisible = false }) { Text("Done") }
            },
        )
    }

    if (aboutVisible) {
        AlertDialog(
            onDismissRequest = { aboutVisible = false },
            title = { Text("TuxSpan") },
            text = {
                Text(
                    "A local, rootless Linux workspace manager for Android. Independent from Termux and device manufacturers.",
                )
            },
            confirmButton = {
                TextButton(onClick = { aboutVisible = false }) { Text("Done") }
            },
        )
    }
}

@Composable
private fun WorkspaceHero(
    state: MainUiState,
    recipe: WorkspaceRecipe,
    phase: WorkspacePhase?,
    onBuild: () -> Unit,
    onLaunch: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onReconnect: () -> Unit,
    onVerify: () -> Unit,
    onOpenBlueprints: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenApps: () -> Unit,
) {
    Card(
        modifier = Modifier.animateContentSize(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(46.dp)
                        .background(
                            if (phase == WorkspacePhase.READY) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else if (phase == WorkspacePhase.NEEDS_REPAIR) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            },
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        when (phase) {
                            WorkspacePhase.READY -> Icons.Outlined.CheckCircle
                            WorkspacePhase.NEEDS_REPAIR -> Icons.Outlined.Build
                            else -> Icons.Outlined.Add
                        },
                        contentDescription = null,
                        tint = when (phase) {
                            WorkspacePhase.READY -> MaterialTheme.colorScheme.onPrimaryContainer
                            WorkspacePhase.NEEDS_REPAIR -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                        },
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Eyebrow(
                        when (phase) {
                            WorkspacePhase.READY -> "Active workspace"
                            WorkspacePhase.NEEDS_REPAIR -> "Repair required"
                            else -> "Suggested workspace"
                        },
                        if (phase == WorkspacePhase.NEEDS_REPAIR) Coral else Lime,
                    )
                    Text(recipe.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        recipe.distroLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            when (phase) {
                WorkspacePhase.READY -> SessionActions(
                    state = state,
                    workspaceKind = recipe.kind,
                    onLaunch = onLaunch,
                    onStop = onStop,
                    onRestart = onRestart,
                    onReconnect = onReconnect,
                )

                WorkspacePhase.INSTALL_DISPATCHED -> {
                    val progress = state.installProgress
                    if (progress != null) {
                        InstallProgressPanel(progress)
                        Spacer(Modifier.height(8.dp))
                        if (
                            progress.status == InstallProgressStatus.RUNNING ||
                            progress.status == InstallProgressStatus.UNKNOWN
                        ) {
                            TextButton(
                                onClick = onBuild,
                                modifier = Modifier.align(Alignment.End),
                            ) {
                                Text("View installation")
                            }
                        } else {
                            Button(onClick = onBuild, modifier = Modifier.fillMaxWidth()) {
                                Text("Review setup")
                            }
                        }
                    } else {
                        Button(
                            onClick = onVerify,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = state.activeOperation == null,
                        ) {
                            if (state.activeOperation == "verify") {
                                SmallProgress()
                                Spacer(Modifier.width(9.dp))
                            }
                            Text(if (state.activeOperation == "verify") "Checking" else "Verify setup")
                        }
                    }
                }

                WorkspacePhase.NEEDS_REPAIR -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    "Desktop packages are incomplete",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                Text(
                                    "Repair will install the missing XFCE components and keep your existing workspace files.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = onBuild, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Build, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Repair ${recipe.name}")
                    }
                }

                else -> Button(onClick = onBuild, modifier = Modifier.fillMaxWidth()) {
                    Text("Build ${recipe.name}")
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
                }
            }
            if (phase == WorkspacePhase.READY) {
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (recipe.kind == WorkspaceKind.DESKTOP) {
                        OutlinedButton(onClick = onOpenSettings, modifier = Modifier.weight(1f)) {
                            Text("Desktop")
                        }
                    }
                    OutlinedButton(onClick = onOpenApps, modifier = Modifier.weight(1f)) {
                        Text("Apps")
                    }
                }
            } else {
                TextButton(onClick = onOpenBlueprints, modifier = Modifier.align(Alignment.End)) {
                    Text("Choose another workspace")
                }
            }
        }
    }
}

@Composable
private fun SessionActions(
    state: MainUiState,
    workspaceKind: WorkspaceKind,
    onLaunch: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onReconnect: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Session", style = MaterialTheme.typography.bodyMedium)
            Text(
                state.sessionStatus.label,
                style = MaterialTheme.typography.labelLarge,
                color = when (state.sessionStatus) {
                    SessionStatus.RUNNING -> Lime
                    SessionStatus.ERROR -> Coral
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
    Spacer(Modifier.height(10.dp))
    if (state.sessionStatus == SessionStatus.ERROR) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.errorContainer,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        "Desktop couldn't start",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        state.sessionError
                            ?: "The desktop process stopped unexpectedly. Retry the launch below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
    when (state.sessionStatus) {
        SessionStatus.RUNNING -> {
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onReconnect()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (workspaceKind == WorkspaceKind.TERMINAL) {
                        "Return to terminal"
                    } else {
                        "Return to desktop"
                    },
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onStop, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Stop, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Stop")
                }
                OutlinedButton(onClick = onRestart, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.RestartAlt, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Restart")
                }
            }
        }

        SessionStatus.STARTING, SessionStatus.STOPPING -> Button(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
        ) {
            SmallProgress()
            Spacer(Modifier.width(9.dp))
            Text(state.sessionStatus.label)
        }

        SessionStatus.STOPPED, SessionStatus.ERROR -> Button(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onLaunch()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (state.sessionStatus == SessionStatus.ERROR) "Retry launch" else "Launch")
        }
    }
}

@Composable
private fun SmallProgress() {
    CircularProgressIndicator(
        modifier = Modifier.size(18.dp),
        strokeWidth = 2.dp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ToolBundles(
    desktop: Boolean,
    installingBundleId: String?,
    onInstall: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (installingBundleId != null) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                "Installing ${installingBundleId.replaceFirstChar { it.uppercase() }} pack…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (desktop) {
            BundleRow("Starter pack", "Everyday apps", "starter", onInstall, "Repair", installingBundleId)
            BundleRow("Creator pack", "Design, audio, and video", "creator", onInstall, "Add", installingBundleId)
        }
        BundleRow("Developer pack", "Code, databases, and debugging", "developer", onInstall, "Add", installingBundleId)
    }
}

@Composable
private fun BundleRow(
    name: String,
    detail: String,
    id: String,
    onInstall: (String) -> Unit,
    actionLabel: String,
    installingBundleId: String?,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(onClick = { onInstall(id) }, enabled = installingBundleId == null) {
                if (installingBundleId == id) SmallProgress() else Text(actionLabel)
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
