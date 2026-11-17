package nl.viter.glider.shared.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
            OutlinedButton(
                onClick = onMore,
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape
            ) {
                Icon(Icons.Outlined.ExpandMore, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Show more")
            }
        }
    }
}
