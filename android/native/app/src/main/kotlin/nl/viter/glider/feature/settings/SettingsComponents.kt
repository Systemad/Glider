package nl.viter.glider.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingRow(
    title: String,
    value: String = "",
    description: String = "",
    enabled: Boolean = true,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    ListItem(
        modifier = Modifier.fillMaxWidth().alpha(
            if (enabled) 1f else 0.38f
        ).clickable(enabled = enabled, onClick = onClick),
        headlineContent = {
            Text(title)
        },
        supportingContent = if (description.isEmpty()) {
            null
        } else {
            { Text(description) }
        },
        trailingContent =
            trailing ?: if (value.isEmpty()) {
                null
            } else {
                { Text(value, style = MaterialTheme.typography.bodyMedium) }
            },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
internal fun SettingSwitch(
    title: String,
    description: String = "",
    value: Boolean,
    onChange: (Boolean) -> Unit
) {
    SettingRow(title, description = description, onClick = {
        onChange(!value)
    }, trailing = { Switch(value, onChange) })
}

@Composable
internal fun SettingsHeading(title: String, divider: Boolean = true) {
    if (divider) HorizontalDivider(Modifier.padding(vertical = 8.dp))
    Text(
        title,
        Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
