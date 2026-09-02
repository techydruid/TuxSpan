package dev.tuxspan.mobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.tuxspan.mobile.ui.theme.Ink
import dev.tuxspan.mobile.ui.theme.Lime
import dev.tuxspan.mobile.ui.theme.Violet

@Composable
fun TuxSpanMark(
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .background(container, RoundedCornerShape(12.dp))
            .padding(7.dp),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawTuxSpanGlyph()
        }
    }
}

private fun DrawScope.drawTuxSpanGlyph() {
    val barHeight = size.height * 0.22f
    val stemWidth = size.width * 0.22f
    drawRect(
        color = Lime,
        topLeft = Offset(size.width * 0.08f, size.height * 0.12f),
        size = androidx.compose.ui.geometry.Size(size.width * 0.76f, barHeight),
    )
    drawRect(
        color = Lime,
        topLeft = Offset(size.width * 0.35f, size.height * 0.26f),
        size = androidx.compose.ui.geometry.Size(stemWidth, size.height * 0.62f),
    )
    val arrow = Path().apply {
        moveTo(size.width * 0.58f, size.height * 0.48f)
        lineTo(size.width * 0.94f, size.height * 0.66f)
        lineTo(size.width * 0.58f, size.height * 0.84f)
        close()
    }
    drawPath(arrow, Violet)
}

@Composable
fun BrandLockup(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        TuxSpanMark()
        Spacer(Modifier.width(11.dp))
        Text(
            text = "TuxSpan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
        )
    }
}
