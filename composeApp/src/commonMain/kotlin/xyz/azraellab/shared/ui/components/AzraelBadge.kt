package xyz.azraellab.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.azraellab.shared.ui.theme.AzraelCornerSm
import xyz.azraellab.shared.ui.theme.AzraelSpace

/**
 * Бейдж с текстом: роль пользователя, статус устройства, счётчик (бывшие `RoleBadge`
 * и круглые счётчики непрочитанного). Значения совпадают с текущими - 22% заливки,
 * 50% рамки, скругление 10dp.
 */
@Composable
fun AzraelBadge(
    text: String,
    accent: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    selected: Boolean = true
) {
    val fill = if (selected) accent.copy(alpha = 0.22f) else Color.Transparent
    val stroke = if (selected) accent.copy(alpha = 0.50f) else accent.copy(alpha = 0.18f)
    val content = if (selected) MaterialTheme.colorScheme.onSurface else accent.copy(alpha = 0.6f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(AzraelCornerSm))
            .background(fill)
            .border(AzraelSpace.stroke, stroke, RoundedCornerShape(AzraelCornerSm))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            color = content,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Круглый счётчик на иконке - например, непрочитанные сообщения. */
@Composable
fun AzraelCountBadge(
    count: Int,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    max: Int = 99
) {
    if (count <= 0) return
    val label = if (count > max) "$max+" else count.toString()
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(accent)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Кружок-индикатор размера [size] акцентом [accent] - статус «онлайн» и подобное. */
@Composable
fun AzraelStatusDot(
    accent: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(accent)
    )
}
