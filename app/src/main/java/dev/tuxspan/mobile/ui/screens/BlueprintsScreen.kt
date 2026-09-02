package dev.tuxspan.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.tuxspan.mobile.MainUiState
import dev.tuxspan.mobile.model.WorkspaceCatalog
import dev.tuxspan.mobile.model.WorkspaceKind
import dev.tuxspan.mobile.model.WorkspaceRecipe
import dev.tuxspan.mobile.ui.Eyebrow
import dev.tuxspan.mobile.ui.ScreenHeader
import dev.tuxspan.mobile.ui.theme.Coral
import dev.tuxspan.mobile.ui.theme.Lime
import dev.tuxspan.mobile.ui.theme.Violet
import dev.tuxspan.mobile.workspace.WorkspacePhase

@Composable
fun BlueprintsScreen(
    state: MainUiState,
    onChoose: (WorkspaceRecipe) -> Unit,
    onBuild: (WorkspaceRecipe) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp, 20.dp, 18.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenHeader(
                eyebrow = "Workspaces",
                title = "Blueprints",
            )
        }
        WorkspaceCatalog.all.forEachIndexed { index, recipe ->
            item(key = recipe.id) {
                BlueprintCard(
                    recipe = recipe,
                    selected = state.selectedRecipe.id == recipe.id ||
                        state.activeWorkspaceId == recipe.id,
                    ready = state.workspace(recipe.id)?.phase == WorkspacePhase.READY,
                    needsRepair = state.workspace(recipe.id)?.phase == WorkspacePhase.NEEDS_REPAIR,
                    active = state.activeWorkspaceId == recipe.id,
                    accent = listOf(Lime, Violet, Coral)[index],
                    compatible = state.device.ramGb >= recipe.recommendedRamGb &&
                        state.device.freeStorageGb >= recipe.estimatedStorageGb,
                    onChoose = { onChoose(recipe) },
                    onBuild = { onBuild(recipe) },
                )
            }
        }
    }
}

@Composable
private fun BlueprintCard(
    recipe: WorkspaceRecipe,
    selected: Boolean,
    ready: Boolean,
    needsRepair: Boolean,
    active: Boolean,
    accent: Color,
    compatible: Boolean,
    onChoose: () -> Unit,
    onBuild: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = 1.dp,
        onClick = onChoose,
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = accent,
                ) {
                    Icon(
                        imageVector = if (recipe.kind == WorkspaceKind.DESKTOP) {
                            Icons.Outlined.DesktopWindows
                        } else {
                            Icons.Outlined.Terminal
                        },
                        contentDescription = null,
                        tint = Color(0xFF15131B),
                        modifier = Modifier.padding(11.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Eyebrow(recipe.eyebrow.lowercase().replaceFirstChar(Char::titlecase), accent)
                    Text(recipe.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        recipe.distroLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FitChip(
                    compatible = compatible,
                    ready = ready,
                    active = active,
                    needsRepair = needsRepair,
                )
                Spacer(Modifier.weight(1f))
                Button(onClick = onBuild) {
                    Text(
                        when {
                            needsRepair -> "Repair"
                            active -> "Open"
                            ready -> "Use"
                            else -> "Review"
                        },
                    )
                    Spacer(Modifier.width(7.dp))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun FitChip(
    compatible: Boolean,
    ready: Boolean,
    active: Boolean,
    needsRepair: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (needsRepair) {
            MaterialTheme.colorScheme.errorContainer
        } else if (compatible) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
    ) {
        Text(
            text = when {
                needsRepair -> "Repair needed"
                active -> "Active"
                ready -> "Installed"
                compatible -> "Good fit"
                else -> "Check fit"
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (needsRepair) {
                MaterialTheme.colorScheme.onErrorContainer
            } else if (compatible) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onErrorContainer
            },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
        )
    }
}
