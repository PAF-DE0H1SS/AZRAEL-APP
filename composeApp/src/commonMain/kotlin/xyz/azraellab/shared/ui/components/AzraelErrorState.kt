package xyz.azraellab.shared.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import xyz.azraellab.shared.ui.theme.AzraelSpace

/**
 * Состояние ошибки для списков и секций: та же геометрия, что [AzraelEmptyState] и
 * [AzraelLoadingState], но с приглушённым текстом ошибки и необязательной кнопкой
 * «Повторить» ([action]) — чтобы загрузка/пусто/ошибка не меняли высоту блока.
 *
 * @param actionLabel подпись кнопки повтора; если `null`, кнопка не рисуется.
 * @param onAction нажатие кнопки повтора.
 */
@Composable
fun AzraelErrorState(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AzraelSpace.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AzraelSpace.sm)
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(28.dp)
        )
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            AzraelButton(
                label = actionLabel,
                onClick = onAction,
                tone = AzraelButtonTone.Ghost,
                icon = Icons.Filled.Refresh
            )
        }
    }
}
