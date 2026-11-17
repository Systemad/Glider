package nl.viter.glider.feature.item

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import nl.viter.glider.shared.data.HnItem
import nl.viter.glider.shared.data.Preferences
import nl.viter.glider.shared.ui.ItemRow

@Composable
internal fun ThreadComment(
    row: CommentRow,
    prefs: Preferences,
    collapsed: Boolean,
    descendantCount: Int,
    storyAuthor: String?,
    onItem: (Long) -> Unit,
    onUser: (String) -> Unit,
    onActions: (HnItem) -> Unit,
    onToggle: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val preview = androidx.compose.ui.platform.LocalInspectionMode.current
    val animation = if (preview) Modifier else Modifier.animateContentSize()
    Column {
        if (row.depth == 1 && row.item.type == "comment") {
            Spacer(Modifier.height(8.dp))
        }
        nl.viter.glider.shared.ui.IndentedContent(
            if (row.item.type == "pollopt") 0 else (row.depth - 1).coerceAtLeast(0),
            rainbow = prefs.rainbowThreads,
            modifier = Modifier.background(colors.surfaceContainerLow)
        ) {
            Surface(
                modifier = animation,
                color = if (row.isNew) {
                    colors.surfaceColorAtElevation(
                        4.dp
                    )
                } else {
                    colors.surfaceContainerLow
                }
            ) {
                ItemRow(
                    row.item, prefs, onItem, onUser, onActions,
                    showVisited = false,
                    compact = true,
                    collapsed = collapsed,
                    collapsedCount = descendantCount,
                    storyAuthor = storyAuthor,
                    onClick = if (row.item.deleted) ({}) else onToggle,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}
