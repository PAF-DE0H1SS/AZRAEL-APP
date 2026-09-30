package xyz.azraellab.shared.data.vm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.azraellab.shared.core.api.AppClient
import xyz.azraellab.shared.data.UiState
import xyz.azraellab.shared.data.Repos
import xyz.azraellab.shared.data.model.ShortLinkDto

/**
 * Состояние вкладки сокращателя: список ссылок живёт в [rows] (UiState), все операции
 * с сервером — suspend-функции, возвращающие UiState; UI сам показывает статус.
 */
class ShortenerViewModel(
    private val client: AppClient,
    externalScope: CoroutineScope? = null
) : AppViewModel(externalScope) {

    private val _rows = MutableStateFlow<UiState<List<ShortLinkDto>>>(UiState.Loading)
    val rows: StateFlow<UiState<List<ShortLinkDto>>> = _rows.asStateFlow()

    fun refresh() = loadInto(_rows) { Repos.shortLinks(client) }

    suspend fun create(url: String, custom: String): UiState<String> =
        Repos.shortenerCreate(client, url, custom.trim().ifBlank { null })

    suspend fun delete(code: String): UiState<Unit> = Repos.shortenerDelete(client, code)

    suspend fun qr(code: String): UiState<String> = Repos.shortenerQr(client, code)
}