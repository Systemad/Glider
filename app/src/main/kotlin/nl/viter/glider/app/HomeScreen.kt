package nl.viter.glider.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import nl.viter.glider.feature.favorites.SavedScreen
import nl.viter.glider.feature.inbox.InboxScreen
import nl.viter.glider.feature.search.CatchUpScreen
import nl.viter.glider.feature.stories.FeedScreen
import nl.viter.glider.shared.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    tab: String,
    onTab: (String) -> Unit,
    prefs: Preferences,
    repository: HnRepository,
    snackbar: SnackbarHostState,
    onItem: (Long) -> Unit,
    onUser: (String) -> Unit,
    onActions: (HnItem) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onAccount: () -> Unit,
    onSubmit: () -> Unit
) {
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val destinations =
        listOf("Frontpage", "Catch up", "Favorites") +
            if (prefs.username.isNotBlank()) listOf("Inbox") else emptyList()
    val savedTabs = rememberSaveableStateHolder()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var navigationVisible by remember { mutableStateOf(true) }
    val scroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput &&
                    available.y != 0f
                ) {
                    navigationVisible = available.y > 0
                }
                return Offset.Zero
            }
        }
    }
    ModalNavigationDrawer(drawerState = drawer, gesturesEnabled = prefs.drawer, drawerContent = {
        if (prefs.drawer) {
            ModalDrawerSheet {
                Text(
                    "Glider",
                    Modifier.padding(24.dp),
                    style = MaterialTheme.typography.headlineMedium
                )
                destinations.forEach { destination ->
                    NavigationDrawerItem(
                        label = { Text(destination) },
                        selected =
                            tab == destination,
                        onClick = {
                            onTab(destination)
                            scope.launch { drawer.close() }
                        }
                    )
                }
                TextButton(onClick = {
                    scope.launch { drawer.close() }
                    onSettings()
                }) { Text("Settings") }
            }
        }
    }) {
        Scaffold(modifier = Modifier.nestedScroll(scroll), snackbarHost = {
            SnackbarHost(snackbar)
        }, topBar = {
            HomeTopBar(tab, prefs.drawer, {
                scope.launch { drawer.open() }
            }, onSearch, onSettings, onAccount)
        }, bottomBar = {
            if (!prefs.drawer && !wide &&
                navigationVisible
            ) {
                HomeNavigation(destinations, tab, onTab, false)
            }
        }, floatingActionButton = {
            if (prefs.username.isNotBlank()) {
                FloatingActionButton(onClick = onSubmit) {
                    Icon(Icons.Outlined.Add, "Submit")
                }
            }
        }) { padding ->
            Row(Modifier.padding(padding)) {
                if (wide && !prefs.drawer) HomeNavigation(destinations, tab, onTab, true)
                Box(Modifier.weight(1f)) {
                    savedTabs.SaveableStateProvider(tab) {
                        when (tab) {
                            "Frontpage" -> FeedScreen(repository, prefs, onItem, onUser, onActions)

                            "Catch up" -> CatchUpScreen(
                                repository,
                                prefs,
                                onItem,
                                onUser,
                                onActions
                            )

                            "Favorites" -> SavedScreen(repository, prefs, onItem, onUser, onActions)

                            "Inbox" -> InboxScreen(repository, prefs, onItem, onUser, onActions)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    tab: String,
    drawer: Boolean,
    onDrawer: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onAccount: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    TopAppBar(title = { Text(tab) }, navigationIcon = {
        if (drawer) IconButton(onClick = onDrawer) { Icon(Icons.Outlined.Menu, "Navigation") }
    }, actions = {
        if (tab == "Frontpage") {
            IconButton(onClick = onSearch) {
                Icon(Icons.Outlined.Search, "Search")
            }
        }
        IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "More") }
        DropdownMenu(menu, { menu = false }) {
            DropdownMenuItem(text = { Text("Settings") }, onClick = {
                menu = false
                onSettings()
            })
            DropdownMenuItem(text = { Text("Account") }, onClick = {
                menu = false
                onAccount()
            })
        }
    })
}

@Composable
private fun HomeNavigation(
    destinations: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    rail: Boolean
) {
    if (rail) {
        NavigationRail {
            destinations.forEach { destination ->
                NavigationRailItem(selected == destination, {
                    onSelect(destination)
                }, icon = { DestinationIcon(destination) }, label = { Text(destination) })
            }
        }
    } else {
        NavigationBar {
            destinations.forEach { destination ->
                NavigationBarItem(selected == destination, {
                    onSelect(destination)
                }, icon = { DestinationIcon(destination) }, label = { Text(destination) })
            }
        }
    }
}

@Composable
private fun DestinationIcon(destination: String) {
    Icon(
        when (destination) {
            "Frontpage" -> Icons.Outlined.Whatshot
            "Catch up" -> Icons.Outlined.FastRewind
            "Favorites" -> Icons.Outlined.FavoriteBorder
            else -> Icons.Outlined.Inbox
        },
        destination
    )
}
