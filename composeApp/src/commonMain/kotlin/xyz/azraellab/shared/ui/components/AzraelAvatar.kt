package xyz.azraellab.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import xyz.azraellab.shared.ui.theme.AzraelSpace

/**
 * Круглый аватар: картинка, если она уже загружена, иначе первая буква имени.
 * Бывший `AvatarCircle` умел только букву, хотя профиль умеет показывать фото -
 * поэтому здесь оставлен слот [painter], а сама загрузка остаётся у вызывающего кода
 * (на desktop это Skia-декодирование, общее для всех платформ тянуть нельзя).
 */
@Composable
fun AzraelAvatar(
    name: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 40.dp,
    painter: Painter? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.35f))
            .border(AzraelSpace.stroke, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (painter != null) {
            Image(
                painter = painter,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(CircleShape)
            )
        } else {
            Text(
                name.trim().take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
