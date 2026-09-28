package xyz.azraellab.shared.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Палитра приложения = палитра сайта azrael-lab.xyz (app/globals.css):
// плоский почти-чёрный фон, светлый текст, единственный зелёный акцент.
// Раньше здесь был фиолетово-циановый glassmorphism, из-за чего приложение
// выглядело иначе, чем сайт, при том же бренде.
val AzraelPrimary = Color(0xFF4ADE80)      // --accent сайта
val AzraelOnPrimary = Color(0xFF07170C)
val AzraelSecondary = Color(0xFFEDEDED)    // --foreground сайта
val AzraelDanger = Color(0xFFF43F5E)       // красный токен сайта (ошибки/удаление)
val AzraelBgDeep = Color(0xFF0A0A0A)       // --background сайта
val AzraelBgCard = Color(0x0DFFFFFF)       // панели: rgba(255,255,255,0.05)
val AzraelBorder = Color(0x1AFFFFFF)       // рамки: white/10
val AzraelTextDim = Color(0xD9EDEDED)      // вторичный текст: 85% foreground

private val AzraelScheme = darkColorScheme(
    primary = AzraelPrimary,
    onPrimary = AzraelOnPrimary,
    secondary = AzraelSecondary,
    onSecondary = AzraelOnPrimary,
    tertiary = AzraelDanger,
    background = AzraelBgDeep,
    onBackground = AzraelSecondary,
    surface = AzraelBgCard,
    onSurface = AzraelSecondary,
    surfaceVariant = Color(0x1AFFFFFF),
    onSurfaceVariant = AzraelTextDim,
    error = AzraelDanger,
    onError = AzraelOnPrimary,
    outline = AzraelBorder,
    outlineVariant = AzraelBorder
)

@Composable
fun AzraelTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AzraelScheme, content = content)
}

@Composable
fun GlassBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFF0C110C),
                    0.4f to AzraelBgDeep,
                    1f to Color(0xFF070707)
                )
            )
    ) {
        AmbientGlow(color = AzraelPrimary, size = 460.dp, alpha = 0.10f, Modifier.align(Alignment.TopStart))
        AmbientGlow(color = AzraelPrimary, size = 380.dp, alpha = 0.06f, Modifier.align(Alignment.TopEnd))
        AmbientGlow(color = AzraelSecondary, size = 420.dp, alpha = 0.03f, Modifier.align(Alignment.BottomEnd))
        // Звёзды и белые кометы как на сайте (/e2) — под контентом, мышь не ловит.
        StarfieldBackground(Modifier.matchParentSize())
        content()
    }
}

@Composable
private fun AmbientGlow(color: Color, size: Dp, alpha: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(size)
            .height(size)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                    center = Offset(size.value / 2f, size.value / 2f),
                    radius = size.value / 2f
                ),
                shape = CircleShape
            )
    )
}

// «Стеклянная» обёртка карточки: скруглённые углы, полупрозрачная заливка с градиентом
// и тонкая светлая рамка — glassmorphism в духе фирменного стиля.
fun Modifier.glass(
    corner: Dp = 22.dp,
    borderColor: Color = AzraelBorder,
    fill: Color = AzraelBgCard
): Modifier = this
    .clip(RoundedCornerShape(corner))
    .background(
        brush = Brush.linearGradient(
            colors = listOf(fill.copy(alpha = 0.86f), fill.copy(alpha = 0.62f)),
            start = Offset(0f, 0f),
            end = Offset(Size(0f, 400f).width, Size(0f, 400f).height)
        )
    )
    .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(corner))