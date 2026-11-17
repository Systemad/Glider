package nl.viter.glider.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.viter.glider.shared.data.Preferences

@Composable
internal fun FiltersDialog(
    prefs: Preferences,
    onChange: ((Preferences) -> Preferences) -> Unit,
    onClose: () -> Unit
) {
    var word by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onClose, title = { Text("Filters") }, text = {
        Column(
            Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            FilterField("Words", "foobar", word, { word = it }, prefs.words, { value ->
                onChange {
                    it.copy(
                        words =
                            it.words + value
                    )
                }
                word = ""
            }, { value -> onChange { it.copy(words = it.words - value) } })
            FilterField("Domains", "example.com", domain, { domain = it }, prefs.domains, { value ->
                onChange {
                    it.copy(
                        domains =
                            it.domains + value
                    )
                }
                domain = ""
            }, { value -> onChange { it.copy(domains = it.domains - value) } })
        }
    }, confirmButton = { TextButton(onClick = onClose) { Text("OK") } })
}

@Composable
private fun FilterField(
    title: String,
    hint: String,
    value: String,
    onValue: (String) -> Unit,
    entries: Set<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextField(value, onValue, placeholder = {
                Text(hint)
            }, singleLine = true, modifier = Modifier.weight(1f))
            FilledIconButton(onClick = {
                if (value.isNotBlank()) onAdd(value.trim().lowercase())
            }) { Icon(Icons.Outlined.Add, "Add $title filter") }
        }
        entries.forEach { entry ->
            InputChip(selected = false, onClick = {
            }, label = {
                Text(entry)
            }, trailingIcon = {
                IconButton(onClick = {
                    onRemove(entry)
                }) { Icon(Icons.Outlined.Close, "Remove $entry") }
            })
        }
    }
}
