package nl.viter.glider.feature.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import nl.viter.glider.shared.data.*
import nl.viter.glider.shared.ui.ItemRow
import nl.viter.glider.shared.ui.LoadingFooter

@Composable
fun SearchScreen(
    repository: HnRepository,
    prefs: Preferences,
    onItem: (Long) -> Unit,
    onUser: (String) -> Unit,
    onActions: (HnItem) -> Unit,
    onBack: () -> Unit,
    tags: String = "story",
    since: Long? = null,
    until: Long? = null,
    title: String = "Search",
    embedded: Boolean = false
) {
    val model: SearchViewModel =
        viewModel(
            key = if (embedded) "catch-up" else "search:$tags",
            factory = viewModelFactory {
                initializer {
                    SearchViewModel(repository, tags, since, until, createSavedStateHandle())
                }
            }
        )
    val state by model.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(since, until) { model.range(since, until) }
    SearchContent(
        state, prefs, onItem, onUser, onActions, onBack, title, embedded, tags == "story",
        model::query, model::sort, model::toggleComments, model::loadMore, model::retry
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchContent(
    state: SearchUiState,
    prefs: Preferences,
    onItem: (Long) -> Unit,
    onUser: (String) -> Unit,
    onActions: (HnItem) -> Unit,
    onBack: () -> Unit,
    title: String,
    embedded: Boolean,
    allowComments: Boolean,
    onQuery: (String) -> Unit,
    onSort: (Boolean) -> Unit,
    onComments: () -> Unit,
    onMore: () -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(
        contentWindowInsets = if (embedded) {
            WindowInsets(
                0,
                0,
                0,
                0
            )
        } else {
            ScaffoldDefaults.contentWindowInsets
        },
        topBar = {
            if (!embedded) {
                TopAppBar(title = {
                    TextField(
                        state.request.query,
                        onQuery,
                        placeholder = {
                            Text(title)
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }, actions = {
                    IconButton(onClick = {
                        onQuery("")
                    }) { Icon(Icons.Outlined.Close, "Clear search") }
                }, navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                })
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp)) {
            if (!embedded &&
                allowComments
            ) {
                SearchControls(state.request, allowComments, onSort, onComments)
            }
            LazyColumn {
                items(state.results, key = {
                    it.id
                }, contentType = { it.type }) {
                    ItemRow(
                        it,
                        prefs,
                        onItem,
                        onUser,
                        onActions,
                        showBody =
                            !allowComments || it.type == "comment",
                        showHost = allowComments
                    )
                }
                item { LoadingFooter(state.loading, state.error, state.hasMore, onRetry, onMore) }
            }
        }
    }
}

@Composable
private fun SearchControls(
    request: SearchRequest,
    allowComments: Boolean,
    onSort: (Boolean) -> Unit,
    onComments: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(!request.byDate, { onSort(false) }, label = { Text("Relevance") })
        FilterChip(request.byDate, { onSort(true) }, label = { Text("Newest") })
        if (allowComments) {
            FilterChip(request.tags == "comment", onComments, label = {
                Text("Comments")
            })
        }
    }
}
