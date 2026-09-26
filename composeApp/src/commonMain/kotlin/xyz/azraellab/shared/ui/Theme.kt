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

// Фирменные цвета проекта azrael-lab.xyz (violet / cyan / rose).
val AzraelViolet = Color(0xFF9D6BFF)
val AzraelCyan = Color(0xFF4DD8FF)
val AzraelRose = Color(0xFFFF5CA8)
val AzraelBgDeep = Color(0xFF07070D)
val AzraelBgCard = Color(0x14030308)

private val AzraelScheme = darkColorScheme(
    primary = AzraelViolet,
    secondary = AzraelCyan,
    tertiary = AzraelRose,
    background = AzraelBgDeep,
    surface = AzraelBgCard,
    surfaceVariant = Color(0x14030308),
    onPrimary = Color(0xFF10051E),
    onSecondary = Color(0xFF001418),
    onBackground = Color(0xFFECECF4),
    onSurface = Color(0xFFECECF4)
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
                    0f to Color(0xFF0D0A1C),
                    0.5f to AzraelBgDeep,
                    1f to Color(0xFF061218)
                )
            )
    ) {
        AmbientGlow(color = AzraelViolet, size = 420.dp, alpha = 0.16f, Modifier.align(Alignment.TopStart))
        AmbientGlow(color = AzraelCyan, size = 380.dp, alpha = 0.12f, Modifier.align(Alignment.TopEnd))
        AmbientGlow(color = AzraelRose, size = 460.dp, alpha = 0.10f, Modifier.align(Alignment.BottomEnd))
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
    borderColor: Color = Color.White.copy(alpha = 0.14f),
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