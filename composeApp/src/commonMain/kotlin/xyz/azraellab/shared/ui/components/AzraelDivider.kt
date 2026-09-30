package xyz.azraellab.shared.ui.components

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import xyz.azraellab.shared.ui.theme.AzraelSpace

/**
 * Разделители на токенах. В `App.kt` линии рисовались вручную через `Box(Modifier.height(1.dp))`
 * или не рисовались вовсе, из-за чего сегменты на тёмном фоне местами пропадали.
 */
@Composable
fun AzraelDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = AzraelSpace.divider
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = thickness,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
fun AzraelVerticalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = AzraelSpace.divider
) {
    VerticalDivider(
        modifier = modifier,
        thickness = thickness,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
