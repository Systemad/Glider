package nl.viter.glider.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LocalGroupedSettings = staticCompositionLocalOf { false }

@Composable
internal fun SettingsGroup(
    title: String,
    headingTopPadding: androidx.compose.ui.unit.Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            title,
            Modifier.padding(start = 8.dp, top = headingTopPadding, bottom = 8.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        CompositionLocalProvider(LocalGroupedSettings provides true) {
            Column(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                content = content
            )
        }
    }
}

@Composable
internal fun SettingsInfoRow(title: String, description: String) {
    ListItem(
        modifier = Modifier.clip(RoundedCornerShape(4.dp)),
        supportingContent = { Text(description) },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) { Text(title) }
}

@Composable
internal fun SettingRow(
    title: String,
    value: String = "",
    description: String = "",
    enabled: Boolean = true,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    val grouped = LocalGroupedSettings.current
    ListItem(
        modifier = Modifier.fillMaxWidth()
            .then(if (grouped) Modifier.clip(RoundedCornerShape(4.dp)) else Modifier)
            .alpha(if (enabled) 1f else 0.38f)
            .clickable(enabled = enabled, onClick = onClick),
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
        colors = ListItemDefaults.colors(
            containerColor = if (grouped) {
                MaterialTheme.colorScheme.surfaceContainer
            } else {
                Color.Transparent
            }
        )
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
