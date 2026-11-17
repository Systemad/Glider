package nl.viter.glider.shared.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.jsoup.Jsoup

@Composable
fun ArticleLink(url: String, showThumbnail: Boolean = false) {
    val links = LocalUriHandler.current
    if (showThumbnail && !LocalInspectionMode.current && url.startsWith("https://")) {
        val image by produceState<String?>(null, url) {
            value = null
            value = runInterruptible(Dispatchers.IO) {
                try {
                    val page = Jsoup.connect(url).timeout(4_000).maxBodySize(256 * 1024).get()
                    page.head().selectFirst("meta[property=og:image], meta[name=twitter:image]")
                        ?.absUrl("content")
                        ?.takeIf { it.startsWith("https://") }
                } catch (_: IOException) {
                    null
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
        }
        image?.let {
            SubcomposeAsyncImage(
                model = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                success = {
                    SubcomposeAsyncImageContent(
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            )
        }
    }
    OutlinedButton(
        onClick = {
            links.openUri(url)
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.secondary
        )
    ) {
        Text(
            url,
            style = MaterialTheme.typography.bodyMedium.copy(
                textDecoration = TextDecoration.Underline
            )
        )
    }
}
