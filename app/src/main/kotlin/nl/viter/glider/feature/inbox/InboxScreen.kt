package nl.viter.glider.feature.inbox

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import nl.viter.glider.feature.auth.AuthScreen
import nl.viter.glider.feature.item.ItemScreen
import nl.viter.glider.feature.search.SearchScreen
import nl.viter.glider.feature.settings.SettingsScreen
import nl.viter.glider.feature.submit.EditorScreen
import nl.viter.glider.feature.user.UserScreen
import nl.viter.glider.shared.data.*
import nl.viter.glider.shared.ui.IndentedContent
import nl.viter.glider.shared.ui.ItemRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    repository: HnRepository,
    prefs: Preferences,
    onItem: (Long) -> Unit,
    onUser: (String) -> Unit,
    onActions: (HnItem) -> Unit
) {
    var refresh by remember { mutableIntStateOf(0) }
    var rows by remember { mutableStateOf(emptyList<Pair<HnItem, HnItem>>()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    LaunchedEffect(prefs.username, refresh) {
        loading = true
        error = false
        try {
            rows = repository.replies(prefs.username)
        } catch (
            e: CancellationException
        ) {
            throw e
        } catch (e: Exception) {
            error =
                true
        } finally {
            loading = false
        }
    }
    PullToRefreshBox(isRefreshing = loading && rows.isNotEmpty(), onRefresh = { refresh++ }) {
        LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
            if (loading) item { LinearProgressIndicator() }
            if (rows.isEmpty() &&
                !loading
            ) {
                item { Text(if (error) "Could not load replies" else "No replies found") }
            }
            items(rows, key = { it.second.id }, contentType = { "reply-pair" }) { (parent, reply) ->
                Column {
                    ItemRow(
                        parent,
                        prefs,
                        onItem,
                        onUser,
                        onActions,
                        showBody = true,
                        showHost = false,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    IndentedContent(1, rainbow = prefs.rainbowThreads) {
                        ItemRow(
                            reply,
                            prefs,
                            onItem,
                            onUser,
                            onActions,
                            showBody = true,
                            showHost = false,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}
