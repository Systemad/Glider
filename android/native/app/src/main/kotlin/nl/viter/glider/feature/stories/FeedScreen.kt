package nl.viter.glider.feature.stories

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import nl.viter.glider.shared.data.*
import nl.viter.glider.shared.ui.ItemRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    repository: HnRepository,
    prefs: Preferences,
    onItem: (Long) -> Unit,
    onUser: (String) -> Unit,
    onActions: (HnItem) -> Unit
) {
    val model: FeedViewModel =
        viewModel(factory = viewModelFactory { initializer { FeedViewModel(repository) } })
    val state by model.state.collectAsStateWithLifecycle()
    FeedContent(state, prefs, onItem, onUser, onActions, model::select, {
        model.load(true)
    }, { model.load() }, { model.load(state.rows.isEmpty()) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FeedContent(
    state: FeedState,
    prefs: Preferences,
    onItem: (Long) -> Unit,
    onUser: (String) -> Unit,
    onActions: (HnItem) -> Unit,
    onKind: (String) -> Unit,
    onRefresh: () -> Unit,
    onMore: () -> Unit,
    onRetry: () -> Unit
) {
    Column {
        FeedTabs(state.kind, prefs.jobs, onKind)
        PullToRefreshBox(
            isRefreshing = state.loading && state.rows.isNotEmpty(),
            onRefresh = onRefresh
        ) {
            LazyColumn(contentPadding = PaddingValues(16.dp), modifier = Modifier.fillMaxSize()) {
                items(
                    state.rows.distinctBy {
                        it.id
                    }.filter { prefs.jobs || it.type != "job" },
                    key = { it.id },
                    contentType = { it.type }
                ) {
                    ItemRow(it, prefs, onItem, onUser, onActions)
                }
                item {
                    nl.viter.glider.shared.ui.LoadingFooter(
                        state.loading,
                        state.error,
                        state.more && state.rows.isNotEmpty(),
                        onRetry,
                        onMore
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedTabs(selected: String, jobs: Boolean, onSelect: (String) -> Unit) {
    val tabs =
        listOf("top" to "Top", "new" to "New", "best" to "Best", "ask" to "Ask", "show" to "Show") +
            if (jobs) listOf("job" to "Jobs") else emptyList()
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tabs, key = { it.first }, contentType = { "feed-chip" }) { (kind, title) ->
            FilterChip(selected == kind, {
                onSelect(kind)
            }, label = { Text(title) }, leadingIcon = {
                Icon(
                    when (kind) {
                        "top" -> Icons.Outlined.Whatshot
                        "new" -> Icons.Outlined.NewReleases
                        "best" -> Icons.Outlined.Recommend
                        "ask" -> Icons.AutoMirrored.Outlined.HelpOutline
                        "show" -> Icons.Outlined.PlayCircleOutline
                        else -> Icons.Outlined.MonetizationOn
                    },
                    null,
                    Modifier.size(18.dp)
                )
            })
        }
    }
}
