package dev.tuxspan.mobile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.tuxspan.mobile.platform.CompanionInspector
import dev.tuxspan.mobile.ui.BuildWorkspaceSheet
import dev.tuxspan.mobile.ui.MobileGuideDialog
import dev.tuxspan.mobile.ui.screens.BlueprintsScreen
import dev.tuxspan.mobile.ui.screens.ConnectionsScreen
import dev.tuxspan.mobile.ui.screens.DeviceLabScreen
import dev.tuxspan.mobile.ui.screens.WorkbenchScreen
import dev.tuxspan.mobile.ui.theme.TuxSpanTheme

private enum class AppPage(
    val label: String,
    val icon: ImageVector,
) {
    WORKBENCH("Home", Icons.Outlined.Dashboard),
    BLUEPRINTS("Plans", Icons.Outlined.GridView),
    DEVICE_LAB("Device", Icons.Outlined.Memory),
    CONNECTIONS("Settings", Icons.Outlined.Settings),
}

@Composable
fun TuxSpanRoot(viewModel: MainViewModel = viewModel()) {
    TuxSpanTheme {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val snackbarHost = remember { SnackbarHostState() }
        var page by rememberSaveable { mutableStateOf(AppPage.WORKBENCH) }
        var showRemoveConfirmation by rememberSaveable { mutableStateOf(false) }
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { viewModel.refresh() }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        LaunchedEffect(state.message) {
            val message = state.message ?: return@LaunchedEffect
            snackbarHost.showSnackbar(message)
            viewModel.dismissMessage()
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 760.dp
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbarHost) },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar {
                            AppPage.entries.forEach { item ->
                                NavigationBarItem(
                                    selected = page == item,
                                    onClick = { page = item },
                                    icon = { Icon(item.icon, contentDescription = null) },
                                    label = {
                                        Text(
                                            item.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                        )
                                    },
                                )
                            }
                        }
                    }
                },
            ) { padding ->
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    if (useRail) {
                        NavigationRail {
                            AppPage.entries.forEach { item ->
                                NavigationRailItem(
                                    selected = page == item,
                                    onClick = { page = item },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                )
                            }
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        when (page) {
                            AppPage.WORKBENCH -> WorkbenchScreen(
                                state = state,
                                onBuild = viewModel::openActiveBuildSheet,
                                onLaunch = viewModel::launchWorkspace,
                                onStop = viewModel::stopWorkspace,
                                onRestart = viewModel::restartWorkspace,
                                onReconnect = viewModel::reconnectWorkspace,
                                onVerify = viewModel::verifyWorkspace,
                                onOpenBlueprints = { page = AppPage.BLUEPRINTS },
                                onOpenSettings = { page = AppPage.CONNECTIONS },
                                onShowGuide = viewModel::showTutorial,
                                onInstallBundle = viewModel::installBundle,
                                onRemove = { showRemoveConfirmation = true },
                            )

                            AppPage.BLUEPRINTS -> BlueprintsScreen(
                                state = state,
                                onChoose = viewModel::chooseRecipe,
                                onBuild = { recipe ->
                                    viewModel.chooseRecipe(recipe)
                                    if (
                                        state.workspace(recipe.id)?.phase ==
                                        dev.tuxspan.mobile.workspace.WorkspacePhase.READY
                                    ) {
                                        viewModel.activateWorkspace(recipe)
                                        page = AppPage.WORKBENCH
                                    } else {
                                        viewModel.openBuildSheet(recipe)
                                    }
                                },
                            )

                            AppPage.DEVICE_LAB -> DeviceLabScreen(state.device)

                            AppPage.CONNECTIONS -> ConnectionsScreen(
                                state = state,
                                onDownloadTermux = {
                                    CompanionInspector.openUrl(
                                        context,
                                        CompanionInspector.TERMUX_URL,
                                    )
                                },
                                onDownloadX11 = {
                                    CompanionInspector.openUrl(
                                        context,
                                        CompanionInspector.X11_URL,
                                    )
                                },
                                onGrantPermission = {
                                    permissionLauncher.launch(CompanionInspector.RUN_COMMAND_PERMISSION)
                                },
                                onOpenTermux = viewModel::openTermux,
                                onCopyOptIn = { copyOptInCommand(context) },
                                onRemove = { showRemoveConfirmation = true },
                                onShowGuide = viewModel::showTutorial,
                                onInstallBundle = viewModel::installBundle,
                                onDisplayProfile = viewModel::setDisplayProfile,
                                onScreenOrientation = viewModel::setScreenOrientation,
                                onTouchMode = viewModel::setTouchMode,
                                onPerformancePreset = viewModel::setPerformancePreset,
                                onDownloadsBridge = viewModel::setDownloadsBridge,
                                onRequestDownloadsAccess = viewModel::requestDownloadsAccess,
                                onOpenTermuxStorageSettings = viewModel::openTermuxStorageSettings,
                                onShowControls = viewModel::showX11Controls,
                                onHideControls = viewModel::hideX11Controls,
                            )
                        }
                    }
                }
            }

            if (state.showBuildSheet) {
                BuildWorkspaceSheet(
                    state = state,
                    installScript = viewModel.currentInstallScript(),
                    onDismiss = viewModel::closeBuildSheet,
                    onTogglePreview = viewModel::toggleCommandPreview,
                    onDownloadTermux = {
                        CompanionInspector.openUrl(context, CompanionInspector.TERMUX_URL)
                    },
                    onDownloadX11 = {
                        CompanionInspector.openUrl(context, CompanionInspector.X11_URL)
                    },
                    onGrantPermission = {
                        permissionLauncher.launch(CompanionInspector.RUN_COMMAND_PERMISSION)
                    },
                    onOpenTermux = viewModel::openTermux,
                    onCopyOptIn = { copyOptInCommand(context) },
                    onCopyManualSetup = {
                        copyCommand(
                            context = context,
                            label = "TuxSpan manual setup",
                            command = viewModel.currentManualInstallCommand(),
                        )
                        Toast.makeText(
                            context,
                            "Setup command copied. Paste it in Termux and press Enter.",
                            Toast.LENGTH_LONG,
                        ).show()
                    },
                    onInstall = {
                        copyCommand(
                            context = context,
                            label = "TuxSpan setup fallback",
                            command = viewModel.currentManualInstallCommand(),
                        )
                        viewModel.dispatchInstall()
                    },
                    onVerify = viewModel::verifyWorkspace,
                )
            }

            if (showRemoveConfirmation) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showRemoveConfirmation = false },
                    title = { Text("Remove this workspace?") },
                    text = {
                        Text("The Linux root filesystem and everything inside it will be deleted by PRoot Distro. This cannot be undone.")
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                showRemoveConfirmation = false
                                viewModel.removeWorkspace()
                            },
                        ) { Text("Remove workspace") }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(
                            onClick = { showRemoveConfirmation = false },
                        ) { Text("Cancel") }
                    },
                )
            }

            if (
                state.activeWorkspace?.phase == dev.tuxspan.mobile.workspace.WorkspacePhase.READY &&
                !state.experience.tutorialCompleted
            ) {
                MobileGuideDialog(onDone = viewModel::completeTutorial)
            }
        }
    }
}

private fun copyOptInCommand(context: Context) {
    val command = "mkdir -p ~/.termux && printf '\\nallow-external-apps=true\\n' >> ~/.termux/termux.properties && termux-reload-settings"
    copyCommand(context, "TuxSpan Termux opt-in", command)
}

private fun copyCommand(context: Context, label: String, command: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText(label, command))
}
