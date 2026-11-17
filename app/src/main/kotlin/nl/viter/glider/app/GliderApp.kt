package nl.viter.glider.app

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import nl.viter.glider.feature.auth.AuthScreen
import nl.viter.glider.feature.favorites.SavedScreen
import nl.viter.glider.feature.inbox.InboxScreen
import nl.viter.glider.feature.item.ItemScreen
import nl.viter.glider.feature.search.CatchUpScreen
import nl.viter.glider.feature.search.SearchScreen
import nl.viter.glider.feature.settings.SettingsScreen
import nl.viter.glider.feature.stories.FeedScreen
import nl.viter.glider.feature.submit.EditorScreen
import nl.viter.glider.feature.user.UserScreen
import nl.viter.glider.shared.data.*
import nl.viter.glider.shared.ui.ItemRow

@Serializable
data class Destination(val route: String) : NavKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GliderApp(store: AppStore, repository: HnRepository, deepLink: String? = null) {
    val actions = remember { HnActions(store) }
    val prefs by store.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val routes = rememberNavBackStack(Destination("home"))
    var tab by rememberSaveable { mutableStateOf("Frontpage") }
    var valueAction by remember { mutableStateOf<Pair<HnItem, Boolean>?>(null) }
    var selectedText by remember { mutableStateOf<String?>(null) }
    var actionItem by remember { mutableStateOf<HnItem?>(null) }
    var confirmDelete by remember { mutableStateOf<HnItem?>(null) }
    var consumedLink by rememberSaveable { mutableStateOf<String?>(null) }
    fun push(route: String) {
        routes.add(Destination(route))
    }
    fun back() {
        if (routes.size > 1) routes.removeLastOrNull()
    }
    fun message(text: String) {
        scope.launch { snackbar.showSnackbar(text) }
    }
    fun item(id: Long) {
        store.update { it.copy(visited = it.visited + id) }
        push("item:$id")
    }
    fun user(name: String) {
        push("user:$name")
    }
    val route = (routes.last() as Destination).route
    BackHandler(routes.size > 1) { back() }

