package xyz.azraellab.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import xyz.azraellab.shared.ui.theme.AzraelCornerSm
import xyz.azraellab.shared.ui.theme.AzraelSpace

/**
 * Цвета чипа — чистые функции, чтобы контракт 1.4.3/1.4.11 проверялся тестом по
 * тому коду, который реально рисуется (см. комментарий в [AzraelButton]).
 */

/** Рамка 3:1: у выбранного акцентом, у невыбранного `outline` (было `secondary@8%` = 1.16). */
internal fun chipBorderColor(scheme: ColorScheme, accent: Color, selected: Boolean): Color =
    if (selected) accent else scheme.outline

internal fun chipContainerColor(scheme: ColorScheme, accent: Color, selected: Boolean): Color =
    if (selected) accent.copy(alpha = 0.18f) else scheme.secondary.copy(alpha = 0.04f)

/**
 * Подпись чипа — текст, поэтому 4.5:1 (WCAG 1.4.3).
 *
 * Выбранный чип красим в `onSurface`, а не акцентом: на заливке `accent@18%`
 * светлый `primary` давал ~3.9:1. У невыбранного заливка почти совпадает с
 * подложкой, поэтому хватает `onSurfaceVariant` (4.00 в светлой, 6.40 в тёмной).
 */
internal fun chipLabelColor(scheme: ColorScheme, selected: Boolean): Color =
    if (selected) scheme.onSurface else scheme.onSurfaceVariant

/**
 * Переключатель-чип: выбранное состояние — заливка 18% и рамка акцентом,
 * невыбранное — 4% заливки и рамка `outline`. Раньше эти числа повторялись
 * в `GenderChips` и в чипах настроек отдельно, поэтому расходились.
 *
 * Границы и цвет подписи здесь несут смысл (по рамке видно, что это элемент
 * выбора, по подписи — что он выбран), поэтому WCAG 1.4.11/1.4.3 применимы
 * полностью: рамка 3:1, подпись 4.5:1. Прежние `secondary@8%` и `primary@45%`
 * давали 1.16 и 2.85, а подпись — `secondary@45%` (4.04) и `primary` поверх
 * собственной заливки (3.77 на светлой) — то есть невыбранный чип и его
 * подпись на светлой теме были практически не видны.
 *
 * Взяты `Row` + `clickable`, а не `FilterChip` из Material3: у M3-свои ripple
 * и размеры, из-за чего чипы выглядели бы иначе, чем остальной «стеклянный» UI.
 */
@Composable
fun AzraelChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(AzraelCornerSm)
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(shape)
            .background(chipContainerColor(scheme, accent, selected))
            // Выбранный чип помечен рамкой акцентом без альфы: альфа здесь съедала
            // контраст (2.85/1.75 вместо 8.15/5.18).
            .border(AzraelSpace.stroke, chipBorderColor(scheme, accent, selected), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .defaultMinSize(minHeight = AzraelSpace.controlHeight)
            .padding(horizontal = AzraelSpace.lg, vertical = AzraelSpace.md)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            // Выбранный чип: состояние уже показывают заливка и рамка, поэтому подпись
            // красится в onSurface. Акцентом подписи был текст на собственной заливке
            // акцентом — 3.77 на светлой.
            color = chipLabelColor(scheme, selected)
        )
    }
}

/** Чип-фильтр для списков — то же оформление, отдельное имя для читаемости вызовов. */
@Composable
fun AzraelFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true
) {
    AzraelChoiceChip(
        label = label,
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        accent = accent,
        enabled = enabled
    )
}
