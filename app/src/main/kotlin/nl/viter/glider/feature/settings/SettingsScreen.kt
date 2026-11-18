package nl.viter.glider.feature.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.viter.glider.shared.data.*
import nl.viter.glider.shared.ui.ItemRow
import nl.viter.glider.shared.ui.openLink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(store: AppStore, onBack: () -> Unit) {
    val prefs by store.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val version = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
    SettingsContent(prefs, version, onBack, store::update)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsContent(
    prefs: Preferences,
    version: String,
    onBack: () -> Unit,
    onUpdate: ((Preferences) -> Preferences) -> Unit
) {
    val context = LocalContext.current
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var choice by remember { mutableStateOf<String?>(null) }
    var filters by remember { mutableStateOf(false) }
    var clear by remember { mutableStateOf(false) }
    Scaffold(modifier = Modifier.nestedScroll(scroll.nestedScrollConnection), topBar = {
        MediumTopAppBar(title = {
            Text("Settings")
        }, navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
            }
        }, scrollBehavior = scroll)
    }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            item(key = "theme") {
                SettingsGroup("Theme", headingTopPadding = 12.dp) {
                    SettingRow(
                        "Theme mode",
                        prefs.theme.replaceFirstChar {
                            it.uppercase()
                        },
                        onClick = {
                            choice =
                                "Theme mode"
                        }
                    )
                    SettingSwitch("Dynamic theme", "Uses system color scheme", prefs.dynamic) { v ->
                        onUpdate { it.copy(dynamic = v) }
                    }
                    SettingRow("Theme color", enabled = !prefs.dynamic, onClick = {
                        choice = "Theme color"
                    }, trailing = {
                        Box(Modifier.size(32.dp).background(Color(prefs.seed), CircleShape))
                    })
                    SettingRow("Theme variant", prefs.variant, enabled = !prefs.dynamic, onClick = {
                        choice =
                            "Theme variant"
                    })
                    SettingSwitch(
                        "Pure background",
                        "White on light theme, black on dark theme",
                        prefs.pure
                    ) { v ->
                        onUpdate { it.copy(pure = v) }
                    }
                    SettingRow("Font", prefs.font, onClick = { choice = "Font" })
                }
            }
            item(key = "appearance") {
                SettingsGroup("Appearance") {
                    SettingSwitch(
                        "Rainbow comment threads",
                        "Use different colors for comment threads",
                        prefs.rainbowThreads
                    ) { v ->
                        onUpdate { it.copy(rainbowThreads = v) }
                    }
                    SettingSwitch("Large stories", "Shows URL and larger title", prefs.large) { v ->
                        onUpdate { it.copy(large = v) }
                    }
                    SettingRow(
                        "Story title lines",
                        if (prefs.lines <
                            0
                        ) {
                            "Variable"
                        } else {
                            prefs.lines.toString()
                        },
                        onClick = { choice = "Title lines" }
                    )
                    SettingSwitch("Story link icons", value = prefs.favicons) { v ->
                        onUpdate { it.copy(favicons = v) }
                    }
                    SettingSwitch(
                        "Article thumbnails",
                        "Load preview images from article sites when you open a discussion",
                        prefs.articleThumbnails
                    ) { enabled ->
                        onUpdate { it.copy(articleThumbnails = enabled) }
                    }
                    SettingSwitch("Story metadata", "Always shows on some pages", prefs.metadata) { v ->
                        onUpdate { it.copy(metadata = v) }
                    }
                    SettingSwitch("User avatars", value = prefs.avatars) { v ->
                        onUpdate { it.copy(avatars = v) }
                    }
                    SettingSwitch(
                        "Action buttons",
                        "Makes some metadata interactive",
                        prefs.actions
                    ) { v ->
                        onUpdate { it.copy(actions = v) }
                    }
                }
                SettingsPreview(prefs)
            }
            item(key = "behavior") {
                SettingsGroup("Behavior") {
                    SettingSwitch("Show job stories", "Also shows jobs tab", prefs.jobs) { v ->
                        onUpdate { it.copy(jobs = v) }
                    }
                    SettingSwitch(
                        "Thread navigation",
                        "Allows jumps between top-level comments",
                        prefs.threadNavigation
                    ) { v ->
                        onUpdate { it.copy(threadNavigation = v) }
                    }
                    SettingSwitch(
                        "Downvoting",
                        "Requires an eligible HN account",
                        prefs.downvoting
                    ) { v ->
                        onUpdate { it.copy(downvoting = v) }
                    }
                    SettingSwitch(
                        "Custom tabs",
                        "Opens links in in-app browser if possible",
                        prefs.customTabs
                    ) { v ->
                        onUpdate { it.copy(customTabs = v) }
                    }
                    SettingSwitch(
                        "Navigation drawer",
                        "Replaces navigation bar or rail",
                        prefs.drawer
                    ) { v ->
                        onUpdate { it.copy(drawer = v) }
                    }
                    SettingRow(
                        "Filters",
                        description = "Hides stories based on words or domains",
                        onClick = {
                            filters =
                                true
                        }
                    )
                }
            }
            item(key = "data") {
                SettingsGroup("Data") {
                    SettingRow("Export favorites", description = "Shares favorites as JSON", onClick = {
                        context.startActivity(
                            Intent.createChooser(
                                Intent(
                                    Intent.ACTION_SEND
                                ).setType(
                                    "application/json"
                                ).putExtra(
                                    Intent.EXTRA_TEXT,
                                    prefs.favorites.joinToString(",", "[", "]")
                                ),
                                "Export favorites"
                            )
                        )
                    })
                    SettingRow("Clear read stories", onClick = { clear = true })
                }
            }
            item(key = "about") {
                SettingsGroup("About") {
                    SettingsInfoRow("App version", version)
                    listOf(
                        "Privacy policy" to "blob/master/PRIVACY.md",
                        "License" to "blob/master/LICENSE",
                        "Source code" to "",
                        "Report an issue" to "issues"
                    ).forEach { (title, path) ->
                        SettingRow(
                            title,
                            description = if (title ==
                                "License"
                            ) {
                                "MIT"
                            } else if (title ==
                                "Source code"
                            ) {
                                "https://github.com/Systemad/Glider"
                            } else {
                                ""
                            },
                            onClick = {
                                openLink(
                                    context,
                                    "https://github.com/Systemad/Glider/$path",
                                    prefs.customTabs
                                )
                            },
                            trailing = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, null) }
                        )
                    }
                    SettingRow(
                        "Original app",
                        description = "Glider by Marcel Dopita (Mosc)",
                        onClick = {
                            openLink(context, "https://github.com/Mosc/Glider", prefs.customTabs)
                        },
                        trailing = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, null) }
                    )
                }
            }
        }
    }
    choice?.let { title ->
        SettingsChoice(title, prefs, onUpdate) { choice = null }
    }
    if (filters) FiltersDialog(prefs, onUpdate) { filters = false }
    if (clear) {
        AlertDialog(onDismissRequest = {
            clear = false
        }, title = { Text("Clear read stories?") }, confirmButton = {
            TextButton(onClick = {
                onUpdate { it.copy(visited = emptySet()) }
                clear =
                    false
            }) { Text("Clear") }
        }, dismissButton = { TextButton(onClick = { clear = false }) { Text("Cancel") } })
    }
}

