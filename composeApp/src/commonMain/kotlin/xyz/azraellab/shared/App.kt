package xyz.azraellab.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import xyz.azraellab.shared.ui.AppThemeRoot
import xyz.azraellab.shared.ui.GlassBackground

/**
 * Точка входа приложения: тема + стеклянный фон + загрузка языка.
 * Канальный ключ, вход, главный экран и все разделы живут в ui/screens/.
 */
@Composable
fun App(nativeGreeting: () -> String) {
    AppThemeRoot {
        GlassBackground {
            // Язык с устройства — до первой сети, чтобы входной экран тоже был переведён.
            LaunchedEffect(Unit) { I18n.load() }
            AppRoot(nativeGreeting)
        }
    }
}