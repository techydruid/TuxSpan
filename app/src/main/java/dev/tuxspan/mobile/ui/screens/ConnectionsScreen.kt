package dev.tuxspan.mobile.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import dev.tuxspan.mobile.MainUiState
import dev.tuxspan.mobile.ui.ReadinessRow
import dev.tuxspan.mobile.ui.ScreenHeader
import dev.tuxspan.mobile.workspace.DisplayProfile
import dev.tuxspan.mobile.workspace.PerformancePreset
import dev.tuxspan.mobile.workspace.ScreenOrientation
import dev.tuxspan.mobile.workspace.TouchMode

@Composable
fun ConnectionsScreen(
    state: MainUiState,
    onDownloadTermux: () -> Unit,
    onDownloadX11: () -> Unit,
    onGrantPermission: () -> Unit,
    onOpenTermux: () -> Unit,
    onCopyOptIn: () -> Unit,
    onRemove: () -> Unit,
    onShowGuide: () -> Unit,
    onInstallBundle: (String) -> Unit,
    onDisplayProfile: (DisplayProfile) -> Unit,
    onScreenOrientation: (ScreenOrientation) -> Unit,
    onTouchMode: (TouchMode) -> Unit,
    onPerformancePreset: (PerformancePreset) -> Unit,
    onDownloadsBridge: (Boolean) -> Unit,
    onRequestDownloadsAccess: () -> Unit,
    onOpenTermuxStorageSettings: () -> Unit,
    onShowControls: () -> Unit,
    onHideControls: () -> Unit,
) {
    var desktopExpanded by rememberSaveable { mutableStateOf(false) }
    var inputExpanded by rememberSaveable { mutableStateOf(false) }
    var storageExpanded by rememberSaveable { mutableStateOf(false) }
    var companionsExpanded by rememberSaveable { mutableStateOf(false) }
    var advancedExpanded by rememberSaveable { mutableStateOf(false) }
    val readyCompanions = listOf(
        state.companions.termuxInstalled,
        state.companions.x11Installed,
        state.companions.runCommandGranted,
    ).count { it }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp, 20.dp, 18.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader(eyebrow = "Preferences", title = "Settings") }
        item {
            SettingsSection(
                title = "Desktop",
                summary = "${state.experience.displayProfile.label} · ${state.experience.screenOrientation.label}",
                icon = Icons.Outlined.Computer,
                expanded = desktopExpanded,
                onToggle = { desktopExpanded = !desktopExpanded },
            ) {
                ChoiceFlow(
                    label = "Layout",
                    values = DisplayProfile.entries,
                    selected = state.experience.displayProfile,
                    title = DisplayProfile::label,
                    onChoose = onDisplayProfile,
                )
                Spacer(Modifier.height(14.dp))
                ChoiceFlow(
                    label = "Orientation",
                    values = ScreenOrientation.entries,
                    selected = state.experience.screenOrientation,
                    title = ScreenOrientation::label,
                    onChoose = onScreenOrientation,
                )
            }
        }
        item {
            SettingsSection(
                title = "Input",
                summary = "${state.experience.touchMode.label} · Hidden controls",
                icon = Icons.Outlined.TouchApp,
                expanded = inputExpanded,
                onToggle = { inputExpanded = !inputExpanded },
            ) {
                ChoiceFlow(
                    label = "Touch control",
                    values = TouchMode.entries,
                    selected = state.experience.touchMode,
                    title = TouchMode::label,
                    onChoose = onTouchMode,
                )
                Spacer(Modifier.height(14.dp))
                Button(onClick = onHideControls, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.VisibilityOff, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Hide desktop controls")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onShowControls, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Keyboard, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Show desktop controls")
                }
            }
        }
        item {
            SettingsSection(
                title = "Storage",
                summary = when {
                    !state.experience.downloadsBridgeEnabled -> "Workspace files stay private"
                    state.storageBridgeChecking -> "Checking Android Downloads"
                    state.storageBridgeReady -> "Android Downloads ready"
                    else -> "Termux storage permission required"
                },
                icon = Icons.Outlined.Folder,
                expanded = storageExpanded,
                onToggle = { storageExpanded = !storageExpanded },
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Share Android Downloads", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Makes the phone's Downloads folder available inside Linux.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.experience.downloadsBridgeEnabled,
                        onCheckedChange = onDownloadsBridge,
                    )
                }
                if (state.experience.downloadsBridgeEnabled) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = when {
                            state.storageBridgeChecking -> "Checking read and write access…"
                            state.storageBridgeReady ->
                                "Ready in Linux Home as Android-Downloads. Restart Linux if it is already running."
                            else ->
                                "Termux cannot read and write the phone's Downloads folder yet."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.storageBridgeReady) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    if (!state.storageBridgeReady && !state.storageBridgeChecking) {
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onRequestDownloadsAccess,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Request storage access")
                        }
                        Spacer(Modifier.height(6.dp))
                        TextButton(
                            onClick = onOpenTermuxStorageSettings,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Open Termux permissions")
                        }
                    }
                }
            }
        }
        item {
            SettingsSection(
                title = "Companion apps",
                summary = "$readyCompanions of 3 ready",
                icon = Icons.Outlined.Apps,
                expanded = companionsExpanded,
                onToggle = { companionsExpanded = !companionsExpanded },
            ) {
                ReadinessRow(
                    label = "Termux",
                    detail = if (state.companions.termuxInstalled) "Installed" else "Required",
                    ready = state.companions.termuxInstalled,
                    action = {
                        TextButton(
                            onClick = if (state.companions.termuxInstalled) onOpenTermux else onDownloadTermux,
                        ) { Text(if (state.companions.termuxInstalled) "Open" else "Get") }
                    },
                )
                Spacer(Modifier.height(8.dp))
                ReadinessRow(
                    label = "Termux:X11",
                    detail = if (state.companions.x11Installed) "Installed" else "Required for desktops",
                    ready = state.companions.x11Installed,
                    action = {
                        if (!state.companions.x11Installed) {
                            TextButton(onClick = onDownloadX11) { Text("Get") }
                        }
                    },
                )
                Spacer(Modifier.height(8.dp))
                ReadinessRow(
                    label = "Command access",
                    detail = if (state.companions.runCommandGranted) "Allowed" else "Permission required",
                    ready = state.companions.runCommandGranted,
                    action = {
                        if (!state.companions.runCommandGranted) {
                            TextButton(onClick = onGrantPermission) { Text("Allow") }
                        }
                    },
                )
                if (state.activeWorkspace == null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Termux needs one-time consent before TuxSpan can run commands.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onCopyOptIn, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Copy consent command")
                    }
                    Button(onClick = onOpenTermux, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open Termux")
                    }
                }
            }
        }
        item {
            SettingsSection(
                title = "Advanced",
                summary = "${state.experience.performancePreset.label} power preset",
                icon = Icons.Outlined.Tune,
                expanded = advancedExpanded,
                onToggle = { advancedExpanded = !advancedExpanded },
            ) {
                ChoiceFlow(
                    label = "Power",
                    values = PerformancePreset.entries,
                    selected = state.experience.performancePreset,
                    title = PerformancePreset::label,
                    onChoose = onPerformancePreset,
                )
                Spacer(Modifier.height(14.dp))
                OutlinedButton(onClick = onShowGuide, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Help & controls")
                }
                if (state.activeWorkspace != null) {
                    OutlinedButton(
                        onClick = { onInstallBundle("starter") },
                        enabled = state.activeOperation != "bundle",
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Build, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Repair starter apps")
                    }
                    OutlinedButton(onClick = onRemove, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Remove workspace")
                    }
                }
                Text(
                    "Independent project · not affiliated with Termux or device manufacturers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    summary: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Surface(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggle()
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp).size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                )
            }
            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
                Column(Modifier.padding(16.dp)) { content() }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceFlow(
    label: String,
    values: List<T>,
    selected: T,
    title: (T) -> String,
    onChoose: (T) -> Unit,
) {
    Text(label, style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(6.dp))
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        values.forEach { value ->
            FilterChip(
                selected = value == selected,
                onClick = { onChoose(value) },
                label = { Text(title(value), style = MaterialTheme.typography.labelMedium) },
            )
        }
    }
}