@Composable
private fun SettingsPreview(prefs: Preferences) {
    Card(
        Modifier.padding(16.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Visibility, null, Modifier.size(18.dp))
                Text("Preview")
            }
            ItemRow(
                HnItem(
                    id = 0,
                    title = "Show HN: A fat cat on a treadmill under water [video]",
                    by = "cats4ever",
                    score = 42,
                    descendants = 7,
                    time =
                        System.currentTimeMillis() / 1000,
                    url = "https://www.youtube.com"
                ),
                prefs.copy(
                    upvoted =
                        prefs.upvoted + 0L
                ),
                {},
                {},
                {}
            )
        }
    }
}

@Composable
private fun SettingsChoice(
    title: String,
    prefs: Preferences,
    onChange: ((Preferences) -> Preferences) -> Unit,
    onClose: () -> Unit
) {
    val options = when (title) {
        "Theme mode" -> listOf("System", "Light", "Dark")

        "Font" -> listOf(
            "Crimson Pro", "EB Garamond", "Fira Sans", "IBM Plex Sans",
            "IBM Plex Serif", "Inter", "Noto Sans", "Noto Serif", "Open Sans",
            "Roboto", "Roboto Serif", "Source Sans 3", "Source Serif 4", "Monospace"
        )

        "Theme variant" -> listOf(
            "Monochrome", "Neutral", "Tonal spot", "Vibrant", "Expressive",
            "Fidelity", "Content", "Rainbow", "Fruit salad"
        )

        "Title lines" -> listOf("1", "2", "Variable")

        else -> listOf("Purple", "Blue", "Green", "Orange")
    }
    AlertDialog(onDismissRequest = onClose, title = { Text(title) }, text = {
        LazyColumn(Modifier.heightIn(max = 400.dp)) {
            items(options, key = { it }) { value ->
                SettingRow(value, onClick = {
                    onChange { p ->
                        when (title) {
                            "Theme mode" -> p.copy(theme = value.lowercase())

                            "Font" -> p.copy(font = value)

                            "Theme variant" -> p.copy(variant = value)

                            "Title lines" -> p.copy(lines = value.toIntOrNull() ?: -1)

                            else -> p.copy(
                                seed = when (value) {
                                    "Blue" -> 0xff1565c0
                                    "Green" -> 0xff2e7d32
                                    "Orange" -> 0xffe65100
                                    else -> 0xff6750a4
                                }
                            )
                        }
                    }
                    onClose()
                })
            }
        }
    }, confirmButton = { TextButton(onClick = onClose) { Text("Cancel") } })
}
