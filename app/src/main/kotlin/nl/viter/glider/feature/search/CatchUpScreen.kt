package nl.viter.glider.feature.search

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
fun CatchUpScreen(
    repository: HnRepository,
    prefs: Preferences,
    onItem: (Long) -> Unit,
    onUser: (String) -> Unit,
    onActions: (HnItem) -> Unit
) {
    var days by rememberSaveable { mutableIntStateOf(3) }
    var custom by remember { mutableStateOf(false) }
    var start by rememberSaveable { mutableStateOf<Long?>(null) }
    var end by rememberSaveable { mutableStateOf<Long?>(null) }
    val now = remember(days, start, end) { System.currentTimeMillis() / 1000 }
    Column {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { FilterChip(days == 0, { custom = true }, label = { Text("Custom") }) }
            items(
                listOf(
                    1 to "Past day",
                    3 to "Past 3 days",
                    7 to "Past week",
                    30 to "Past month",
                    365 to "Past year"
                )
            ) { (value, label) ->
                FilterChip(
                    days == value,
                    {
                        days = value
                    },
                    label = { Text(label) },
                    leadingIcon = if (days ==
                        value
                    ) {
                        { Icon(Icons.Outlined.Check, null) }
                    } else {
                        null
                    }
                )
            }
        }
        SearchScreen(
            repository, prefs, onItem, onUser, onActions, {},
            since = if (days ==
                0
            ) {
                start
            } else {
                now - days * 86400L
            },
            until = if (days == 0) end else now, title = "Catch up", embedded = true
        )
    }
    if (custom) {
        CustomRange(onClose = { custom = false }, onSelect = { a, b ->
            start = a / 1000
            end = b / 1000 + 86399
            days = 0
            custom =
                false
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomRange(onClose: () -> Unit, onSelect: (Long, Long) -> Unit) {
    val state = rememberDateRangePickerState()
    DatePickerDialog(onDismissRequest = onClose, confirmButton = {
        TextButton(
            enabled =
                state.selectedStartDateMillis != null && state.selectedEndDateMillis != null,
            onClick = {
                onSelect(state.selectedStartDateMillis!!, state.selectedEndDateMillis!!)
            }
        ) { Text("OK") }
    }, dismissButton = {
        TextButton(onClick = onClose) { Text("Cancel") }
    }) { DateRangePicker(state, modifier = Modifier.height(480.dp)) }
}
