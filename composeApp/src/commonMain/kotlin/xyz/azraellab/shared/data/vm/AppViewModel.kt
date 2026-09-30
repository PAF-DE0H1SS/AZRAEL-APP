package xyz.azraellab.shared.data.vm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.azraellab.shared.data.UiState

/**
 * KMP-совместимый ViewModel без зависимости от androidx.lifecycle.
 *
 * Собственный [CoroutineScope] на [Dispatchers.Default] (repos сами переключаются на
 * [Dispatchers.IO]); закрывается методом [close] (вызывает composable-фабрика
 * `rememberViewModel` при уходе с экрана). Внешний scope можно подсунуть в тестах,
 * чтобы контролировать жизненный цикл.
 *
 * Тип состояния экрана — [UiState] (Loading/Ready/Error), загрузки выполняется через
 * [loadInto], чтобы путь load -> data/error был единым и типизированным.
 */
open class AppViewModel(private val externalScope: CoroutineScope? = null) {
    private val job = SupervisorJob()
    private val vmScope: CoroutineScope = externalScope ?: CoroutineScope(Dispatchers.Default + job)

    protected fun launch(block: suspend CoroutineScope.() -> Unit): Job = vmScope.launch(block = block)

    /**
     * Единый типизированный цикл загрузки: сначала [UiState.Loading], затем результат
     * loader'а (репозиторий уже вернул Ready/Error через runStateIO).
     */
    protected fun <T> loadInto(stream: MutableStateFlow<UiState<T>>, loader: suspend () -> UiState<T>) {
        launch {
            stream.value = UiState.Loading
            stream.value = loader()
        }
    }

    /** Удобный мост UiState<MutableList> -> StateFlow для наблюдаемых состояний. */
    protected fun <T> MutableStateFlow<UiState<List<T>>>.mutableList(): StateFlow<UiState<List<T>>> = asStateFlow()

    fun close() {
        job.cancel()
    }
}