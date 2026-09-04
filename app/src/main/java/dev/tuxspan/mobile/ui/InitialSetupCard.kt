package dev.tuxspan.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tuxspan.mobile.MainUiState
import dev.tuxspan.mobile.ui.theme.Lime

@Composable
fun InitialSetupCard(
    state: MainUiState,
    onDownloadTermux: () -> Unit,
    onDownloadX11: () -> Unit,
    onGrantPermission: () -> Unit,
    onCopyOptIn: () -> Unit,
    onOpenTermux: () -> Unit,
    onVerifyConsent: () -> Unit,
    onContinue: () -> Unit,
) {
    val checkingConsent = state.activeOperation == "termux_consent"
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Eyebrow("FIRST-TIME SETUP")
            Text("Connect TuxSpan", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Complete these steps once. You will choose a Linux workspace after the connection is ready.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SetupStep(
                number = 1,
                title = "Install Termux",
                detail = if (state.companions.termuxInstalled) {
                    "Installed"
                } else {
                    "Download the recommended F-Droid build."
                },
                complete = state.companions.termuxInstalled,
            ) {
                if (!state.companions.termuxInstalled) {
                    FilledTonalButton(onClick = onDownloadTermux) {
                        Icon(Icons.Outlined.Download, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Get Termux")
                    }
                }
            }

            SetupStep(
                number = 2,
                title = "Install Termux:X11",
                detail = if (state.companions.x11Installed) {
                    "Installed for graphical desktops"
                } else {
                    "Required for Canvas and Studio. Spark can run without it."
                },
                complete = state.companions.x11Installed,
                optional = true,
            ) {
                if (!state.companions.x11Installed) {
                    FilledTonalButton(onClick = onDownloadX11) {
                        Icon(Icons.Outlined.Download, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Get Termux:X11")
                    }
                }
            }

            SetupStep(
                number = 3,
                title = "Allow Termux commands",
                detail = if (state.companions.runCommandGranted) {
                    "Permission granted"
                } else {
                    "Allows TuxSpan to send only the setup and workspace commands you approve."
                },
                complete = state.companions.runCommandGranted,
            ) {
                if (!state.companions.runCommandGranted) {
                    Button(
                        onClick = onGrantPermission,
                        enabled = state.companions.termuxInstalled,
                    ) {
                        Icon(Icons.Outlined.Security, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Grant permission")
                    }
                }
            }

            SetupStep(
                number = 4,
                title = "Enable Termux consent",
                detail = if (state.experience.termuxConsentCompleted) {
                    "Connection verified"
                } else {
                    "Copy the command, run it in Termux, then return and check the connection."
                },
                complete = state.experience.termuxConsentCompleted,
            ) {
                if (!state.experience.termuxConsentCompleted) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = onCopyOptIn,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Copy")
                            }
                            OutlinedButton(
                                onClick = onOpenTermux,
                                modifier = Modifier.weight(1f),
                                enabled = state.companions.termuxInstalled,
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Open Termux")
                            }
                        }
                        Button(
                            onClick = onVerifyConsent,
                            enabled = state.companions.runCommandGranted && !checkingConsent,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (checkingConsent) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Checking…")
                            } else {
                                Text("I ran the command — check connection")
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onContinue,
                enabled = state.coreSetupReady,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Choose a Linux workspace")
            }
        }
    }
}

@Composable
fun NoWorkspaceCard(onChooseWorkspace: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Eyebrow("READY")
            Text("Choose your Linux workspace", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Compare Canvas, Studio, and Spark, then install the one that fits this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onChooseWorkspace, modifier = Modifier.fillMaxWidth()) {
                Text("View workspaces")
            }
        }
    }
}

@Composable
private fun SetupStep(
    number: Int,
    title: String,
    detail: String,
    complete: Boolean,
    optional: Boolean = false,
    action: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                if (complete) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (complete) Lime else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Step $number${if (optional) " · Optional" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        action()
    }
}
