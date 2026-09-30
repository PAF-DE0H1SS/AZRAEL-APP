package xyz.azraellab.shared.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.azraellab.shared.ui.theme.AzraelSpace

/**
 * Строка списка: ведущий слот, заголовок, подпись и хвостовой слот.
 *
 * Собственная реализация вместо `ListItem` из Material3: у M3 свой фон, отступы и
 * минимальная высота, из-за чего строка выбивалась бы из «стеклянных» карточек.
 */
@Composable
fun AzraelListTile(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    titleColor: Color? = null,
    minHeight: Dp = AzraelSpace.touchTarget
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
            .defaultMinSize(minHeight = minHeight)
            .padding(horizontal = AzraelSpace.lg, vertical = AzraelSpace.md)
    ) {
        if (leading != null) {
            leading()
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = if (leading != null) AzraelSpace.md else 0.dp),
            verticalArrangement = Arrangement.spacedBy(AzraelSpace.xxs)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = titleColor ?: MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Box(
                modifier = Modifier.padding(start = AzraelSpace.md),
                contentAlignment = Alignment.Center
            ) { trailing() }
        }
    }
}

/** Строка списка с одним заголовком во всю ширину — для групп настроек. */
@Composable
fun AzraelListTileContent(
    title: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
            .padding(horizontal = AzraelSpace.lg, vertical = AzraelSpace.md)
    ) {
        Text(title, style = MaterialTheme.typography.labelMedium)
        content()
    }
}
