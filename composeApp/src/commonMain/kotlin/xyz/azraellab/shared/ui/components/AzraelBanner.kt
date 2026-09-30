package xyz.azraellab.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import xyz.azraellab.shared.ui.theme.AzraelCornerSm
import xyz.azraellab.shared.ui.theme.AzraelSpace

/**
 * Семантика баннера: иконка и тон в одной паре, чтобы не забыть иконку при новом тоне.
 *
 * Цвет раньше лежал в конструкторе enum'а — это работало, пока палитра была одна.
 * Со светлой темой токен перестал быть «просто зелёным/красным», и жёстко зашитый
 * `Color` в enum'е физически не может прочитать текущую `ColorScheme`. Поэтому
 * цвет вынесен в [accent], который получает схему явно, а [AzraelBanner] зовёт
 * composable-обёртку: токен и иконка по-прежнему ходят парой, но следуют за темой.
 */
enum class AzraelBannerTone(val icon: ImageVector) {
    Info(Icons.Filled.Info),
    Success(Icons.Filled.CheckCircle),
    Warning(Icons.Filled.Warning),
    Error(Icons.Filled.Warning);

    /** Акцент тона в конкретной схеме; чистая функция — её и проверяет тест. */
    fun accent(scheme: ColorScheme): Color = when (this) {
        Info -> scheme.secondary
        Success -> scheme.primary
        Warning -> scheme.tertiary
        Error -> scheme.error
    }
}

@Composable
internal fun AzraelBannerTone.accent(): Color = accent(MaterialTheme.colorScheme)

/**
 * Полоса статуса поверх формы/списка — бывший `StatusBanner`, но тон выбирается
 * явно и по умолчанию стал `Info` вместо «ошибка или нет» флагом из двух булей.
 */
@Composable
fun AzraelBanner(
    text: String,
    modifier: Modifier = Modifier,
    tone: AzraelBannerTone = AzraelBannerTone.Info
) {
    val accent = tone.accent()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AzraelCornerSm))
            .background(accent.copy(alpha = 0.10f))
            .border(AzraelSpace.stroke, accent.copy(alpha = 0.28f), RoundedCornerShape(AzraelCornerSm))
            .padding(horizontal = AzraelSpace.md, vertical = AzraelSpace.md)
    ) {
        Icon(
            tone.icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(AzraelSpace.sm))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
        )
    }
}
