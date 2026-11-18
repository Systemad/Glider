package nl.viter.glider.shared.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

internal enum class TextBlockKind { Paragraph, Quote, Code }
internal data class HnTextBlock(val content: String, val kind: TextBlockKind)

/** Keep server HTML inline spans/links while preserving HN paragraph and quote boundaries. */
internal fun parseHnText(html: String): List<HnTextBlock> {
    val blocks = mutableListOf<HnTextBlock>()
    val inline = StringBuilder()
    fun add(content: String, kind: TextBlockKind) {
        if (content.isBlank()) return
        val last = blocks.lastOrNull()
        if (kind == TextBlockKind.Quote && last?.kind == kind) {
            blocks[blocks.lastIndex] = last.copy(content = last.content + "<p>" + content + "</p>")
        } else {
            blocks += HnTextBlock(content, kind)
        }
    }
    fun paragraph(content: String) {
        val quoted = content.trimStart().startsWith("&gt;") || content.trimStart().startsWith(">")
        val clean = if (quoted) {
            content.trimStart().replaceFirst(
                Regex("^(?:&gt;|>)\\s?"),
                ""
            )
        } else {
            content
        }
        add(clean, if (quoted) TextBlockKind.Quote else TextBlockKind.Paragraph)
    }
    fun flush() {
        paragraph(inline.toString())
        inline.clear()
    }
    val body = Jsoup.parseBodyFragment(html).body()
    body.ownerDocument()?.outputSettings()?.prettyPrint(false)
    body.select("a[href]").forEach { link ->
        if (link.text().startsWith("http") ||
            link.text().startsWith("www.")
        ) {
            link.text(link.attr("href"))
        }
    }

    body.childNodes().forEach { node ->
        if (node is Element) {
            when (node.normalName()) {
                "p" -> {
                    flush()
                    paragraph(node.html())
                }

                "blockquote" -> {
                    flush()
                    add(node.html(), TextBlockKind.Quote)
                }

                "pre" -> {
                    flush()
                    add(node.wholeText(), TextBlockKind.Code)
                }

                else -> inline.append(node.outerHtml())
            }
        } else {
            inline.append(node.outerHtml())
        }
    }
    flush()
    return blocks
}

@Composable
fun RichText(html: String, selectable: Boolean = true) {
    // A lazy comment row needs its final text height on its first measure.
    val measureWithText = !selectable || LocalInspectionMode.current
    val initial = remember(html, measureWithText) {
        if (measureWithText) parseHnText(html) else emptyList()
    }
    val blocks = if (measureWithText) {
        initial
    } else {
        val parsed by produceState(initial, html) {
            value = emptyList()
            value = withContext(Dispatchers.Default) { parseHnText(html) }
        }
        parsed
    }
    val content: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            blocks.forEach { block ->
                when (block.kind) {
                    TextBlockKind.Paragraph -> HtmlParagraph(block.content)

                    TextBlockKind.Quote -> Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                            HtmlParagraph(block.content)
                        }
                    }

                    TextBlockKind.Code -> Surface(
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                    ) {
                        Text(
                            block.content,
                            modifier = Modifier.fillMaxWidth().horizontalScroll(
                                rememberScrollState()
                            ).padding(16.dp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
    if (selectable) {
        SelectionContainer(content = content)
    } else {
        content()
    }
}

@Composable
private fun HtmlParagraph(html: String) {
    val primary = MaterialTheme.colorScheme.primary
    val text = remember(html, primary) {
        val parsed = AnnotatedString.fromHtml(
            html,
            linkStyles = TextLinkStyles(
                style = SpanStyle(color = primary, textDecoration = TextDecoration.Underline)
            )
        )
        val first = parsed.text.indexOfFirst { !it.isWhitespace() }
        if (first <
            0
        ) {
            AnnotatedString("")
        } else {
            parsed.subSequence(
                first,
                parsed.text.indexOfLast { !it.isWhitespace() } + 1
            )
        }
    }
    Text(text, style = MaterialTheme.typography.bodyMedium)
}
