package nl.viter.glider.feature.user

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import nl.viter.glider.shared.data.*
import nl.viter.glider.shared.ui.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(
    name: String,
    repository: HnRepository,
    store: AppStore,
    actions: HnActions,
    onBack: () -> Unit,
    onItem: (Long) -> Unit,
    onUser: (String) -> Unit,
    onActions: (HnItem) -> Unit,
    onSearch: () -> Unit,
    onMessage: (String) -> Unit
) {
    val prefs by store.state.collectAsStateWithLifecycle()
    var user by remember(name) { mutableStateOf<HnUser?>(null) }
    var posts by remember(name) { mutableStateOf(emptyList<HnItem>()) }
    var error by remember(name) { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }
    var menuMode by remember { mutableStateOf<String?>(null) }
    var selecting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    LaunchedEffect(name) {
        try {
            user = repository.user(name)
            posts =
                repository.search(tags = "author_$name", byDate = true).items
        } catch (
            e: CancellationException
        ) {
            throw e
        } catch (e: Exception) {
            error = "Could not load user"
        }
    }
    Scaffold(topBar = {
        TopAppBar(title = {
            Text(name)
        }, navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    if (name ==
                        prefs.username
                    ) {
                        Icons.Outlined.Close
                    } else {
                        Icons.AutoMirrored.Outlined.ArrowBack
                    },
                    "Back"
                )
            }
        }, actions = {
            if (name !=
                prefs.username
            ) {
                IconButton(onClick = onSearch) { Icon(Icons.Outlined.Search, "Search user") }
            }
            IconButton(onClick = {
                menuMode = null
                menu = true
            }) { Icon(Icons.Outlined.MoreVert, "More") }
            DropdownMenu(menu, { menu = false }) {
                if (menuMode == null) {
                    if (name !=
                        prefs.username
                    ) {
                        DropdownMenuItem(text = {
                            Text(
                                if (name in
                                    prefs.blocked
                                ) {
                                    "Unblock"
                                } else {
                                    "Block"
                                }
                            )
                        }, onClick = {
                            store.update {
                                it.copy(
                                    blocked = if (name in
                                        it.blocked
                                    ) {
                                        it.blocked - name
                                    } else {
                                        it.blocked + name
                                    }
                                )
                            }
                            menu = false
                        })
                    }
                    if (user?.about?.isNotBlank() ==
                        true
                    ) {
                        DropdownMenuItem(text = { Text("Select") }, onClick = {
                            menu = false
                            selecting =
                                true
                        })
                    }
                    listOf("Copy", "Share").forEach { action ->
                        DropdownMenuItem(text = {
                            Text(action)
                        }, trailingIcon = {
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null)
                        }, onClick = {
                            menuMode =
                                action
                        })
                    }
                    if (name ==
                        prefs.username
                    ) {
                        DropdownMenuItem(text = { Text("Logout") }, onClick = {
                            store.logout()
                            menu =
                                false
                            onBack()
                        })
                    }
                } else {
                    val values = listOf(
                        "Username" to name,
                        "About" to AnnotatedString.fromHtml(user?.about.orEmpty()).text,
                        "Profile link" to "https://news.ycombinator.com/user?id=$name"
                    ).filter {
                        it.second.isNotBlank()
                    }
                    values.forEach { (label, value) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = {
                            val copy = menuMode == "Copy"
                            menu = false
                            if (copy) {
                                scope.launch {
                                    clipboard.setClipEntry(
                                        ClipEntry(ClipData.newPlainText(label, value))
                                    )
                                }
                            } else {
                                context.startActivity(
                                    Intent.createChooser(
                                        Intent(
                                            Intent.ACTION_SEND
                                        ).setType("text/plain").putExtra(Intent.EXTRA_TEXT, value),
                                        "Share profile"
                                    )
                                )
                            }
                        })
                    }
                }
            }
        })
    }, floatingActionButton = {
        if (name ==
            prefs.username
        ) {
            ExtendedFloatingActionButton(onClick = {
                scope.launch {
                    try {
                        actions.synchronize()
                        onMessage("Account synchronized")
                    } catch (
                        e: CancellationException
                    ) {
                        throw e
                    } catch (e: Exception) {
                        onMessage(e.message ?: "Synchronization failed")
                    }
                }
            }) {
                Icon(Icons.Outlined.CloudDownload, null)
                Spacer(Modifier.width(8.dp))
                Text("Synchronize")
            }
        }
    }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp)) {
            user?.let { profile ->
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MetadataValue(Icons.Outlined.ArrowUpward, profile.karma.toString())
                        MetadataValue(
                            Icons.Outlined.ChatBubbleOutline,
                            profile.submitted.size.toString()
                        )
                        Spacer(Modifier.weight(1f))
                        val created =
                            remember(profile.created) {
                                java.text.DateFormat.getDateInstance(
                                    java.text.DateFormat.LONG
                                ).format(java.util.Date(profile.created * 1000))
                            }
                        Text("since $created", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(16.dp))
                    if (profile.about.isNotBlank()) RichText(profile.about)
                    Spacer(Modifier.height(16.dp))
                }
            }
            items(posts, key = {
                it.id
            }, contentType = {
                it.type
            }) { ItemRow(it, prefs, onItem, onUser, onActions, showBody = true, showHost = false) }
            item {
                error?.let { Text(it) }
                if (user == null && error == null) LinearProgressIndicator()
            }
        }
    }
    if (selecting) {
        AlertDialog(onDismissRequest = {
            selecting = false
        }, text = {
            SelectionContainer { Text(AnnotatedString.fromHtml(user?.about.orEmpty())) }
        }, confirmButton = {
            TextButton(onClick = {
                selecting =
                    false
            }) { Text("Close") }
        })
    }
}
