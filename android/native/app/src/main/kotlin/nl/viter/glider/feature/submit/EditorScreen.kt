package nl.viter.glider.feature.submit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import nl.viter.glider.shared.data.*
import nl.viter.glider.shared.ui.RichText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    mode: String,
    id: Long?,
    repository: HnRepository,
    store: AppStore,
    actions: HnActions,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val key = "$mode-${id ?: 0}"
    var title by rememberSaveable(key) { mutableStateOf(store.draft("$key-title")) }
    var url by rememberSaveable(key) { mutableStateOf(store.draft("$key-url")) }
    var text by rememberSaveable(key) { mutableStateOf(store.draft("$key-text")) }
    var preview by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(mode, id) {
        if (mode == "Edit" && id != null && title.isEmpty() && text.isEmpty()) {
            try {
                val original = actions.editDraft(id)
                title = original.first
                text = original.second
            } catch (
                e: CancellationException
            ) {
                throw e
            } catch (e: Exception) {
                error =
                    "Could not load original"
            }
        }
    }
    LaunchedEffect(title, url, text) {
        store.draft("$key-title", title)
        store.draft("$key-url", url)
        store.draft("$key-text", text)
    }
    val valid = if (mode ==
        "Submit"
    ) {
        title.isNotBlank() && title.length <= 80 && (url.isNotBlank() xor text.isNotBlank())
    } else if (mode ==
        "Edit"
    ) {
        title.length <= 80 && (title.isNotBlank() || text.isNotBlank())
    } else {
        text.isNotBlank()
    }
    Scaffold(topBar = {
        MediumTopAppBar(title = {
            Text(mode)
        }, navigationIcon = { TextButton(onClick = onBack) { Text("Close") } }, actions = {
            TextButton(onClick = {
                preview =
                    !preview
            }) { Text(if (preview) "Edit" else "Preview") }
        })
    }, floatingActionButton = {
        if (valid && !busy) {
            ExtendedFloatingActionButton(onClick = {
                scope.launch {
                    busy = true
                    error = null
                    try {
                        actions.write(mode, id, title, url, text)
                        store.draft("$key-title", "")
                        store.draft("$key-url", "")
                        store.draft("$key-text", "")
                        onDone()
                    } catch (
                        e: CancellationException
                    ) {
                        throw e
                    } catch (e: Exception) {
                        error =
                            e.message ?: "Could not confirm submission. Refresh before retrying."
                    } finally {
                        busy =
                            false
                    }
                }
            }) { Text(mode) }
        }
    }) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (mode !=
                "Reply"
            ) {
                OutlinedTextField(
                    title,
                    {
                        title = it
                    },
                    label = {
                        Text("Title")
                    },
                    supportingText = { Text("${title.length}/80") },
                    isError =
                        title.length > 80,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (mode == "Submit") {
                OutlinedTextField(url, {
                    url = it
                }, label = { Text("Link") }, modifier = Modifier.fillMaxWidth())
                if (url.isNotBlank()) {
                    TextButton(onClick = {
                        scope.launch {
                            try {
                                title = repository.pageTitle(url)
                            } catch (
                                e: CancellationException
                            ) {
                                throw e
                            } catch (e: Exception) {
                                error =
                                    "Could not autofill title"
                            }
                        }
                    }) { Text("Autofill title") }
                }
            }
            if (preview) {
                RichText(text)
            } else {
                OutlinedTextField(text, {
                    text = it
                }, label = { Text("Text") }, minLines = 6, modifier = Modifier.fillMaxWidth())
            }
            if (mode ==
                "Submit"
            ) {
                Text("Use either a link or text.", style = MaterialTheme.typography.bodySmall)
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(80.dp))
        }
    }
}
