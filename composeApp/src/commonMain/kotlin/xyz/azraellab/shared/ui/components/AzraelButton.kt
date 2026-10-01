package xyz.azraellab.shared.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.azraellab.shared.ui.theme.AzraelCornerMd
import xyz.azraellab.shared.ui.theme.AzraelSpace

/**
 * Тон кнопки. Раньше каждая кнопка в `App.kt` собирала `ButtonDefaults.buttonColors`
 * и `border` руками - четыре повторяющихся набора, которые расходились между экранами.
 */
enum class AzraelButtonTone {
    /** Акцентная заливка 30% - основное действие списка (бывший `AccentButton`). */
    Accent,

    /** Стеклянная белая кнопка во всю ширину с состоянием работы (бывший `PrimaryAuthButton`). */
    Glass,

    /** Прозрачная с рамкой - второстепенное действие. */
    Ghost,

    /** Деструктивное действие. */
    Danger
}

/**
 * Цвета кнопки вынесены в отдельные чистые функции, а не собраны прямо в composable.
 *
 * Так контраст можно проверить тестом: `colors()` в Material3 - `@Composable`,
 * вернуть его из обычной функции нельзя, а проверять надо именно то, что рисуется,
 * иначе тест повторит ошибку, а не поймает её.
 */
internal fun buttonContainerColor(tone: AzraelButtonTone, scheme: ColorScheme, busy: Boolean): Color =
    when (tone) {
        AzraelButtonTone.Accent -> scheme.primary.copy(alpha = 0.30f)
        AzraelButtonTone.Glass -> scheme.secondary.copy(alpha = if (busy) 0.04f else 0.07f)
        AzraelButtonTone.Ghost -> scheme.secondary.copy(alpha = 0.04f)
        AzraelButtonTone.Danger -> scheme.errorContainer
    }

/**
 * Подпись кнопки - текст, поэтому 4.5:1 (WCAG 1.4.3), не 3:1.
 *
 * У `Glass` состояние работы гасит и заливку, и подпись, но подпись там остаётся
 * текстом (спиннер рядом, а `busyLabel` сообщает, что происходит), и «декоративное
 * приглушение» освобождения от 1.4.3 не даёт. `0.45` давало 4.04/2.85 - мимо;
 * `0.65` даёт 7.16/5.23 с запасом.
 */
internal fun buttonContentColor(tone: AzraelButtonTone, scheme: ColorScheme, busy: Boolean): Color =
    when (tone) {
        AzraelButtonTone.Accent -> scheme.secondary
        AzraelButtonTone.Glass -> scheme.secondary.copy(alpha = if (busy) 0.65f else 0.88f)
        AzraelButtonTone.Ghost -> scheme.secondary.copy(alpha = 0.75f)
        AzraelButtonTone.Danger -> scheme.onErrorContainer
    }

/**
 * Рамка кнопки - смысловая граница: по ней кнопка опознаётся как кнопка, поэтому
 * 3:1 к фону страницы (WCAG 1.4.11). Раньше `Accent` вовсе не имел рамки (заливка 30%
 * давала 1.94/1.51 против страницы), а `Glass`/`Ghost` рисовали `secondary@10%` (1.23).
 */
internal fun buttonBorderColor(tone: AzraelButtonTone, scheme: ColorScheme): Color =
    when (tone) {
        AzraelButtonTone.Accent -> scheme.primary
        AzraelButtonTone.Glass, AzraelButtonTone.Ghost -> scheme.outline
        AzraelButtonTone.Danger -> scheme.error
    }

/**
 * Единственная кнопка приложения. Значения совпадают с теми, что были размазаны
 * по экранам, поэтому подстановка компонента не меняет вид.
 *
 * @param busy включает спиннер и подменяет подпись на [busyLabel]; кнопка при этом не нажимается.
 */
@Composable
fun AzraelButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: AzraelButtonTone = AzraelButtonTone.Accent,
    icon: ImageVector? = null,
    busy: Boolean = false,
    busyLabel: String = label,
    enabled: Boolean = true,
    fullWidth: Boolean = false,
    height: Dp = AzraelSpace.controlHeight,
    labelStyle: TextStyle = MaterialTheme.typography.titleMedium
) {
    val scheme = MaterialTheme.colorScheme
    val container = buttonContainerColor(tone, scheme, busy)
    val content = buttonContentColor(tone, scheme, busy)
    // Рамка есть у всех тонов: заливка Accent 30% сама по себе границы не даёт
    // (1.94/1.51 против страницы), а поднимать её до 3:1 пришлось бы по-разному в
    // тёмной (0.45) и светлой (0.73) темах - темы стали бы выглядеть разными.
    val border = BorderStroke(AzraelSpace.stroke, buttonBorderColor(tone, scheme))

    val indicator = height * 0.32f

    Button(
        onClick = onClick,
        enabled = enabled && !busy,
        shape = RoundedCornerShape(AzraelCornerMd),
        border = border,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.02f),
            disabledContentColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
        ),
        contentPadding = PaddingValues(horizontal = AzraelSpace.lg, vertical = AzraelSpace.sm),
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .defaultMinSize(minHeight = height)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(indicator.coerceAtLeast(12.dp)),
                    color = content,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(AzraelSpace.sm))
            } else if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(height * 0.42f))
                Spacer(Modifier.width(AzraelSpace.sm))
            }
            Text(
                text = if (busy) busyLabel else label,
                style = labelStyle,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 0.dp)
            )
        }
    }
}
