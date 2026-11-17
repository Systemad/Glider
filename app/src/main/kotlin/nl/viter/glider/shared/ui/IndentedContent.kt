package nl.viter.glider.shared.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Composable
fun IndentedContent(
    depth: Int,
    rainbow: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    fun threadColor(level: Int): Color = if (rainbow) {
        Color.hsv(((level - 1) * 55f) % 360f, if (dark) 0.55f else 0.8f, if (dark) 0.95f else 0.65f)
    } else {
        when (level % 3) {
            0 -> colors.primary
            1 -> colors.secondary
            else -> colors.tertiary
        }
    }
    val indent = (16 * depth).dp
    Box(
        modifier.fillMaxWidth().drawWithContent {
            drawContent()
            fun x(level: Int): Float {
                val position = (16 * level).dp.toPx()
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
                    if (rainbow) {
                        threadColor(
                            level
                        )
                    } else {
                        colors.outline.copy(alpha = 0.25f)
                    },
                    Offset(x(level), 0f),
                    Offset(x(level), size.height),
                    1.dp.toPx()
                )
            }
            if (depth > 0) {
                val color = threadColor(depth)
                drawLine(
                    color,
                    Offset(x(depth), if (rainbow) 0f else 4.dp.toPx()),
                    Offset(x(depth), size.height - if (rainbow) 0f else 4.dp.toPx()),
                    1.dp.toPx()
                )
            }
        }.padding(start = indent)
    ) { content() }
}
