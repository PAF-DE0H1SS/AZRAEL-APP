package xyz.azraellab.shared.ui.vm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow
import xyz.azraellab.shared.data.UiState
import xyz.azraellab.shared.data.vm.AppViewModel

/**
 * Создаёт ViewModel один раз на композицию и закрывает при уходе с экрана
 * (DisposableEffect). Живёт дольше рекомпозиций, но не переживает смену графа.
 */
@Composable
fun <VM : AppViewModel> rememberViewModel(factory: () -> VM): VM {
    val vm = remember { factory() }
    DisposableEffect(vm) {
        onDispose { vm.close() }
    }
    return vm
}

/** stateFlow<UiState<T>> из ViewModel → локальный State<UiState<T>> для рендера. */
@Composable
fun <T> StateFlow<UiState<T>>.collectAsUiState(): State<UiState<T>> = collectAsState()