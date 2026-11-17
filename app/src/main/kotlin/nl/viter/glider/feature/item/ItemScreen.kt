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
import androidx.compose.ui.graphics.luminance
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    val prefs by store.state.collectAsStateWithLifecycle()
    val model: ItemViewModel =
        viewModel(
            key = "item-$id",
            factory = viewModelFactory {
                initializer { ItemViewModel(id, repository) }
            }
        )
    val state by model.state.collectAsStateWithLifecycle()
    DiscussionContent(id, state, prefs, onBack, onUser, onActions, onItem, model::refresh)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DiscussionContent(
    id: Long,
    state: ItemState,
    prefs: Preferences,
    onBack: () -> Unit,
    onUser: (String) -> Unit,
    onActions: (HnItem) -> Unit,
    onItem: (Long) -> Unit,
    onRetry: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val preview = androidx.compose.ui.platform.LocalInspectionMode.current
    var collapsed by rememberSaveable(id) { mutableStateOf(listOf<Long>()) }
    var collapseAll by rememberSaveable(id) { mutableStateOf(false) }
    var threadMenu by remember { mutableStateOf(false) }
    LaunchedEffect(collapseAll, state.comments) {
        if (collapseAll) collapsed = state.comments.map { it.item.id }
    }
    var query by rememberSaveable(id) { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
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
            Box {
                IconButton(onClick = { threadMenu = true }) {
                    Icon(Icons.Outlined.UnfoldMore, "Comment controls")
                }
                DropdownMenu(threadMenu, onDismissRequest = { threadMenu = false }) {
                    DropdownMenuItem(text = { Text("Collapse all") }, onClick = {
                        collapseAll = true
                        collapsed = state.comments.map { it.item.id }
                        threadMenu = false
                        scope.launch { list.scrollToItem(0) }
                    })
                    DropdownMenuItem(text = { Text("Expand all") }, onClick = {
                        collapseAll = false
                        collapsed = emptyList()
                        threadMenu = false
                    })
                }
            }
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
                    Column(Modifier.padding(horizontal = 8.dp)) {
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
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                color = with(MaterialTheme.colorScheme) {
                                    if (surface.luminance() < onSurface.luminance()) {
                                        surfaceContainerHigh
                                    } else {
                                        surfaceContainer
                                    }
                                }
                            ) {
                                ItemRow(
                                    root,
                                    prefs.copy(
                                        metadata = true,
                                        visited = emptySet()
                                    ),
                                    onItem,
                                    onUser,
                                    onActions,
                                    compact = true,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    showBody = true,
                                    showHost = false,
                                    onClick = {
                                        onActions(root)
                                    }
                                )
                            }
                        }
                    }
                }
                items(visible, key = { it.item.id }, contentType = { it.item.type }) { row ->
                    Column(if (preview) Modifier else Modifier.animateItem()) {
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
                            collapseAll = false
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
                }
                item {
                    if (state.loading) {
                        Box(
                            Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            LoadingIndicator()
                        }
                    }
                    state.error?.let {
                        Text(it)
                        TextButton(onClick = onRetry) { Text("Retry") }
                    }
                }
            }
        }
    }
}
