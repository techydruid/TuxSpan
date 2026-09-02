package dev.tuxspan.mobile.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.SwipeUp
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private data class GuidePage(
    val icon: ImageVector,
    val eyebrow: String,
    val title: String,
    val body: String,
)

private val guidePages = listOf(
    GuidePage(
        icon = Icons.Outlined.TouchApp,
        eyebrow = "1 of 3 · Pointer",
        title = "Readable and touchpad-ready",
        body = "TuxSpan starts with larger desktop text, icons, window titles, and pointer. The launcher dock stays visible just above the bottom edge; move the pointer to the top edge for the hidden Applications panel. Without a mouse, drag one finger to move the pointer.",
    ),
    GuidePage(
        icon = Icons.Outlined.Keyboard,
        eyebrow = "2 of 3 · Keyboard",
        title = "Back toggles the keyboard",
        body = "Tap Android Back—or use its one-finger edge gesture—to show or hide the software keyboard. When an external alphabetic keyboard connects, Termux:X11 automatically keeps the software keyboard out of the way.",
    ),
    GuidePage(
        icon = Icons.Outlined.SwipeUp,
        eyebrow = "3 of 3 · More space",
        title = "Controls stay out of the way",
        body = "TuxSpan starts fullscreen with the extra-key bar hidden. To temporarily show Ctrl, Alt, Esc, or arrow keys, use Settings → Input → Show desktop controls. This avoids gestures that some phones reserve for screenshots.",
    ),
)

@Composable
fun MobileGuideDialog(onDone: () -> Unit) {
    var pageIndex by remember { mutableIntStateOf(0) }
    val page = guidePages[pageIndex]
    AlertDialog(
        onDismissRequest = onDone,
        icon = { Icon(page.icon, contentDescription = null) },
        title = {
            Column {
                Eyebrow(page.eyebrow)
                Spacer(Modifier.height(8.dp))
                Text(page.title)
            }
        },
        text = {
            Text(
                page.body,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (pageIndex == guidePages.lastIndex) onDone() else pageIndex++
                },
            ) {
                Text(if (pageIndex == guidePages.lastIndex) "Start using Linux" else "Next")
            }
        },
        dismissButton = {
            TextButton(onClick = onDone) { Text("Skip") }
        },
    )
}
