package nl.viter.glider.shared.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nl.viter.glider.shared.data.*

@Composable
internal fun StoryTitle(
    item: HnItem,
    prefs: Preferences,
    host: String,
    showHost: Boolean,
    compact: Boolean
) {
    val style = if (prefs.large) {
        MaterialTheme.typography.titleMedium
    } else {
        MaterialTheme.typography.titleSmall
    }
    val lines = if (prefs.lines > 0) {
        prefs.lines
    } else if (prefs.large) {
        3
    } else {
        2
    }
    val parts = remember(item.title) { splitTitle(item.title) }
    val labels =
        listOfNotNull(
            parts.prefix?.let {
                it to MaterialTheme.colorScheme.secondaryContainer
            },
            parts.suffix?.let {
                it to
                    MaterialTheme.colorScheme.primaryContainer
            },
            parts.date?.let {
                it to
                    MaterialTheme.colorScheme.secondaryContainer
            },
            parts.batch?.let {
                it to
                    MaterialTheme.colorScheme.tertiaryContainer
            }
        )
    val measurer = rememberTextMeasurer()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val labelStyle = MaterialTheme.typography.labelSmall
    val title = buildAnnotatedString {
        parts.prefix?.let {
            appendInlineContent(it, it)
            append(" ")
        }
        append(parts.text)
        labels.filter { it.first != parts.prefix }.forEach { (label, _) ->
            append(" ")
            appendInlineContent(label, label)
        }
        if (showHost && prefs.large &&
            host.isNotBlank()
        ) {
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = MaterialTheme.typography.titleSmall.fontSize
                )
            ) {
                append(" ($host)")
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Text(
            title,
            inlineContent = labels.associate { (label, color) ->
                val labelSize = measurer.measure(label, style = labelStyle).size
                val width =
                    with(density) { (labelSize.width.toDp() + if (compact) 8.dp else 12.dp).toSp() }
                val height = with(density) { (labelSize.height.toDp() + 2.dp).toSp() }
                label to
                    InlineTextContent(
                        Placeholder(
                            width,
                            height,
                            PlaceholderVerticalAlign.Center
                        )
                    ) {
                        Surface(
                            color = color,
                            shape = CircleShape,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(label, style = labelStyle, maxLines = 1, softWrap = false)
                            }
                        }
                    }
            },
            modifier = Modifier.weight(1f),
            style = style.copy(fontWeight = FontWeight.Normal),
            maxLines = lines,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (showHost && prefs.favicons && host.isNotBlank()) {
            Icon(
                imageVector = Icons.Outlined.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private data class TitleParts(
    val text: String,
    val prefix: String?,
    val suffix: String?,
    val date: String?,
    val batch: String?
)
private fun splitTitle(title: String): TitleParts {
    var text = title
    fun extract(pattern: String): String? {
        val match = Regex(pattern).find(text) ?: return null
        text = text.removeRange(match.range)
        return match.groupValues[1]
    }
    val prefix = extract("^(\\S+\\s+(?:HN|YC|PG)|Poll):\\s+")
    val suffix = extract("\\s+\\[(\\S+)\\](?=(?:\\s|$))")?.lowercase()
    val date = extract("\\s+\\(((?:\\S+\\s+)?\\d{4})\\)$")
    val batch = extract("\\s+\\((YC\\s*[SW]\\d{2})\\)")
    return TitleParts(text, prefix, suffix, date, batch)
}
