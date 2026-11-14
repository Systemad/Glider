package nl.viter.glider.feature.item

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlin.math.pow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import nl.viter.glider.shared.data.*
import nl.viter.glider.shared.ui.ItemRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemScreen(
    id: Long,
    onBack: () -> Unit,
    repository: HnRepository,
    store: AppStore,
    onUser: (String) -> Unit,
    onActions: (HnItem) -> Unit,
    onItem: (Long) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val prefs by store.state.collectAsStateWithLifecycle()
    val model: ItemViewModel =
        viewModel(
            key = "item-$id",
            factory = viewModelFactory {
                initializer { ItemViewModel(id, repository) }
            }
        )
    val state by model.state.collectAsStateWithLifecycle()
    var collapsed by rememberSaveable(id) { mutableStateOf(listOf<Long>()) }
    var query by rememberSaveable(id) { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var similar by remember(id) { mutableStateOf(emptyList<HnItem>()) }
    var similarError by remember(id) { mutableStateOf(false) }
    var showSimilar by rememberSaveable(id) { mutableStateOf(false) }
    LaunchedEffect(showSimilar, state.item?.url) {
        if (showSimilar) {
            state.item?.takeIf { it.url.isNotBlank() }?.let { root ->
                try {
                    similar = repository.similar(root)
                    similarError = false
                } catch (
                    e: CancellationException
                ) {
                    throw e
                } catch (e: Exception) {
                    similarError =
                        true
                }
            }
        }
    }
    val context = LocalContext.current
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val visible = remember(state.comments, collapsed, query) {
        buildList {
            var hiddenBelow = Int.MAX_VALUE
            for (row in state.comments) {
                if (row.depth > hiddenBelow) continue
                hiddenBelow = if (row.item.id in collapsed) row.depth else Int.MAX_VALUE
                if (query.isBlank() || row.item.text.contains(query, true) ||
                    row.item.by.contains(query, true)
                ) {
                    add(row)
                }
            }
        }
    }
    val descendantCounts = remember(state.comments) {
        val counts = mutableMapOf<Long, Int>()
        val ancestors = java.util.ArrayDeque<CommentRow>()
        state.comments.forEachIndexed { index, row ->
            while (ancestors.isNotEmpty() && ancestors.last().depth >= row.depth) {
                val parent = ancestors.removeLast()
                counts[parent.item.id] = index - (counts[parent.item.id] ?: index) - 1
            }
            counts[row.item.id] = index
            ancestors.addLast(row)
        }
        while (ancestors.isNotEmpty()) {
            val parent = ancestors.removeLast()
            counts[parent.item.id] =
                state.comments.size - (counts[parent.item.id] ?: state.comments.size) - 1
        }
        counts
    }
    Scaffold(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection), topBar = {
        TopAppBar(scrollBehavior = scrollBehavior, title = {
        }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
        }, actions = {
            IconButton(onClick = {
                searching =
                    !searching
            }) { Icon(Icons.Outlined.Search, "Search discussion") }
            state.item?.let { root ->
                IconButton(onClick = { onActions(root) }) { Icon(Icons.Outlined.MoreVert, "More") }
            }
        })
    }, floatingActionButton = {
        if (prefs.threadNavigation) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SmallFloatingActionButton(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = {
                        scope.launch {
                            val previous = visible.indices.lastOrNull {
                                it +
                                    1 <
                                    list.firstVisibleItemIndex &&
                                    visible[it].depth == 1
                            }
                            list.animateScrollToItem(previous?.plus(1) ?: 0)
                        }
                    }
                ) { Icon(Icons.Outlined.KeyboardArrowUp, "Previous thread") }
                SmallFloatingActionButton(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = {
                        scope.launch {
                            visible.indices.firstOrNull {
                                it + 1 >
                                    list.firstVisibleItemIndex &&
                                    visible[it].depth == 1
                            }?.let { list.animateScrollToItem(it + 1) }
                        }
                    }
                ) { Icon(Icons.Outlined.KeyboardArrowDown, "Next thread") }
            }
        }
    }) { padding ->
        Column(Modifier.padding(padding)) {
            if (searching) {
                OutlinedTextField(query, {
                    query = it
                }, label = {
                    Text("Find in discussion")
                }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            }
            LazyColumn(state = list, contentPadding = PaddingValues(bottom = 136.dp)) {
                item(key = "root", contentType = "root") {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        state.item?.let { root ->
                            root.parent?.let { parent ->
                                OutlinedButton(onClick = {
                                    onItem(parent)
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Outlined.ArrowUpward, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Load parent")
                                }
                            }
                            ItemRow(
                                root,
                                prefs.copy(
                                    metadata = true,
                                    visited = emptySet()
                                ),
                                onItem,
                                onUser,
                                onActions,
                                showBody = true,
                                showHost = false,
                                onClick = {
                                    onActions(root)
                                }
                            )
                        }
                    }
                }
                items(visible, key = { it.item.id }, contentType = { it.item.type }) { row ->
                    ThreadComment(
                        row,
                        prefs,
                        row.item.id in collapsed,
                        descendantCounts[row.item.id] ?: 0,
                        state.item?.by,
                        onItem,
                        onUser,
                        onActions
                    ) {
                        collapsed =
                            if (row.item.id in
                                collapsed
                            ) {
                                collapsed - row.item.id
                            } else {
                                collapsed + row.item.id
                            }
                        val index = visible.indexOfFirst { it.item.id == row.item.id } + 1
                        scope.launch {
                            val offset =
                                list.layoutInfo.visibleItemsInfo.firstOrNull {
                                    it.index == index
                                }?.offset ?: 0
                            if (offset < 0) list.animateScrollToItem(index)
                        }
                    }
                }
                item {
                    if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    state.error?.let {
                        Text(it)
                        TextButton(onClick = { model.refresh() }) { Text("Retry") }
                    }
                    if (state.item?.url?.isNotBlank() == true) {
                        TextButton(onClick = {
                            showSimilar = !showSimilar
                        }) { Text(if (showSimilar) "Hide similar stories" else "Similar stories") }
                        if (showSimilar) {
                            similar.forEach { ItemRow(it, prefs, onItem, onUser, onActions) }
                            if (similarError) Text("Could not load similar stories")
                        }
                    }
                }
            }
        }
    }
}
