package xyz.azraellab.shared.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.azraellab.shared.ui.theme.AzraelSpace

/**
 * Одна полоса «кости» - основа всех скелетонов.
 *
 * Цвет берётся от `onSurface` с небольшой альфой, а не от `surfaceContainer*`: токены
 * поверхностей в тёмной схеме почти совпадают с фоном (`#171717` на `#0A0A0A`), и скелетон
 * на них просто исчезал. `onSurface` даёт одинаково читаемую полосу в обеих схемах -
 * светлее фона в тёмной и темнее в светлой.
 *
 * @param animate пульсировать ли. Выключается в тестах и при `prefers-reduced-motion`
 *   не различается: анимация тут чисто косметическая, состояние загрузки не меняется.
 */
@Composable
fun AzraelSkeletonBlock(
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
    corner: Dp = 6.dp,
    baseAlpha: Float = 0.09f,
    animate: Boolean = true
) {
    val alpha = if (animate) {
        val transition = rememberInfiniteTransition(label = "azrael-skeleton")
        transition.animateFloat(
            initialValue = baseAlpha,
            targetValue = baseAlpha * 2.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900),
                repeatMode = RepeatMode.Reverse
            ),
            label = "azrael-skeleton-alpha"
        ).value
    } else {
        baseAlpha
    }
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
    )
}

/**
 * Скелетон строки списка: круглая плашка слева (аватар/иконка) и две полосы справа.
 *
 * Геометрия повторяет реальную строку списка чатов, поэтому приход данных не сдвигает
 * соседние элементы - это и есть смысл скелетона в отличие от центрированного спиннера.
 */
@Composable
fun AzraelSkeletonRow(
    modifier: Modifier = Modifier,
    avatarSize: Dp = 40.dp,
    animate: Boolean = true
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AzraelSpace.md)
    ) {
        Box(
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.onSurface.copy(
                        alpha = if (animate) 0.11f else 0.09f
                    )
                )
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AzraelSpace.sm)
        ) {
            AzraelSkeletonBlock(
                modifier = Modifier.fillMaxWidth(0.55f),
                height = 13.dp,
                animate = animate
            )
            AzraelSkeletonBlock(
                modifier = Modifier.fillMaxWidth(0.85f),
                height = 11.dp,
                animate = animate
            )
        }
    }
}

/**
 * Скелетон списка из [rows] строк. Заменяет [AzraelLoadingState] там, где строки известной
 * формы: пользователь видит структуру будущего списка, а не крутилки в середине экрана.
 *
 * [label] обязателен: сами полосы декоративны и для TalkBack пусты, поэтому без текста
 * скринридер во время загрузки сообщал бы просто «ничего» - хуже, чем старый спиннер
 * с подписью.
 */
@Composable
fun AzraelSkeletonList(
    label: String,
    rows: Int = 4,
    modifier: Modifier = Modifier,
    animate: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = label }
            .testTag(AzraelSkeletonListTag),
        verticalArrangement = Arrangement.spacedBy(AzraelSpace.lg)
    ) {
        repeat(rows.coerceAtLeast(1)) {
            AzraelSkeletonRow(animate = animate)
        }
    }
}

/** Тег для UI-тестов: ждать скелетон дешевле, чем таймаутиться на текст ошибки. */
const val AzraelSkeletonListTag: String = "azrael:skeletonList"
