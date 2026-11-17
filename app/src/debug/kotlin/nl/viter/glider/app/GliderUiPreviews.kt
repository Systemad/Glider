package nl.viter.glider.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import nl.viter.glider.feature.item.CommentRow
import nl.viter.glider.feature.item.DiscussionContent
import nl.viter.glider.feature.item.ItemState
import nl.viter.glider.feature.settings.SettingSwitch
import nl.viter.glider.feature.stories.FeedContent
import nl.viter.glider.feature.stories.FeedState
import nl.viter.glider.shared.data.HnItem
import nl.viter.glider.shared.data.Preferences

@Preview(name = "Front page", widthDp = 393, heightDp = 480)
@Composable
private fun FrontPagePreview() {
    PreviewSurface {
        val story = HnItem(
            id = 1,
            title = "Show HN: A small tool for reading long discussions offline",
            url = "https://example.com",
            by = "sample_author",
            score = 142,
            descendants = 32,
            time = System.currentTimeMillis() / 1000
        )
        FeedContent(
            FeedState(
                rows = listOf(story, story.copy(id = 2, title = "Web Browser Engineering (2021)")),
                more = true
            ),
            Preferences(),
            {}, {}, {}, {}, {}, {}, {}
        )
    }
}

@Preview(name = "Discussion · Light", widthDp = 393, heightDp = 852, showSystemUi = true)
@Preview(
    name = "Discussion · Dark",
    widthDp = 393,
    heightDp = 852,
    showSystemUi = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun DiscussionPreview() {
    PreviewSurface {
        val now = System.currentTimeMillis() / 1000
        val story = HnItem(
            id = 1,
            title = "Show HN: An offline reader for long discussions",
            by = "map_reader",
            score = 142,
            descendants = 5,
            time = now - 7200,
            text = "I built a small reader that keeps your place when you return to a thread."
        )
        val replies = listOf(
            CommentRow(
                HnItem(
                    id = 2,
                    type = "comment",
                    by = "fieldnotes",
                    time = now - 3600,
                    text = "I miss saved scroll positions in other readers."
                ),
                depth = 1
            ),
            CommentRow(
                HnItem(
                    id = 3,
                    type = "comment",
                    by = story.by,
                    time = now - 3000,
                    text = "It remembers which branches you collapsed, too."
                ),
                depth = 2
            ),
            CommentRow(
                HnItem(
                    id = 4,
                    type = "comment",
                    by = "fieldnotes",
                    time = now - 2400,
                    text = "That makes a big difference on larger discussions."
                ),
                depth = 3
            ),
            CommentRow(
                HnItem(
                    id = 5,
                    type = "comment",
                    by = "papertrail",
                    time = now - 1800,
                    text = "Does it keep a copy of the discussion for reading on the train?"
                ),
                depth = 1
            ),
            CommentRow(
                HnItem(
                    id = 6,
                    type = "comment",
                    by = "quietbyte",
                    time = now - 900,
                    text = "I would also like that for flights."
                ),
                depth = 2
            )
        )
        DiscussionContent(
            id = story.id,
            state = ItemState(item = story, comments = replies, loading = false),
            prefs = Preferences(rainbowThreads = true, threadNavigation = true),
            onBack = {},
            onUser = {},
            onActions = {},
            onItem = {},
            onRetry = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun AppearanceSettingsPreview() {
    PreviewSurface {
        Column {
            SettingSwitch("Dynamic color", value = true, onChange = {})
            SettingSwitch(
                "Rainbow comment threads",
                description = "Use different colors for comment threads",
                value = false,
                onChange = {}
            )
        }
    }
}

@Composable
private fun PreviewSurface(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    ) {
        Surface(content = content)
    }
}
