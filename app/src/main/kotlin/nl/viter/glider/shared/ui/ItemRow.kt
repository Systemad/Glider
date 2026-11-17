package nl.viter.glider.shared.ui

import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import nl.viter.glider.shared.data.HnItem
import nl.viter.glider.shared.data.Preferences

@Composable
fun Author(
    name: String,
    prefs: Preferences,
    onUser: (String) -> Unit,
    storyAuthor: String? = null
) {
    Surface(
        modifier = Modifier.clickable { onUser(name) },
        shape = CircleShape,
        color = if (name == prefs.username || name == storyAuthor) {
            if (name ==
                prefs.username
            ) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        contentColor = if (name == prefs.username || name == storyAuthor) {
            if (name ==
                prefs.username
            ) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            }
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (prefs.avatars) Avatar(name)
            Text(
                name,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun Avatar(name: String) {
    Canvas(Modifier.size(16.dp)) {
        val seed = name.hashCode()
        val color = Color(0xff000000L or (seed.toLong() and 0xffffff))
        val cell = size.width / 5
        for (y in 0..4) {
            for (x in 0..2) {
                if ((seed ushr (y * 3 + x)) and 1 == 1) {
                    drawRect(color, Offset(x * cell, y * cell), Size(cell, cell))
                    drawRect(color, Offset((4 - x) * cell, y * cell), Size(cell, cell))
                }
            }
        }
    }
}

@Composable
fun ItemRow(
    item: HnItem,
    prefs: Preferences,
    onItem: (Long) -> Unit,
    onUser: (String) -> Unit,
    onActions: (HnItem) -> Unit,
    collapsed: Boolean = false,
    onClick: (() -> Unit)? = null,
    showBody: Boolean = item.type == "comment",
    showHost: Boolean = true,
    showVisited: Boolean = true,
    collapsedCount: Int = 0,
    storyAuthor: String? = null,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val host = remember(item.url) { Uri.parse(item.url).host.orEmpty() }
    val hidden = remember(item.title, item.by, host, prefs.blocked, prefs.words, prefs.domains) {
        fun matches(value: String, filters: Set<String>) = filters.any {
            Regex("\\b${Regex.escape(it)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(value)
        }
        item.by in prefs.blocked || matches(item.title, prefs.words) || matches(host, prefs.domains)
    }
    Column(
        modifier.fillMaxWidth().alpha(
            if (showVisited &&
                item.id in prefs.visited
            ) {
                2f / 3
            } else {
                1f
            }
        ).combinedClickable(onClick = {
            onClick?.invoke() ?: onItem(item.id)
        }, onLongClick = {
            onActions(item)
        }).padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!hidden && item.title.isNotBlank()) StoryTitle(item, prefs, host, showHost)
        ItemMetadata(item, prefs, collapsed, collapsedCount, storyAuthor, onUser, {
            onActions(item)
        })
        when {
            hidden -> Text(
                if (item.by in
                    prefs.blocked
                ) {
                    "[blocked]"
                } else {
                    "[filtered]"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            item.deleted -> Text("[deleted]", style = MaterialTheme.typography.bodySmall)

            showBody && !collapsed && item.text.isNotBlank() -> RichText(item.text)
        }
        if (showBody && !showHost && !hidden && !collapsed &&
            item.url.isNotBlank()
        ) {
            ArticleLink(item.url)
        }
    }
}

@Composable
private fun ItemMetadata(
    item: HnItem,
    prefs: Preferences,
    collapsed: Boolean,
    collapsedCount: Int,
    storyAuthor: String?,
    onUser: (String) -> Unit,
    onActions: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (collapsed) {
            MetadataValue(
                Icons.Outlined.AddCircleOutline,
                if (collapsedCount >
                    0
                ) {
                    collapsedCount.toString()
                } else {
                    ""
                }
            )
        }
        if (item.id in
            prefs.favorites
        ) {
            Icon(
                Icons.Outlined.FavoriteBorder,
                "Favorite",
                Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        if (item.type != "comment" && item.type != "job" && prefs.metadata) {
            val actionModifier = if (prefs.actions) {
                Modifier.clickable(
                    onClick = onActions
                )
            } else {
                Modifier
            }
            MetadataValue(
                Icons.Outlined.ArrowUpward,
                item.score.toString(),
                actionModifier,
                if (item.id in
                    prefs.upvoted
                ) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            MetadataValue(
                Icons.Outlined.ChatBubbleOutline,
                item.descendants.toString(),
                actionModifier
            )
        }
        Box(Modifier.weight(1f)) {
            if (item.by.isNotBlank()) Author(item.by, prefs, onUser, storyAuthor)
        }
        if (item.by ==
            "cats4ever"
        ) {
            Badge(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                Text("purrfect")
            }
        }
        Text(
            DateUtils.getRelativeTimeSpanString(
                item.time * 1000,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            ).toString(),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MetadataValue(
    icon: ImageVector,
    value: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, Modifier.size(16.dp), tint = color)
        if (value.isNotEmpty()) {
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
    }
}
