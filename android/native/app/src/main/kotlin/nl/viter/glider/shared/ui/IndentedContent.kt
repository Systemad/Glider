package nl.viter.glider.shared.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.pow

@Composable
fun IndentedContent(depth: Int, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val indent = if (depth == 0) 0.dp else (16 * depth.toDouble().pow(0.75) - 1).dp
    Box(
        Modifier.fillMaxWidth().drawWithContent {
            drawContent()
            fun x(level: Int): Float {
                val position = (16 * level.toDouble().pow(0.75) - 1).dp.toPx()
                return if (layoutDirection ==
                    LayoutDirection.Ltr
                ) {
                    position
                } else {
                    size.width - position
                }
            }
            for (level in 1 until depth) {
                drawLine(
                    colors.outline.copy(alpha = 0.25f),
                    Offset(x(level), 0f),
                    Offset(x(level), size.height),
                    1.dp.toPx()
                )
            }
            if (depth > 0) {
                val color = when (depth % 3) {
                    0 -> colors.primary
                    1 -> colors.secondary
                    else -> colors.tertiary
                }
                drawLine(
                    color,
                    Offset(x(depth), 4.dp.toPx()),
                    Offset(x(depth), size.height - 4.dp.toPx()),
                    1.dp.toPx()
                )
            }
        }.padding(start = indent)
    ) { content() }
}
