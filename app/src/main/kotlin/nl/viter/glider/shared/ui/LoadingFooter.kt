package nl.viter.glider.shared.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun LoadingFooter(
    loading: Boolean,
    error: String?,
    hasMore: Boolean,
    onRetry: () -> Unit,
    onMore: () -> Unit
) {
    Column {
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (error != null) {
            Text(error)
            TextButton(onClick = onRetry) { Text("Retry") }
        } else if (!loading && hasMore) {
            TextButton(onClick = onMore) { Text("Load more") }
        }
    }
}