    LaunchedEffect(deepLink) {
        deepLink?.let { link ->
            if (consumedLink == link) return@let
            consumedLink = link
            val uri = Uri.parse(link)
            if (uri.host == "news.ycombinator.com") {
                when (uri.path) {
                    "/item" -> uri.getQueryParameter("id")?.toLongOrNull()?.let(::item)
                    "/user" -> uri.getQueryParameter("id")?.let(::user)
                    "/submit" -> push(if (prefs.username.isBlank()) "auth" else "editor:Submit")
                }
            }
        }
    }
    LaunchedEffect(prefs.username) {
        if (tab == "Inbox" && prefs.username.isBlank()) tab = "Frontpage"
    }
    val itemAction: (HnItem) -> Unit = { actionItem = it }
    CompositionLocalProvider(
        LocalUriHandler provides object : UriHandler {
            override fun openUri(uri: String) {
                val link = Uri.parse(uri)
                if (link.host == "news.ycombinator.com") {
                    when (link.path) {
                        "/item" -> link.getQueryParameter("id")?.toLongOrNull()?.let(::item)
                        "/user" -> link.getQueryParameter("id")?.let(::user)
                        else -> nl.viter.glider.shared.ui.openLink(context, uri, prefs.customTabs)
                    }
                } else {
                    nl.viter.glider.shared.ui.openLink(context, uri, prefs.customTabs)
                }
            }
        }
    ) {
        Box {
            NavDisplay(backStack = routes, onBack = { back() }, entryProvider = { key ->
                NavEntry(key) {
                    val route = (key as Destination).route
                    when {
                        route == "settings" -> SettingsScreen(store, ::back)

                        route == "auth" -> AuthScreen(store, ::back)

                        route.startsWith(
                            "item:"
                        ) -> ItemScreen(
                            route.substringAfter(':').toLong(),
                            ::back,
                            repository = repository,
                            store = store,
                            onUser = ::user,
                            onActions = itemAction,
                            onItem = ::item
                        )

                        route.startsWith(
                            "user:"
                        ) -> UserScreen(
                            route.substringAfter(
                                ':'
                            ),
                            repository, store, actions, ::back, ::item, ::user, itemAction, {
                                push("search:author_${route.substringAfter(':')}")
                            }, ::message
                        )

                        route.startsWith(
                            "search:"
                        ) -> SearchScreen(
                            repository,
                            prefs,
                            ::item,
                            ::user,
                            itemAction,
                            ::back,
                            tags = route.substringAfter(':')
                        )

                        route.startsWith("editor:") -> {
                            val parts = route.split(':')
                            EditorScreen(
                                parts[1],
                                parts.getOrNull(
                                    2
                                )?.toLongOrNull(),
                                repository,
                                store,
                                actions,
                                ::back,
                                {
                                    back()
                                    message("Sent to Hacker News")
                                }
                            )
                        }

                        else -> HomeScreen(
                            tab = tab, onTab = {
                                tab = it
                            }, prefs = prefs, repository = repository, snackbar = snackbar,
                            onItem = ::item, onUser = ::user, onActions = itemAction,
                            onSearch = { push("search:story") }, onSettings = { push("settings") },
                            onAccount = {
                                push(
                                    if (prefs.username.isBlank()) {
                                        "auth"
                                    } else {
                                        "user:${prefs.username}"
                                    }
                                )
                            },
                            onSubmit = { push("editor:Submit") }
                        )
                    }
                }
            })
            if (route != "home") SnackbarHost(snackbar, Modifier.padding(top = 80.dp))
        }
        actionItem?.let { selected ->
            ModalBottomSheet(onDismissRequest = { actionItem = null }) {
                ItemActionSheetContent(selected.title.ifBlank { selected.by }) {
                    fun runAction(kind: String) {
                        actionItem = null
                        scope.launch {
                            try {
                                actions.action(selected, kind)
                                message("Updated")
                            } catch (
                                e: CancellationException
                            ) {
                                throw e
                            } catch (e: Exception) {
                                message(
                                    e.message
                                        ?: "Could not confirm action. Refresh before retrying."
                                )
                            }
                        }
                    }
                    ItemActionRow(Icons.Outlined.ChatBubbleOutline, onClick = {
                        actionItem = null
                        item(selected.id)
                    }) {
                        Text(
                            if (selected.type ==
                                "comment"
                            ) {
                                "Open comment thread"
                            } else {
                                "Open discussion"
                            }
                        )
                    }
                    ItemActionRow(Icons.Outlined.Done, onClick = {
                        store.update {
                            it.copy(
                                visited = if (selected.id in
                                    it.visited
                                ) {
                                    it.visited - selected.id
                                } else {
                                    it.visited + selected.id
                                }
                            )
                        }
                        actionItem = null
                    }) { Text(if (selected.id in prefs.visited) "Mark unread" else "Mark read") }
                    if (selected.url.isNotBlank()) {
                        ItemActionRow(Icons.Outlined.Link, onClick = {
                            actionItem = null
                            nl.viter.glider.shared.ui.openLink(
                                context,
                                selected.url,
                                prefs.customTabs
                            )
                        }) { Text("Open link") }
                    }
                    if (!selected.deleted &&
                        selected.type != "job"
                    ) {
                        ItemActionRow(Icons.Outlined.FavoriteBorder, onClick = {
                            runAction("favorite")
                        }) {
                            Text(
                                if (selected.id in
                                    prefs.favorites
                                ) {
                                    "Unfavorite"
                                } else {
                                    "Favorite"
                                }
                            )
                        }
                    }
                    if (!selected.deleted && prefs.username.isNotBlank() &&
                        selected.by != prefs.username &&
                        selected.type != "job"
                    ) {
                        ItemActionRow(Icons.Outlined.ArrowUpward, onClick = {
                            runAction("upvote")
                        }) {
                            Text(
                                if (selected.id in
                                    prefs.upvoted
                                ) {
                                    "Undo upvote"
                                } else {
                                    "Upvote"
                                }
                            )
                        }
                        if (prefs.downvoting) {
                            ItemActionRow(Icons.Outlined.ArrowDownward, onClick = {
                                runAction("downvote")
                            }) {
                                Text(
                                    if (selected.id in
                                        prefs.downvoted
                                    ) {
                                        "Undo downvote"
                                    } else {
                                        "Downvote"
                                    }
                                )
                            }
                        }
                        ItemActionRow(Icons.Outlined.Flag, onClick = { runAction("flag") }) {
                            Text(
                                if (selected.id in
                                    prefs.flagged
                                ) {
                                    "Unflag"
                                } else {
                                    "Flag"
                                }
                            )
                        }
                    }
                    if (!selected.deleted && selected.type != "job" &&
                        prefs.username.isNotBlank()
                    ) {
                        ItemActionRow(Icons.Outlined.Reply, onClick = {
                            actionItem = null
                            push("editor:Reply:${selected.id}")
                        }) { Text("Reply") }
                    }
                    if (!selected.deleted && selected.by == prefs.username &&
                        selected.type in listOf("story", "comment") &&
                        System.currentTimeMillis() / 1000 - selected.time < 7200
                    ) {
                        ItemActionRow(Icons.Outlined.Edit, onClick = {
                            actionItem = null
                            push("editor:Edit:${selected.id}")
                        }) { Text("Edit") }
                        if (selected.kids.isEmpty()) {
                            ItemActionRow(Icons.Outlined.Delete, onClick = {
                                actionItem = null
                                confirmDelete =
                                    selected
                            }) { Text("Delete") }
                        }
                    }
                    if (selected.text.isNotBlank()) {
                        ItemActionRow(Icons.Outlined.TextFields, onClick = {
                            selectedText = selected.text
                            actionItem =
                                null
                        }) { Text("Select text") }
                    }
                    ItemActionRow(Icons.Outlined.ContentCopy, onClick = {
                        valueAction = selected to false
                        actionItem = null
                    }) { Text("Copy...") }
                    ItemActionRow(Icons.Outlined.Share, onClick = {
                        valueAction = selected to true
                        actionItem = null
                    }) { Text("Share...") }
                }
            }
        }
        confirmDelete?.let { selected ->
            AlertDialog(onDismissRequest = {
                confirmDelete = null
            }, title = { Text("Delete this item?") }, confirmButton = {
                TextButton(onClick = {
                    confirmDelete =
                        null
                    scope.launch {
                        try {
                            actions.delete(selected.id)
                            message("Deleted")
                        } catch (
                            e: CancellationException
                        ) {
                            throw e
                        } catch (e: Exception) {
                            message(e.message ?: "Delete failed")
                        }
                    }
                }) { Text("Delete") }
            }, dismissButton = {
                TextButton(onClick = {
                    confirmDelete =
                        null
                }) { Text("Cancel") }
            })
        }
    }
    valueAction?.let { (selected, share) ->
        val values = listOf(
            "Title" to selected.title,
            "Link" to selected.url,
            "Text" to AnnotatedString.fromHtml(selected.text).text,
            "Discussion link" to "https://news.ycombinator.com/item?id=${selected.id}"
        ).filter {
            it.second.isNotBlank()
        }
        AlertDialog(onDismissRequest = {
            valueAction = null
        }, title = { Text(if (share) "Share" else "Copy") }, text = {
            Column {
                values.forEach { (label, value) ->
                    TextButton(onClick = {
                        valueAction = null
                        if (share) {
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(
                                        Intent.ACTION_SEND
                                    ).setType("text/plain").putExtra(Intent.EXTRA_TEXT, value),
                                    "Share"
                                )
                            )
                        } else {
                            scope.launch {
                                clipboard.setClipEntry(
                                    ClipEntry(ClipData.newPlainText(label, value))
                                )
                            }
                        }
                    }) { Text(label) }
                }
            }
        }, confirmButton = {})
    }
    selectedText?.let { text ->
        AlertDialog(onDismissRequest = {
            selectedText = null
        }, text = {
            SelectionContainer {
                Text(AnnotatedString.fromHtml(text), Modifier.verticalScroll(rememberScrollState()))
            }
        }, confirmButton = {
            TextButton(onClick = {
                selectedText =
                    null
            }) { Text("Close") }
        })
    }
}

@Composable
internal fun ItemActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    label: @Composable () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            label()
        }
    }
}

@Composable
internal fun ItemActionSheetContent(title: String, actions: @Composable () -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Text(
            title,
            Modifier.padding(start = 36.dp, end = 8.dp, bottom = 8.dp),
            style = MaterialTheme.typography.titleMedium
        )
        HorizontalDivider(Modifier.padding(bottom = 4.dp))
        actions()
        Spacer(Modifier.height(24.dp))
    }
}
