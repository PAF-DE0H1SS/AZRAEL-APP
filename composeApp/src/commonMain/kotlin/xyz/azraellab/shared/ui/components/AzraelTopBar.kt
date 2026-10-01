package xyz.azraellab.shared.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import xyz.azraellab.shared.ui.theme.AzraelSpace

/** Действие в шапке: иконка + подпись для неё. */
data class AzraelTopBarAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val tint: Color? = null
)

/**
 * Шапка экрана: заголовок, необязательный подзаголовок, кнопка возврата и действия.
 *
 * Контейнер прозрачный - «стеклянный» фон рисует [GlassBackground] под всем экраном,
 * своя подложка у шапки ломала бы градиент.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzraelTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null,
    actions: List<AzraelTopBarAction> = emptyList()
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            if (navigationIcon != null && onNavigationClick != null) {
                IconButton(onClick = onNavigationClick) {
                    Icon(navigationIcon, contentDescription = null)
                }
            }
        },
        actions = {
            actions.forEach { action ->
                IconButton(onClick = action.onClick, enabled = action.enabled) {
                    Icon(
                        action.icon,
                        contentDescription = action.contentDescription,
                        tint = action.tint ?: MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        ),
        windowInsets = androidx.compose.foundation.layout.WindowInsets(
            top = AzraelSpace.xs,
            bottom = 0.dp
        )
    )
}
