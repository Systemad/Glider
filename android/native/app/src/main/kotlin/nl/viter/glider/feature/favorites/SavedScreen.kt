package nl.viter.glider.feature.favorites

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
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
import nl.viter.glider.shared.ui.ItemRow

@Composable
fun SavedScreen(
    repository: HnRepository,
    prefs: Preferences,
    onItem: (Long) -> Unit,
    onUser: (String) -> Unit,
    onActions: (HnItem) -> Unit
) {
    var rows by remember { mutableStateOf(emptyList<HnItem>()) }
    var error by remember { mutableStateOf(false) }
    LaunchedEffect(prefs.favorites) {
        try {
            rows = repository.items(prefs.favorites.sortedDescending())
        } catch (
            e: CancellationException
        ) {
            throw e
        } catch (e: Exception) {
            error =
                true
        }
    }
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        if (rows.isEmpty()) {
            item {
                Text(
                    if (error) {
                        "Could not load favorites"
                    } else {
                        "No favorites yet. Long-press an item to save it."
                    }
                )
            }
        }
        items(rows, key = {
            it.id
        }) { ItemRow(it, prefs, onItem, onUser, onActions, showBody = true, showHost = false) }
    }
}
