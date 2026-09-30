package xyz.azraellab.shared.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Хост содержимого по текущему пункту стека.
 *
 * Экраны пока живут в `App.kt` (это P4), поэтому [NavHost] принимает лямбду
 * `Destination -> Unit`, а не знает про `AdminView`/`ChatsView`. Когда экраны
 * переедут в `screens/`, лямбда заменится на `when` по [Destination], а точки
 * входа у [Navigator] и [AzraelNavScaffold] останутся прежними.
 */
@Composable
fun NavHost(
    state: NavState,
    modifier: Modifier = Modifier,
    content: @Composable (Destination) -> Unit
) {
    Box(modifier) { content(state.current) }
}
