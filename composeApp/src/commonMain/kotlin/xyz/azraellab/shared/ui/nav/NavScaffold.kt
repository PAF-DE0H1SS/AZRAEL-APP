package xyz.azraellab.shared.ui.nav

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import xyz.azraellab.shared.t
import xyz.azraellab.shared.ui.components.AzraelNavItem
import xyz.azraellab.shared.ui.components.AzraelNavigationBar
import xyz.azraellab.shared.ui.glass
import xyz.azraellab.shared.ui.theme.AzraelCornerGlass
import xyz.azraellab.shared.ui.theme.AzraelSpace

/** Порог, ниже которого показываем нижнюю панель, выше — боковую рельсу. Тот же, что был в `MainShell`. */
val NavWideBreakpoint: androidx.compose.ui.unit.Dp = 700.dp

/**
 * Каркас приложения: либо нижняя панель, либо боковая рельса.
 *
 * Раньше обе ветки собирались прямо в `App.kt` из `NavigationBar`/`NavigationRail`
 * Material3, из-за чего панель имела другую высоту и цвет, чем остальное «стекло»,
 * а иконки «прыгали» при смене ширины окна. Здесь обе ветки используют один
 * [AzraelNavigationBar] и `Modifier.glass`.
 *
 * @param onBack вызывается кнопкой «назад» в рельсе; `null` — кнопки нет.
 * @param content экран текущего пункта; каркас не влияет на его содержимое.
 */
@Composable
fun AzraelNavScaffold(
    items: List<AzraelNavItem>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onLogout: () -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wide = maxWidth >= NavWideBreakpoint
        if (wide) {
            Row(Modifier.fillMaxSize()) {
                AzraelSideRail(
                    items = items,
                    selectedId = selectedId,
                    onSelect = onSelect,
                    onLogout = onLogout,
                    onBack = onBack,
                    modifier = Modifier.fillMaxHeight()
                )
                content(Modifier.fillMaxSize())
            }
        } else {
            Scaffold(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                bottomBar = {
                    AzraelNavigationBar(
                        items = items,
                        selectedId = selectedId,
                        onSelect = onSelect,
                        modifier = Modifier.glass(
                            corner = AzraelCornerGlass,
                            fill = MaterialTheme.colorScheme.surface
                        ),
                        // На телефоне рельсы нет, а раньше выход из аккаунта жил
                        // только в ней — на узком экране выйти было нечем.
                        trailing = {
                            IconButton(onClick = onLogout) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = t["action.logout"],
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) { content(Modifier.fillMaxSize()) }
            }
        }
    }
}

/**
 * Рельса для широких экранов. Порядок пунктов как в старом `SideRail`:
 * логотип → (назад) → разделы → выход, чтобы ПК-вид не поехал после перехода.
 */
@Composable
private fun AzraelSideRail(
    items: List<AzraelNavItem>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onLogout: () -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(AzraelSpace.md)
            .glass(corner = AzraelCornerGlass)
            .padding(vertical = AzraelSpace.md, horizontal = AzraelSpace.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            t["app.name"],
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = t["action.back"],
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        items.forEach { item ->
            val selected = item.id == selectedId
            val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AzraelSpace.xxs),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AzraelCornerGlass))
                    .clickable { onSelect(item.id) }
                    .padding(vertical = AzraelSpace.sm)
            ) {
                Icon(item.icon, contentDescription = item.label, tint = tint, modifier = Modifier.size(22.dp))
                Text(item.label, style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1)
            }
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onLogout) {
            Icon(
                Icons.AutoMirrored.Filled.Logout,
                contentDescription = t["action.logout"],
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Адаптивное «список + детали»: на широком экране две колонки, на узком — одна.
 *
 * [detail] nullable не для красоты: пока подэкран не открыт, на телефоне показывать
 * нечего, и пустой экран вместо списка выглядел бы как «приложение зависло».
 * `null` → узкий экран показывает список, широкий — список и [empty].
 */
@Composable
fun AzraelDetailLayout(
    wide: Boolean,
    list: @Composable (Modifier) -> Unit,
    detail: (@Composable (Modifier) -> Unit)?,
    modifier: Modifier = Modifier,
    empty: @Composable (Modifier) -> Unit = { }
) {
    if (wide) {
        Row(modifier.fillMaxSize()) {
            list(Modifier.fillMaxHeight().fillMaxWidth(0.38f))
            if (detail == null) empty(Modifier.fillMaxHeight().fillMaxWidth(0.62f)) else detail(Modifier.fillMaxHeight().fillMaxWidth(0.62f))
        }
    } else {
        Box(modifier.fillMaxSize()) {
            if (detail == null) list(Modifier.fillMaxSize()) else detail(Modifier.fillMaxSize())
        }
    }
}

/** Проверка ширины без передачи `Dp` в вызывающий код: удобно для `remember`. */
fun isWideLayout(width: androidx.compose.ui.unit.Dp): Boolean = width >= NavWideBreakpoint

@Composable
internal fun NavRailSpacer() = Spacer(Modifier.height(AzraelSpace.md))
