package xyz.azraellab.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.azraellab.shared.ui.theme.AzraelSpace

/** Пункт навигации: id совпадает с id табов, которые сервер присылает в `tabConfig`. */
data class AzraelNavItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val badge: Int = 0
)

/**
 * Нижняя панель навигации телефона. Раньше она собиралась прямо в `App.kt`
 * в двух местах (BoxWithConstraints для телефона и рельс для ПК) и получала
 * разные высоты отступов, поэтому иконки «прыгали» при смене ширины окна.
 */
/**
 * @param trailing дополнительный пункт справа (на телефоне - выход из аккаунта).
 *   Он рисуется тем же фоном и той же высотой, что и разделы, иначе кнопка
 *   «выйти» выглядела бы как чужеродная плашка, приклеенная к панели.
 */
@Composable
fun AzraelNavigationBar(
    items: List<AzraelNavItem>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    barHeight: Dp = AzraelSpace.touchTarget,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .height(barHeight + AzraelSpace.md),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val selected = item.id == selectedId
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AzraelSpace.xxs),
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.small)
                    .clickable { onSelect(item.id) }
                    .padding(vertical = AzraelSpace.sm)
            ) {
                Box {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = if (selected) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        modifier = Modifier.size(22.dp)
                    )
                    if (item.badge > 0) {
                        AzraelCountBadge(
                            count = item.badge,
                            accent = accent,
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                    }
                }
                Text(
                    item.label,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (selected) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            }
        }
        if (trailing != null) trailing()
    }
}
