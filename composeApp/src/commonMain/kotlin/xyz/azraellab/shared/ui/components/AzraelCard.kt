package xyz.azraellab.shared.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.azraellab.shared.ui.glass
import xyz.azraellab.shared.ui.theme.AzraelCornerGlass

/**
 * Стеклянная карточка - бывшие `GlassCard` и `SurfaceGlass` в `App.kt`, но в одном API.
 * Скругление, заливка и рамка берутся из токенов, а не из литералов на экране.
 *
 * @param onClick если задан, карточка становится кликабельной в пределах своей области.
 */
@Composable
fun AzraelCard(
    modifier: Modifier = Modifier,
    corner: Dp = AzraelCornerGlass,
    padding: Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .glass(corner = corner)
            .padding(padding)
    ) {
        content()
    }
}
