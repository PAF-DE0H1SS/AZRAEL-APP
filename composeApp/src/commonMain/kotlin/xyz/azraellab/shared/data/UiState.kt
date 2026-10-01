package xyz.azraellab.shared.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Типизированное состояние данных для экрана: загрузка / готово / ошибка.
 * Значение [Error.message] - сырой текст (код или сообщение исключения);
 * перевод для показа выполняет слой UI через errText.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Ready<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>

    fun <R> map(transform: (T) -> R): UiState<R> = when (this) {
        is Loading -> Loading
        is Ready -> Ready(transform(data))
        is Error -> this
    }

    fun getOrNull(): T? = (this as? Ready)?.data
}

suspend fun <T> runStateInt(block: suspend () -> T): UiState<T> = try {
    UiState.Ready(block())
} catch (e: Exception) {
    UiState.Error(e.message ?: "error")
}

suspend fun <T> runStateIO(block: () -> T): UiState<T> = withContext(Dispatchers.IO) {
    runStateInt { block() }
}