package nl.viter.glider.app

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Light", widthDp = 393, showBackground = true)
@Preview(
    name = "Dark",
    widthDp = 393,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun CommentActionSheetPreview() {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        Surface(
            shape = BottomSheetDefaults.ExpandedShape,
            color = BottomSheetDefaults.ContainerColor
        ) {
            androidx.compose.foundation.layout.Column {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    BottomSheetDefaults.DragHandle()
                }
                ItemActionSheetContent("sample_username") {
                    ItemActionRow(Icons.Outlined.ChatBubbleOutline, {
                    }) { Text("Open comment thread") }
                    ItemActionRow(Icons.Outlined.Done, {}) { Text("Mark read") }
                    ItemActionRow(Icons.Outlined.FavoriteBorder, {}) { Text("Favorite") }
                    ItemActionRow(Icons.Outlined.TextFields, {}) { Text("Select text") }
                    ItemActionRow(Icons.Outlined.ContentCopy, {}) { Text("Copy...") }
                    ItemActionRow(Icons.Outlined.Share, {}) { Text("Share...") }
                }
            }
        }
    }
}
