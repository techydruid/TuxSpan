package dev.tuxspan.mobile.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import dev.tuxspan.mobile.ui.theme.Lime
import dev.tuxspan.mobile.workspace.InstallProgress
import dev.tuxspan.mobile.workspace.InstallProgressStatus

@Composable
fun ScreenHeader(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    Column(modifier) {
        Text(
            text = eyebrow,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(6.dp))
        Text(text = title, style = MaterialTheme.typography.headlineLarge)
        if (!description.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.secondary,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(18.dp)) {
            Box(
                Modifier
                    .size(9.dp)
                    .background(accent, CircleShape),
            )
            Spacer(Modifier.height(16.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun ReadinessRow(
    label: String,
    detail: String,
    ready: Boolean,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                RoundedCornerShape(18.dp),
            )
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Icon(
            imageVector = if (ready) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = if (ready) Lime else MaterialTheme.colorScheme.tertiary,
        )
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(
                detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        action?.invoke()
    }
}

@Composable
fun Eyebrow(text: String, color: Color = MaterialTheme.colorScheme.primary) {
    Text(text = text, style = MaterialTheme.typography.labelMedium, color = color)
}

@Composable
fun InstallProgressPanel(
    progress: InstallProgress,
    modifier: Modifier = Modifier,
) {
    val failed = progress.status == InstallProgressStatus.FAILED
    val running = progress.status == InstallProgressStatus.RUNNING ||
        progress.status == InstallProgressStatus.UNKNOWN
    val accent = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val supportingColor = if (failed) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (failed) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        },
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                text = if (progress.step > 0) {
                    "Step ${progress.step} of ${progress.totalSteps}"
                } else {
                    "Working in Termux"
                },
                style = MaterialTheme.typography.labelMedium,
                color = accent,
            )
            Text(progress.title, style = MaterialTheme.typography.titleMedium)
            Text(
                progress.detail,
                style = MaterialTheme.typography.bodySmall,
                color = supportingColor,
            )
            Spacer(Modifier.height(3.dp))
            if (failed) {
                LinearProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = accent,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                )
                Text(
                    "Installation stopped before completion",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            } else if (running) {
                if (progress.percent != null) {
                    var displayedPercent by remember(progress.step) {
                        mutableIntStateOf(progress.percent.coerceIn(0, 99))
                    }
                    val estimateCeiling = when {
                        progress.percent < 12 -> 11
                        progress.percent < 22 -> 21
                        progress.percent < 30 -> 29
                        progress.percent < 42 -> 41
                        progress.percent < 58 -> 57
                        progress.percent < 94 -> 92
                        else -> 99
                    }
                    LaunchedEffect(progress.percent) {
                        if (progress.percent > displayedPercent) {
                            displayedPercent = progress.percent.coerceAtMost(99)
                        }
                    }
                    LaunchedEffect(progress.step, estimateCeiling) {
                        while (true) {
                            delay(15_000)
                            if (displayedPercent < estimateCeiling) {
                                displayedPercent += 1
                            }
                        }
                    }
                    val displayedProgress = animateFloatAsState(
                        targetValue = displayedPercent / 100f,
                        label = "workspaceInstallProgress",
                    )
                    LinearProgressIndicator(
                        progress = { displayedProgress.value },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = accent,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    )
                    Text(
                        "$displayedPercent% estimated · live installation continues in Termux",
                        style = MaterialTheme.typography.labelSmall,
                        color = supportingColor,
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = accent,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    )
                    Text(
                        "Still working in Termux",
                        style = MaterialTheme.typography.labelSmall,
                        color = supportingColor,
                    )
                }
            } else if (progress.percent != null) {
                LinearProgressIndicator(
                    progress = { progress.percent / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = accent,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                )
                Text(
                    "${progress.percent}% complete",
                    style = MaterialTheme.typography.labelSmall,
                    color = supportingColor,
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = accent,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                )
            }
        }
    }
}
